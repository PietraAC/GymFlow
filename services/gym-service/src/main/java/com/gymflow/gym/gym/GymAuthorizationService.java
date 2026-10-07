package com.gymflow.gym.gym;

import com.gymflow.gym.shared.error.ForbiddenOperationException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GymAuthorizationService {
    private final GymAdminMembershipRepository memberships;

    public GymAuthorizationService(GymAdminMembershipRepository memberships) {
        this.memberships = memberships;
    }

    public boolean isAdmin(UUID gymId, String subject) {
        return memberships.existsByGymIdAndIdentitySubject(gymId, subject);
    }

    public void requireAdmin(UUID gymId, String subject) {
        if (!isAdmin(gymId, subject)) {
            throw new ForbiddenOperationException("O administrador não possui vínculo com esta academia");
        }
    }
}

