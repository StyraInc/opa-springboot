package com.styra.opa.springboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OPAProperties {
    public static final String DEFAULT_URL = "http://localhost:8181";
    public static final String DEFAULT_REASON_KEY = "en";

    private String url = DEFAULT_URL;
    private String path;
    private String reasonKey = DEFAULT_REASON_KEY;
    private Request request = new Request();

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

            private String type = DEFAULT_TYPE;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Context {
            public static final String DEFAULT_TYPE = "http";

            private String type = DEFAULT_TYPE;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Subject {
            public static final String DEFAULT_TYPE = "java_authentication";

            private String type = DEFAULT_TYPE;
        }
    }
}
