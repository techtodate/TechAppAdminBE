package com.app.admin.service.institution;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

import com.app.admin.model.City;
import com.app.admin.model.Country;
import com.app.admin.model.AdministrativeArea;
import com.app.admin.repository.CityRepository;
import com.app.admin.repository.CountryRepository;
import com.app.admin.repository.AdministrativeAreaRepository;

@Service
public class InstitutionNormalizationService {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_STRIP = Pattern.compile("[^a-zA-Z0-9\\s]");

    private final CountryRepository countryRepository;
    private final AdministrativeAreaRepository areaRepository;
    private final CityRepository cityRepository;

    public InstitutionNormalizationService(
            CountryRepository countryRepository,
            AdministrativeAreaRepository areaRepository,
            CityRepository cityRepository) {
        this.countryRepository = countryRepository;
        this.areaRepository = areaRepository;
        this.cityRepository = cityRepository;
    }

    /**
     * Preserves original display casing, collapses redundant spaces, trims.
     */
    public String normalizeDisplayName(String rawName) {
        if (rawName == null) return null;
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) return "";
        return MULTI_SPACE.matcher(trimmed).replaceAll(" ");
    }

    /**
     * Creates normalized_name for indexing and matching.
     * Lowercase, punctuation removed, multiple spaces collapsed.
     * Retains key distinguishing terms like University, College, Institute, School, Technology.
     */
    public String createMatchNormalizedName(String rawName) {
        if (rawName == null) return null;
        String cleaned = PUNCTUATION_STRIP.matcher(rawName).replaceAll(" ");
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim();
        return cleaned.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes website display format without destroying URL structure.
     */
    public String normalizeWebsite(String rawUrl) {
        if (rawUrl == null) return null;
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) return null;
        if (!trimmed.toLowerCase().startsWith("http://") && !trimmed.toLowerCase().startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    /**
     * Extracts normalized domain/host for matching (e.g. "www.harvard.edu/path" -> "harvard.edu").
     */
    public String createNormalizedWebsite(String rawUrl) {
        if (rawUrl == null) return null;
        String norm = normalizeWebsite(rawUrl);
        if (norm == null) return null;
        try {
            URI uri = URI.create(norm);
            String host = uri.getHost();
            if (host == null) {
                host = norm.replaceFirst("https?://", "");
                int slash = host.indexOf('/');
                if (slash != -1) host = host.substring(0, slash);
            }
            if (host != null) {
                host = host.toLowerCase(Locale.ROOT);
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host;
            }
        } catch (Exception ignored) {}
        return norm.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes location strings.
     */
    public String normalizeLocation(String location) {
        if (location == null) return null;
        String trimmed = location.trim();
        if (trimmed.isEmpty()) return null;
        return MULTI_SPACE.matcher(trimmed).replaceAll(" ");
    }

    /**
     * Resolves Country by name or ISO code.
     */
    public Optional<Country> resolveCountry(String countryStr) {
        if (countryStr == null || countryStr.trim().isEmpty()) return Optional.empty();
        String trimmed = countryStr.trim();
        Optional<Country> byCode = countryRepository.findByCodeIgnoreCase(trimmed);
        if (byCode.isPresent()) return byCode;
        return countryRepository.findByNameIgnoreCase(trimmed);
    }

    /**
     * Resolves Administrative Area (State) by code or name within a country.
     */
    public Optional<AdministrativeArea> resolveState(Long countryId, String stateStr) {
        if (countryId == null || stateStr == null || stateStr.trim().isEmpty()) return Optional.empty();
        String trimmed = stateStr.trim();
        Optional<AdministrativeArea> byCode = areaRepository.findByCountryIdAndCodeIgnoreCase(countryId, trimmed);
        if (byCode.isPresent()) return byCode;
        return areaRepository.findByCountryIdAndNameIgnoreCase(countryId, trimmed);
    }

    /**
     * Resolves City against city master.
     */
    public Optional<City> resolveCity(Long countryId, Long stateId, String cityStr) {
        if (cityStr == null || cityStr.trim().isEmpty()) return Optional.empty();
        String trimmed = cityStr.trim();
        if (countryId == null) return Optional.empty();
        return stateId == null ? cityRepository.findFirstByCountryIdAndNameIgnoreCase(countryId, trimmed) : cityRepository.findFirstByCountryIdAndAdministrativeAreaIdAndNameIgnoreCase(countryId, stateId, trimmed);
    }
}
