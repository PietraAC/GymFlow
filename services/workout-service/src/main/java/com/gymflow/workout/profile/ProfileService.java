package com.gymflow.workout.profile;

import com.gymflow.workout.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final StudentProfileRepository repository;
    public ProfileService(StudentProfileRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public ProfileModels.ProfileResponse get(String subject) {
        return repository.findById(subject).map(ProfileModels.ProfileResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Perfil do aluno ainda não cadastrado"));
    }
    @Transactional
    public ProfileModels.ProfileResponse upsert(String subject, ProfileModels.ProfileRequest request) {
        StudentProfile profile = repository.findById(subject).orElseGet(() -> new StudentProfile(subject));
        profile.update(request.goal(), request.experienceLevel(), request.daysPerWeek(), request.sessionDurationMinutes(),
            request.preferredEquipmentTypeIds());
        return ProfileModels.ProfileResponse.from(repository.save(profile));
    }
}
