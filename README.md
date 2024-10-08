# OPA Spring Boot SDK

> [!IMPORTANT]
> The documentation for this SDK lives at [https://docs.styra.com/sdk](https://docs.styra.com/sdk), with reference documentation available at [https://styrainc.github.io/opa-springboot/javadoc](https://styrainc.github.io/opa-springboot/javadoc)

You can use the Styra OPA Spring Boot SDK to connect [Open Policy Agent](https://www.openpolicyagent.org/) and [Enterprise OPA](https://www.styra.com/enterprise-opa/) deployments to your [Spring Boot](https://spring.io/projects/spring-boot) applications using the included [AuthorizationManager](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#_the_authorizationmanager) implementation.

> [!IMPORTANT]
> Would you prefer a plain Java API instead of Spring Boot? Check out the [OPA Java SDK](https://github.com/StyraInc/opa-java).

## SDK Installation

This package is published on Maven Central as [`com.styra.opa/springboot`](https://central.sonatype.com/artifact/com.styra.opa/springboot). The Maven Central page includes up-to-date instructions to add it as a dependency to your Java project, tailored to a variety of build systems including Maven and Gradle.

If you wish to build from source and publish the SDK artifact to your local Maven repository (on your filesystem) then use the following command (after cloning the git repo locally):

On Linux/MacOS:

```shell
./gradlew publishToMavenLocal -Pskip.signing
```

On Windows:

```shell
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

## Policy Input/Output Schema

Documentation for the required input and output schema of policies used by the OPA Spring Boot SDK can be found [here](https://docs.styra.com/sdk/springboot/reference/input-output-schema)

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
