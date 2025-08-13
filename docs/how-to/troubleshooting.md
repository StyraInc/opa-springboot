# How To Troubleshoot the OPA Spring Boot SDK

## Enabling Logging

The OPA Spring Boot SDK uses Spring's built in logging facilities. To get the most verbose logging from the OPA Spring Boot SDK, add the following to your `application.properties` file:

```properties
logging.level.com.styra.opa.springboot=TRACE
```

> [!NOTE]
> Is there something you'd like to see the OPA Spring Boot SDK include in its logs? Feel free to open an issue [on the issue tracker](https://github.com/StyraInc/opa-springboot/issues).

## OPA Connectivity Issues

If the dependent OPA Java SDK is not able to connect to an OPA you may see exceptions similar to the ones below:

```javastacktrace
java.net.ConnectException
        at java.net.http/jdk.internal.net.http.HttpClientImpl.send(HttpClientImpl.java:573)
        at java.net.http/jdk.internal.net.http.HttpClientFacade.send(HttpClientFacade.java:123)
        at com.styra.opa.openapi.utils.SpeakeasyHTTPClient.send(SpeakeasyHTTPClient.java:20)
        at com.styra.opa.openapi.OpaApiClient.executePolicyWithInput(OpaApiClient.java:508)
        at com.styra.opa.openapi.models.operations.ExecutePolicyWithInputRequestBuilder.call(ExecutePolicyWithInputRequestBuilder.java:37)
        at com.styra.opa.OPAClient.executePolicy(OPAClient.java:536)
        at com.styra.opa.OPAClient.evaluateMachinery(OPAClient.java:683)
        at com.styra.opa.OPAClient.evaluate(OPAClient.java:354)
        at com.styra.opa.springboot.OPAAuthorizationManager.opaRequest(OPAAuthorizationManager.java:254)
        at com.styra.opa.springboot.OPAAuthorizationManager.check(OPAAuthorizationManager.java:275)
...
```

```javastacktrace
com.styra.opa.OPAException: executing policy at 'Optional[demo/main]' with failed due to exception 'java.net.ConnectException'
        at com.styra.opa.OPAClient.executePolicy(OPAClient.java:547) ~[opa-1.4.1.jar:na]
        at com.styra.opa.OPAClient.evaluateMachinery(OPAClient.java:683) ~[opa-1.4.1.jar:na]
        at com.styra.opa.OPAClient.evaluate(OPAClient.java:354) ~[opa-1.4.1.jar:na]
        at com.styra.opa.springboot.OPAAuthorizationManager.opaRequest(OPAAuthorizationManager.java:254) ~[springboot-0.0.4-plain.jar:na]
        at com.styra.opa.springboot.OPAAuthorizationManager.check(OPAAuthorizationManager.java:275) ~[springboot-0.0.4-plain.jar:na]
        at com.styra.opa.springboot.OPAAuthorizationManager.check(OPAAuthorizationManager.java:33) ~[springboot-0.0.4-plain.jar:na]
        at org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager.check(RequestMatcherDelegatingAuthorizationManager.java:87) ~[spring-security-web-6.3.1.jar:6.3.1]
...
```

If you encounter these types of errors, this typically indicates that the SDK was not able to reach OPA. This can have several causes:

- OPA is running, but the OPA URL the SDK is using is not correct. See [_Configuring `OPAAuthorizationManager`_](./add-sdk#configuring-opaauthorizationmanager) for more information about how to configure the OPA URL.
- OPA is not running. You may have forgotten to start it, or it may have failed to start due to a bad configuration, syntax errors in your policy, etc.
- OPA is running and the OPA URL is configured correctly, but a network problem is preventing the SDK from communicating with OPA.

## Malformed Policy Outputs

If your OPA policy does not correctly follow the output schema described [here](../reference/input-output-schema), the SDK will not be able to interpret the policy decisions. This may result in errors similar to the ones below:

```javastacktrace
java.lang.IllegalArgumentException: Cannot construct instance of `com.styra.opa.springboot.OPAResponse` (although at least one Creator exists): no boolean/Boolean-argument constructor/factory method to deserialize from boolean value (false)
 at [Source: UNKNOWN; byte offset: #UNKNOWN]
        at com.fasterxml.jackson.databind.ObjectMapper._convert(ObjectMapper.java:4624) ~[jackson-databind-2.17.0.jar:2.17.0]
        at com.fasterxml.jackson.databind.ObjectMapper.convertValue(ObjectMapper.java:4565) ~[jackson-databind-2.17.0.jar:2.17.0]
        at com.styra.opa.OPAClient.evaluateMachinery(OPAClient.java:686) ~[opa-1.4.1.jar:na]
        at com.styra.opa.OPAClient.evaluate(OPAClient.java:354) ~[opa-1.4.1.jar:na]
        at com.styra.opa.springboot.OPAAuthorizationManager.opaRequest(OPAAuthorizationManager.java:254) ~[springboot-0.0.4-plain.jar:na]
        at com.styra.opa.springboot.OPAAuthorizationManager.check(OPAAuthorizationManager.java:275) ~[springboot-0.0.4-plain.jar:na]
        at com.styra.opa.springboot.OPAAuthorizationManager.check(OPAAuthorizationManager.java:33) ~[springboot-0.0.4-plain.jar:na]
        at org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager.check(RequestMatcherDelegatingAuthorizationManager.java:87) ~[spring-security-web-6.3.1.jar:6.3.1]
...
```

In the above sample, the SDK was configured to access a rule which evaluated to a boolean value. Don't be fooled by `no boolean/Boolean-argument constructor/factory`, this isn't a Jackson serialization issue, but instead a failure to deserialize a boolean into an [`OPAResponse`](https://styrainc.github.io/opa-springboot/javadoc/com/styra/opa/springboot/OPAResponse.html).

```javastacktrace
java.lang.IllegalArgumentException: Unrecognized field "acmecorp" (class com.styra.opa.springboot.OPAResponse), not marked as ignorable (2 known properties: "decision", "context"])
 at [Source: UNKNOWN; byte offset: #UNKNOWN] (through reference chain: com.styra.opa.springboot.OPAResponse["acmecorp"])
        at com.fasterxml.jackson.databind.ObjectMapper._convert(ObjectMapper.java:4624) ~[jackson-databind-2.17.0.jar:2.17.0]
        at com.fasterxml.jackson.databind.ObjectMapper.convertValue(ObjectMapper.java:4565) ~[jackson-databind-2.17.0.jar:2.17.0]
        at com.styra.opa.OPAClient.evaluateMachinery(OPAClient.java:686) ~[opa-1.4.1.jar:na]
        at com.styra.opa.OPAClient.evaluate(OPAClient.java:354) ~[opa-1.4.1.jar:na]
        at com.styra.opa.springboot.OPAAuthorizationManager.opaRequest(OPAAuthorizationManager.java:254) ~[springboot-0.0.4-plain.jar:na]
...
```

In the above sample, the SDK was configured to access a rule which returned a JSON object, but with incorrect fields. In this case, `acmecorp` isn't a field that is expected when deserializing an `OPAResponse`.

If you see these types of errors, then you should closely look at the output your policy is sending to the SDK. Your decision logs are a good place to start looking.

> [!TIP]
> An easy way to have OPA dump human-readable decision logs to the console is to add the `--log-format=text --log-level=error --set decision_logs.console=true` arguments to your `opa run -s` command.
