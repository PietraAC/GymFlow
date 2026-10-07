package com.gymflow.gym.gym;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.gym.shared.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GymController.class)
@Import(SecurityConfiguration.class)
class GymControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean GymApplicationService service;
    @MockBean JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedRequestIsRejectedWithProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/gyms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nova academia\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void studentCannotCreateGym() throws Exception {
        mvc.perform(post("/api/v1/gyms")
                .with(jwt().jwt(token -> token.subject("student-a").audience(java.util.List.of("gym-service")))
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nova academia\"}"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
