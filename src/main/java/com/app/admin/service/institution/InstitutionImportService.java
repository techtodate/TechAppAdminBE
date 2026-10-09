package com.app.admin.service.institution;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import com.app.admin.dto.*;
import com.app.admin.exception.InstitutionException;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.*;
import com.app.admin.repository.*;
import com.app.admin.security.AdminSecurityContext;

@Service
public class InstitutionImportService {

    private final InstitutionImportRepository importRepository;
    private final InstitutionImportRecordRepository recordRepository;
    private final InstitutionImportRecordMatchRepository matchRepository;
    private final InstitutionSourceRepository sourceRepository;
    private final CountryRepository countryRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionSourceRecordRepository sourceRecordRepository;
    private final InstitutionAliasRepository aliasRepository;
    private final InstitutionSourceReaderRegistry readerRegistry;
    private final InstitutionMappingService mappingService;
    private final InstitutionValidationService validationService;
    private final InstitutionDuplicateDetectionService duplicateDetectionService;
    private final InstitutionNormalizationService normalizationService;
    private final AdminSecurityContext securityContext;
    private final ObjectMapper objectMapper;

    @Value("${app.institution-import.storage-directory:data/imports}")
    private String storagePath = "data/imports";

    public InstitutionImportService(
            InstitutionImportRepository importRepository,
            InstitutionImportRecordRepository recordRepository,
            InstitutionImportRecordMatchRepository matchRepository,
            InstitutionSourceRepository sourceRepository,
            CountryRepository countryRepository,
            InstitutionRepository institutionRepository,
            InstitutionSourceRecordRepository sourceRecordRepository,
            InstitutionAliasRepository aliasRepository,
            InstitutionSourceReaderRegistry readerRegistry,
            InstitutionMappingService mappingService,
            InstitutionValidationService validationService,
            InstitutionDuplicateDetectionService duplicateDetectionService,
            InstitutionNormalizationService normalizationService,
            AdminSecurityContext securityContext,
            ObjectMapper objectMapper) {
        this.importRepository = importRepository;
        this.recordRepository = recordRepository;
        this.matchRepository = matchRepository;
        this.sourceRepository = sourceRepository;
        this.countryRepository = countryRepository;
        this.institutionRepository = institutionRepository;
        this.sourceRecordRepository = sourceRecordRepository;
        this.aliasRepository = aliasRepository;
        this.readerRegistry = readerRegistry;
        this.mappingService = mappingService;
        this.validationService = validationService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.normalizationService = normalizationService;
        this.securityContext = securityContext;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InstitutionImport createImport(CreateInstitutionImportRequest request) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_IMPORT);

        Country country = countryRepository.findById(request.countryId())
                .orElseThrow(() -> InstitutionException.countryNotFound(request.countryId()));

        InstitutionSource source = sourceRepository.findById(request.sourceId())
                .orElseThrow(() -> InstitutionException.sourceNotFound(request.sourceId()));

        if (!source.getCountryId().equals(country.getId())) {
            throw InstitutionException.invalidSourceCountry(source.getId(), country.getId());
        }
        if (!Boolean.TRUE.equals(country.getActive()) || !Boolean.TRUE.equals(source.getActive())) throw new IllegalArgumentException("Country and source must be active");

        InstitutionImport imp = new InstitutionImport();
        imp.setCountryId(country.getId());
        imp.setSourceId(source.getId());
        imp.setFileName(request.fileName() != null && !request.fileName().isBlank() ? request.fileName().trim() : "pending_upload");
        imp.setFilePath("pending_upload");
        imp.setFileChecksum("pending_upload");
        imp.setVersion(request.version() != null ? request.version().trim() : "1.0");
        imp.setImportType(request.importType() != null && !request.importType().isBlank() ? request.importType().trim().toUpperCase() : "FULL");
        imp.setStatus(ImportStatus.CREATED);
        imp.setCreatedBy(securityContext.getCurrentUserId());

        return importRepository.save(imp);
    }

    @Transactional
    public InstitutionImport uploadSourceFile(Long importId, MultipartFile file) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_IMPORT);

        InstitutionImport imp = lockedImport(importId);
        if (imp.getStatus() == ImportStatus.COMPLETED) {
            throw InstitutionException.importAlreadyApplied(importId);
        }
        if (imp.getStatus() == ImportStatus.CANCELLED) {
            throw InstitutionException.importAlreadyCancelled(importId);
        }
        if (file == null || file.isEmpty()) {
            throw InstitutionException.fileEmpty();
        }

        if (imp.getStatus() != ImportStatus.CREATED && imp.getStatus() != ImportStatus.UPLOADED) throw InstitutionException.importNotReady(imp.getStatus().name());
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".csv") && !originalFilename.toLowerCase().endsWith(".txt"))) {
            throw InstitutionException.fileNotSupported("Only CSV files are currently supported for institution imports");
        }

        try {
            String displayName = Paths.get(originalFilename.replace('\\', '/')).getFileName().toString();
            Path directory = Paths.get(storagePath).toAbsolutePath().normalize().resolve(importId.toString());
            Files.createDirectories(directory);
            Path targetFile = directory.resolve("source.csv");
            Path pendingFile = Files.createTempFile(directory, "upload-", ".tmp");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, md);
                 OutputStream os = Files.newOutputStream(pendingFile)) {
                dis.transferTo(os);
            }
            Files.move(pendingFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            byte[] hashBytes = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            imp.setFileName(displayName);
            imp.setFilePath(targetFile.toAbsolutePath().toString());
            imp.setFileChecksum(hexString.toString());
            imp.setStatus(ImportStatus.UPLOADED);
            return importRepository.save(imp);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to store uploaded import file: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    public ImportPreviewResponse validateImport(Long importId) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_IMPORT);

        InstitutionImport imp = lockedImport(importId);
        if (imp.getStatus() == ImportStatus.CREATED) {
            throw InstitutionException.importNotReady("File has not been uploaded yet");
        }
        if (imp.getStatus() == ImportStatus.COMPLETED) {
            throw InstitutionException.importAlreadyApplied(importId);
        }
        if (imp.getStatus() == ImportStatus.CANCELLED) {
            throw InstitutionException.importAlreadyCancelled(importId);
        }

        if (imp.getStatus() != ImportStatus.UPLOADED && imp.getStatus() != ImportStatus.READY_FOR_REVIEW && imp.getStatus() != ImportStatus.VALIDATION_COMPLETED) throw InstitutionException.importNotReady(imp.getStatus().name());
        imp.setStatus(ImportStatus.PROCESSING);
        imp.setStartedAt(LocalDateTime.now());
        importRepository.saveAndFlush(imp);

        Long sourceId = imp.getSourceId();
        Long countryId = imp.getCountryId();
        InstitutionSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> InstitutionException.sourceNotFound(sourceId));
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> InstitutionException.countryNotFound(countryId));

        List<InstitutionSourceMapping> mappings = mappingService.getMappingsForSource(source.getId());
        InstitutionSourceReader reader = readerRegistry.getReader(source);

        File file = uploadedFile(imp).toFile();
        if (!file.exists()) {
            imp.setStatus(ImportStatus.FAILED);
            imp.setErrorMessage("Import file not found on disk");
            importRepository.save(imp);
            throw new ResourceNotFoundException("Import file not found. Upload the source file again.");
        }

        // Delete any existing records from previous validation runs of this import
        matchRepository.deleteAll(matchRepository.findByImportId(importId));
        matchRepository.flush();
        recordRepository.deleteByImportId(importId);
        recordRepository.flush();

        int total = 0;
        int valid = 0;
        int newCount = 0;
        int updatedCount = 0;
        int unchangedCount = 0;
        int duplicateCount = 0;
        int errorCount = 0;

        Set<String> seenIdentifiersInBatch = new HashSet<>();

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            SourceFileMetadata metadata = new SourceFileMetadata(imp.getFileName(), "text/csv", file.length());
            List<SourceRow> rows = reader.read(is, metadata);
            total = rows.size();

            for (SourceRow row : rows) {
                InstitutionImportRecord rec = new InstitutionImportRecord();
                rec.setImportId(importId);
                rec.setRowIndex(row.rowIndex());

                Map<String, Object> rawMap = new LinkedHashMap<>(row.values());
                rec.setRawData(rawMap);

                MappedInstitutionData mapped = mappingService.mapRow(row, mappings, country);
                rec.setSourceIdentifier(mapped.getSourceIdentifier());

                @SuppressWarnings("unchecked")
                Map<String, Object> mappedMap = objectMapper.convertValue(mapped, Map.class);
                rec.setMappedData(mappedMap);

                ValidationResult vr = validationService.validateRow(mapped, source, country, seenIdentifiersInBatch);
                if (!vr.valid()) {
                    rec.setValidationStatus(ValidationStatus.INVALID);
                    rec.setValidationErrors(vr.errors());
                    rec.setClassification(RecordClassification.ERROR);
                    errorCount++;
                    recordRepository.save(rec);
                    continue;
                }

                rec.setValidationStatus(ValidationStatus.VALID);
                valid++;

                DuplicateCheckResult dcr = duplicateDetectionService.checkForDuplicates(source.getId(), mapped);
                rec.setClassification(dcr.classification());
                rec.setTargetInstitutionId(dcr.targetInstitutionId());

                rec = recordRepository.save(rec);

                switch (dcr.classification()) {
                    case NEW -> newCount++;
                    case UPDATE -> updatedCount++;
                    case UNCHANGED -> unchangedCount++;
                    case DUPLICATE -> {
                        duplicateCount++;
                        for (MatchCandidate match : dcr.matches()) {
                            InstitutionImportRecordMatch matchEntity = new InstitutionImportRecordMatch();
                            matchEntity.setImportRecordId(rec.getId());
                            matchEntity.setMatchedInstitutionId(match.institutionId());
                            matchEntity.setMatchScore(java.math.BigDecimal.valueOf(match.score()));
                            matchEntity.setMatchConfidence(match.confidence());
                            matchEntity.setMatchReason(match.reason());
                            matchEntity.setResolutionStatus(ResolutionStatus.PENDING);
                            matchRepository.save(matchEntity);
                        }
                    }
                    default -> {}
                }
            }

            imp.setTotalRecords(total);
            imp.setValidRecords(valid);
            imp.setNewCount(newCount);
            imp.setUpdatedCount(updatedCount);
            imp.setUnchangedCount(unchangedCount);
            imp.setDuplicateCount(duplicateCount);
            imp.setErrorCount(errorCount);

            Map<String, Object> summaryMap = new LinkedHashMap<>();
            summaryMap.put("totalRecords", total);
            summaryMap.put("validRecords", valid);
            summaryMap.put("newCount", newCount);
            summaryMap.put("updatedCount", updatedCount);
            summaryMap.put("unchangedCount", unchangedCount);
            summaryMap.put("duplicateCount", duplicateCount);
            summaryMap.put("errorCount", errorCount);
            imp.setSummary(summaryMap);

            imp.setStatus(ImportStatus.READY_FOR_REVIEW);
            imp = importRepository.save(imp);

            ImportSummaryDto summaryDto = new ImportSummaryDto(total, valid, newCount, updatedCount, unchangedCount, duplicateCount, errorCount);
            return new ImportPreviewResponse(imp.getId(), imp.getStatus(), summaryDto);

        } catch (Exception ex) {
            imp.setStatus(ImportStatus.FAILED);
            imp.setErrorMessage("Validation error: " + ex.getMessage());
            importRepository.save(imp);
            throw new RuntimeException("Validation process failed: " + ex.getMessage(), ex);
        }
    }

    public ImportPreviewResponse previewImport(Long importId) {
        InstitutionImport imp = getImport(importId);
        ImportSummaryDto summary = new ImportSummaryDto(
                imp.getTotalRecords(),
                imp.getValidRecords(),
                imp.getNewCount(),
                imp.getUpdatedCount(),
                imp.getUnchangedCount(),
                imp.getDuplicateCount(),
                imp.getErrorCount()
        );
        return new ImportPreviewResponse(imp.getId(), imp.getStatus(), summary);
    }

    @Transactional
    public InstitutionImport applyImport(Long importId) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_IMPORT);

        InstitutionImport imp = lockedImport(importId);
        if (imp.getStatus() == ImportStatus.COMPLETED) {
            throw InstitutionException.importAlreadyApplied(importId);
        }
        if (imp.getStatus() == ImportStatus.CANCELLED) {
            throw InstitutionException.importAlreadyCancelled(importId);
        }
        if (imp.getStatus() != ImportStatus.READY_FOR_REVIEW && imp.getStatus() != ImportStatus.VALIDATION_COMPLETED) {
            throw InstitutionException.importNotReady(imp.getStatus().name());
        }

        List<InstitutionImportRecord> records = recordRepository.findByImportIdOrderByRowIndexAsc(importId);
        if (records.isEmpty()) throw InstitutionException.importNotReady("No validated records");
        if (records.stream().anyMatch(r -> r.getClassification() == RecordClassification.ERROR || r.getValidationStatus() != ValidationStatus.VALID)) throw InstitutionException.importNotReady("Validation errors must be resolved");
        if (records.stream().anyMatch(r -> r.getClassification() == RecordClassification.DUPLICATE && r.getTargetInstitutionId() == null)) throw InstitutionException.importNotReady("Unresolved duplicate records remain");

        imp.setStatus(ImportStatus.IMPORTING);
        importRepository.saveAndFlush(imp);

        for (InstitutionImportRecord rec : records) {
            if (rec.getValidationStatus() != ValidationStatus.VALID) {
                continue; // Do not insert ERROR records into institution master
            }

            MappedInstitutionData data = objectMapper.convertValue(rec.getMappedData(), MappedInstitutionData.class);

            if (rec.getClassification() == RecordClassification.NEW) {
                Institution inst = new Institution();
                inst.setName(data.getName());
                inst.setNormalizedName(data.getNormalizedName());
                inst.setShortName(data.getShortName());
                inst.setInstitutionType(data.getInstitutionType());
                inst.setCountryId(data.getCountryId());
                inst.setStateId(data.getStateId());
                inst.setCityId(data.getCityId());
                inst.setCityName(data.getResolvedCityName());
                inst.setAddress(data.getAddress());
                inst.setWebsite(data.getWebsite());
                inst.setNormalizedWebsite(data.getNormalizedWebsite());
                inst.setActive(true);
                inst.setSourceId(imp.getSourceId());
                inst.setSourceIdentifier(data.getSourceIdentifier());
                inst = institutionRepository.save(inst);

                rec.setTargetInstitutionId(inst.getId());
                recordRepository.save(rec);

                // Insert source record
                InstitutionSourceRecord isr = new InstitutionSourceRecord();
                isr.setInstitutionId(inst.getId());
                isr.setSourceId(imp.getSourceId());
                isr.setSourceIdentifier(data.getSourceIdentifier());
                isr.setRawData(rec.getRawData());
                isr.setLastSeenAt(LocalDateTime.now());
                    isr.setLastImportId(importId);
                sourceRecordRepository.save(isr);

                // Add alias if shortName present
                if (data.getShortName() != null && !data.getShortName().isBlank()) {
                    InstitutionAlias alias = new InstitutionAlias();
                    alias.setInstitutionId(inst.getId());
                    alias.setAliasName(data.getShortName().trim());
                    alias.setNormalizedAlias(normalizationService.createMatchNormalizedName(data.getShortName()));
                    alias.setAliasType("ACRONYM");
                    aliasRepository.save(alias);
                }

            } else if (rec.getClassification() == RecordClassification.UPDATE) {
                if (rec.getTargetInstitutionId() != null) {
                    Optional<Institution> instOpt = institutionRepository.findById(rec.getTargetInstitutionId());
                    if (instOpt.isEmpty()) throw new ResourceNotFoundException("Target institution", rec.getTargetInstitutionId());
                    if (instOpt.isPresent()) {
                        Institution inst = instOpt.get();
                        // Update allowed master fields
                        if (data.getName() != null) {
                            inst.setName(data.getName());
                            inst.setNormalizedName(data.getNormalizedName());
                        }
                        if (data.getShortName() != null) inst.setShortName(data.getShortName());
                        if (data.getInstitutionType() != null) inst.setInstitutionType(data.getInstitutionType());
                        if (data.getAddress() != null) inst.setAddress(data.getAddress());
                        if (data.getWebsite() != null) {
                            inst.setWebsite(data.getWebsite());
                            inst.setNormalizedWebsite(data.getNormalizedWebsite());
                        }
                        if (data.getStateId() != null) inst.setStateId(data.getStateId());
                        if (data.getCityId() != null) inst.setCityId(data.getCityId());
                        institutionRepository.save(inst);

                        // Update or insert source record
                        Optional<InstitutionSourceRecord> isrOpt = sourceRecordRepository
                                .findBySourceIdAndSourceIdentifier(imp.getSourceId(), data.getSourceIdentifier());
                        InstitutionSourceRecord isr = isrOpt.orElseGet(InstitutionSourceRecord::new);
                        isr.setInstitutionId(inst.getId());
                        isr.setSourceId(imp.getSourceId());
                        isr.setSourceIdentifier(data.getSourceIdentifier());
                        isr.setRawData(rec.getRawData());
                        isr.setLastSeenAt(LocalDateTime.now());
                    isr.setLastImportId(importId);
                        sourceRecordRepository.save(isr);
                    }
                }

            } else if (rec.getClassification() == RecordClassification.UNCHANGED) {
                // Update last_seen_at
                Optional<InstitutionSourceRecord> isrOpt = sourceRecordRepository
                        .findBySourceIdAndSourceIdentifier(imp.getSourceId(), data.getSourceIdentifier());
                if (isrOpt.isPresent()) {
                    InstitutionSourceRecord isr = isrOpt.get();
                    isr.setLastSeenAt(LocalDateTime.now());
                    isr.setLastImportId(importId);
                    sourceRecordRepository.save(isr);
                }

            } else if (rec.getClassification() == RecordClassification.DUPLICATE) {
                // Check if duplicate was resolved
                if (rec.getTargetInstitutionId() != null) {
                    // Merged to existing target
                    if (!institutionRepository.existsById(rec.getTargetInstitutionId())) throw new ResourceNotFoundException("Target institution", rec.getTargetInstitutionId());
                    Optional<InstitutionSourceRecord> isrOpt = sourceRecordRepository
                            .findBySourceIdAndSourceIdentifier(imp.getSourceId(), data.getSourceIdentifier());
                    InstitutionSourceRecord isr = isrOpt.orElseGet(InstitutionSourceRecord::new);
                    isr.setInstitutionId(rec.getTargetInstitutionId());
                    isr.setSourceId(imp.getSourceId());
                    isr.setSourceIdentifier(data.getSourceIdentifier());
                    isr.setRawData(rec.getRawData());
                    isr.setLastSeenAt(LocalDateTime.now());
                    isr.setLastImportId(importId);
                    sourceRecordRepository.save(isr);
                }
                // If not resolved or unresolved duplicate: do not automatically create a second institution
            }
        }

        imp.setStatus(ImportStatus.COMPLETED);
        imp.setCompletedAt(LocalDateTime.now());
        imp.setAppliedAt(LocalDateTime.now());
        return importRepository.save(imp);
    }

    @Transactional
    public InstitutionImport cancelImport(Long importId) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_IMPORT);

        InstitutionImport imp = lockedImport(importId);
        if (imp.getStatus() == ImportStatus.COMPLETED) {
            throw InstitutionException.importAlreadyApplied(importId);
        }
        if (imp.getStatus() == ImportStatus.CANCELLED) {
            throw InstitutionException.importAlreadyCancelled(importId);
        }

        imp.setStatus(ImportStatus.CANCELLED);
        return importRepository.save(imp);
    }

    public InstitutionImport getImport(Long importId) {
        return importRepository.findById(importId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution import not found with id: " + importId));
    }

    private InstitutionImport lockedImport(Long id) {
        return importRepository.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Institution import", id));
    }

    Path uploadedFile(InstitutionImport item) {
        Path directory = Paths.get(storagePath).toAbsolutePath().normalize().resolve(item.getId().toString());
        Path current = directory.resolve("source.csv");
        if (Files.isRegularFile(current)) return current;
        if (item.getFileName() != null) {
            Path legacy = directory.resolve(item.getFileName()).normalize();
            if (legacy.startsWith(directory) && Files.isRegularFile(legacy)) return legacy;
        }
        return current;
    }

    private void requireReviewable(InstitutionImport item) {
        if (item.getStatus() != ImportStatus.READY_FOR_REVIEW && item.getStatus() != ImportStatus.VALIDATION_COMPLETED) throw InstitutionException.importNotReady(item.getStatus().name());
    }

    public Page<InstitutionImportRecord> getImportRecords(Long importId, Pageable pageable, RecordClassification classification) {
        if (classification != null) {
            return recordRepository.findByImportIdAndClassification(importId, classification, pageable);
        }
        return recordRepository.findByImportId(importId, pageable);
    }

    public InstitutionImportRecord getImportRecord(Long importId, Long recordId) {
        InstitutionImportRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Import record not found with id: " + recordId));
        if (!record.getImportId().equals(importId)) {
            throw new ResourceNotFoundException("Record ID " + recordId + " does not belong to import ID " + importId);
        }
        return record;
    }

    public List<InstitutionImportRecordMatch> getImportDuplicates(Long importId) {
        return matchRepository.findByImportId(importId);
    }

    @Transactional
    public InstitutionImportRecordMatch mergeDuplicate(Long importId, Long matchId) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_MANAGE);
        requireReviewable(lockedImport(importId));

        InstitutionImportRecordMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        InstitutionImportRecord record = recordRepository.findById(match.getImportRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("Import record not found for match: " + matchId));

        if (!record.getImportId().equals(importId)) {
            throw new ResourceNotFoundException("Match does not belong to import ID: " + importId);
        }
        if (record.getClassification() != RecordClassification.DUPLICATE || record.getTargetInstitutionId() != null) throw InstitutionException.importNotReady("Duplicate record has already been resolved");

        match.setResolutionStatus(ResolutionStatus.MERGED);
        match.setResolvedBy(securityContext.getCurrentUserId());
        match.setResolvedAt(LocalDateTime.now());
        matchRepository.save(match);

        record.setTargetInstitutionId(match.getMatchedInstitutionId());
        recordRepository.save(record);

        return match;
    }

    @Transactional
    public InstitutionImportRecordMatch keepDuplicateSeparate(Long importId, Long matchId) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_MANAGE);
        InstitutionImport item = lockedImport(importId);
        requireReviewable(item);

        InstitutionImportRecordMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        InstitutionImportRecord record = recordRepository.findById(match.getImportRecordId())
                .orElseThrow(() -> new ResourceNotFoundException("Import record not found for match: " + matchId));

        if (!record.getImportId().equals(importId)) {
            throw new ResourceNotFoundException("Match does not belong to import ID: " + importId);
        }
        if (record.getClassification() != RecordClassification.DUPLICATE || record.getTargetInstitutionId() != null) throw InstitutionException.importNotReady("Duplicate record has already been resolved");

        match.setResolutionStatus(ResolutionStatus.KEPT_SEPARATE);
        match.setResolvedBy(securityContext.getCurrentUserId());
        match.setResolvedAt(LocalDateTime.now());
        matchRepository.save(match);

        // If kept separate, allow this record to be treated as NEW
        record.setClassification(RecordClassification.NEW);
        record.setTargetInstitutionId(null);
        recordRepository.save(record);

        item.setNewCount(item.getNewCount() + 1);
        item.setDuplicateCount(Math.max(0, item.getDuplicateCount() - 1));
        importRepository.save(item);

        return match;
    }

    public Page<InstitutionImport> getImportHistory(Pageable pageable) {
        return importRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
