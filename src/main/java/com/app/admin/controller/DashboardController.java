package com.app.admin.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.admin.repository.CategoryRepository;
import com.app.admin.repository.FieldRepository;
import com.app.admin.repository.MedicalCategoryRepository;
import com.app.admin.repository.MedicalSourceRepository;
import com.app.admin.repository.MedicalSubjectRepository;
import com.app.admin.repository.MedicalUpdateRepository;
import com.app.admin.repository.ProfileTypeRepository;
import com.app.admin.repository.TechnologyRepository;
import com.app.admin.repository.TechSourceRepository;
import com.app.admin.repository.TechUpdateRepository;
import com.app.admin.repository.UserTypeRepository;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final FieldRepository fieldRepository;
    private final CategoryRepository categoryRepository;
    private final TechnologyRepository technologyRepository;
    private final TechSourceRepository techSourceRepository;
    private final TechUpdateRepository techUpdateRepository;
    private final MedicalCategoryRepository medicalCategoryRepository;
    private final MedicalSubjectRepository medicalSubjectRepository;
    private final MedicalSourceRepository medicalSourceRepository;
    private final MedicalUpdateRepository medicalUpdateRepository;
    private final UserTypeRepository userTypeRepository;
    private final ProfileTypeRepository profileTypeRepository;

    public DashboardController(
            FieldRepository fieldRepository,
            CategoryRepository categoryRepository,
            TechnologyRepository technologyRepository,
            TechSourceRepository techSourceRepository,
            TechUpdateRepository techUpdateRepository,
            MedicalCategoryRepository medicalCategoryRepository,
            MedicalSubjectRepository medicalSubjectRepository,
            MedicalSourceRepository medicalSourceRepository,
            MedicalUpdateRepository medicalUpdateRepository,
            UserTypeRepository userTypeRepository,
            ProfileTypeRepository profileTypeRepository) {
        this.fieldRepository = fieldRepository;
        this.categoryRepository = categoryRepository;
        this.technologyRepository = technologyRepository;
        this.techSourceRepository = techSourceRepository;
        this.techUpdateRepository = techUpdateRepository;
        this.medicalCategoryRepository = medicalCategoryRepository;
        this.medicalSubjectRepository = medicalSubjectRepository;
        this.medicalSourceRepository = medicalSourceRepository;
        this.medicalUpdateRepository = medicalUpdateRepository;
        this.userTypeRepository = userTypeRepository;
        this.profileTypeRepository = profileTypeRepository;
    }

    @GetMapping("/counts")
    public Map<String, Long> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("fields", fieldRepository.count());
        counts.put("categories", categoryRepository.count());
        counts.put("technologies", technologyRepository.count());
        counts.put("tech_sources", techSourceRepository.count());
        counts.put("tech_updates", techUpdateRepository.count());
        counts.put("medical_categories", medicalCategoryRepository.count());
        counts.put("medical_subjects", medicalSubjectRepository.count());
        counts.put("medical_sources", medicalSourceRepository.count());
        counts.put("medical_updates", medicalUpdateRepository.count());
        counts.put("user_types", userTypeRepository.count());
        counts.put("profile_types", profileTypeRepository.count());
        return counts;
    }
}
