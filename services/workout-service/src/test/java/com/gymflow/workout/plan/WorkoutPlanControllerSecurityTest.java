package com.gymflow.workout.plan;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gymflow.workout.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkoutPlanController.class)
@Import(SecurityConfiguration.class)
class WorkoutPlanControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean WorkoutPlanService service;
    @MockBean JwtDecoder jwtDecoder;

    @Test void unauthenticatedRequestUsesProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/me/plans"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test void gymAdminCannotReadStudentPlans() throws Exception {
        mvc.perform(get("/api/v1/me/plans").with(jwt().jwt(token -> token.subject("admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_GYM_ADMIN"))))
            .andExpect(status().isForbidden());
    }
}
