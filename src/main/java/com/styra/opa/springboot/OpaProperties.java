package com.styra.opa.springboot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpaProperties {
    public static final String DEFAULT_URL = "http://localhost:8181";
    public static final String DEFAULT_REASON_KEY = "en";

    private String url = DEFAULT_URL;
    private String path;
    private String reasonKey = DEFAULT_REASON_KEY;
    private Request request = new Request();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        private Resource resource = new Resource();
        private Context context = new Context();
        private Subject subject = new Subject();

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Resource {
            public static final String DEFAULT_TYPE = "endpoint";

            @Builder.Default
            private String type = DEFAULT_TYPE;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Context {
            public static final String DEFAULT_TYPE = "http";

            @Builder.Default
            private String type = DEFAULT_TYPE;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Subject {
            public static final String DEFAULT_TYPE = "java_authentication";

            @Builder.Default
            private String type = DEFAULT_TYPE;
        }
    }
}
