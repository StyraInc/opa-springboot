package com.styra.opa.springboot.properties;

import com.styra.opa.springboot.OpaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Run using: ./gradlew testModifiedSystemEnvProperties
@EnableConfigurationProperties(value = OpaProperties.class)
@ExtendWith(SpringExtension.class)
public class ModifiedSystemEnvPropertiesTest {

    @Autowired
    private OpaProperties opaProperties;

    @Test
    public void test() {
        assertEquals("http://localhost:8183", opaProperties.getUrl());
        assertEquals("/tickets/main2", opaProperties.getPath());
    }
}
