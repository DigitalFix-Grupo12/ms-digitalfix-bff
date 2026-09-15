package cl.duoc.digitalfix.bff.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @GetMapping("/kpis")
    @PreAuthorize("hasRole('Admin')")
    public Map<String, Object> kpis(@RequestParam(defaultValue = "last24h") String range) {
        // En la evaluación de streaming, esto vendrá de ms-digitalfix-report (consumer Kafka).
        return Map.of(
            "range", range,
            "ordenesPorHora", 7,
            "tiempoResolucionPromedioMin", 54,
            "estadosActivos", 15
        );
    }
}
