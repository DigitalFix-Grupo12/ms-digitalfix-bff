package cl.duoc.digitalfix.bff.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Gateway hacia los microservicios de dominio. Los controllers del BFF NO
 * interpretan el body: autorizan por rol y reenvian status + body tal cual.
 *
 *  - Timeouts cortos: un servicio colgado no bloquea hilos del BFF.
 *  - Servicio caido -> 503 JSON (en vez de un 500 generico).
 *  - Solo se reenvia Content-Type: headers hop-by-hop (Transfer-Encoding,
 *    Connection) o CORS de aguas abajo romperian la respuesta.
 *  - Propaga la identidad del JWT en X-User-Name para la auditoria. El
 *    header lo pone SIEMPRE el BFF (nunca se copia desde el cliente).
 */
final class ProxySupport {

    static final String USER_HEADER = "X-User-Name";

    private ProxySupport() {}

    static RestClient client(String baseUrl) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(2000);
        rf.setReadTimeout(8000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(rf).build();
    }

    static ResponseEntity<byte[]> forward(RestClient client, String service, HttpMethod method,
                                          Object body, String uriTemplate, Object... uriVars) {
        try {
            RestClient.RequestBodySpec spec = client.method(method)
                .uri(uriTemplate, uriVars)
                .header(USER_HEADER, currentUser());
            if (body != null) {
                spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return spec.exchange((request, response) -> {
                HttpHeaders headers = new HttpHeaders();
                MediaType ct = response.getHeaders().getContentType();
                if (ct != null) headers.setContentType(ct);
                byte[] payload = response.getBody().readAllBytes();
                return ResponseEntity.status(response.getStatusCode()).headers(headers).body(payload);
            });
        } catch (ResourceAccessException ex) {
            String json = "{\"timestamp\":\"%s\",\"status\":503,\"message\":\"Servicio %s no disponible\"}"
                .formatted(Instant.now(), service);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** preferred_username (UPN/email) > upn > oid > sub. Todos son ASCII-safe para un header. */
    static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            for (String claim : new String[]{"preferred_username", "upn", "oid", "sub"}) {
                String v = jwt.getClaimAsString(claim);
                if (v != null && !v.isBlank()) return v;
            }
        }
        return "desconocido";
    }
}
