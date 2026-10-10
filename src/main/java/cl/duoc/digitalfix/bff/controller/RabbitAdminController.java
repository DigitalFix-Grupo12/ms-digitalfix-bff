package cl.duoc.digitalfix.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Gateway hacia ms-digitalfix-rabbit-admin. Solo rol Admin: administrar
 * colas, exchanges y bindings afecta a todo el sistema. La validacion de
 * nombres y parametros la hace el microservicio (respuesta 400 se reenvia tal cual).
 */
@RestController
@RequestMapping("/api/rabbit")
@PreAuthorize("hasRole('Admin')")
public class RabbitAdminController {

    private static final String SERVICE = "rabbit-admin";
    private final RestClient client;

    public RabbitAdminController(@Value("${digitalfix.services.rabbit-admin-url}") String baseUrl) {
        this.client = ProxySupport.client(baseUrl);
    }

    @GetMapping("/overview")
    public ResponseEntity<byte[]> overview() {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/rabbit/overview");
    }

    // ---------------------------------------------------------------- colas

    @GetMapping("/queues")
    public ResponseEntity<byte[]> queues() {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/rabbit/queues");
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<byte[]> queue(@PathVariable String name) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/rabbit/queues/{n}", name);
    }

    @PostMapping("/queues")
    public ResponseEntity<byte[]> createQueue(@RequestBody String body) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.POST, body, "/api/rabbit/queues");
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<byte[]> deleteQueue(@PathVariable String name,
                                              @RequestParam(defaultValue = "false") boolean ifUnused,
                                              @RequestParam(defaultValue = "false") boolean ifEmpty) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.DELETE, null,
            "/api/rabbit/queues/{n}?ifUnused={u}&ifEmpty={e}", name, ifUnused, ifEmpty);
    }

    @GetMapping("/queues/{name}/messages")
    public ResponseEntity<byte[]> peek(@PathVariable String name, @RequestParam(defaultValue = "10") int count) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null,
            "/api/rabbit/queues/{n}/messages?count={c}", name, count);
    }

    @DeleteMapping("/queues/{name}/messages")
    public ResponseEntity<byte[]> purge(@PathVariable String name) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.DELETE, null, "/api/rabbit/queues/{n}/messages", name);
    }

    @PostMapping("/queues/{name}/replay")
    public ResponseEntity<byte[]> replay(@PathVariable String name, @RequestParam(defaultValue = "10") int max) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.POST, null,
            "/api/rabbit/queues/{n}/replay?max={m}", name, max);
    }

    // ---------------------------------------------------------------- exchanges

    @GetMapping("/exchanges")
    public ResponseEntity<byte[]> exchanges(@RequestParam(defaultValue = "false") boolean includeSystem) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null,
            "/api/rabbit/exchanges?includeSystem={s}", includeSystem);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<byte[]> createExchange(@RequestBody String body) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.POST, body, "/api/rabbit/exchanges");
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<byte[]> deleteExchange(@PathVariable String name) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.DELETE, null, "/api/rabbit/exchanges/{n}", name);
    }

    // ---------------------------------------------------------------- bindings

    @GetMapping("/bindings")
    public ResponseEntity<byte[]> bindings() {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/rabbit/bindings");
    }

    @PostMapping("/bindings")
    public ResponseEntity<byte[]> createBinding(@RequestBody String body) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.POST, body, "/api/rabbit/bindings");
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<byte[]> deleteBinding(@RequestParam String exchange, @RequestParam String queue,
                                                @RequestParam(defaultValue = "") String routingKey) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.DELETE, null,
            "/api/rabbit/bindings?exchange={e}&queue={q}&routingKey={k}", exchange, queue, routingKey);
    }

    // ---------------------------------------------------------------- mensajes

    @PostMapping("/messages")
    public ResponseEntity<byte[]> publish(@RequestBody String body) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.POST, body, "/api/rabbit/messages");
    }
}
