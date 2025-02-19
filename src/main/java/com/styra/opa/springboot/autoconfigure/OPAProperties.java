package com.styra.opa.springboot.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for OPA authorization support.
 */
@ConfigurationProperties(prefix = "opa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OPAProperties {
    public static final String DEFAULT_URL = "http://localhost:8181";

    /**
     * URL of the OPA server. Default is {@value DEFAULT_URL}.
     */
    private String url = DEFAULT_URL;
    /**
     * Policy path in OPA. Default is null.
     */
    private String path;
    private Request request = new Request();
    private Response response = new Response();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        private Resource resource = new Resource();
        private Context context = new Context();
        private Subject subject = new Subject();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Resource {
            public static final String DEFAULT_TYPE = "endpoint";

            /**
             * Type of the resource. Default is {@value DEFAULT_TYPE}.
             */
            private String type = DEFAULT_TYPE;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Context {
            public static final String DEFAULT_TYPE = "http";

            /**
             * Type of the context. Default is {@value DEFAULT_TYPE}.
             */
            private String type = DEFAULT_TYPE;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Subject {
            public static final String DEFAULT_TYPE = "java_authentication";

            /**
             * Type of the subject. Default is {@value DEFAULT_TYPE}.
             */
            private String type = DEFAULT_TYPE;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private Context context = new Context();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Context {
            public static final String DEFAULT_REASON_KEY = "en";

            /**
             * Key to search for decision reasons in the response. Default is {@value DEFAULT_REASON_KEY}.
             *
             * @see <a href="https://openid.github.io/authzen/#reason-field">AuthZEN Reason Field</a>
             */
            private String reasonKey = DEFAULT_REASON_KEY;
        }
    }
}
