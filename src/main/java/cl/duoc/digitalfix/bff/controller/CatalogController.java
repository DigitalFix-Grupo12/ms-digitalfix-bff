package cl.duoc.digitalfix.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/** Gateway hacia ms-digitalfix-catalog. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private static final String SERVICE = "catalog";
    private final RestClient client;

    public CatalogController(@Value("${digitalfix.services.catalog-url}") String baseUrl) {
        this.client = ProxySupport.client(baseUrl);
    }

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor')")
    public ResponseEntity<byte[]> services(@RequestParam(required = false) String tipo) {
        return tipo == null
            ? ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/catalog/services")
            : ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/catalog/services?tipo={t}", tipo);
    }

    @GetMapping("/services/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Supervisor')")
    public ResponseEntity<byte[]> byId(@PathVariable Long id) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/catalog/services/{id}", id);
    }
}
