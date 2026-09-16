package cl.duoc.digitalfix.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Gateway hacia ms-digitalfix-audit. Solo lectura: el POST de ingesta de
 * eventos es interno entre microservicios y NO se expone por el BFF.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final String SERVICE = "audit";
    private final RestClient client;

    public AuditController(@Value("${digitalfix.services.audit-url}") String baseUrl) {
        this.client = ProxySupport.client(baseUrl);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Auditor')")
    public ResponseEntity<byte[]> timeline(@RequestParam(defaultValue = "100") int limit) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/audit?limit={l}", limit);
    }
}
