package cl.duoc.digitalfix.bff.audit;

import cl.duoc.digitalfix.bff.audit.event.AuditOutcome;
import cl.duoc.digitalfix.bff.audit.event.AuditTimelineEvent;
import cl.duoc.digitalfix.bff.config.SecurityConfig;
import cl.duoc.digitalfix.bff.controller.KafkaAdminController;
import cl.duoc.digitalfix.bff.controller.RabbitAdminController;
import cl.duoc.digitalfix.bff.controller.WorkOrdersController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria del BFF: que entradas se publican en audit.timeline (con un
 * publicador en memoria en vez de Kafka). Los microservicios no estan
 * levantados, por eso las peticiones autorizadas terminan en 503.
 */
@WebMvcTest(controllers = {WorkOrdersController.class, RabbitAdminController.class, KafkaAdminController.class})
@Import({SecurityConfig.class, AuditTrailFilterTest.RecordingPublisher.class})
class AuditTrailFilterTest {

    static final List<AuditTimelineEvent> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class RecordingPublisher {
        @Bean
        AuditTimelinePublisher recordingAuditPublisher() {
            return SENT::add;
        }
    }

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void clear() {
        SENT.clear();
    }

    @Test
    void cambioDeEstadoSeAuditaConUsuarioRolEIpDeOrigen() throws Exception {
        mvc.perform(put("/api/workorders/12/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ASIGNADA\",\"tecnicoId\":\"t1\"}")
                        .header("X-Forwarded-For", "200.1.2.3, 10.0.0.5")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "sup@test").claim("oid", "oid-sup"))
                                .authorities(new SimpleGrantedAuthority("ROLE_Supervisor"))))
                .andExpect(status().isServiceUnavailable());

        assertThat(SENT).hasSize(1);
        AuditTimelineEvent e = SENT.get(0);
        assertThat(e.action()).isEqualTo("WORKORDERS_STATUS_UPDATE");
        assertThat(e.resourceType()).isEqualTo("WORKORDERS");
        assertThat(e.resourceId()).isEqualTo("12");
        assertThat(e.actor().username()).isEqualTo("sup@test");
        assertThat(e.actor().id()).isEqualTo("oid-sup");
        assertThat(e.actor().role()).isEqualTo("Supervisor");
        assertThat(e.sourceIp()).isEqualTo("200.1.2.3");
        assertThat(e.outcome()).isEqualTo(AuditOutcome.ERROR); // 503: servicio caido
        assertThat(e.source()).isNotBlank();
        assertThat(e.eventId()).isNotBlank();
    }

    @Test
    void accesoDenegadoSeAuditaComoDenied() throws Exception {
        mvc.perform(post("/api/rabbit/queues").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"q\"}")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "cli@test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
                .andExpect(status().isForbidden());

        assertThat(SENT).singleElement().satisfies(e -> {
            assertThat(e.outcome()).isEqualTo(AuditOutcome.DENIED);
            assertThat(e.action()).isEqualTo("RABBIT_QUEUES_CREATE");
            assertThat(e.actor().role()).isEqualTo("Cliente");
        });
    }

    @Test
    void sinTokenSeAuditaComoAnonimo() throws Exception {
        mvc.perform(get("/api/workorders")).andExpect(status().isUnauthorized());

        assertThat(SENT).singleElement().satisfies(e -> {
            assertThat(e.outcome()).isEqualTo(AuditOutcome.DENIED);
            assertThat(e.actor().username()).isEqualTo("anonimo");
        });
    }

    @Test
    void lecturasExitosasNoSeAuditan() throws Exception {
        mvc.perform(get("/api/workorders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Supervisor"))))
                .andExpect(status().isServiceUnavailable());

        assertThat(SENT).isEmpty();
    }

    @Test
    void adminDeKafkaSoloParaAdmin() throws Exception {
        mvc.perform(get("/api/kafka/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Supervisor"))))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/kafka/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Servicio kafka-admin no disponible"));
    }
}
