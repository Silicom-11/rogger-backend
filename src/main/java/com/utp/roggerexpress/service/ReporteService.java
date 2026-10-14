package com.utp.roggerexpress.service;

import com.utp.roggerexpress.dto.ResumenResponse;
import com.utp.roggerexpress.model.Agencia;
import com.utp.roggerexpress.model.EstadoEnvio;
import com.utp.roggerexpress.repository.AgenciaRepository;
import com.utp.roggerexpress.security.AlcanceConsulta;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Indicadores de operacion que alimentan el tablero de control del sistema.
 * Los calculos se resuelven con agregaciones de MongoDB.
 *
 * Todo indicador se calcula dentro del alcance de quien consulta. Los importes
 * de la red solo se calculan cuando el solicitante esta autorizado a verlos; el
 * arqueo del propio usuario, en cambio, es su herramienta de trabajo.
 */
@Service
public class ReporteService {

    private static final String COLECCION = "envios";

    /** Dias tras los cuales una encomienda sin recoger merece atencion. */
    private static final int DIAS_DE_ESPERA = 3;

    private final MongoTemplate mongoTemplate;
    private final AgenciaRepository agenciaRepository;

    public ReporteService(MongoTemplate mongoTemplate, AgenciaRepository agenciaRepository) {
        this.mongoTemplate = mongoTemplate;
        this.agenciaRepository = agenciaRepository;
    }

    public ResumenResponse resumen(AlcanceConsulta alcance, boolean incluirImportes, String username) {

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Map<String, Long> porEstado = conteoPorEstado(alcance);

        long total = mongoTemplate.count(new Query(criterios(alcance, null)), COLECCION);

        long entregadosHoy = mongoTemplate.count(
                new Query(criterios(alcance,
                        Criteria.where("estadoActual").is(EstadoEnvio.ENTREGADO.name())
                                .and("fechaRegistro").gte(inicioDia))), COLECCION);

        // Encomiendas que llevan varios dias ocupando el almacen: es lo primero
        // que necesita ver quien administra la agencia de destino.
        long demoradas = mongoTemplate.count(
                new Query(criterios(alcance,
                        Criteria.where("estadoActual").is(EstadoEnvio.LISTO_PARA_RECOJO.name())
                                .and("fechaListoParaRecojo")
                                .lt(LocalDateTime.now().minusDays(DIAS_DE_ESPERA)))), COLECCION);

        // Arqueo del turno: lo que este usuario admitio y cobro hoy.
        Long misAdmisiones = null;
        BigDecimal miCaja = null;
        if (username != null && !username.isBlank()) {
            Criteria mias = Criteria.where("registradoPor").is(username)
                    .and("fechaRegistro").gte(inicioDia)
                    .and("estadoActual").ne(EstadoEnvio.ANULADO.name());
            misAdmisiones = mongoTemplate.count(new Query(mias), COLECCION);
            miCaja = sumaIngresos(mias);
        }

        // Los importes de la red son informacion economica: se omiten por
        // completo cuando quien consulta no esta autorizado.
        BigDecimal ingresosHoy = null;
        BigDecimal ingresosMes = null;
        BigDecimal ticket = null;

        if (incluirImportes) {
            ingresosHoy = sumaIngresos(criterios(alcance, Criteria.where("fechaRegistro").gte(inicioDia)));
            ingresosMes = sumaIngresos(criterios(alcance, Criteria.where("fechaRegistro").gte(inicioMes)));
            ticket = total == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : sumaIngresos(criterios(alcance, null))
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        return new ResumenResponse(
                total,
                porEstado.getOrDefault(EstadoEnvio.EN_TRANSITO.name(), 0L),
                porEstado.getOrDefault(EstadoEnvio.LISTO_PARA_RECOJO.name(), 0L),
                entregadosHoy,
                demoradas,
                misAdmisiones,
                miCaja,
                ingresosHoy,
                ingresosMes,
                ticket,
                porEstado,
                conteoPorAgencia(alcance));
    }

    /** Cantidad de encomiendas agrupadas por estado. */
    public Map<String, Long> conteoPorEstado(AlcanceConsulta alcance) {

        List<AggregationOperation> etapas = new ArrayList<>();
        Criteria porAgencia = EnvioService.criterioAlcance(alcance);
        if (porAgencia != null) {
            etapas.add(Aggregation.match(porAgencia));
        }
        etapas.add(Aggregation.group("estadoActual").count().as("cantidad"));
        etapas.add(Aggregation.sort(org.springframework.data.domain.Sort.by("_id")));

        AggregationResults<Document> resultado = mongoTemplate.aggregate(
                Aggregation.newAggregation(etapas), COLECCION, Document.class);

        Map<String, Long> conteo = new LinkedHashMap<>();
        for (EstadoEnvio estado : EstadoEnvio.values()) {
            conteo.put(estado.name(), 0L);
        }
        for (Document doc : resultado.getMappedResults()) {
            Object id = doc.get("_id");
            if (id != null) {
                conteo.put(id.toString(), ((Number) doc.get("cantidad")).longValue());
            }
        }
        return conteo;
    }

    /** Cantidad de encomiendas por agencia de destino. */
    public List<ResumenResponse.ConteoAgencia> conteoPorAgencia(AlcanceConsulta alcance) {

        Map<String, String> nombres = agenciaRepository.findAll().stream()
                .collect(Collectors.toMap(Agencia::getCodigo, Agencia::getNombre, (a, b) -> a));

        List<AggregationOperation> etapas = new ArrayList<>();
        Criteria porAgencia = EnvioService.criterioAlcance(alcance);
        if (porAgencia != null) {
            etapas.add(Aggregation.match(porAgencia));
        }
        etapas.add(Aggregation.group("agenciaDestinoId").count().as("cantidad"));
        etapas.add(Aggregation.sort(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "cantidad")));

        AggregationResults<Document> resultado = mongoTemplate.aggregate(
                Aggregation.newAggregation(etapas), COLECCION, Document.class);

        return resultado.getMappedResults().stream()
                .filter(doc -> doc.get("_id") != null)
                .map(doc -> new ResumenResponse.ConteoAgencia(
                        doc.get("_id").toString(),
                        nombres.getOrDefault(doc.get("_id").toString(), doc.get("_id").toString()),
                        ((Number) doc.get("cantidad")).longValue()))
                .toList();
    }

    /** Suma de las tarifas de las encomiendas que cumplen el criterio dado. */
    private BigDecimal sumaIngresos(Criteria filtro) {

        // valueOf(...) referencia el campo ($tarifa.total); $toDecimal admite
        // importes guardados como Decimal128 o como texto (documentos creados
        // por versiones anteriores del sistema).
        var importe = ConvertOperators.valueOf("tarifa.total").convertToDecimal();

        AggregationResults<Document> resultado = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(filtro == null ? new Criteria() : filtro),
                        Aggregation.group().sum(importe).as("ingresos")),
                COLECCION, Document.class);

        Document doc = resultado.getUniqueMappedResult();
        if (doc == null || doc.get("ingresos") == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(doc.get("ingresos").toString()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Combina el aislamiento por agencia con el filtro propio del indicador. */
    private Criteria criterios(AlcanceConsulta alcance, Criteria adicional) {

        List<Criteria> partes = new ArrayList<>();
        Criteria porAgencia = EnvioService.criterioAlcance(alcance);

        if (porAgencia != null) {
            partes.add(porAgencia);
        }
        if (adicional != null) {
            partes.add(adicional);
        }
        return partes.isEmpty() ? new Criteria() : new Criteria().andOperator(partes);
    }
}
