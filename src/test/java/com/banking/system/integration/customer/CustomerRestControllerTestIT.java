package com.banking.system.integration.customer;

import com.banking.system.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerRestControllerTestIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnNotFound_whenRequestingOwnProfile_withProperAuthority_butNoCustomer() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/customers/me")
                .with(user(userId.toString())
                        .authorities(new SimpleGrantedAuthority("CUSTOMER_VIEW_OWN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbidden_whenRequestingOwnProfile_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFound_whenRequestingOwnProfile_withInvalidUserId() throws Exception {
        UUID invalidUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/customers/me")
                .with(user(invalidUserId.toString())
                        .authorities(new SimpleGrantedAuthority("CUSTOMER_VIEW_OWN"))))
                .andExpect(status().isNotFound());
    }
}
