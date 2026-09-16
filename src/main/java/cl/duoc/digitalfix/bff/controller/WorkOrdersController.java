package cl.duoc.digitalfix.bff.controller;

import cl.duoc.digitalfix.bff.dto.WorkOrderDto;
import cl.duoc.digitalfix.bff.entity.WorkOrder;
import cl.duoc.digitalfix.bff.repository.WorkOrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Persistencia real via Spring Data JPA + H2 (ver application.yml). En la
 * evaluacion de microservicios este controller pasara a delegar a
 * ms-digitalfix-workorders via HTTP en lugar de tener su propio repositorio.
 */
@RestController
@RequestMapping("/api/workorders")
public class WorkOrdersController {

    private final WorkOrderRepository repository;

    public WorkOrdersController(WorkOrderRepository repository) {
        this.repository = repository;
    }

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
    public List<WorkOrder> list(@RequestParam(required = false) String status) {
        if (status == null) {
            return repository.findAll();
        }
        return repository.findByStatusIgnoreCase(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public WorkOrder getById(@PathVariable Long id) {
        return findOrThrow(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public ResponseEntity<WorkOrder> create(@Valid @RequestBody WorkOrderDto dto) {
        WorkOrder order = new WorkOrder();
        order.setDescripcion(dto.getDescripcion());
        order.setClienteId(dto.getClienteId());
        order.setStatus("CREADA");
        WorkOrder saved = repository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Cambia de estado. Si el body incluye "tecnicoId" y la transición es
     * CREADA -> ASIGNADA, además asigna el técnico (requerido por el caso:
     * no se puede pasar a EN_EJECUCION sin haber asignado a alguien).
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor')")
    public WorkOrder updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        WorkOrder order = findOrThrow(id);
        String newStatus = body.get("status");
        String tecnicoId = body.get("tecnicoId");

        List<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), List.of());
        if (!allowed.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Transición inválida: %s -> %s".formatted(order.getStatus(), newStatus));
        }

        if ("ASIGNADA".equals(newStatus)) {
            if (tecnicoId == null || tecnicoId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Falta tecnicoId: no se puede asignar sin indicar el técnico");
            }
            order.setTecnicoId(tecnicoId);
        }

        order.setStatus(newStatus);
        return repository.save(order);
    }

    private WorkOrder findOrThrow(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada: " + id));
    }
}
