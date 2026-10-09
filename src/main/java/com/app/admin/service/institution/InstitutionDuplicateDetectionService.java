package com.app.admin.service.institution;

import java.util.*;
import org.springframework.stereotype.Service;

import com.app.admin.dto.DuplicateCheckResult;
import com.app.admin.dto.MappedInstitutionData;
import com.app.admin.dto.MatchCandidate;
import com.app.admin.model.*;
import com.app.admin.repository.InstitutionAliasRepository;
import com.app.admin.repository.InstitutionRepository;
import com.app.admin.repository.InstitutionSourceRecordRepository;

@Service
public class InstitutionDuplicateDetectionService {

    public static final int THRESHOLD_HIGH_CONFIDENCE = 90;
    public static final int THRESHOLD_POSSIBLE_DUPLICATE = 75;

    private final InstitutionSourceRecordRepository sourceRecordRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionAliasRepository aliasRepository;

    public InstitutionDuplicateDetectionService(
            InstitutionSourceRecordRepository sourceRecordRepository,
            InstitutionRepository institutionRepository,
            InstitutionAliasRepository aliasRepository) {
        this.sourceRecordRepository = sourceRecordRepository;
        this.institutionRepository = institutionRepository;
        this.aliasRepository = aliasRepository;
    }

    public DuplicateCheckResult checkForDuplicates(Long sourceId, MappedInstitutionData data) {
        // Stage 1: Exact source match (source_id + source_identifier)
        if (data.getSourceIdentifier() != null && !data.getSourceIdentifier().isBlank()) {
            Optional<InstitutionSourceRecord> existingSrcRec = sourceRecordRepository
                    .findBySourceIdAndSourceIdentifier(sourceId, data.getSourceIdentifier().trim());

            if (existingSrcRec.isPresent()) {
                Long institutionId = existingSrcRec.get().getInstitutionId();
                Optional<Institution> instOpt = institutionRepository.findById(institutionId);
                if (instOpt.isPresent()) {
                    Institution inst = instOpt.get();
                    boolean hasChanges = checkFieldChanges(inst, data);
                    RecordClassification classification = hasChanges ? RecordClassification.UPDATE : RecordClassification.UNCHANGED;
                    return DuplicateCheckResult.of(classification, institutionId);
                }
            }
        }

        // Stage 2: Exact normalized match (country_id + normalized_name + city_id)
        if (data.getCountryId() != null && data.getNormalizedName() != null) {
            if (data.getCityId() != null) {
                List<Institution> exactCityMatch = institutionRepository
                        .findByCountryIdAndNormalizedNameAndCityId(data.getCountryId(), data.getNormalizedName(), data.getCityId());
                if (!exactCityMatch.isEmpty()) {
                    return DuplicateCheckResult.duplicate(exactCityMatch.stream().map(i -> buildExactCandidate(i, data, "Stage 2: Exact country, name, and city match")).toList());
                }
            }

            // Also check country_id + normalized_name + state_id if state is present
            if (data.getStateId() != null) {
                List<Institution> exactStateMatch = institutionRepository
                        .findByCountryIdAndNormalizedNameAndStateId(data.getCountryId(), data.getNormalizedName(), data.getStateId());
                if (!exactStateMatch.isEmpty()) {
                    return DuplicateCheckResult.duplicate(exactStateMatch.stream().map(i -> buildExactCandidate(i, data, "Stage 2: Exact country, name, and state match")).toList());
                }
            }
        }

        // Stage 3: Possible duplicate matching (fuzzy / component scoring)
        List<MatchCandidate> matchCandidates = new ArrayList<>();
        if (data.getCountryId() != null) {
            List<Institution> candidates = institutionRepository.findByCountryId(data.getCountryId());

            for (Institution candidate : candidates) {
                int score = 0;
                Map<String, Object> reason = new LinkedHashMap<>();

                // 1. Name match (up to 50 points)
                int nameScore = computeNameScore(candidate, data);
                if (nameScore > 0) {
                    score += nameScore;
                    reason.put("name_score", nameScore);
                }

                // If name similarity is zero or negligible, skip this candidate
                if (nameScore < 20) {
                    continue;
                }

                // 2. Country match (15 points)
                if (candidate.getCountryId().equals(data.getCountryId())) {
                    score += 15;
                    reason.put("country_match", 15);
                }

                // 3. City match (15 points)
                if (data.getCityId() != null && candidate.getCityId() != null && candidate.getCityId().equals(data.getCityId())) {
                    score += 15;
                    reason.put("city_match", 15);
                } else if (data.getCity() != null && candidate.getCityName() != null
                        && data.getCity().equalsIgnoreCase(candidate.getCityName())) {
                    score += 15;
                    reason.put("city_match", 15);
                }

                // 4. Administrative Area match (10 points)
                if (data.getStateId() != null && candidate.getStateId() != null && candidate.getStateId().equals(data.getStateId())) {
                    score += 10;
                    reason.put("administrative_area_match", 10);
                }

                // 5. Website/domain match (10 points)
                if (data.getNormalizedWebsite() != null && candidate.getNormalizedWebsite() != null
                        && !data.getNormalizedWebsite().isBlank()
                        && data.getNormalizedWebsite().equalsIgnoreCase(candidate.getNormalizedWebsite())) {
                    score += 10;
                    reason.put("website_match", 10);
                }

                score = Math.min(score, 100);

                if (score >= THRESHOLD_POSSIBLE_DUPLICATE) {
                    MatchConfidence confidence = score >= THRESHOLD_HIGH_CONFIDENCE
                            ? MatchConfidence.HIGH_CONFIDENCE
                            : MatchConfidence.POSSIBLE_DUPLICATE;
                    reason.put("total_score", score);
                    matchCandidates.add(new MatchCandidate(candidate.getId(), candidate.getName(), score, confidence, reason));
                }
            }
        }

        if (!matchCandidates.isEmpty()) {
            // Sort by score descending
            matchCandidates.sort((a, b) -> Integer.compare(b.score(), a.score()));
            return DuplicateCheckResult.duplicate(matchCandidates);
        }

        return DuplicateCheckResult.of(RecordClassification.NEW, null);
    }

    private boolean checkFieldChanges(Institution inst, MappedInstitutionData data) {
        if (!Objects.equals(inst.getName(), data.getName())) return true;
        if (!Objects.equals(inst.getShortName(), data.getShortName())) return true;
        if (!Objects.equals(inst.getInstitutionType(), data.getInstitutionType())) return true;
        if (!Objects.equals(inst.getAddress(), data.getAddress())) return true;
        if (!Objects.equals(inst.getWebsite(), data.getWebsite())) return true;
        if (data.getStateId() != null && !Objects.equals(inst.getStateId(), data.getStateId())) return true;
        if (data.getCityId() != null && !Objects.equals(inst.getCityId(), data.getCityId())) return true;
        return false;
    }

    private MatchCandidate buildExactCandidate(Institution inst, MappedInstitutionData data, String stageDesc) {
        Map<String, Object> reason = new LinkedHashMap<>();
        reason.put("stage", stageDesc);
        reason.put("name_score", 50);
        reason.put("country_match", 15);
        int score = 65;
        if (data.getCityId() != null && Objects.equals(data.getCityId(), inst.getCityId())) {
            score += 15;
            reason.put("city_match", 15);
        }
        if (data.getStateId() != null && Objects.equals(data.getStateId(), inst.getStateId())) {
            score += 10;
            reason.put("administrative_area_match", 10);
        }
        if (data.getNormalizedWebsite() != null && Objects.equals(data.getNormalizedWebsite(), inst.getNormalizedWebsite())) {
            score += 10;
            reason.put("website_match", 10);
        }
        score = Math.min(score, 100);
        reason.put("total_score", score);
        return new MatchCandidate(inst.getId(), inst.getName(), score, MatchConfidence.HIGH_CONFIDENCE, reason);
    }

    private int computeNameScore(Institution candidate, MappedInstitutionData data) {
        if (candidate.getNormalizedName() == null || data.getNormalizedName() == null) return 0;
        String s1 = candidate.getNormalizedName();
        String s2 = data.getNormalizedName();

        // Exact match
        if (s1.equals(s2)) {
            return 50;
        }

        // Alias match
        List<InstitutionAlias> aliases = aliasRepository.findByInstitutionId(candidate.getId());
        for (InstitutionAlias alias : aliases) {
            if (s2.equals(alias.getNormalizedAlias())) {
                return 50;
            }
        }

        // Fuzzy similarity calculation
        double sim = calculateSimilarity(s1, s2);
        if (sim >= 0.90) return 45;
        if (sim >= 0.80) return 35;
        if (sim >= 0.70) return 25;

        // Substring / word overlap
        if (s1.contains(s2) || s2.contains(s1)) {
            return 30;
        }

        return 0;
    }

    /**
     * Bigram / Dice coefficient similarity for fast and accurate string similarity.
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;
        if (s1.length() < 2 || s2.length() < 2) return 0.0;

        Set<String> set1 = new HashSet<>();
        for (int i = 0; i < s1.length() - 1; i++) {
            set1.add(s1.substring(i, i + 2));
        }

        Set<String> set2 = new HashSet<>();
        for (int i = 0; i < s2.length() - 1; i++) {
            set2.add(s2.substring(i, i + 2));
        }

        int intersection = 0;
        for (String pair : set1) {
            if (set2.contains(pair)) {
                intersection++;
            }
        }

        return (2.0 * intersection) / (set1.size() + set2.size());
    }
}
