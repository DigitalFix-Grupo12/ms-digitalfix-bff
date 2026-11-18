package cl.duoc.digitalfix.bff.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Gateway hacia ms-digitalfix-kafka-admin (topicos, configuraciones, particiones y
 * consumer groups). Solo rol Admin: estas operaciones afectan a toda la plataforma
 * de streaming. Reenvia metodo, ruta, query y body tal cual; la validacion la hace
 * el microservicio y su respuesta (incluidos 400/404/409 RFC 7807) se devuelve intacta.
 */
@RestController
@RequestMapping("/api/kafka")
@PreAuthorize("hasRole('Admin')")
public class KafkaAdminController {

    private static final String SERVICE = "kafka-admin";
    private final RestClient client;

    public KafkaAdminController(@Value("${digitalfix.services.kafka-admin-url}") String baseUrl) {
        this.client = ProxySupport.client(baseUrl);
    }

    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<byte[]> forward(HttpServletRequest request,
                                          @RequestBody(required = false) String body) {
        String target = request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        return ProxySupport.forwardRaw(client, SERVICE, HttpMethod.valueOf(request.getMethod()), body, target);
    }
}
