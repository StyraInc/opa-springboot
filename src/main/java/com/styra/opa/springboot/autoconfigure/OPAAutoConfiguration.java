package com.styra.opa.springboot.autoconfigure;

import com.styra.opa.OPAClient;
import com.styra.opa.springboot.OPAAuthorizationManager;
import com.styra.opa.springboot.OPAPathSelector;
import com.styra.opa.springboot.authorization.OPAAuthorizationEventPublisher;
import com.styra.opa.springboot.input.OPAInputValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OPA authorization support.
 */
@AutoConfiguration
@EnableConfigurationProperties(OPAProperties.class)
@AutoConfigureBefore(SecurityAutoConfiguration.class)
@ConditionalOnClass(OPAClient.class)
public class OPAAutoConfiguration {

    /**
     * Create an {@link OPAClient} bean using {@link OPAProperties#getUrl()}.
     */
    @Bean
    @ConditionalOnMissingBean(OPAClient.class)
    public OPAClient opaClient(OPAProperties opaProperties) {
        return new OPAClient(opaProperties.getUrl());
    }

    /**
     * Create an {@link OPAPathSelector} bean using {@link OPAProperties#getPath()}.
     */
    @Bean
    @ConditionalOnMissingBean
    public OPAPathSelector opaPathSelector(OPAProperties opaProperties) {
        return (authentication, requestAuthorizationContext, opaInput) -> opaProperties.getPath();
    }

    /**
     * Create an {@link OPAAuthorizationManager} bean using {@link OPAClient} bean and {@link OPAProperties#getPath()}.
     */
    @Bean
    @ConditionalOnMissingBean(OPAAuthorizationManager.class)
    public OPAAuthorizationManager opaAuthorizationManager(OPAClient opaClient, OPAProperties opaProperties) {
        return new OPAAuthorizationManager(opaClient, opaProperties.getPath());
    }

    /**
     * Create an {@link OPAInputValidator} to validate the OPA input's required fields before sending request to the
     * OPA server.
     */
    @Bean
    public OPAInputValidator opaInputValidator() {
        return new OPAInputValidator();
    }

    /**
     * Create an {@link OPAAuthorizationEventPublisher} to publish denied/granted authorization events.
     */
    @Bean
    public OPAAuthorizationEventPublisher opaAuthorizationEventPublisher(ApplicationEventPublisher publisher,
                                                                         OPAProperties opaProperties) {
        return new OPAAuthorizationEventPublisher(publisher, opaProperties);
    }
}
