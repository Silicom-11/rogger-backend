package com.utp.roggerexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Coleccion principal del sistema: una encomienda admitida en agencia.
 *
 * Los datos que siempre se leen junto al envio (remitente, destinatario, carga,
 * tarifa e historial) se guardan embebidos en el mismo documento; los catalogos
 * reutilizables (agencias) se enlazan por referencia.
 */
@Document(collection = "envios")
@CompoundIndex(name = "idx_destino_estado", def = "{'agenciaDestinoId': 1, 'estadoActual': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Envio {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigoRastreo;

    /**
     * Codigo corto que se imprime en el comprobante. El cliente debe presentarlo
     * junto con el codigo de rastreo para consultar su encomienda: el codigo de
     * rastreo es correlativo y, por si solo, permitiria ver los envios ajenos.
     */
    private String codigoVerificacion;

    /**
     * Clave de seguridad para el recojo, guardada cifrada.
     *
     * Se entrega al remitente en el comprobante y este se la comunica a quien
     * recibe. En la agencia se pide junto con el DNI. Nunca se guarda en claro:
     * si se pierde, se genera una nueva.
     */
    private String claveRecojoHash;

    private Persona remitente;
    private Persona destinatario;

    /**
     * Personas que, ademas del destinatario, pueden recoger la encomienda.
     * Resuelve el caso corriente de que el titular no pueda acercarse.
     */
    @Builder.Default
    private List<Persona> autorizados = new ArrayList<>();

    private Carga carga;
    private Tarifa tarifa;

    private String agenciaOrigenId;
    private String agenciaDestinoId;

    @Indexed
    private EstadoEnvio estadoActual;

    @Builder.Default
    private List<EventoSeguimiento> historial = new ArrayList<>();

    private Entrega entrega;

    @Indexed
    private LocalDateTime fechaRegistro;

    /**
     * Momento en que la encomienda quedo disponible para el recojo. Permite
     * saber cuanto tiempo lleva ocupando el almacen esperando a su destinatario.
     */
    private LocalDateTime fechaListoParaRecojo;

    /** Usuario que admitio la encomienda en ventanilla (auditoria). */
    private String registradoPor;

    /** Agrega un evento al historial de seguimiento del envio. */
    public void agregarEvento(EstadoEnvio estado, String agenciaId, String usuario, String observacion) {
        if (this.historial == null) {
            this.historial = new ArrayList<>();
        }
        this.historial.add(EventoSeguimiento.builder()
                .estado(estado)
                .fecha(LocalDateTime.now())
                .agenciaId(agenciaId)
                .usuario(usuario)
                .observacion(observacion)
                .build());
    }

    /** Indica si el documento presentado corresponde al destinatario o a un autorizado. */
    public Persona quienRecoge(String dni) {
        if (dni == null) {
            return null;
        }
        if (destinatario != null && dni.equals(destinatario.getDni())) {
            return destinatario;
        }
        if (autorizados != null) {
            return autorizados.stream().filter(a -> dni.equals(a.getDni())).findFirst().orElse(null);
        }
        return null;
    }
}
