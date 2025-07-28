# OPA Spring Boot SDK Policy Input/Output Schema

The OPA Spring Boot SDK makes calls to Enterprise OPA or Open Policy Agent to request an authorization decision.

The policy that processes these authorization decision requests must know the structure of the input given by OPA Spring Boot, and must return an appropriately structured output.

The following is a reference for these schemas:

## Endpoint Authorization

With endpoint authorization, the OPA Spring Boot SDK sends an authorization request on every call to an API endpoint.

### Input

| Parameter       | Type   | Value          | Description |
| --------------- | ------ | ---------------- | --- |
| `input.resource.type`       | String | `endpoint` | A constant describing the type of resource being accessed. |
| `input.resource.id`         | String | Endpoint [servlet path](https://javadoc.io/static/jakarta.servlet/jakarta.servlet-api/5.0.0/jakarta/servlet/http/HttpServletRequest.html#getServletPath--) |
| `input.action.name`         | String | `GET`, `POST`, `PUT`, `PATCH`, `HEAD`, `OPTIONS`, `TRACE`, or `DELETE` | HTTP request method |
| `input.action.protocol`     | String | HTTP protocol for request, e.g. `HTTP 1.1` | |
| `input.action.headers`      | Map[String, Any] | HTTP headers of request | Not guaranteed to be present. |
| `input.context.type`        | String | `http` | A constant describing the type of contextual information provided |
| `input.context.host`        | String | HTTP remote host of request | |
| `input.context.ip`          | String | HTTP remote IP of request | |
| `input.context.port`        | String | HTTP remote port for request | |
| `input.context.data`        | Map[String, Any] | | Optional supplemental data you can inject using a `ContextDataProvider` implementation |
| `input.subject.type`        | String | `java_authentication` | A constant describing the kind of subject being provided. |
| `input.subject.id`          | String | Spring authN [principal](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/Authentication.html#getPrincipal()) | ID representing the subject being authorized. |
| `input.subject.details`     | String | Spring authN [details](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/Authentication.html#getDetails()) | |
| `input.subject.authorities` | String | Spring authN [authorities](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/Authentication.html#getAuthorities()) |

### Output

| Parameter       | Type   | Required |Description |
| --------------- | ------ | ---------| --- |
| `output.decision`   | Boolean. `true` if and only if the request should be allowed to proceed, else `false` | Yes | The decision of the authorization request |
| `output.context.id` | String | Yes | AuthZEN [Reason Object](https://openid.github.io/authzen/#name-reason-object) ID |
| `output.context.reason_admin` | Map[String, String] | No |  AuthZEN [Reason Field Object](https://openid.github.io/authzen/#reason-field), for administrative use |
| `output.context.reason_user` | Map[String, String] | No | AuthZEN [Reason Field Object](https://openid.github.io/authzen/#reason-field), for user-facing error messages |
| `output.context.data` | Map[String, Any] | No | Optional supplemental data provided by your OPA policy |
