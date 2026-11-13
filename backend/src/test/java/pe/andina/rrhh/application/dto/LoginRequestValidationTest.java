package pe.andina.rrhh.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.andina.rrhh.application.dto.AuthDtos.LoginRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private static String usuarioValido60Caracteres() {
        return "a" + "b".repeat(59);
    }

    @Test
    @DisplayName("CP-AUTH-03: usuario con menos de 3 caracteres es inválido (valor límite)")
    void usuarioCortoInvalido() {
        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(new LoginRequest("ab", "Andina2026"));

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombreUsuario")));
    }

    @Test
    @DisplayName("CP-AUTH-04: usuario de 60 caracteres cumple el patrón (valor límite válido)")
    void usuarioEnLimiteSuperiorValido() {
        String usuario = usuarioValido60Caracteres();
        assertEquals(60, usuario.length());

        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(new LoginRequest(usuario, "Andina2026"));

        assertTrue(violations.isEmpty(), () -> violations.toString());
    }

    @Test
    @DisplayName("Usuario con 61 caracteres viola el patrón")
    void usuarioDemasiadoLargo() {
        String usuario = "a" + "b".repeat(60);
        assertEquals(61, usuario.length());

        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(new LoginRequest(usuario, "Andina2026"));

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Contraseña en blanco es inválida")
    void passwordVacia() {
        Set<ConstraintViolation<LoginRequest>> violations =
                validator.validate(new LoginRequest("juan.espinoza", "   "));

        assertFalse(violations.isEmpty());
    }
}
