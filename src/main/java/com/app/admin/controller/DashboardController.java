package com.app.admin.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.admin.repository.CategoryRepository;
import com.app.admin.repository.EventRepository;
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
import com.app.admin.repository.CityRepository;
import com.app.admin.repository.CountryRepository;
import com.app.admin.repository.DistrictRepository;
import com.app.admin.repository.LanguageRepository;
import com.app.admin.repository.StateRepository;
import com.app.admin.repository.EventDeliveryModeRepository;
import com.app.admin.repository.TrainingDeliveryModeRepository;
import com.app.admin.repository.EventTypeRepository;
import com.app.admin.repository.TrainingTypeRepository;
import com.app.admin.repository.OpportunityTypeRepository;
import com.app.admin.repository.OrganizationTypeRepository;

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
    private final EventRepository eventRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final CityRepository cityRepository;
    private final LanguageRepository languageRepository;
    private final EventDeliveryModeRepository eventDeliveryModeRepository;
    private final TrainingDeliveryModeRepository trainingDeliveryModeRepository;
    private final EventTypeRepository eventTypeRepository;
    private final TrainingTypeRepository trainingTypeRepository;
    private final OpportunityTypeRepository opportunityTypeRepository;
    private final OrganizationTypeRepository organizationTypeRepository;

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
            ProfileTypeRepository profileTypeRepository,
            EventRepository eventRepository,
            CountryRepository countryRepository,
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            CityRepository cityRepository,
            LanguageRepository languageRepository,
            EventDeliveryModeRepository eventDeliveryModeRepository,
            TrainingDeliveryModeRepository trainingDeliveryModeRepository,
            EventTypeRepository eventTypeRepository,
            TrainingTypeRepository trainingTypeRepository,
            OpportunityTypeRepository opportunityTypeRepository,
            OrganizationTypeRepository organizationTypeRepository) {
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
        this.eventRepository = eventRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.cityRepository = cityRepository;
        this.languageRepository = languageRepository;
        this.eventDeliveryModeRepository = eventDeliveryModeRepository;
        this.trainingDeliveryModeRepository = trainingDeliveryModeRepository;
        this.eventTypeRepository = eventTypeRepository;
        this.trainingTypeRepository = trainingTypeRepository;
        this.opportunityTypeRepository = opportunityTypeRepository;
        this.organizationTypeRepository = organizationTypeRepository;
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
        counts.put("events", eventRepository.count());
        counts.put("countries", countryRepository.count());
        counts.put("states", stateRepository.count());
        counts.put("districts", districtRepository.count());
        counts.put("cities", cityRepository.count());
        counts.put("languages", languageRepository.count());
        counts.put("event_delivery_modes", eventDeliveryModeRepository.count());
        counts.put("training_delivery_modes", trainingDeliveryModeRepository.count());
        counts.put("event_types", eventTypeRepository.count());
        counts.put("training_types", trainingTypeRepository.count());
        counts.put("opportunity_types", opportunityTypeRepository.count());
        counts.put("organization_types", organizationTypeRepository.count());
        return counts;
    }
}
