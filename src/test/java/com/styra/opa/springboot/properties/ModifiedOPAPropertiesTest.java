package com.styra.opa.springboot.properties;

import com.styra.opa.springboot.OPAProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {
    "opa.url=http://localhost:8182",
    "opa.path=tickets/main",
    "opa.reason-key=de",
    "opa.request.resource.type=stomp_endpoint",
    "opa.request.context.type=websocket",
    "opa.request.subject.type=oauth2_resource_owner"
})
@EnableConfigurationProperties(value = OPAProperties.class)
@ExtendWith(SpringExtension.class)
public class ModifiedOPAPropertiesTest {

    @Autowired
    private OPAProperties opaProperties;

    @Test
    public void test() {
        assertEquals("http://localhost:8182", opaProperties.getUrl());
        assertEquals("tickets/main", opaProperties.getPath());
        assertEquals("de", opaProperties.getReasonKey());
        assertEquals("stomp_endpoint", opaProperties.getRequest().getResource().getType());
        assertEquals("websocket", opaProperties.getRequest().getContext().getType());
        assertEquals("oauth2_resource_owner", opaProperties.getRequest().getSubject().getType());
    }
}

