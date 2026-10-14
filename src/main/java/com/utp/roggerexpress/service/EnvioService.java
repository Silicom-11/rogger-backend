package com.utp.roggerexpress.service;

import com.utp.roggerexpress.dto.ResultadoLoteResponse;
import com.utp.roggerexpress.exception.DatosInvalidosException;
import com.utp.roggerexpress.exception.EntregaNoPermitidaException;
import com.utp.roggerexpress.exception.RecursoNoEncontradoException;
import com.utp.roggerexpress.exception.RegistroDuplicadoException;
import com.utp.roggerexpress.exception.TransicionInvalidaException;
import com.utp.roggerexpress.model.*;
import com.utp.roggerexpress.repository.EnvioRepository;
import com.utp.roggerexpress.security.AlcanceConsulta;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logica de negocio de la gestion de encomiendas: registro, busqueda,
 * seguimiento, cambios de estado, anulacion y entrega verificada.
 *
 * Toda consulta del personal se resuelve dentro de un {@link AlcanceConsulta}:
 * la administracion ve toda la red y cada agencia solo las encomiendas que
 * salen de ella o llegan a ella.
 */
@Service
public class EnvioService {

    /** Alfabeto sin caracteres que se confunden al dictarlos (0/O, 1/I). */
    private static final String ALFABETO_VERIFICACION = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final EnvioRepository envioRepository;
    private final TarifaService tarifaService;
    private final AgenciaService agenciaService;
    private final NotificacionService notificacionService;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;

    // Inyeccion de dependencias por constructor
    public EnvioService(EnvioRepository envioRepository,
                        TarifaService tarifaService,
                        AgenciaService agenciaService,
                        NotificacionService notificacionService,
                        MongoTemplate mongoTemplate,
                        PasswordEncoder passwordEncoder) {
        this.envioRepository = envioRepository;
        this.tarifaService = tarifaService;
        this.agenciaService = agenciaService;
        this.notificacionService = notificacionService;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Resultado de una admision: la encomienda y los dos datos que solo se
     * muestran una vez, al imprimir el comprobante.
     */
    public record Admision(Envio envio, String codigoVerificacion, String claveRecojo) {
    }

    /** Transiciones permitidas del ciclo de vida de la encomienda. */
    private static final Map<EstadoEnvio, Set<EstadoEnvio>> TRANSICIONES = new EnumMap<>(EstadoEnvio.class);

    /** Estados desde los que todavia es posible anular la admision. */
    private static final Set<EstadoEnvio> ANULABLES =
            EnumSet.of(EstadoEnvio.REGISTRADO, EstadoEnvio.EN_ALMACEN_ORIGEN);

    /** Ventana en la que dos admisiones identicas se consideran un duplicado. */
    private static final int MINUTOS_ANTIDUPLICADO = 2;

    static {
        TRANSICIONES.put(EstadoEnvio.REGISTRADO,
                Set.of(EstadoEnvio.EN_ALMACEN_ORIGEN, EstadoEnvio.INCIDENCIA));
        TRANSICIONES.put(EstadoEnvio.EN_ALMACEN_ORIGEN,
                Set.of(EstadoEnvio.EN_TRANSITO, EstadoEnvio.INCIDENCIA));
        TRANSICIONES.put(EstadoEnvio.EN_TRANSITO,
                Set.of(EstadoEnvio.EN_ALMACEN_DESTINO, EstadoEnvio.INCIDENCIA));
        TRANSICIONES.put(EstadoEnvio.EN_ALMACEN_DESTINO,
                Set.of(EstadoEnvio.LISTO_PARA_RECOJO, EstadoEnvio.INCIDENCIA));
        TRANSICIONES.put(EstadoEnvio.LISTO_PARA_RECOJO,
                Set.of(EstadoEnvio.ENTREGADO, EstadoEnvio.DEVUELTO, EstadoEnvio.INCIDENCIA));
        TRANSICIONES.put(EstadoEnvio.INCIDENCIA,
                Set.of(EstadoEnvio.EN_ALMACEN_ORIGEN, EstadoEnvio.EN_TRANSITO,
                        EstadoEnvio.EN_ALMACEN_DESTINO, EstadoEnvio.DEVUELTO));
        TRANSICIONES.put(EstadoEnvio.ENTREGADO, Set.of());
        TRANSICIONES.put(EstadoEnvio.DEVUELTO, Set.of());
        TRANSICIONES.put(EstadoEnvio.ANULADO, Set.of());
    }

    /** Estados a los que puede pasar la encomienda desde su estado actual. */
    public static List<EstadoEnvio> siguientesEstados(EstadoEnvio actual) {
        return new ArrayList<>(TRANSICIONES.getOrDefault(actual, Set.of()));
    }

    /**
     * Registra una nueva encomienda: calcula la tarifa, genera el codigo de
     * rastreo, el codigo de verificacion y la clave de seguridad del recojo.
     *
     * @param claveSolicitada clave dictada por el cliente; si viene vacia el
     *                        sistema genera una.
     */
    public Admision registrar(Envio envio, String claveSolicitada, String usuario) {

        verificarContacto(envio);
        verificarAutorizados(envio);
        verificarDuplicado(envio);

        Agencia origen = agenciaService.buscarPorCodigo(envio.getAgenciaOrigenId());
        Agencia destino = agenciaService.buscarPorCodigo(envio.getAgenciaDestinoId());

        String ruta = origen.getCiudad() + "-" + destino.getCiudad();

        String verificacion = generarCodigoVerificacion();
        String clave = (claveSolicitada == null || claveSolicitada.isBlank())
                ? generarClaveRecojo()
                : claveSolicitada.trim();

        envio.setTarifa(tarifaService.calcular(envio.getCarga(), ruta));
        envio.setCodigoRastreo(generarCodigo());
        envio.setCodigoVerificacion(verificacion);
        envio.setClaveRecojoHash(passwordEncoder.encode(clave));
        envio.setEstadoActual(EstadoEnvio.REGISTRADO);
        envio.setFechaRegistro(LocalDateTime.now());
        envio.setRegistradoPor(usuario);
        envio.agregarEvento(EstadoEnvio.REGISTRADO, origen.getCodigo(), usuario,
                "Encomienda admitida en " + origen.getNombre());

        return new Admision(envioRepository.save(envio), verificacion, clave);
    }

    /**
     * El aviso de llegada es parte del servicio: sin un numero de contacto del
     * destinatario la encomienda quedaria en el almacen sin que nadie sepa que
     * llego, asi que la admision no se acepta sin ese dato.
     */
    private void verificarContacto(Envio envio) {

        String telefono = envio.getDestinatario() == null ? null : envio.getDestinatario().getTelefono();

        if (telefono == null || telefono.isBlank()) {
            throw new DatosInvalidosException(
                    "Indique el telefono del destinatario: es el numero al que el sistema avisara "
                            + "cuando la encomienda llegue a la agencia de destino");
        }
    }

    /** Una persona autorizada no puede repetir el documento del destinatario. */
    private void verificarAutorizados(Envio envio) {

        if (envio.getAutorizados() == null || envio.getAutorizados().isEmpty()) {
            return;
        }
        String dniDestinatario = envio.getDestinatario().getDni();

        for (Persona autorizado : envio.getAutorizados()) {
            if (autorizado.getDni() != null && autorizado.getDni().equals(dniDestinatario)) {
                throw new DatosInvalidosException(
                        "El destinatario ya puede recoger su encomienda: no hace falta autorizarlo de nuevo");
            }
        }
    }

    /**
     * Evita el doble registro por un segundo clic o un reintento de red:
     * si en los ultimos minutos se admitio una encomienda con el mismo
     * remitente, destinatario, peso y ruta, se rechaza con 409.
     */
    private void verificarDuplicado(Envio envio) {

        Query query = new Query(new Criteria().andOperator(
                Criteria.where("remitente.dni").is(envio.getRemitente().getDni()),
                Criteria.where("destinatario.dni").is(envio.getDestinatario().getDni()),
                Criteria.where("carga.pesoKg").is(envio.getCarga().getPesoKg()),
                Criteria.where("agenciaOrigenId").is(envio.getAgenciaOrigenId()),
                Criteria.where("agenciaDestinoId").is(envio.getAgenciaDestinoId()),
                Criteria.where("estadoActual").ne(EstadoEnvio.ANULADO),
                Criteria.where("fechaRegistro").gte(LocalDateTime.now().minusMinutes(MINUTOS_ANTIDUPLICADO))));

        Envio previo = mongoTemplate.findOne(query, Envio.class);

        if (previo != null) {
            throw new RegistroDuplicadoException(previo.getCodigoRastreo(), MINUTOS_ANTIDUPLICADO);
        }
    }

    /** Busqueda por codigo sin restriccion, para uso interno del sistema. */
    public Envio buscarPorCodigo(String codigoRastreo) {
        return envioRepository.findByCodigoRastreo(codigoRastreo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una encomienda con el codigo " + codigoRastreo));
    }

    /**
     * Consulta publica del cliente: exige el codigo de rastreo y el codigo de
     * verificacion impreso en el comprobante.
     *
     * El codigo de rastreo es correlativo; sin este segundo dato cualquiera
     * podria recorrer los numeros y ver las encomiendas de los demas. Si alguno
     * no coincide se responde igual que si la encomienda no existiera.
     */
    public Envio rastrear(String codigoRastreo, String codigoVerificacion) {

        String codigo = codigoRastreo == null ? "" : codigoRastreo.trim().toUpperCase();
        String verificacion = codigoVerificacion == null ? "" : codigoVerificacion.trim().toUpperCase();

        return envioRepository.findByCodigoRastreo(codigo)
                .filter(e -> e.getCodigoVerificacion() != null
                        && e.getCodigoVerificacion().equalsIgnoreCase(verificacion))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No encontramos una encomienda con esos datos. Verifique el número y el código "
                                + "de su comprobante."));
    }

    /**
     * Busqueda por codigo para el personal: si la encomienda no corresponde a
     * su agencia se responde igual que si no existiera, para no revelar la
     * operacion de otras sedes.
     */
    public Envio buscarVisible(String codigoRastreo, AlcanceConsulta alcance) {
        Envio envio = buscarPorCodigo(codigoRastreo);
        verificarAlcance(envio, alcance);
        return envio;
    }

    /**
     * Busqueda paginada con filtros combinables: estado, agencia, rango de
     * fechas y texto libre (codigo de rastreo, DNI o nombre del cliente).
     */
    public List<Envio> buscar(EstadoEnvio estado, String agenciaDestinoId, String agenciaOrigenId,
                              LocalDate desde, LocalDate hasta, String texto,
                              AlcanceConsulta alcance, int pagina, int tamanio) {

        Query query = new Query(construirCriterios(estado, agenciaDestinoId, agenciaOrigenId,
                        desde, hasta, texto, alcance))
                .with(Sort.by(Sort.Direction.DESC, "fechaRegistro"))
                .skip((long) pagina * tamanio)
                .limit(tamanio);

        return mongoTemplate.find(query, Envio.class);
    }

    public long contar(EstadoEnvio estado, String agenciaDestinoId, String agenciaOrigenId,
                       LocalDate desde, LocalDate hasta, String texto, AlcanceConsulta alcance) {
        return mongoTemplate.count(
                new Query(construirCriterios(estado, agenciaDestinoId, agenciaOrigenId,
                        desde, hasta, texto, alcance)),
                Envio.class);
    }

    /**
     * Encomiendas que un documento puede retirar en este momento.
     *
     * Es el orden real del mostrador: el cliente llega y presenta su DNI, no un
     * codigo. Devuelve tanto las suyas como aquellas en las que figura como
     * persona autorizada.
     */
    public List<Envio> buscarParaEntrega(String dni, AlcanceConsulta alcance) {

        if (dni == null || !dni.matches("\\d{8}")) {
            throw new DatosInvalidosException("Indique un DNI de 8 dígitos para buscar la encomienda");
        }

        List<Criteria> condiciones = new ArrayList<>();
        condiciones.add(Criteria.where("estadoActual").is(EstadoEnvio.LISTO_PARA_RECOJO));
        condiciones.add(new Criteria().orOperator(
                Criteria.where("destinatario.dni").is(dni),
                Criteria.where("autorizados.dni").is(dni)));

        Criteria porAgencia = criterioAlcance(alcance);
        if (porAgencia != null) {
            condiciones.add(porAgencia);
        }

        Query query = new Query(new Criteria().andOperator(condiciones))
                .with(Sort.by(Sort.Direction.ASC, "fechaListoParaRecojo"));

        return mongoTemplate.find(query, Envio.class);
    }

    private Criteria construirCriterios(EstadoEnvio estado, String agenciaDestinoId, String agenciaOrigenId,
                                        LocalDate desde, LocalDate hasta, String texto,
                                        AlcanceConsulta alcance) {

        List<Criteria> condiciones = new ArrayList<>();

        // Aislamiento por agencia: se aplica antes que cualquier filtro que
        // llegue de la peticion, de modo que no pueda esquivarse desde el
        // cliente cambiando los parametros de busqueda.
        Criteria porAgencia = criterioAlcance(alcance);
        if (porAgencia != null) {
            condiciones.add(porAgencia);
        }
        if (estado != null) {
            condiciones.add(Criteria.where("estadoActual").is(estado));
        }
        if (agenciaDestinoId != null && !agenciaDestinoId.isBlank()) {
            condiciones.add(Criteria.where("agenciaDestinoId").is(agenciaDestinoId));
        }
        if (agenciaOrigenId != null && !agenciaOrigenId.isBlank()) {
            condiciones.add(Criteria.where("agenciaOrigenId").is(agenciaOrigenId));
        }
        if (desde != null) {
            condiciones.add(Criteria.where("fechaRegistro").gte(desde.atStartOfDay()));
        }
        if (hasta != null) {
            condiciones.add(Criteria.where("fechaRegistro").lte(hasta.atTime(23, 59, 59)));
        }
        if (texto != null && !texto.isBlank()) {
            String patron = java.util.regex.Pattern.quote(texto.trim());
            condiciones.add(new Criteria().orOperator(
                    Criteria.where("codigoRastreo").regex(patron, "i"),
                    Criteria.where("destinatario.dni").regex(patron, "i"),
                    Criteria.where("destinatario.nombres").regex(patron, "i"),
                    Criteria.where("remitente.dni").regex(patron, "i")));
        }

        return condiciones.isEmpty() ? new Criteria() : new Criteria().andOperator(condiciones);
    }

    /** Criterio que limita la consulta a la agencia del usuario, o null si ve toda la red. */
    public static Criteria criterioAlcance(AlcanceConsulta alcance) {
        if (alcance == null || alcance.todaLaRed() || alcance.agenciaId() == null) {
            return null;
        }
        return new Criteria().orOperator(
                Criteria.where("agenciaOrigenId").is(alcance.agenciaId()),
                Criteria.where("agenciaDestinoId").is(alcance.agenciaId()));
    }

    /** La encomienda de otra sede se trata como inexistente. */
    private void verificarAlcance(Envio envio, AlcanceConsulta alcance) {

        if (alcance == null || alcance.todaLaRed()) {
            return;
        }
        String propia = alcance.agenciaId();
        boolean visible = propia != null
                && (propia.equals(envio.getAgenciaOrigenId()) || propia.equals(envio.getAgenciaDestinoId()));

        if (!visible) {
            throw new RecursoNoEncontradoException(
                    "No existe una encomienda con el codigo " + envio.getCodigoRastreo() + " en su agencia");
        }
    }

    /** Cambia el estado de la encomienda validando que la transicion sea valida. */
    public Envio actualizarEstado(String codigoRastreo, EstadoEnvio nuevoEstado,
                                  String agenciaId, String usuario, String observacion) {
        return actualizarEstado(codigoRastreo, nuevoEstado, agenciaId, usuario, observacion,
                AlcanceConsulta.global());
    }

    public Envio actualizarEstado(String codigoRastreo, EstadoEnvio nuevoEstado,
                                  String agenciaId, String usuario, String observacion,
                                  AlcanceConsulta alcance) {

        Envio envio = buscarVisible(codigoRastreo, alcance);

        Set<EstadoEnvio> permitidos = TRANSICIONES.getOrDefault(envio.getEstadoActual(), Set.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new TransicionInvalidaException(envio.getEstadoActual(), nuevoEstado);
        }

        envio.setEstadoActual(nuevoEstado);
        envio.agregarEvento(nuevoEstado, agenciaId, usuario, observacion);

        if (nuevoEstado == EstadoEnvio.LISTO_PARA_RECOJO) {
            envio.setFechaListoParaRecojo(LocalDateTime.now());
        }

        Envio guardado = envioRepository.save(envio);

        if (nuevoEstado == EstadoEnvio.LISTO_PARA_RECOJO) {
            notificacionService.notificarListoParaRecojo(guardado);
        }

        return guardado;
    }

    /**
     * Aplica el mismo cambio de estado a varias encomiendas. Cada una se
     * procesa por separado: si alguna falla, las demas continuan y el
     * resultado detalla que ocurrio con cada codigo.
     */
    public ResultadoLoteResponse actualizarEstadoLote(List<String> codigos, EstadoEnvio estado,
                                                      String agenciaId, String usuario,
                                                      String observacion) {
        return actualizarEstadoLote(codigos, estado, agenciaId, usuario, observacion,
                AlcanceConsulta.global());
    }

    public ResultadoLoteResponse actualizarEstadoLote(List<String> codigos, EstadoEnvio estado,
                                                      String agenciaId, String usuario,
                                                      String observacion, AlcanceConsulta alcance) {

        List<String> actualizados = new ArrayList<>();
        List<ResultadoLoteResponse.Fallido> fallidos = new ArrayList<>();

        for (String codigo : codigos) {
            try {
                actualizarEstado(codigo, estado, agenciaId, usuario, observacion, alcance);
                actualizados.add(codigo);
            } catch (RuntimeException ex) {
                fallidos.add(new ResultadoLoteResponse.Fallido(codigo, ex.getMessage()));
            }
        }

        return new ResultadoLoteResponse(codigos.size(), actualizados.size(), actualizados, fallidos);
    }

    /**
     * Anula una admision registrada por error. Solo es posible mientras la
     * carga no haya salido de la agencia de origen, exige un motivo y deja
     * constancia en el historial: el registro nunca se elimina.
     */
    public Envio anular(String codigoRastreo, String motivo, String usuario) {
        return anular(codigoRastreo, motivo, usuario, AlcanceConsulta.global());
    }

    public Envio anular(String codigoRastreo, String motivo, String usuario, AlcanceConsulta alcance) {

        Envio envio = buscarVisible(codigoRastreo, alcance);

        if (!ANULABLES.contains(envio.getEstadoActual())) {
            throw new TransicionInvalidaException(
                    "Solo puede anularse una encomienda que aun no ha salido de la agencia de origen. "
                            + "Estado actual: " + envio.getEstadoActual());
        }

        envio.setEstadoActual(EstadoEnvio.ANULADO);
        envio.agregarEvento(EstadoEnvio.ANULADO, envio.getAgenciaOrigenId(), usuario,
                "Anulada: " + motivo);

        return envioRepository.save(envio);
    }

    /**
     * Genera una clave de seguridad nueva cuando el cliente perdio la anterior.
     * La clave previa deja de servir en el mismo momento.
     */
    public String regenerarClaveRecojo(String codigoRastreo, String usuario, AlcanceConsulta alcance) {

        Envio envio = buscarVisible(codigoRastreo, alcance);

        if (envio.getEstadoActual() == EstadoEnvio.ENTREGADO
                || envio.getEstadoActual() == EstadoEnvio.ANULADO) {
            throw new TransicionInvalidaException(
                    "La encomienda ya está cerrada: no tiene sentido cambiar su clave de seguridad");
        }

        String clave = generarClaveRecojo();
        envio.setClaveRecojoHash(passwordEncoder.encode(clave));
        envio.agregarEvento(envio.getEstadoActual(), envio.getAgenciaDestinoId(), usuario,
                "Se generó una nueva clave de seguridad a pedido del cliente");

        envioRepository.save(envio);
        return clave;
    }

    /** Registra la entrega final validando la identidad del receptor. */
    public Envio registrarEntrega(String codigoRastreo, Entrega entrega) {
        return registrarEntrega(codigoRastreo, entrega, null, AlcanceConsulta.global());
    }

    /**
     * Registra la entrega en el mostrador.
     *
     * Se comprueban dos cosas, en el mismo orden que en la agencia: que quien
     * se lleva la encomienda tenga derecho a hacerlo (es el destinatario o una
     * persona autorizada) y que digite la clave de seguridad. Si el cliente no
     * la recuerda, el personal puede entregar validando solo la identidad, pero
     * debe indicar el motivo y este queda en el historial.
     */
    public Envio registrarEntrega(String codigoRastreo, Entrega entrega,
                                  String claveDigitada, AlcanceConsulta alcance) {

        Envio envio = buscarVisible(codigoRastreo, alcance);

        if (envio.getEstadoActual() != EstadoEnvio.LISTO_PARA_RECOJO) {
            throw new EntregaNoPermitidaException(
                    "La encomienda debe estar en estado LISTO_PARA_RECOJO. Estado actual: "
                            + envio.getEstadoActual());
        }

        Persona quienRecoge = envio.quienRecoge(entrega.getReceptorDni());
        if (quienRecoge == null) {
            throw new EntregaNoPermitidaException(
                    "El documento presentado no corresponde al destinatario ni a una persona autorizada");
        }

        boolean esTitular = entrega.getReceptorDni().equals(envio.getDestinatario().getDni());
        entrega.setRelacion(esTitular ? "TITULAR" : "AUTORIZADO");

        verificarClaveRecojo(envio, entrega, claveDigitada);

        entrega.setFechaEntrega(LocalDateTime.now());
        envio.setEntrega(entrega);
        envio.setEstadoActual(EstadoEnvio.ENTREGADO);
        envio.agregarEvento(EstadoEnvio.ENTREGADO, envio.getAgenciaDestinoId(), entrega.getUsuario(),
                detalleEntrega(entrega));

        return envioRepository.save(envio);
    }

    private void verificarClaveRecojo(Envio envio, Entrega entrega, String claveDigitada) {

        boolean hayClave = claveDigitada != null && !claveDigitada.isBlank();

        if (hayClave) {
            if (envio.getClaveRecojoHash() == null
                    || !passwordEncoder.matches(claveDigitada.trim(), envio.getClaveRecojoHash())) {
                throw new EntregaNoPermitidaException(
                        "La clave de seguridad no coincide. Verifíquela con el cliente o genere una nueva");
            }
            entrega.setClaveVerificada(true);
            entrega.setMotivoSinClave(null);
            return;
        }

        // Sin clave solo se entrega dejando constancia del motivo: la
        // encomienda sale igual, pero la agencia queda respaldada.
        if (entrega.getMotivoSinClave() == null || entrega.getMotivoSinClave().trim().length() < 5) {
            throw new EntregaNoPermitidaException(
                    "Pida la clave de seguridad al cliente. Si no la tiene, indique el motivo por el que "
                            + "se entrega solo con el documento");
        }
        entrega.setClaveVerificada(false);
        entrega.setMotivoSinClave(entrega.getMotivoSinClave().trim());
    }

    private String detalleEntrega(Entrega entrega) {

        String base = "Entregado a " + entrega.getReceptorNombres()
                + " (DNI " + entrega.getReceptorDni() + ", " + entrega.getRelacion().toLowerCase() + ")";

        return entrega.isClaveVerificada()
                ? base + ", clave de seguridad verificada"
                : base + ", sin clave: " + entrega.getMotivoSinClave();
    }

    /** Genera un codigo de rastreo unico con el formato RE-AAAA-NNNNNN. */
    private String generarCodigo() {
        long secuencia = envioRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("RE-%d-%06d", Year.now().getValue(), secuencia);
            secuencia++;
        } while (envioRepository.existsByCodigoRastreo(codigo));
        return codigo;
    }

    /** Codigo corto que acompana al de rastreo en la consulta del cliente. */
    private String generarCodigoVerificacion() {
        StringBuilder codigo = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            codigo.append(ALFABETO_VERIFICACION.charAt(ALEATORIO.nextInt(ALFABETO_VERIFICACION.length())));
        }
        return codigo.toString();
    }

    /** Clave numerica de seis digitos, comoda de dictar por telefono. */
    private String generarClaveRecojo() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }
}
