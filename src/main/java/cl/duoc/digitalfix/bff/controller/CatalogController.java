package cl.duoc.digitalfix.bff.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor')")
    public List<Map<String, Object>> services() {
        return List.of(
            Map.of("id", 1, "nombre", "Cambio de tablero eléctrico", "stock", 12, "tarifa", 45000),
            Map.of("id", 2, "nombre", "Reparación de cortocircuito", "stock", 8, "tarifa", 32000)
        );
    }
}
