# OPA-SpringBoot

> [!IMPORTANT]
> **Under Construction!** This repository is a work in progress. Bear with us while we get things ready for prime time.

TODO: docs links

You can use the Styra OPA-SpringBoot SDK to connect [Open Policy Agent](https://www.openpolicyagent.org/) and [Enterprise OPA](https://www.styra.com/enterprise-opa/) deployments to your [Spring Boot](https://spring.io/projects/spring-boot) applications using the included [AuthorizationManager](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#_the_authorizationmanager) implementation. The policy inputs and outputs follow the [AuthZEN specification](https://openid.github.io/authzen).

> [!IMPORTANT]
> Would you prefer a plain Java API instead of Spring Boot? Check out [OPA-Java](https://github.com/StyraInc/opa-java).

## SDK Installation

This package is published on Maven Central as [`com.styra.opa/springboot`](https://central.sonatype.com/artifact/com.styra.opa/springboot). The Maven Central page includes up-to-date instructions to add it as a dependency to your Java project, tailored to a variety of build systems including Maven and Gradle.

If you wish to build from source and publish the SDK artifact to your local Maven repository (on your filesystem) then use the following command (after cloning the git repo locally):

On Linux/MacOS:

```
./gradlew publishToMavenLocal -Pskip.signing
```

On Windows:

```
gradlew.bat publishToMavenLocal -Pskip.signing
```

## SDK Example Usage (high-level)


```java
// ... 

import com.styra.opa.springboot.OPAAuthorizationManager;
import com.styra.opa.OPAClient;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        String opaURL = "http://localhost:8181";
        String opaURLEnv = System.getenv("OPA_URL");
        if (opaURLEnv != null) {
            opaURL = opaURLEnv;
        }
        OPAClient opa = new OPAClient(opaURL);

        AuthorizationManager<RequestAuthorizationContext> am = new OPAAuthorizationManager(opa, "tickets/spring/main");

        http.authorizeHttpRequests(authorize -> authorize.anyRequest().access(am));

        return http.build();
    }

}

```

## Policy Inputs & Outputs

In order to make OPA-SpringBoot compatible with [AuthZEN](https://openid.github.io/authzen), the policy inputs should be structured according to the following table:

| JSON Path                   | Description |
|-----------------------------|-------------|
| `input.subject.type`        | Constant string `java_authentication` |
| `input.subject.id`          | Spring authN [principal](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/Authentication.html#getPrincipal()) |
| `input.subject.details`     | Spring authN [details](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/Authentication.html#getDetails()) |
| `input.subject.authorities` | Spring authN [authorities](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/Authentication.html#getAuthorities()) |
| `input.resource.type`       | Constant string `endpoint` |
| `input.resource.id`         | Endpoint [servlet path](https://javadoc.io/static/jakarta.servlet/jakarta.servlet-api/5.0.0/jakarta/servlet/http/HttpServletRequest.html#getServletPath--) |
| `input.action.name`         | HTTP request method |
| `input.action.protocol`     | HTTP protocol for request |
| `input.action.headers`      | HTTP headers for request |
| `input.context.type`        | Constant string `http` |
| `input.context.host`        | HTTP remote host of request |
| `input.context.ip`          | HTTP remote IP of request |
| `input.context.port`        | HTTP remote port for request |
| `input.context.data`        | Optional supplemental data you can inject using a `ContextDataProvider` implementation |

... and the policy outputs must be structured according to the following table:

| JSON Path           | Description |
|---------------------|-------------|
| `output.decision`   | `true` if and only if the request should be allowed to proceed, else `false` |
| `output.context.id` | AuthZEN [Reason Object](https://openid.github.io/authzen/#name-reason-object) ID |
| `output.context.reason_admin` | AuthZEN [Reason Field Object](https://openid.github.io/authzen/#reason-field), for administrative use |
| `output.context.reason_user` | AuthZEN [Reason Field Object](https://openid.github.io/authzen/#reason-field), for user-facing error messages |
| `output.context.data` | Optional supplemental data provided by your OPA policy |

## Build Instructions

**To build the SDK**, use `./gradlew build`, the resulting JAR will be placed in `./build/libs/api.jar`.

**To build the documentation** site, including JavaDoc, run `./scripts/build_docs.sh OUTPUT_DIR`. You should replace `OUTPUT_DIR` with a directory on your local system where you would like the generated docs to be placed. You can also preview the documentation site ephemerally using `./scripts/serve_docs.sh`, which will serve the docs on `http://localhost:8000` until you use Ctrl+C to exit the script.

**To run the unit tests**, you can use `./gradlew test`.

**To run the linter**, you can use `./gradlew lint`

## Community

For questions, discussions and announcements related to Styra products, services and open source projects, please join
the Styra community on [Slack](https://communityinviter.com/apps/styracommunity/signup)!

## Development

For development docs, see [DEVELOPMENT.md](./DEVELOPMENT.md).
