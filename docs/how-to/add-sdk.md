# How To Add the OPA Spring Boot SDK to an Existing Spring Application

This guide explains how you can add the [OPA Spring Boot SDK](https://github.com/StyraInc/opa-springboot) to an existing Spring application in order to implement HTTP request authorization. For full usage options, consult the repository [README](https://github.com/StyraInc/opa-springboot?tab=readme-ov-file#sdk-example-usage).

## Overview

The OPA Spring Boot SDK wraps the [OPA Java SDK](https://github.com/StyraInc/opa-java/) with the [`OPAAuthorizationManager`](https://styrainc.github.io/opa-springboot/javadoc/com/styra/opa/springboot/OPAAuthorizationManager.html) class, which implements Spring's [`AuthorizationManager`](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/authorization/AuthorizationManager.html) interface, allowing it to be utilized with [Spring's `HttpSecurity`](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html). In this guide, you will learn:

- How to add the OPA Spring Boot SDK as a dependency for your application.
- How to configure the `OPAAuthorizationManager` type for your application.
- How to add the `OPAAuthorizationManager` to your security configuration.
- How to inject application-specific information into your OPA requests using the [`ContextDataProvider`](https://styrainc.github.io/opa-springboot/javadoc/com/styra/opa/springboot/ContextDataProvider.html) interface.

This guide is intended for developers who are already comfortable working in Java with Spring. If you would like to learn more about spring, consider their [Getting Started](https://docs.spring.io/spring-security/reference/index.html#_getting_started) page.

> [!NOTE]
> Spring's `AuthorizationManager` type has a type parameter allowing that single interface to be used for both [request](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html) and [method](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html) security. At time of writing, `OPAAuthorizationManager` only implements `AuthorizationManager<RequestAuthorizationContext>`, meaning it is not able to support method security yet.

> [!TIP]
> The Spring documentation has a section on [how to adapt legacy `AccessDecisionManager` and `AccessDecisionVoters`](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#authz-voter-adaptation) which may be of interest if your application still utilizes these types.

## Adding the OPA Spring Boot SDK to your Project

Follow the instructions on the [Maven Central Repository page for the `com.styra.opa/springboot`](https://central.sonatype.com/artifact/com.styra.opa/springboot) to add the OPA Spring Boot SDK as a dependency.

Here are two examples of using the OPA Spring Boot SDK in Gradle based projects:

- [`build.gradle` for `opa-springboot-demo`](https://github.com/StyraInc/opa-sdk-demos/blob/main/opa-springboot-demo/after/build.gradle#L27)
- [`build.gradle` for TicketHub demo](https://github.com/StyraInc/styra-demo-tickethub/blob/main/server/springboot/build.gradle#L39)

## Configuring `OPAAuthorizationManager`

Configuration properties are defined in `OPAProperties` and can be set [externally](https://docs.spring.io/spring-boot/reference/features/external-config.html), e.g. via `application.properties`, `application.yaml`, system properties, or environment variables.

Example `application.yaml`:

```yaml
opa:
    url: http://localhost:8182 # OPA server URL. Default is "http://localhost:8181".
    path: foo/bar # Policy path in OPA. Default is null.
    request:
        resource:
            type: stomp_endpoint # Type of the request's resource. Default is "endpoint".
        context:
            type: websocket # Type of the request's context. Default is "http".
        subject:
            type: oauth2_resource_owner # Type of the request's subject. Default is "java_authentication".
    response:
        context:
            reason-key: de # Key to search for decision reasons in the response. Default is "en".
```

> [!TIP]
> A "default decision" corresponds to the `default_decision` OPA configuration key, see OPA's [Configuration doc](https://www.openpolicyagent.org/docs/configuration/) for more information.

Auto-configuration will be done using `OPAAutoConfiguration` with autowired `OPAClient` and `OPAAuthorizationManager` beans, which can be overridden by custom beans if you need to customize their behavior (not just change their configuration).

## Adding `OPAAuthorizationManager` to your Security Configuration

To cause Spring to secure your HTTP APIs using the `OPAAuthorizationManager`, you must add it to your `SecurityFilterChain`. Typically, this happens in a method inside of your security configuration class, which should have the `@Configuration` and `@EnableWebSecurity` annotations. Inside of the method where your `SecurityFilterChain` is created, you should have an [`authorizeHttpRequests`](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/builders/HttpSecurity.html#authorizeHttpRequests()) call where you can use a request [matcher](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/util/matcher/RequestMatcher.html) such as [`.anyRequest()`](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/access/intercept/RequestMatcherDelegatingAuthorizationManager.Builder.html#anyRequest()) with the [`.access()`](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/messaging/access/intercept/MessageMatcherDelegatingAuthorizationManager.Builder.Constraint.html#access(org.springframework.security.authorization.AuthorizationManager)) method to register the authorization manager.

The following code snippet shows a minimal security configuration that only sets up an `OPAAuthorizationManager` and applies it to every request.

```java
package com.example;

import com.styra.opa.springboot.OPAAuthorizationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    OPAAuthorizationManager opaAuthorizationManager;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().access(opaAuthorizationManager));
        // Other security configs
        return http.build();
    }

}
```

Here are some examples of adding an `OPAAuthorizationManager` to a Spring security configuration:

- [`opa-springboot-demo` `WebConfig.java`](https://github.com/StyraInc/opa-sdk-demos/blob/bff96964338e61b88bbfe90029a14bf2039f2d67/opa-springboot-demo/after/src/main/java/com/example/demo/WebConfig.java#L40)
- [TicketHub demo `SecurityConfig.java`](https://github.com/StyraInc/styra-demo-tickethub/blob/7a5b18000734de9a1d41a24290d500857104e1a0/server/springboot/src/main/java/com/styra/tickethub_springboot/dao/model/SecurityConfig.java#L59)

## Injecting Application-Specific Data using OPA Input Customizers

The OPA Spring Boot SDK creates a rich input document with most of the information about each HTTP request included, following the AuthZEN format.  You can find more details [here](../reference/input-output-schema). However, for some advanced use cases, it may be necessary to include additional data in the policy input or override the input schema completely.

The top level `input` contains a tuple of (`subject`, `resource`, `action`, and `context`). To override any of these, the SDK exposes four beans that, when defined, override their corresponding parts of the input schema:

- `OPAInputSubjectCustomizer`
- `OPAInputResourceCustomizer`
- `OPAInputActionCustomizer`
- `OPAInputContextCustomizer`

After applying `input` customization, `input` will be validated to ensure it at least contains these
keys with not-null values:

- `resource.[type, id]`
- `action.name`
- `subject.[type, id]`
- `context.type`, if `context` exists

### OPAInputSubjectCustomizer

Clients could define an `OPAInputSubjectCustomizer` bean to customize the `subject` part of the `input`. `subject` map
must at least contain `type` and `id` keys with not-null values, though their values could be modified.

Example `OPAInputSubjectCustomizer` bean:

```java
import static com.styra.opa.springboot.input.InputConstants.SUBJECT;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_AUTHORITIES;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_TYPE;

@Configuration
public class OPAConfig {
    @Bean
    public OPAInputSubjectCustomizer opaInputSubjectCustomizer() {
        return (authentication, requestAuthorizationContext, subject) -> {
            var customSubject = new HashMap<>(subject);
            customSubject.remove(SUBJECT_AUTHORITIES); // Remove an existing attribute.
            customSubject.put(SUBJECT_TYPE, "oauth2_resource_owner"); // Change an existing attribute.
            customSubject.put("subject_key", "subject_value"); // Add a new attribute.
            return customSubject;
        };
    }
}
```

### OPAInputResourceCustomizer

Clients could define an `OPAInputResourceCustomizer` bean to customize the `resource` part of the `input`. `resource`
map must at least contain `type` and `id` keys with not-null values, though their values could be modified.

Example `OPAInputResourceCustomizer` bean:

```java
import static com.styra.opa.springboot.input.InputConstants.RESOURCE;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE_TYPE;

@Configuration
public class OPAConfig {
    @Bean
    public OPAInputResourceCustomizer opaInputResourceCustomizer() {
        return (authentication, requestAuthorizationContext, resource) -> {
            var customResource = new HashMap<>(resource);
            customResource.put(RESOURCE_TYPE, "stomp_endpoint"); // Change an existing attribute.
            customResource.put("resource_key", "resource_value"); // Add a new attribute.
            return customResource;
        };
    }
}
```

### OPAInputActionCustomizer

Clients could define an `OPAInputActionCustomizer` bean to customize the `action` part of the `input`. `action` map
must at least contain `name` key with a not-null value, though its value could be modified.

Example `OPAInputActionCustomizer` bean:

```java
import static com.styra.opa.springboot.input.InputConstants.ACTION;
import static com.styra.opa.springboot.input.InputConstants.ACTION_HEADERS;
import static com.styra.opa.springboot.input.InputConstants.ACTION_NAME;

@Configuration
public class OPAConfig {
    @Bean
    public OPAInputActionCustomizer opaInputActionCustomizer() {
        return (authentication, requestAuthorizationContext, action) -> {
            var customAction = new HashMap<>(action);
            customAction.remove(ACTION_HEADERS); // Remove an existing attribute.
            customAction.put(ACTION_NAME, "read"); // Change an existing attribute.
            customAction.put("action_key", "action_value"); // Add a new attribute.
            return customAction;
        };
    }
}
```

### OPAInputContextCustomizer

Clients could define an `OPAInputContextCustomizer` bean to customize the `context` part of the `input`. `context` map
could be null; however if it is not-null, it must at least contain `type` key with a not-null value, though its value
could be modified.

Example `OPAInputContextCustomizer` bean which makes `context` null (removes it from `input` map):

```java
import static com.styra.opa.springboot.input.InputConstants.CONTEXT;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_TYPE;

@Configuration
public class OPAConfig {
    @Bean
    public OPAInputContextCustomizer opaInputContextCustomizer() {
        return (authentication, requestAuthorizationContext, context) -> null;
    }
}
```
