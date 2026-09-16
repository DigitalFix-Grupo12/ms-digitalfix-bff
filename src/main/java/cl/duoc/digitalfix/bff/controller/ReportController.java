package cl.duoc.digitalfix.bff.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @GetMapping("/kpis")
    @PreAuthorize("hasRole('Admin')")
    public Map<String, Object> kpis(@RequestParam(defaultValue = "last24h") String range) {
        // En la evaluación de streaming, esto vendrá de ms-digitalfix-report (consumer Kafka).
        // La "serie" es un desglose por bloques de 4h de las últimas 24h, para graficar.
        List<Map<String, Object>> serie = List.of(
            bucket("00-04", 2), bucket("04-08", 1), bucket("08-12", 6),
            bucket("12-16", 9), bucket("16-20", 7), bucket("20-24", 3)
        );

        return Map.of(
            "range", range,
            "ordenesPorHora", 7,
            "tiempoResolucionPromedioMin", 54,
            "estadosActivos", 15,
            "serie", serie
        );
    }

    private Map<String, Object> bucket(String label, int base) {
        // Pequeña variación para que no se vea perfectamente estático entre recargas.
        int jitter = ThreadLocalRandom.current().nextInt(-1, 2);
        return Map.of("label", label, "ordenes", Math.max(0, base + jitter));
    }
}
