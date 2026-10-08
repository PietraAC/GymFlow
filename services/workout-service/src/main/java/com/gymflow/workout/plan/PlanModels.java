package com.gymflow.workout.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PlanModels {
    private PlanModels() {}
    public record CreatePlanRequest(@NotNull UUID unitId, @NotBlank @Size(max = 120) String name) {}
    public record UpdatePlanRequest(@NotNull @Min(0) Long version, @NotNull UUID unitId,
        @NotBlank @Size(max = 120) String name, @NotNull @Size(max = 7) List<@Valid DayRequest> days) {}
    public record TransitionRequest(@NotNull @Min(0) Long version) {}
    public record ApplySuggestionRequest(@NotNull UUID suggestionId, @NotNull @Min(0) Long expectedPlanVersion) {}
    public record DayRequest(UUID id, @Min(1) @Max(7) int position, @NotBlank @Size(max = 80) String name,
        @NotNull List<@Valid ItemRequest> items) {}
    public record ItemRequest(UUID id, @NotNull UUID exerciseId, @Min(1) int position,
        @Min(1) @Max(10) Integer sets, @Min(1) @Max(100) Integer repetitionMin,
        @Min(1) @Max(100) Integer repetitionMax, @Min(5) @Max(1800) Integer durationSeconds,
        @Min(0) @Max(600) Integer restSeconds, @DecimalMin("0.0") BigDecimal optionalLoadKg,
        @Size(max = 500) String notes) {}
    public record PlanSummary(UUID id, UUID unitId, String name, PlanStatus status, long version, int dayCount,
                              boolean inventoryRevalidationRequired, Instant updatedAt) {}
    public record PlanIssue(String code, String message, List<UUID> exerciseIds) {}
    public record ItemResponse(UUID id, UUID exerciseId, int position, Integer sets, Integer repetitionMin,
        Integer repetitionMax, Integer durationSeconds, Integer restSeconds, BigDecimal optionalLoadKg, String notes) {}
    public record DayResponse(UUID id, int position, String name, List<ItemResponse> items) {}
    public record PlanResponse(UUID id, UUID unitId, String name, PlanStatus status, long version,
        List<DayResponse> days, boolean inventoryRevalidationRequired, boolean eligibilityCheckAvailable,
        List<PlanIssue> issues, Instant createdAt, Instant updatedAt) {}
}
