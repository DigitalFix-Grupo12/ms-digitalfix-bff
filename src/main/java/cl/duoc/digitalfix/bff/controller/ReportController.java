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

/** Gateway hacia ms-digitalfix-report. */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final String SERVICE = "report";
    private final RestClient client;

    public ReportController(@Value("${digitalfix.services.report-url}") String baseUrl) {
        this.client = ProxySupport.client(baseUrl);
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<byte[]> kpis(@RequestParam(defaultValue = "last24h") String range) {
        return ProxySupport.forward(client, SERVICE, HttpMethod.GET, null, "/api/report/kpis?range={r}", range);
    }
}
