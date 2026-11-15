package com.app.admin.service.institution;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

import com.app.admin.model.City;
import com.app.admin.model.Country;
import com.app.admin.model.State;
import com.app.admin.repository.CityRepository;
import com.app.admin.repository.CountryRepository;
import com.app.admin.repository.StateRepository;

@Service
public class InstitutionNormalizationService {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_STRIP = Pattern.compile("[^a-zA-Z0-9\\s]");

    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;

    public InstitutionNormalizationService(
            CountryRepository countryRepository,
            CityRepository cityRepository,
            StateRepository stateRepository) {
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
        this.stateRepository = stateRepository;
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
        // The country master stores ISO-3 codes. Preserve import support for ISO-2 values.
        if (trimmed.length() == 2) {
            try {
                String iso3 = new java.util.Locale.Builder().setRegion(trimmed).build().getISO3Country();
                if (!iso3.isBlank()) {
                    byCode = countryRepository.findByCodeIgnoreCase(iso3);
                    if (byCode.isPresent()) return byCode;
                }
            } catch (java.util.IllformedLocaleException | java.util.MissingResourceException ignored) {
                // Fall through to an exact country-name lookup.
            }
        }
        return countryRepository.findByNameIgnoreCase(trimmed);
    }

    /**
     * Resolves Administrative Area (State) by code or name within a country.
     */
    public Optional<State> resolveState(Long countryId, String stateStr) {
        if (countryId == null || stateStr == null || stateStr.trim().isEmpty()) return Optional.empty();
        String trimmed = stateStr.trim();
        Optional<State> byCode = stateRepository.findByCountryIdAndCodeIgnoreCase(countryId, trimmed);
        if (byCode.isPresent()) return byCode;
        return stateRepository.findByCountryIdAndNameIgnoreCase(countryId, trimmed);
    }

    public boolean isIndia(Long countryId) {
        return countryId != null && countryRepository.findById(countryId)
                .map(country -> "IN".equalsIgnoreCase(country.getCode())
                        || "IND".equalsIgnoreCase(country.getCode())
                        || "India".equalsIgnoreCase(country.getName()))
                .orElse(false);
    }

    public Optional<State> resolveIndianState(Long countryId, String stateStr) {
        if (countryId == null || stateStr == null || stateStr.trim().isEmpty()) return Optional.empty();
        String trimmed = stateStr.trim();
        Optional<State> byCode = stateRepository.findByCountryIdAndCodeIgnoreCase(countryId, trimmed);
        return byCode.isPresent() ? byCode : stateRepository.findByCountryIdAndNameIgnoreCase(countryId, trimmed);
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
