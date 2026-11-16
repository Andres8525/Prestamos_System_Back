package com.example.cobro.controller;

import com.example.cobro.dto.AuthDTO;
import com.example.cobro.security.AuthFilter;
import com.example.cobro.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<AuthDTO.Respuesta> registrar(@Valid @RequestBody AuthDTO.Registro dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
    }

    /** Paso 1 del acceso: ¿el email tiene cuenta? (decide entre pedir contraseña o crear cuenta). */
    @PostMapping("/auth/verificar-email")
    public AuthDTO.EstadoEmail verificarEmail(@Valid @RequestBody AuthDTO.VerificarEmail dto) {
        return authService.verificarEmail(dto);
    }

    @PostMapping("/auth/login")
    public AuthDTO.Respuesta login(@Valid @RequestBody AuthDTO.Login dto) {
        return authService.iniciarSesion(dto);
    }

    /** Datos del usuario de la sesión actual (requiere token). */
    @GetMapping("/auth/me")
    public AuthDTO.UsuarioInfo perfil(HttpServletRequest request) {
        return authService.perfil((Long) request.getAttribute(AuthFilter.ATRIBUTO_USUARIO_ID));
    }

    /**
     * Cierre de sesión. Los tokens no guardan estado en el servidor: el cliente descarta el suyo
     * y este expira por tiempo.
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    /** Verificación de disponibilidad (público, usado por Render). */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
