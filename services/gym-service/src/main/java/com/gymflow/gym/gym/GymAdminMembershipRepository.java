package com.gymflow.gym.gym;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymAdminMembershipRepository extends JpaRepository<GymAdminMembership, UUID> {
    boolean existsByGymIdAndIdentitySubject(UUID gymId, String identitySubject);
    List<GymAdminMembership> findByIdentitySubject(String identitySubject);
}

