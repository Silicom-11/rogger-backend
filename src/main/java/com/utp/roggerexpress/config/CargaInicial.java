package com.utp.roggerexpress.config;

import com.utp.roggerexpress.model.Agencia;
import com.utp.roggerexpress.model.Rol;
import com.utp.roggerexpress.model.Tarifario;
import com.utp.roggerexpress.model.Usuario;
import com.utp.roggerexpress.repository.AgenciaRepository;
import com.utp.roggerexpress.repository.TarifarioRepository;
import com.utp.roggerexpress.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga los datos minimos de operacion (agencias, tarifario y usuarios) la
 * primera vez que arranca la aplicacion. Las contrasenas se guardan cifradas
 * con BCrypt.
 */
@Component
@ConditionalOnProperty(name = "app.carga-inicial", havingValue = "true", matchIfMissing = true)
public class CargaInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private final AgenciaRepository agenciaRepository;
    private final TarifarioRepository tarifarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CargaInicial(AgenciaRepository agenciaRepository,
                        TarifarioRepository tarifarioRepository,
                        UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder) {
        this.agenciaRepository = agenciaRepository;
        this.tarifarioRepository = tarifarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (agenciaRepository.count() == 0) {
            agenciaRepository.saveAll(List.of(
                    Agencia.builder().codigo("AG-LIM-VIC").nombre("Lima - La Victoria")
                            .ciudad("Lima").direccion("Av. Mexico 1234 - La Victoria")
                            .telefono("013456789").activo(true).build(),
                    Agencia.builder().codigo("AG-LIM-CER").nombre("Lima - Cercado")
                            .ciudad("Lima").direccion("Jr. Montevideo 456 - Cercado de Lima")
                            .telefono("013456790").activo(true).build(),
                    Agencia.builder().codigo("AG-HYO-CEN").nombre("Huancayo - Central")
                            .ciudad("Huancayo").direccion("Av. Ferrocarril 890 - El Tambo")
                            .telefono("064234567").activo(true).build()));
            log.info("Agencias iniciales registradas");
        }

        if (tarifarioRepository.count() == 0) {
            tarifarioRepository.saveAll(List.of(
                    Tarifario.builder().ruta("Lima-Huancayo")
                            .montoBase(new BigDecimal("8.00")).precioPorKg(new BigDecimal("3.00"))
                            .factorVolumetrico(6000).recargoFragil(new BigDecimal("5.00"))
                            .vigenteDesde(LocalDate.now()).activo(true).build(),
                    Tarifario.builder().ruta("Huancayo-Lima")
                            .montoBase(new BigDecimal("8.00")).precioPorKg(new BigDecimal("3.00"))
                            .factorVolumetrico(6000).recargoFragil(new BigDecimal("5.00"))
                            .vigenteDesde(LocalDate.now()).activo(true).build()));
            log.info("Tarifario inicial registrado");
        }

        if (usuarioRepository.count() == 0) {
            usuarioRepository.saveAll(List.of(
                    usuario("operador.lima", "Yefrie Camposano", Rol.OPERADOR, "AG-LIM-VIC"),
                    usuario("almacen.huancayo", "Andrey Alvarado", Rol.ALMACEN, "AG-HYO-CEN"),
                    usuario("admin", "Marc Aquino", Rol.ADMIN, "AG-LIM-VIC")));
            log.info("Usuarios iniciales registrados (contrasena por defecto: Rogger2026)");
        }
    }

    private Usuario usuario(String username, String nombre, Rol rol, String agencia) {
        return Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode("Rogger2026"))
                .nombreCompleto(nombre)
                .rol(rol)
                .agenciaId(agencia)
                .activo(true)
                .build();
    }
}
