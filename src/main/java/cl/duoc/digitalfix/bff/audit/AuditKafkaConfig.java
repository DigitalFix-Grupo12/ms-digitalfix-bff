package cl.duoc.digitalfix.bff.audit;

import cl.duoc.digitalfix.bff.audit.event.AuditTimelineEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Productor Kafka del BFF: publica en {@code audit.timeline} quien hizo que, cuando
 * y desde donde. El BFF es el unico que conoce el JWT (usuario, rol) y la IP de
 * origen, por eso es el productor natural de este topico.
 *
 * <p>Clave = recurso afectado (ej. {@code workorders-12}): las entradas de un mismo
 * recurso quedan en la misma particion y en orden. El envio es asincrono y nunca
 * hace fallar la peticion del usuario: si Kafka no responde, queda en el log.</p>
 */
@Configuration
public class AuditKafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConfig.class);

    @Bean
    @ConditionalOnProperty(prefix = "digitalfix.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
    public KafkaTemplate<String, AuditTimelineEvent> auditTimelineTemplate(ProducerFactory<Object, Object> base,
                                                                           ObjectMapper objectMapper) {
        JsonSerializer<AuditTimelineEvent> serializer = new JsonSerializer<AuditTimelineEvent>(objectMapper).noTypeInfo();
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                base.getConfigurationProperties(), new StringSerializer(), serializer));
    }

    @Bean
    @ConditionalOnProperty(prefix = "digitalfix.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuditTimelinePublisher kafkaAuditTimelinePublisher(KafkaTemplate<String, AuditTimelineEvent> auditTimelineTemplate,
                                                              @Value("${digitalfix.kafka.topics.audit-timeline}") String topic) {
        return event -> {
            String key = event.resourceId() == null || event.resourceId().isBlank()
                    ? event.resourceType().toLowerCase()
                    : event.resourceType().toLowerCase() + "-" + event.resourceId();
            try {
                auditTimelineTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[AUDIT-NO-PUBLICADO] {} {} {}: {}", event.action(), event.actor().username(),
                                event.outcome(), ex.getMessage());
                    } else {
                        var meta = result.getRecordMetadata();
                        log.info("[AUDIT] {} {} {} -> {}-{}@{}", event.action(), event.actor().username(),
                                event.outcome(), meta.topic(), meta.partition(), meta.offset());
                    }
                });
            } catch (RuntimeException ex) {
                // p. ej. metadata no disponible: la auditoria nunca rompe la respuesta al usuario
                log.error("[AUDIT-NO-PUBLICADO] {}: {}", event.action(), ex.getMessage());
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "digitalfix.kafka", name = "enabled", havingValue = "false")
    public AuditTimelinePublisher loggingAuditTimelinePublisher() {
        return event -> log.info("[AUDIT-KAFKA-DESHABILITADO] {} {} {}", event.action(),
                event.actor().username(), event.outcome());
    }
}
