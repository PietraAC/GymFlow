package com.gymflow.gym.gym;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gymflow.gym.shared.error.ForbiddenOperationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GymAuthorizationServiceTest {
    @Mock GymAdminMembershipRepository memberships;

    @Test
    void administratorFromGymACannotAdministerGymB() {
        UUID gymB = UUID.randomUUID();
        when(memberships.existsByGymIdAndIdentitySubject(gymB, "admin-a")).thenReturn(false);

        GymAuthorizationService service = new GymAuthorizationService(memberships);

        assertThatThrownBy(() -> service.requireAdmin(gymB, "admin-a"))
            .isInstanceOf(ForbiddenOperationException.class)
            .hasMessageContaining("não possui vínculo");
    }
}

