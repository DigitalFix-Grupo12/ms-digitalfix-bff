package cl.duoc.digitalfix.bff.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sin esto, Spring Security devuelve páginas de error HTML/genéricas.
 * El frontend (WorkordersService, etc.) espera JSON para poder mostrar
 * mensajes de error útiles según el código HTTP.
 */
public class RestAuthEntryPoints {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) ->
                writeError(res, HttpServletResponse.SC_UNAUTHORIZED,
                        "No autenticado: token ausente, expirado o inválido (" + ex.getMessage() + ")");
    }

    public static AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) ->
                writeError(res, HttpServletResponse.SC_FORBIDDEN,
                        "No autorizado: tu rol no tiene permiso para este recurso");
    }

    private static void writeError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("message", message);
        MAPPER.writeValue(res.getWriter(), body);
    }
}
