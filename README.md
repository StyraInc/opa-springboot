# OPA Spring Boot SDK

> [!IMPORTANT]
> The documentation for this SDK lives at [https://docs.styra.com/sdk](https://docs.styra.com/sdk), with reference documentation available at [https://styrainc.github.io/opa-springboot/javadoc](https://styrainc.github.io/opa-springboot/javadoc)

You can use the Styra OPA Spring Boot SDK to connect [Open Policy Agent](https://www.openpolicyagent.org/) and [Enterprise OPA](https://www.styra.com/enterprise-opa/) deployments to your [Spring Boot](https://spring.io/projects/spring-boot) applications using the included [AuthorizationManager](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#_the_authorizationmanager) implementation.

> [!IMPORTANT]
> Would you prefer a plain Java API instead of Spring Boot? Check out the [OPA Java SDK](https://github.com/StyraInc/opa-java).

## SDK Installation

This package is published on Maven Central as [`com.styra.opa:springboot`](https://central.sonatype.com/artifact/com.styra.opa/springboot). The Maven Central page includes up-to-date instructions to add it as a dependency to your Java project, tailored to a variety of build systems including Maven and Gradle.

If you wish to build from source and publish the SDK artifact to your local Maven repository (on your filesystem) then use the following command (after cloning the git repo locally):

On Linux/MacOS:

```shell
./gradlew publishToMavenLocal -Pskip.signing
```

On Windows:

```shell
gradlew.bat publishToMavenLocal -"Pskip.signing"
```

## SDK Example Usage
### OPAAuthorizationManager
Using `OPAAuthorizationManager`, HTTP requests could be authorized:

```java
import com.styra.opa.springboot.OPAAuthorizationManager;

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
Auto-configuration will be done using `OPAAutoConfiguration`. If any customization be needed, custom `OPAClient`
or `OPAAuthorizationManager` beans could be defined by clients.

### OPAClient
A custom `OPAClient` bean could be defined to send custom headers to the OPA server, or using custom
`com.styra.opa.openapi.utils.HTTPClient`, such as:

```java
import com.styra.opa.OPAClient;

@Configuration
public class OPAConfig {

    @Bean
    public OPAClient opaClient(OPAProperties opaProperties) {
        var headers = Map.ofEntries(entry("Authorization", "Bearer secret"));
        return new OPAClient(opaProperties.getUrl(), headers);
    }
}
```

### OPAProperties
Configuration properties are defined in `OPAProperties` and can be set
[externally](https://docs.spring.io/spring-boot/reference/features/external-config.html), e.g. via
`application.properties`, `application.yaml`, system properties, or environment variables.

Example `application.yaml` to modify properties:
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
    authorization-event:
        denied:
            enabled: false # Whether to publish an AuthorizationDeniedEvent when a request is denied. Default is true.
        granted:
            enabled: true # Whether to publish an AuthorizationGrantedEvent when a request is granted. Default is false.
```

### OPAPathSelector
By default, OPAAuthorizationManager does not use any path when calling OPA (evaluating policies). Clients could define
an `OPAPathSelector` bean, which could select paths based on the `Authentication`, `RequestAuthorizationContext`, or
opaInput `Map`.

Example `OPAPathSelector` bean:
```java
@Configuration
public class OPAConfig {
    @Bean
    public OPAPathSelector opaPathSelector() {
        return (authentication, requestAuthorizationContext, opaInput) -> {
            String httpRequestPath = requestAuthorizationContext.getRequest().getServletPath();
            if (httpRequestPath.startsWith("/foo")) {
                return "foo/main";
            } else if (httpRequestPath.startsWith("/bar")) {
                return "bar/main";
            } else {
                return "default/main";
            }
        };
    }
}
```

### OPAInput*Customizers
OPA `input` is a `Map<String, Object>` which will be sent to the
[Get a Document (with Input) endpoint](https://www.openpolicyagent.org/docs/latest/rest-api/#get-a-document-with-input)
as the `input` field in the request body and is accessible in OPA policies as the
[input variable](https://www.openpolicyagent.org/docs/latest/philosophy/#the-opa-document-model). To enable clients to
customize different parts of `input` (`subject`, `resource`, `action`, and `context`), `OPAInput*Customizer` beans
could be defined. After applying `input` customization, `input` will be validated to ensure it at least contains these
keys with not-null values:
- `resource.[type, id]`
- `action.name`
- `subject.[type, id]`
- `context.type`, if `context` exists

#### 1. OPAInputSubjectCustomizer
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

#### 2. OPAInputResourceCustomizer
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

#### 3. OPAInputActionCustomizer
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

#### 4. OPAInputContextCustomizer
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

### Authorization Events
Spring-Security supports
[Authorization Events](https://docs.spring.io/spring-security/reference/servlet/authorization/events.html) which could
be used to publish events when a request is authorized or denied. By following the spring-security convention,
`OPAAuthorizationEventPublisher` publishes `AuthorizationDeniedEvent` when a request is denied, however does not
publish `AuthorizationGrantedEvent` when a request is granted (since it could be quite noisy). Clients could change
this behavior via `opa.authorization-event.denied.enabled` and `opa.authorization-event.granted.enabled` properties.

Emitted `AuthorizationDeniedEvent` and `AuthorizationGrantedEvent` contain `OPAAuthorizationDecision` which has a
reference to the corresponding `OPAResponse` and clients could access the response returned from the OPA server.

In order to listen to these events, clients could annotate a method with `@EventListener` in a bean, such as:
```java
import org.springframework.context.event.EventListener;

@Component
public class OPAAuthorizationEventListener {

    @EventListener
    public void onDeny(AuthorizationDeniedEvent denied) {
        // ...
    }

    @EventListener
    public void onGrant(AuthorizationGrantedEvent granted) {
        // ...
    }
}
```

## Policy Input/Output Schema

Documentation for the required input and output schema of policies used by the OPA Spring Boot SDK can be found [here](https://docs.styra.com/sdk/springboot/reference/input-output-schema).

## Build Instructions

**To build the SDK**, use `./gradlew build`, the resulting JAR will be placed in `./build/libs/api.jar`.

**To build the documentation** site, including JavaDoc, run `./scripts/build_docs.sh OUTPUT_DIR`. You should replace `OUTPUT_DIR` with a directory on your local system where you would like the generated docs to be placed. You can also preview the documentation site ephemerally using `./scripts/serve_docs.sh`, which will serve the docs on `http://localhost:8000` until you use Ctrl+C to exit the script.

**To run the unit tests**, you can use `./gradlew test`.

**To run the linter**, you can use `./gradlew lint`.

## Community

For questions, discussions and announcements related to Styra products, services and open source projects, please join
the Styra community on [Slack](https://communityinviter.com/apps/styracommunity/signup)!

## Development

For development docs, see [DEVELOPMENT.md](./DEVELOPMENT.md).
