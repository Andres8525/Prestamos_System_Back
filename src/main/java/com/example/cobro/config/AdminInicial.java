package com.example.cobro.config;

import com.example.cobro.dto.AuthDTO;
import com.example.cobro.repository.AdministradorRepository;
import com.example.cobro.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta admin@cobro.com cuando no hay ningún administrador y se definió
 * DEMO_ADMIN_PASSWORD. Es independiente de la cartera de demostración (APP_SEED_ENABLED),
 * así producción puede tener su administrador sin cargar datos de ejemplo.
 */
@Component
@Order(5)
public class AdminInicial implements CommandLineRunner {

    static final String EMAIL = "admin@cobro.com";
    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final AdministradorRepository administradorRepository;
    private final AuthService authService;
    private final String clave;

    public AdminInicial(AdministradorRepository administradorRepository, AuthService authService,
                        @Value("${app.seed.admin-password:}") String clave) {
        this.administradorRepository = administradorRepository;
        this.authService = authService;
        this.clave = clave;
    }

    @Override
    public void run(String... args) {
        if (clave == null || clave.isBlank()) {
            if (administradorRepository.count() == 0) {
                log.warn("No hay administradores: defina DEMO_ADMIN_PASSWORD o registre una cuenta desde el login.");
            }
            return;
        }
        if (administradorRepository.count() > 0) return;
        authService.registrar(new AuthDTO.Registro("Administrador Demo", EMAIL, clave));
        log.info("Administrador inicial creado: {}", EMAIL);
    }
}
