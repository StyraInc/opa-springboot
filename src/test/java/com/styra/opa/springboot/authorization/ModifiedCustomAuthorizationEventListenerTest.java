package com.styra.opa.springboot.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import static com.styra.opa.springboot.input.InputConstants.ACTION;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
    "opa.authorization-event.denied.enabled=false",
    "opa.authorization-event.granted.enabled=true"
})
public class ModifiedCustomAuthorizationEventListenerTest extends BaseAuthorizationEventListenerTest {

    @WithMockUser(username = "denied_user")
    @Test
    public void testDisabledAuthorizationDeniedEvent() throws Exception {
        getMockMvc().perform(get("/test/hello"))
            .andExpect(status().isForbidden());

        assertThat(getAuthorizationEventListener().getLastAuthorizationDeniedEvent()).isNull();
    }

    @WithMockUser(username = "granted_user")
    @Test
    public void testDefaultAuthorizationGrantedEvent() throws Exception {
        getMockMvc().perform(get("/test/hello"))
            .andExpect(status().isOk());

        assertThat(getAuthorizationEventListener().getLastAuthorizationGrantedEvent()).isNotNull();
        assertThat(getAuthorizationEventListener().getLastAuthorizationGrantedEvent().getAuthorizationDecision())
            .isInstanceOf(OPAAuthorizationDecision.class);
        var opaResponse = ((OPAAuthorizationDecision) getAuthorizationEventListener().getLastAuthorizationGrantedEvent()
            .getAuthorizationDecision()).getOpaResponse();
        assertThat(opaResponse.getDecision()).isEqualTo(true);
        assertThat(opaResponse.getContext()).isNotNull();
        assertThat(opaResponse.getContext().getData().get(SUBJECT)).isNotNull();
        assertThat(opaResponse.getContext().getData().get(RESOURCE)).isNotNull();
        assertThat(opaResponse.getContext().getData().get(ACTION)).isNotNull();
        assertThat(opaResponse.getContext().getData().get(CONTEXT)).isNotNull();
    }
}
