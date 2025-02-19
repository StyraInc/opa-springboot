package com.styra.opa.springboot.autoconfigure.properties;

import com.styra.opa.springboot.autoconfigure.OPAProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Run using: ./gradlew testModifiedSystemEnvProperties
@EnableConfigurationProperties(OPAProperties.class)
@ExtendWith(SpringExtension.class)
public class ModifiedSystemEnvOPAPropertiesTest {

    @Autowired
    private OPAProperties opaProperties;

    @Test
    public void test() {
        assertEquals("http://localhost:8183", opaProperties.getUrl());
        assertEquals("foo/bar2", opaProperties.getPath());
    }
}
