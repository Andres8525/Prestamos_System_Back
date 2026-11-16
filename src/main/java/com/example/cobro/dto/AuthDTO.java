package com.example.cobro.dto;

import com.example.cobro.model.Administrador;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTOs de autenticación: registro, inicio de sesión y respuesta con el token. */
public final class AuthDTO {

    private AuthDTO() {}

    public record Registro(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
            String nombre,

            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El email no es válido")
            String email,

            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                     message = "La contraseña debe incluir letras y números")
            String password
    ) {}

    public record Login(
            @NotBlank(message = "El email es obligatorio") String email,
            @NotBlank(message = "La contraseña es obligatoria") String password
    ) {}

    public record UsuarioInfo(Long id, String nombre, String email, List<String> permisos) {
        public static UsuarioInfo de(Administrador a) {
            return new UsuarioInfo(a.getId(), a.getNombre(), a.getEmail(), a.getPermisos());
        }
    }

    public record Respuesta(String token, String tipo, long expiraEnSegundos, UsuarioInfo usuario) {}
}
