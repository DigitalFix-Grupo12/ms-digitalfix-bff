package cl.duoc.digitalfix.bff.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Auditor')")
    public List<Map<String, Object>> timeline() {
        // En la evaluación de streaming, esto vendrá de ms-digitalfix-audit (consumer Kafka).
        return List.of(
            Map.of("id", 1, "usuario", "supervisor1@tenant.onmicrosoft.com",
                   "accion", "ASIGNO orden #1", "fecha", Instant.now().toString())
        );
    }
}
