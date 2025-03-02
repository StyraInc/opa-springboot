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
import com.styra.opa.OPAClient;

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
```
### OPAPathSelector
By default, OPAAuthorizationManager does not use any path when calling OPA (evaluating policies). Clients could define
an `OPAPathSelector` bean, which could select paths based on the `Authentication`, `RequestAuthorizationContext`, or
opaRequestBody `Map`.

Example `OPAPathSelector` bean:
```java
@Configuration
public class OPAConfig {
    @Bean
    public OPAPathSelector opaPathSelector() {
        return (authentication, requestAuthorizationContext, opaRequestBody) -> {
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
