package com.gymflow.gym.eligibility;

import java.util.Set;
import java.util.UUID;

public record EligibilityDecision(UUID exerciseId, boolean eligible, Set<EligibilityReason> reasons) {}

