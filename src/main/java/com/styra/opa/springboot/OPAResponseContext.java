package com.styra.opa.springboot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class models the data to be returned from an OPA-SpringBoot policy
 * under the context key.
 *
 * This corresponds to the Context object in the AuthZen spec, see:
 * https://openid.github.io/authzen/#section-5.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OPAResponseContext {

    private String id;
    private Map<String, String> reasonAdmin;
    private Map<String, String> reasonUser;
    private Map<String, Object> data;

    public String getId() {
        return this.id;
    }

    @JsonProperty("id")
    public void setId(String newId) {
        this.id = newId;
    }

    public Map<String, String> getReasonAdmin() {
        return this.reasonAdmin;
    }

    @JsonProperty("reason_admin")
    public void setReasonAdmin(Map<String, String> newReasonAdmin) {
        this.reasonAdmin = newReasonAdmin;
    }

    public Map<String, String> getReasonUser() {
        return this.reasonUser;
    }

    @JsonProperty("reason_user")
    public void setReasonUser(Map<String, String> newReasonUser) {
        this.reasonUser = newReasonUser;
    }

    /**
     * The extra 'data' field allows for the OPA policy to pass back arbitrary
     * structured data in addition to the expected reason information.
     */
    public void setData(Map<String, Object> newData) {
        this.data = newData;
    }

    @JsonProperty("data")
    public Map<String, Object> getData() {
        return this.data;
    }

    /**
     * This method selects an appropriate reason to use for creating Spring
     * authorization decisions. Currently, it will select the search key if it
     * is present in the reason_user object, and if not it will select the key
     * which sorts first from reason_user. It will not consider data in
     * reason_admin.
     */
    public String getReasonForDecision(String searchKey) {
        if (this.reasonUser == null) {
            return null;
        }

        if (this.reasonUser.containsKey(searchKey)) {
            return this.reasonUser.get(searchKey);
        }

        List<String> keys = new ArrayList<>(this.reasonUser.keySet());
        Collections.sort(keys);

        if (keys.isEmpty()) {
            return null;
        }

        return this.reasonUser.get(keys.get(0));
    }
}
