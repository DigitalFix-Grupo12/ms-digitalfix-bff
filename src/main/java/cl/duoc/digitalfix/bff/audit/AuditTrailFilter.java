package cl.duoc.digitalfix.bff.audit;

import cl.duoc.digitalfix.bff.audit.event.Actor;
import cl.duoc.digitalfix.bff.audit.event.AuditOutcome;
import cl.duoc.digitalfix.bff.audit.event.AuditTimelineEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Registra en el timeline de auditoria (Kafka audit.timeline) cada accion que
 * cambia datos (POST/PUT/PATCH/DELETE) y cada acceso rechazado (401/403),
 * con usuario y rol del JWT, IP de origen y resultado. Las lecturas exitosas
 * no se auditan (el panel en vivo consulta cada pocos segundos).
 *
 * <p>Se registra dentro de la cadena de Spring Security, despues de validar
 * el token: asi ve la identidad y tambien las respuestas 403 del control de
 * acceso. La publicacion ocurre al terminar la peticion y no altera la respuesta.</p>
 */
public class AuditTrailFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String[] ROLE_PRIORITY = {"Admin", "Supervisor", "Auditor", "Cliente"};

    private final AuditTimelinePublisher publisher;
    private final String source;

    public AuditTrailFilter(AuditTimelinePublisher publisher, String source) {
        this.publisher = publisher;
        this.source = source;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            boolean denied = status == 401 || status == 403;
            if (MUTATIONS.contains(request.getMethod()) || denied) {
                publisher.publish(toEvent(request, status));
            }
        }
    }

    AuditTimelineEvent toEvent(HttpServletRequest request, int status) {
        // /api/workorders/12/status        -> WORKORDERS, id 12, sub STATUS
        // /api/rabbit/queues               -> RABBIT_QUEUES (subcoleccion), sin id
        // /api/kafka/topics/audit.timeline -> KAFKA_TOPICS, id audit.timeline
        String[] segments = request.getRequestURI().replaceFirst("^/api/", "").split("/");
        String resource = sanitize(segments.length > 0 && !segments[0].isBlank() ? segments[0] : "API");
        int idIndex = 1;
        if (segments.length > 1 && segments[1].matches("[A-Za-z-]+")) {
            resource = resource + "_" + sanitize(segments[1]);
            idIndex = 2;
        }
        String resourceId = segments.length > idIndex ? truncate(segments[idIndex], 100) : null;
        String sub = segments.length > idIndex + 1 ? "_" + sanitize(segments[idIndex + 1]) : "";
        String action = truncate(resource + sub + "_" + verb(request.getMethod()), 60);

        AuditOutcome outcome = status == 401 || status == 403 ? AuditOutcome.DENIED
                : status >= 400 ? AuditOutcome.ERROR : AuditOutcome.SUCCESS;
        String detail = truncate("HTTP %d %s %s".formatted(status, request.getMethod(), request.getRequestURI()), 300);

        return new AuditTimelineEvent(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                UUID.randomUUID().toString().substring(0, 16),
                source,
                currentActor(),
                action,
                truncate(resource, 50),
                resourceId,
                clientIp(request),
                outcome,
                detail);
    }

    private static Actor currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return new Actor("anonymous", "anonimo", "NONE");
        }
        String id = firstClaim(jwt, "oid", "sub");
        String username = firstClaim(jwt, "preferred_username", "upn", "email", "oid", "sub");
        String role = "SIN_ROL";
        for (String candidate : ROLE_PRIORITY) {
            if (auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_" + candidate))) {
                role = candidate;
                break;
            }
        }
        return new Actor(truncate(id, 100), truncate(username, 255), role);
    }

    private static String firstClaim(Jwt jwt, String... claims) {
        for (String claim : claims) {
            String value = jwt.getClaimAsString(claim);
            if (value != null && !value.isBlank()) return value;
        }
        return "desconocido";
    }

    /** API Gateway agrega X-Forwarded-For con la IP real del cliente como primer valor. */
    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return truncate(ip, 45);
    }

    private static String verb(String method) {
        return switch (method) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "READ";
        };
    }

    private static String sanitize(String value) {
        String s = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return s.isBlank() || !Character.isLetter(s.charAt(0)) ? "API_" + s : s;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
