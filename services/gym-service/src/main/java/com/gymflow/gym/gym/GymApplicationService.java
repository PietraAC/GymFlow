package com.gymflow.gym.gym;

import com.gymflow.gym.shared.api.PageRequestFactory;
import com.gymflow.gym.shared.api.PageResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GymApplicationService {
    private static final Set<String> SORTS = Set.of("name", "createdAt");
    private final GymRepository gyms;
    private final GymAdminMembershipRepository memberships;

    public GymApplicationService(GymRepository gyms, GymAdminMembershipRepository memberships) {
        this.gyms = gyms;
        this.memberships = memberships;
    }

    @Transactional
    public GymModels.GymResponse create(GymModels.CreateGymRequest request, String subject) {
        Gym gym = gyms.save(new Gym(request.name().trim(), request.active() == null || request.active()));
        memberships.save(new GymAdminMembership(gym.getId(), subject));
        return GymModels.GymResponse.from(gym);
    }

    @Transactional(readOnly = true)
    public PageResponse<GymModels.GymResponse> list(String subject, boolean administrator, int page, int size, String sort) {
        var pageable = PageRequestFactory.create(page, size, sort, SORTS);
        Page<Gym> result;
        if (administrator) {
            List<UUID> gymIds = memberships.findByIdentitySubject(subject).stream().map(GymAdminMembership::getGymId).toList();
            result = gymIds.isEmpty() ? Page.empty(pageable) : gyms.findByIdIn(gymIds, pageable);
        } else {
            result = gyms.findByActiveTrue(pageable);
        }
        return PageResponse.from(result, GymModels.GymResponse::from);
    }
}

