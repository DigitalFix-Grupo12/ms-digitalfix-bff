package cl.duoc.digitalfix.bff.controller;

import cl.duoc.digitalfix.bff.dto.WorkOrderDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NOTA: almacenamiento en memoria solo para EP1 (demostrar el flujo
 * MSAL -> BFF -> autorización -> respuesta). En la evaluación de
 * microservicios este controller delegará a ms-digitalfix-workorders
 * vía HTTP (RestClient/WebClient) en lugar de guardar el estado aquí.
 */
@RestController
@RequestMapping("/api/workorders")
public class WorkOrdersController {

    private final Map<Long, WorkOrderDto> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    // Regla del caso: CREADA -> ASIGNADA -> EN_DESPLAZAMIENTO -> EN_EJECUCION -> CERRADA/CANCELADA
    // "No se puede pasar a EN_EJECUCION sin ASIGNAR"
    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = Map.of(
        "CREADA", List.of("ASIGNADA", "CANCELADA"),
        "ASIGNADA", List.of("EN_DESPLAZAMIENTO", "CANCELADA"),
        "EN_DESPLAZAMIENTO", List.of("EN_EJECUCION", "CANCELADA"),
        "EN_EJECUCION", List.of("CERRADA")
    );

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public List<WorkOrderDto> list(@RequestParam(required = false) String status) {
        return store.values().stream()
            .filter(o -> status == null || status.equalsIgnoreCase(o.getStatus()))
            .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public WorkOrderDto getById(@PathVariable Long id) {
        return findOrThrow(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public ResponseEntity<WorkOrderDto> create(@Valid @RequestBody WorkOrderDto dto) {
        long id = sequence.getAndIncrement();
        dto.setId(id);
        dto.setStatus("CREADA");
        store.put(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor')")
    public WorkOrderDto updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        WorkOrderDto order = findOrThrow(id);
        String newStatus = body.get("status");

        List<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), List.of());
        if (!allowed.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Transición inválida: %s -> %s".formatted(order.getStatus(), newStatus));
        }

        order.setStatus(newStatus);
        return order;
    }

    private WorkOrderDto findOrThrow(Long id) {
        WorkOrderDto order = store.get(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada: " + id);
        }
        return order;
    }
}
