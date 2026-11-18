package cl.duoc.digitalfix.bff.audit;

import cl.duoc.digitalfix.bff.audit.event.AuditTimelineEvent;

/** Publica una entrada del timeline de auditoria (topico audit.timeline). */
@FunctionalInterface
public interface AuditTimelinePublisher {

    void publish(AuditTimelineEvent event);
}
