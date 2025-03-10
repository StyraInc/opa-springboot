package com.styra.opa.springboot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class models the data to be returned from an OPA policy under the {@code context} key.
 * <br/><br/>
 * This corresponds to the {@code Context} object in the
 * <a href="https://openid.github.io/authzen/#section-5.4">AuthZen spec</a>.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OPAResponseContext {

    private String id;
    @JsonProperty("reason_admin")
    private Map<String, String> reasonAdmin;
    @JsonProperty("reason_user")
    private Map<String, String> reasonUser;
    /**
     * The extra {@code data} field allows for the OPA policy to pass back arbitrary structured data in addition to the
     * expected reason information.
     */
    private Map<String, Object> data;

    /**
     * This method selects an appropriate reason to use for creating {@link AuthorizationDecision}s. Currently, it will
     * select the search key if it is present in the {@code reasonUser}, and if not it will select the key which sorts
     * lexicographically first from the {@code reasonUser}. It will not consider data in the {@code reasonAdmin}.
     */
    public String getReasonForDecision(String searchKey) {
        if (reasonUser == null) {
            return null;
        }

        if (reasonUser.containsKey(searchKey)) {
            return reasonUser.get(searchKey);
        }

        List<String> keys = new ArrayList<>(reasonUser.keySet());
        Collections.sort(keys);
        return !keys.isEmpty() ? reasonUser.get(keys.get(0)) : null;
    }
}
