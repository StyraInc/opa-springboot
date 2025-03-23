package com.styra.opa.springboot.authorization;

import com.styra.opa.springboot.autoconfigure.OPAProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.authorization.event.AuthorizationGrantedEvent;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

/**
 * Publishes OPA authorization granted/denied events. By default, only denied events are published. To change default
 * behavior, the following configuration properties could be used:
 * <ul>
 *     <li>
 *         <code>opa.authorization-event.denied.enabled</code>
 *     </li>
 *     <li>
 *         <code>opa.authorization-event.granted.enabled</code>
 *     </li>
 * </ul>
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/authorization/events.html">
 *     Authorization Events</a>
 */
public class OPAAuthorizationEventPublisher implements AuthorizationEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(OPAAuthorizationEventPublisher.class);

    private final ApplicationEventPublisher publisher;
    private final AuthorizationEventPublisher delegate;
    private final OPAProperties opaProperties;

    public OPAAuthorizationEventPublisher(ApplicationEventPublisher publisher, OPAProperties opaProperties) {
        this.publisher = publisher;
        this.delegate = new SpringAuthorizationEventPublisher(publisher);
        this.opaProperties = opaProperties;
    }

    @Override
    public <T> void publishAuthorizationEvent(Supplier<Authentication> authentication, T object,
                                              AuthorizationDecision decision) {
        if (!(decision instanceof OPAAuthorizationDecision)) {
            return;
        }

        if (!decision.isGranted() && opaProperties.getAuthorizationEvent().getDenied().isEnabled()) {
            // Use `delegate` (instead of directly publishing denied events) to be forward-compatible with it:
            this.delegate.publishAuthorizationEvent(authentication, object, decision);
            LOGGER.trace("OPA AuthorizationDeniedEvent published.");
            return;
        }
        if (decision.isGranted() && opaProperties.getAuthorizationEvent().getGranted().isEnabled()) {
            AuthorizationGrantedEvent<T> granted = new AuthorizationGrantedEvent<>(authentication, object, decision);
            this.publisher.publishEvent(granted);
            LOGGER.trace("OPA AuthorizationGrantedEvent published.");
        }
    }
}
