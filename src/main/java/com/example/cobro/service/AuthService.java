package com.example.cobro.service;

import com.example.cobro.dto.AuthDTO;
import com.example.cobro.exception.NoAutorizadoException;
import com.example.cobro.exception.ReglaNegocioException;
import com.example.cobro.model.Administrador;
import com.example.cobro.repository.AdministradorRepository;
import com.example.cobro.security.PasswordHasher;
import com.example.cobro.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos";

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private TokenService tokenService;

    public AuthDTO.Respuesta registrar(AuthDTO.Registro dto) {
        String email = normalizar(dto.email());
        if (administradorRepository.existsByEmail(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con ese email");
        }
        Administrador admin = new Administrador();
        admin.setNombre(dto.nombre().trim());
        admin.setEmail(email);
        admin.setContrasena(passwordHasher.hash(dto.password()));
        admin.setUltimoAcceso(LocalDateTime.now());
        admin = administradorRepository.save(admin);
        admin.setCodigoAdmin(String.format("ADM-%04d", admin.getId()));
        return respuesta(administradorRepository.save(admin));
    }

    /** Primer paso del acceso: indica si el email ya está registrado. */
    @Transactional(readOnly = true)
    public AuthDTO.EstadoEmail verificarEmail(AuthDTO.VerificarEmail dto) {
        String email = normalizar(dto.email());
        return new AuthDTO.EstadoEmail(email, administradorRepository.existsByEmail(email));
    }

    public AuthDTO.Respuesta iniciarSesion(AuthDTO.Login dto) {
        // Mismo mensaje para email inexistente y clave errónea: no se revela qué cuentas existen.
        Administrador admin = administradorRepository.findByEmail(normalizar(dto.email()))
                .orElseThrow(() -> new NoAutorizadoException(CREDENCIALES_INVALIDAS));
        if (!admin.autenticar() || !passwordHasher.coincide(dto.password(), admin.getContrasena())) {
            throw new NoAutorizadoException(CREDENCIALES_INVALIDAS);
        }
        admin.setUltimoAcceso(LocalDateTime.now());
        return respuesta(administradorRepository.save(admin));
    }

    @Transactional(readOnly = true)
    public AuthDTO.UsuarioInfo perfil(Long usuarioId) {
        return administradorRepository.findById(usuarioId)
                .filter(Administrador::isActivo)
                .map(AuthDTO.UsuarioInfo::de)
                .orElseThrow(() -> new NoAutorizadoException("La sesión ya no es válida"));
    }

    private AuthDTO.Respuesta respuesta(Administrador admin) {
        return new AuthDTO.Respuesta(tokenService.emitir(admin), "Bearer",
                tokenService.getSegundosVigencia(), AuthDTO.UsuarioInfo.de(admin));
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
