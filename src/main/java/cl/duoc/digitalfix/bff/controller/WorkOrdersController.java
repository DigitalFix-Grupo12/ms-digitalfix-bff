package cl.duoc.digitalfix.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Gateway hacia ms-digitalfix-workorders. El BFF NO tiene la tabla ni la
 * maquina de estados: autoriza por rol y reenvia.
 */
@RestController
@RequestMapping("/api/workorders")
public class WorkOrdersController {

    private static final String SERVICE = "workorders";
    private final RestClient client;

    public WorkOrdersController(@Value("${digitalfix.services.workorders-url}") String baseUrl) {
        this.client = ProxySupport.client(baseUrl);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public ResponseEntity<byte[]> list(@RequestParam(required = false) String status) {
        return status == null
            ? ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/workorders")
            : ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/workorders?status={s}", status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public ResponseEntity<byte[]> getById(@PathVariable Long id) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/workorders/{id}", id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor', 'Cliente')")
    public ResponseEntity<byte[]> create(@RequestBody Map<String, Object> body) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.POST, body, "/api/workorders");
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor')")
    public ResponseEntity<byte[]> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.PUT, body, "/api/workorders/{id}/status", id);
    }
}
