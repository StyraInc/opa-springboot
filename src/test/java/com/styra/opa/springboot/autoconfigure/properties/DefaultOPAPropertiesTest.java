package com.styra.opa.springboot.autoconfigure.properties;

import com.styra.opa.springboot.autoconfigure.OPAProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableConfigurationProperties(OPAProperties.class)
@ExtendWith(SpringExtension.class)
public class DefaultOPAPropertiesTest {

    @Autowired
    private OPAProperties opaProperties;

    @Test
    public void test() {
        assertEquals(OPAProperties.DEFAULT_URL, opaProperties.getUrl());
        assertNull(opaProperties.getPath());
        assertNotNull(opaProperties.getRequest());
        assertEquals(OPAProperties.Request.Resource.DEFAULT_TYPE, opaProperties.getRequest().getResource().getType());
        assertNotNull(opaProperties.getRequest().getContext());
        assertEquals(OPAProperties.Request.Context.DEFAULT_TYPE, opaProperties.getRequest().getContext().getType());
        assertEquals(OPAProperties.Request.Subject.DEFAULT_TYPE,
            opaProperties.getRequest().getSubject().getType());
        assertEquals(OPAProperties.Response.Context.DEFAULT_REASON_KEY,
            opaProperties.getResponse().getContext().getReasonKey());
    }
}
