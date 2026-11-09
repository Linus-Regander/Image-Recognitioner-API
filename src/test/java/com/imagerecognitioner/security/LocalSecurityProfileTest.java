package com.imagerecognitioner.security;

import com.imagerecognitioner.controller.ImageController;
import com.imagerecognitioner.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ensures local-profile requests are not anonymous unless explicitly enabled. */
@WebMvcTest(controllers = ImageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("local")
@TestPropertySource(properties = "app.security.local-auth-enabled=false")
class LocalSecurityProfileTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    @Test
    void localProfileDoesNotBypassAuthenticationWhenOptInIsDisabled() throws Exception {
        mockMvc.perform(get("/api/images/metadata"))
                .andExpect(status().isUnauthorized());
    }
}
