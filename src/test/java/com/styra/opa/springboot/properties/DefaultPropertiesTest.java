package com.styra.opa.springboot.properties;

import com.styra.opa.springboot.OpaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableConfigurationProperties(value = OpaProperties.class)
@ExtendWith(SpringExtension.class)
public class DefaultPropertiesTest {

    @Autowired
    private OpaProperties opaProperties;

    @Test
    public void test() {
        assertEquals(OpaProperties.DEFAULT_URL, opaProperties.getUrl());
        assertNull(opaProperties.getPath());
        assertEquals(OpaProperties.DEFAULT_REASON_KEY, opaProperties.getReasonKey());
        assertNotNull(opaProperties.getRequest());
        assertEquals(OpaProperties.Request.Resource.DEFAULT_TYPE, opaProperties.getRequest().getResource().getType());
        assertNotNull(opaProperties.getRequest().getContext());
        assertEquals(OpaProperties.Request.Context.DEFAULT_TYPE, opaProperties.getRequest().getContext().getType());
        assertEquals(OpaProperties.Request.Subject.DEFAULT_TYPE,
                opaProperties.getRequest().getSubject().getType());
    }
}
