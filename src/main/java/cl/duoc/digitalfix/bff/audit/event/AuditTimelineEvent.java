package cl.duoc.digitalfix.bff.audit.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Evento del topico {@code audit.timeline}: quien, que, cuando y desde donde.
 * Lo publica el BFF por cada accion relevante (incluidos accesos denegados), con
 * la IP de origen y el rol tomado del JWT; complementa a workorders.events, que
 * solo trae hechos de negocio.
 */
public record AuditTimelineEvent(
        @NotBlank @Size(max = 64) String eventId,
        @Min(1) int schemaVersion,
        @NotNull Instant occurredAt,
        @Size(max = 64) String traceId,
        @NotBlank @Size(max = 100) String source,
        @NotNull @Valid Actor actor,
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{2,59}", message = "debe ser un codigo en MAYUSCULAS, ej. WORKORDER_STATUS_UPDATE")
        String action,
        @NotBlank @Size(max = 50) String resourceType,
        @Size(max = 100) String resourceId,
        @Size(max = 45) String sourceIp,
        @NotNull AuditOutcome outcome,
        @Size(max = 300) String detail) {}
