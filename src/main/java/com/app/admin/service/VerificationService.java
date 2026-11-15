package com.app.admin.service;

import com.app.admin.dto.VerificationAssignmentRequest;
import com.app.admin.dto.VerificationChangesRequest;
import com.app.admin.dto.VerificationDecisionRequest;
import com.app.admin.dto.VerificationSectionReviewRequest;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.AdminUser;
import com.app.admin.model.UserVerificationRequest;
import com.app.admin.model.VerificationEvidenceReview;
import com.app.admin.model.VerificationReviewHistory;
import com.app.admin.model.VerificationSection;
import com.app.admin.repository.AdminUserRepository;
import com.app.admin.repository.UserVerificationRequestRepository;
import com.app.admin.repository.VerificationEvidenceReviewRepository;
import com.app.admin.repository.VerificationReadRepository;
import com.app.admin.repository.VerificationReviewHistoryRepository;
import com.app.admin.repository.VerificationSectionRepository;
import com.app.admin.security.AdminSecurityContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VerificationService {
    private static final Set<String> QUEUE_STATUSES = Set.of("SUBMITTED", "UNDER_REVIEW", "CHANGES_REQUIRED");
    private static final List<String> REVIEW_SECTIONS = List.of("PERSONAL", "EDUCATION", "PROFESSIONAL", "WORK_EXPERIENCE", "TRAINER", "IDENTITY", "EVIDENCE");

    private final UserVerificationRequestRepository requests;
    private final VerificationSectionRepository sections;
    private final VerificationEvidenceReviewRepository evidenceReviews;
    private final VerificationReviewHistoryRepository history;
    private final AdminUserRepository adminUsers;
    private final VerificationReadRepository reads;
    private final AdminSecurityContext security;
    private final MediaStorage storage;
    private final AdminAuditService adminAudit;

    public VerificationService(UserVerificationRequestRepository requests, VerificationSectionRepository sections,
            VerificationEvidenceReviewRepository evidenceReviews,
            VerificationReviewHistoryRepository history, AdminUserRepository adminUsers,
            VerificationReadRepository reads, AdminSecurityContext security, MediaStorage storage,
            AdminAuditService adminAudit) {
        this.requests = requests;
        this.sections = sections;
        this.evidenceReviews = evidenceReviews;
        this.history = history;
        this.adminUsers = adminUsers;
        this.reads = reads;
        this.security = security;
        this.storage = storage;
        this.adminAudit = adminAudit;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> menu() {
        view();
        return map("code", "VERIFICATION", "label", "Verification", "path", "/verification", "permission", AdminSecurityContext.PERMISSION_VERIFICATION_VIEW);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(int page, int size, String search, String status, String type, Long reviewer, String sortBy, String direction) {
        view();
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedStatus != null && !QUEUE_STATUSES.contains(normalizedStatus)) throw new IllegalArgumentException("Unsupported queue status: " + status);
        String normalizedType = type == null || type.isBlank() ? null : type.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedDirection = direction == null || direction.isBlank() ? "desc" : direction.toLowerCase(java.util.Locale.ROOT);
        if (!Set.of("asc", "desc").contains(normalizedDirection)) throw new IllegalArgumentException("direction must be asc or desc");
        String normalizedSort = sortBy == null || sortBy.isBlank() ? "submittedAt" : sortBy;
        VerificationReadRepository.QueueRows result = reads.queueRows(page, size, search, normalizedStatus, normalizedType, reviewer,
                normalizedSort, normalizedDirection.toUpperCase(java.util.Locale.ROOT));
        return map("content", result.rows(), "total_elements", result.total(),
                "total_pages", (int) Math.ceil(result.total() / (double) size), "number", page, "size", size, "counts", reads.counts());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> counts() {
        view();
        return reads.counts();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        view();
        return detailData(id);
    }

    private Map<String, Object> detailData(long id) {
        Map<String, Object> request = reads.requestDetail(id);
        if (request == null) throw new ResourceNotFoundException("Verification request", "requested record");
        long userId = ((Number) request.get("user_id")).longValue();
        Map<String, Object> result = map("id", request.get("id"),
                "user", map("id", request.get("subject_id"), "name", request.get("user_name"), "email", request.get("user_email")),
                "type", request.get("verification_type"), "status", request.get("status"),
                "progress", request.get("progress_percentage"), "current_step", request.get("current_step"),
                "reviewer", map("id", request.get("assigned_reviewer_id"), "username", request.get("reviewer_username"), "name", request.get("reviewer_name")),
                "submitted_at", request.get("submitted_at"), "reviewed_at", request.get("reviewed_at"),
                "verified_at", request.get("verified_at"), "expires_at", request.get("expires_at"),
                "changes_requested_at", request.get("changes_requested_at"), "created_at", request.get("created_at"),
                "updated_at", request.get("updated_at"), "admin_notes", request.get("admin_notes"),
                "rejection_reason", request.get("rejection_reason"));
        result.put("personal_information", reads.profileRows("users", userId));
        result.put("profile", reads.profileRows("user_profiles", userId));
        result.put("addresses", reads.profileRows("user_addresses", userId));
        result.put("professional", reads.profileRows("user_professional_profile", userId));
        result.put("education", reads.profileRows("user_education", userId));
        result.put("work_experience", reads.profileRows("user_work_experience", userId));
        result.put("trainer", reads.profileRows("training_profiles", userId));
        List<Map<String, Object>> identityRows = withDocumentUrls(reads.identityRows(id, userId));
        result.put("sections", reads.sectionRows(id));
        List<Map<String, Object>> allAttachments = withDocumentUrls(reads.evidenceRows(id));
        List<Map<String, Object>> identityDocuments = allAttachments.stream()
                .filter(row -> Set.of("SELFIE", "IDENTITY_DOCUMENT").contains(
                        String.valueOf(row.get("source_type")).toUpperCase(java.util.Locale.ROOT)))
                .toList();
        List<Map<String, Object>> evidenceRows = allAttachments.stream().filter(row -> !identityDocuments.contains(row)).toList();
        identityRows.forEach(row -> row.put("documents", identityDocuments));
        result.put("identity", identityRows);
        result.put("identity_documents", identityDocuments);
        result.put("evidence", evidenceRows);
        result.put("approval_readiness", approvalReadiness(id, String.valueOf(request.get("status"))));
        result.put("review_history", reads.historyRows(id));
        return result;
    }

    private Map<String, Object> approvalReadiness(long requestId, String requestStatus) {
        Map<String, String> sectionStatuses = new HashMap<>();
        reads.sectionRows(requestId).forEach(row -> sectionStatuses.put(
                String.valueOf(row.get("section_code")), String.valueOf(row.get("review_status"))));
        List<String> blockers = new ArrayList<>();
        if (!"UNDER_REVIEW".equals(requestStatus)) {
            blockers.add("Request status must be UNDER_REVIEW (current: " + requestStatus + ")");
        }
        for (String code : REVIEW_SECTIONS) {
            String status = sectionStatuses.get(code);
            if (!"APPROVED".equals(status) && !"NOT_REQUIRED".equals(status)) {
                blockers.add(code + " section is " + (status == null ? "not reviewed" : status));
            }
        }
        for (Map<String, Object> attachment : reads.evidenceRows(requestId)) {
            Object rawStatus = attachment.containsKey("status") ? attachment.get("status") : attachment.get("upload_status");
            String uploadStatus = rawStatus == null ? "UNKNOWN" : String.valueOf(rawStatus).toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("PENDING", "UPLOADED").contains(uploadStatus)) {
                Object fileName = attachment.getOrDefault("original_file_name", attachment.getOrDefault("title", attachment.get("id")));
                blockers.add((isIdentityEvidence(attachment) ? "Identity" : "Evidence")
                        + " upload is not confirmed: " + fileName + " (" + uploadStatus + ")");
            }
            // Identity files are reviewed together with the IDENTITY section. All other files
            // require an individual row in verification_evidence_reviews.
            if (isIdentityEvidence(attachment)) continue;
            Object perFileReview = attachment.get("review_status");
            String fileReviewStatus = perFileReview == null ? "PENDING"
                    : String.valueOf(perFileReview).toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("APPROVED", "NOT_REQUIRED").contains(fileReviewStatus)) {
                Object fileName = attachment.getOrDefault("original_file_name", attachment.getOrDefault("title", attachment.get("id")));
                blockers.add("Evidence still needs review: " + fileName + " (" + fileReviewStatus + ")");
            }
        }
        return map("can_approve", blockers.isEmpty(), "blockers", blockers, "sections", sectionStatuses);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> approvalReadiness(long requestId) {
        view();
        Map<String, Object> request = reads.requestDetail(requestId);
        if (request == null) throw new ResourceNotFoundException("Verification request", "requested record");
        return approvalReadiness(requestId, String.valueOf(request.get("status")));
    }

    private List<Map<String, Object>> withDocumentUrls(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> source : rows) {
            Map<String, Object> row = new LinkedHashMap<>(source);
            if (row.containsKey("original_file_name")) row.putIfAbsent("file_name", row.get("original_file_name"));
            if (row.containsKey("status")) row.putIfAbsent("upload_status", row.get("status"));
            List<Map<String, String>> documentUrls = new ArrayList<>();
            for (String field : new ArrayList<>(row.keySet())) {
                if (!field.startsWith("_download_key_")) continue;
                Object storageKey = row.remove(field);
                if (storageKey == null) continue;
                String key = String.valueOf(storageKey);
                if (key.isBlank()) continue;
                Object containerValue = row.get("storage_container");
                String container = containerValue == null ? null : String.valueOf(containerValue);
                Object statusValue = row.containsKey("status") ? row.get("status") : row.get("upload_status");
                if (statusValue != null && !Set.of("PENDING", "UPLOADED").contains(
                        String.valueOf(statusValue).toUpperCase(java.util.Locale.ROOT))) continue;
                String name = field.substring("_download_key_".length());
                Map<String, String> urls = Map.of(
                        "field", name,
                        "preview_url", storage.shortLivedViewUrl(key, container),
                        "download_url", storage.shortLivedDownloadUrl(key, container));
                documentUrls.add(urls);
            }
            if (!documentUrls.isEmpty()) {
                row.put("document_download_urls", documentUrls);
                row.putIfAbsent("document_url", documentUrls.get(0).get("preview_url"));
                row.putIfAbsent("preview_url", documentUrls.get(0).get("preview_url"));
                row.putIfAbsent("download_url", documentUrls.get(0).get("download_url"));
            }
            result.add(row);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> assign(long id, VerificationAssignmentRequest request) {
        requirePermission("VERIFICATION_ASSIGN");
        UserVerificationRequest verification = lockRequest(id);
        if (!QUEUE_STATUSES.contains(verification.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active verification requests can be assigned");
        AdminUser reviewer = adminUsers.findById(request.reviewerUserId())
                .filter(AdminUser::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer", request.reviewerUserId()));
        Long adminId = currentAdminId();
        String previous = verification.getStatus();
        verification.setAssignedReviewerId(reviewer.getId());
        requests.save(verification);
        auditAs(id, adminId, "VERIFICATION_ASSIGNED", null, null, previous, previous, "Assigned reviewer " + reviewer.getUsername());
        return detailInternal(id);
    }

    @Transactional
    public Map<String, Object> startReview(long id) {
        requirePermission("VERIFICATION_REVIEW");
        UserVerificationRequest request = lockRequest(id);
        String previous = request.getStatus();
        if (!"SUBMITTED".equals(previous)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only SUBMITTED requests can be started");
        request.setStatus("UNDER_REVIEW");
        if (request.getReviewedAt() == null) request.setReviewedAt(LocalDateTime.now());
        requests.save(request);
        audit(id, "VERIFICATION_REVIEW_STARTED", null, null, previous, "UNDER_REVIEW", null);
        return detailInternal(id);
    }

    @Transactional
    public Map<String, Object> section(long id, String code, VerificationSectionReviewRequest request) {
        requireReviewAction(request.action());
        validateCode(code);
        UserVerificationRequest parent = lockOpenRequest(id);
        if ("EVIDENCE".equals(code) && "APPROVE".equals(request.action())) requireEvidenceReviewed(id);
        VerificationSection section = sections.findForUpdate(id, code).orElseGet(() -> newSection(id, code));
        if (Set.of("APPROVED", "REJECTED", "NOT_REQUIRED").contains(section.getReviewStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This verification section has already been finalized");
        if (Set.of("REQUEST_CHANGES", "REJECT").contains(request.action())) requireComment(request.comments());
        String previous = section.getReviewStatus();
        String target = reviewStatus(request.action());
        LocalDateTime reviewedAt = LocalDateTime.now();
        section.setReviewStatus(target);
        section.setUserStatus("REQUEST_CHANGES".equals(request.action()) ? "CHANGES_REQUIRED" : "COMPLETED");
        section.setReviewedBy(adminUsers.getReferenceById(currentAdminId()));
        section.setReviewedAt(reviewedAt);
        section.setReviewNotes(request.comments());
        if ("REQUEST_CHANGES".equals(request.action())) {
            parent.setStatus("CHANGES_REQUIRED");
            parent.setChangesRequestedAt(reviewedAt);
            appendAdminNote(parent, code + ": " + request.comments());
            requests.save(parent);
        } else if ("REJECT".equals(request.action())) {
            rejectRequest(parent, request.comments(), reviewedAt);
        }
        sections.save(section);
        String auditAction = switch (request.action()) {
            case "APPROVE" -> "VERIFICATION_SECTION_APPROVED";
            case "REJECT" -> "VERIFICATION_SECTION_REJECTED";
            default -> "VERIFICATION_CHANGES_REQUESTED";
        };
        audit(id, auditAction, code, null, previous, target, request.comments());
        return detailInternal(id);
    }

    @Transactional
    public Map<String, Object> evidence(long id, long evidenceId, String action, String comments) {
        requireEvidenceAction(action);
        UserVerificationRequest parent = lockOpenRequest(id);
        Map<String, Object> item = reads.evidenceItem(id, evidenceId);
        if (item == null) throw new ResourceNotFoundException("Evidence", evidenceId);
        Object rawUploadStatus = item.containsKey("status") ? item.get("status") : item.get("upload_status");
        String uploadStatus = rawUploadStatus == null ? "UNKNOWN" : String.valueOf(rawUploadStatus).toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("PENDING", "UPLOADED").contains(uploadStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Evidence upload is not confirmed");
        }
        VerificationEvidenceReview review = evidenceReviews.findForUpdate(id, evidenceId)
                .orElseGet(() -> {
                    VerificationEvidenceReview created = new VerificationEvidenceReview();
                    created.setVerificationRequestId(id);
                    created.setSourceEvidenceId(evidenceId);
                    return created;
                });
        if (Set.of("REQUEST_CHANGES", "REJECT").contains(action)) requireComment(comments);
        if (Set.of("APPROVED", "REJECTED").contains(review.getReviewStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This evidence has already been finalized");
        String previous = review.getReviewStatus();
        String target = reviewStatus(action);
        Long reviewerId = currentAdminId();
        String reviewerUsername = currentAdminUsername();
        LocalDateTime reviewedAt = LocalDateTime.now();
        review.setReviewStatus(target);
        review.setReviewedBy(adminUsers.getReferenceById(reviewerId));
        review.setReviewedAt(reviewedAt);
        review.setReviewNotes(comments);
        evidenceReviews.save(review);
        String sectionCode = evidenceSectionCode(item);
        if ("REQUEST_CHANGES".equals(action)) {
            markSectionChangesRequired(id, sectionCode, reviewerId, comments);
            parent.setStatus("CHANGES_REQUIRED");
            parent.setChangesRequestedAt(reviewedAt);
            appendAdminNote(parent, sectionCode + ": " + comments);
            requests.save(parent);
        } else if ("REJECT".equals(action)) {
            rejectRequest(parent, comments, reviewedAt);
            if ("IDENTITY".equals(sectionCode)) {
                VerificationSection identitySection = sections.findForUpdate(id, "IDENTITY")
                        .orElseGet(() -> newSection(id, "IDENTITY"));
                identitySection.setUserStatus("COMPLETED");
                identitySection.setReviewStatus("REJECTED");
                identitySection.setReviewedBy(adminUsers.getReferenceById(reviewerId));
                identitySection.setReviewedAt(reviewedAt);
                identitySection.setReviewNotes(comments);
                sections.save(identitySection);
                reads.updateIdentityReview(id, parent.getUserId(), "REJECTED", reviewedAt, comments);
            } else {
                markSectionRejected(id, sectionCode, reviewerId, comments, reviewedAt);
            }
        }
        String auditAction = "APPROVE".equals(action) ? "VERIFICATION_EVIDENCE_APPROVED"
                : "REJECT".equals(action) ? "VERIFICATION_EVIDENCE_REJECTED" : "VERIFICATION_EVIDENCE_CHANGES_REQUESTED";
        audit(id, auditAction, sectionCode, evidenceId, previous, target, comments);
        return map("id", evidenceId, "section_code", sectionCode, "review_status", target,
                "reviewed_by", reviewerId, "reviewed_by_username", reviewerUsername);
    }

    private String evidenceSectionCode(Map<String, Object> evidence) {
        String sourceType = String.valueOf(evidence.get("source_type")).toUpperCase(java.util.Locale.ROOT);
        if (Set.of("SELFIE", "IDENTITY_DOCUMENT").contains(sourceType)) return "IDENTITY";
        String type = String.valueOf(evidence.get("evidence_type")).toUpperCase(java.util.Locale.ROOT);
        return REVIEW_SECTIONS.contains(type) ? type : "EVIDENCE";
    }

    private boolean isIdentityEvidence(Map<String, Object> evidence) {
        String sourceType = String.valueOf(evidence.get("source_type")).toUpperCase(java.util.Locale.ROOT);
        return Set.of("SELFIE", "IDENTITY_DOCUMENT").contains(sourceType);
    }

    private void requireIdentityUploadsConfirmed(long requestId) {
        for (Map<String, Object> evidence : reads.evidenceRows(requestId)) {
            if (!isIdentityEvidence(evidence)) continue;
            Object rawStatus = evidence.containsKey("status") ? evidence.get("status") : evidence.get("upload_status");
            String uploadStatus = rawStatus == null ? "UNKNOWN" : String.valueOf(rawStatus).toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("PENDING", "UPLOADED").contains(uploadStatus)) {
                Object fileName = evidence.getOrDefault("original_file_name", evidence.getOrDefault("title", evidence.get("id")));
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Identity document upload is not confirmed: " + fileName + " (" + uploadStatus + ")");
            }
        }
    }

    private void requireEvidenceReviewed(long requestId) {
        for (Map<String, Object> evidence : reads.evidenceRows(requestId)) {
            if (isIdentityEvidence(evidence)) continue;
            Object fileName = evidence.getOrDefault("original_file_name", evidence.getOrDefault("title", evidence.get("id")));
            Object rawUploadStatus = evidence.containsKey("status") ? evidence.get("status") : evidence.get("upload_status");
            String uploadStatus = rawUploadStatus == null ? "UNKNOWN" : String.valueOf(rawUploadStatus).toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("PENDING", "UPLOADED").contains(uploadStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Evidence upload is not confirmed: " + fileName + " (" + uploadStatus + ")");
            }
            Object rawReviewStatus = evidence.get("review_status");
            String reviewStatus = rawReviewStatus == null ? "PENDING"
                    : String.valueOf(rawReviewStatus).toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("APPROVED", "NOT_REQUIRED").contains(reviewStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Approve each evidence document before approving the Evidence section: "
                                + fileName + " (" + reviewStatus + ")");
            }
        }
    }

    @Transactional(readOnly = true)
    public String download(long id, long evidenceId) {
        view();
        Map<String, Object> item = reads.evidenceFile(id, evidenceId);
        if (item == null) throw new ResourceNotFoundException("Evidence", evidenceId);
        String uploadStatus = String.valueOf(item.get("upload_status")).toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("PENDING", "UPLOADED").contains(uploadStatus)) throw new IllegalArgumentException("Evidence upload is not confirmed");
        return storage.shortLivedDownloadUrl((String) item.get("storage_key"), (String) item.get("storage_container"));
    }

    @Transactional
    public Map<String, Object> identity(long id, VerificationSectionReviewRequest request) {
        requireReviewAction(request.action());
        UserVerificationRequest parent = lockOpenRequest(id);
        if (Set.of("REQUEST_CHANGES", "REJECT").contains(request.action())) requireComment(request.comments());
        if ("APPROVE".equals(request.action())) requireIdentityUploadsConfirmed(id);
        VerificationSection section = sections.findForUpdate(id, "IDENTITY").orElseGet(() -> newSection(id, "IDENTITY"));
        if (Set.of("APPROVED", "REJECTED", "NOT_REQUIRED").contains(section.getReviewStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Identity verification has already been finalized");
        String previous = section.getReviewStatus();
        String target = reviewStatus(request.action());
        Long reviewerId = currentAdminId();
        String reviewerUsername = currentAdminUsername();
        LocalDateTime reviewedAt = LocalDateTime.now();
        section.setUserStatus("REQUEST_CHANGES".equals(request.action()) ? "CHANGES_REQUIRED" : "COMPLETED");
        section.setReviewStatus(target);
        section.setReviewedBy(adminUsers.getReferenceById(reviewerId));
        section.setReviewedAt(reviewedAt);
        section.setReviewNotes(request.comments());
        sections.save(section);
        reads.updateIdentityReview(id, parent.getUserId(), target, reviewedAt, request.comments());
        if ("REQUEST_CHANGES".equals(request.action())) {
            parent.setStatus("CHANGES_REQUIRED");
            parent.setChangesRequestedAt(reviewedAt);
            requests.save(parent);
        } else if ("REJECT".equals(request.action())) {
            rejectRequest(parent, request.comments(), reviewedAt);
        }
        String auditAction = switch (request.action()) {
            case "APPROVE" -> "VERIFICATION_IDENTITY_APPROVED";
            case "REJECT" -> "VERIFICATION_IDENTITY_REJECTED";
            default -> "VERIFICATION_CHANGES_REQUESTED";
        };
        audit(id, auditAction, "IDENTITY", null, previous, target, request.comments());
        return map("review_status", target, "reviewed_by", reviewerId, "reviewed_by_username", reviewerUsername);
    }

    @Transactional
    public Map<String, Object> approve(long id) {
        requirePermission("VERIFICATION_APPROVE");
        UserVerificationRequest request = lockRequest(id);
        if (!"UNDER_REVIEW".equals(request.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only UNDER_REVIEW requests can be approved");
        Map<String, Object> readiness = approvalReadiness(id, request.getStatus());
        @SuppressWarnings("unchecked")
        List<String> blockers = (List<String>) readiness.get("blockers");
        if (!blockers.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Verification cannot be approved: " + String.join("; ", blockers));
        String previous = request.getStatus();
        LocalDateTime now = LocalDateTime.now();
        request.setStatus("VERIFIED");
        request.setVerifiedAt(now);
        request.setReviewedAt(now);
        requests.save(request);
        audit(id, "VERIFICATION_APPROVED", null, null, previous, "VERIFIED", null);
        return detailInternal(id);
    }

    @Transactional
    public Map<String, Object> requestChanges(long id, VerificationChangesRequest request) {
        requirePermission("VERIFICATION_REQUEST_CHANGES");
        UserVerificationRequest parent = lockRequest(id);
        if (!"UNDER_REVIEW".equals(parent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Start review before requesting changes (current status: " + parent.getStatus() + ")");
        }
        validateCode(request.section());
        requireComment(request.comments());
        String previous = parent.getStatus();
        LocalDateTime now = LocalDateTime.now();
        appendAdminNote(parent, request.section() + ": " + request.comments());
        if (requests.markChangesRequired(id, now, parent.getAdminNotes()) == 0) {
            throw new ResourceNotFoundException("Verification request", "requested record");
        }
        markSectionChangesRequired(id, request.section(), currentAdminId(), request.comments());
        if ("IDENTITY".equals(request.section())) {
            reads.updateIdentityReview(id, parent.getUserId(), "CHANGES_REQUIRED", now, request.comments());
        }
        audit(id, "VERIFICATION_CHANGES_REQUESTED", request.section(), null, previous, "CHANGES_REQUIRED", request.comments());
        return detailInternal(id);
    }

    @Transactional
    public Map<String, Object> reject(long id, VerificationDecisionRequest request) {
        requirePermission("VERIFICATION_REJECT");
        UserVerificationRequest verification = lockRequest(id);
        String previous = verification.getStatus();
        if (!"UNDER_REVIEW".equals(previous)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Start review before rejecting this request (current status: " + previous + ")");
        }
        verification.setStatus("REJECTED");
        verification.setReviewedAt(LocalDateTime.now());
        verification.setRejectionReason(request.reason());
        requests.save(verification);
        audit(id, "VERIFICATION_REJECTED", null, null, previous, "REJECTED", request.reason());
        return detailInternal(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(long id) {
        view();
        if (!requests.existsById(id)) throw new ResourceNotFoundException("Verification request", "requested record");
        return history.findAllByVerificationRequestIdOrderByCreatedAtAscIdAsc(id).stream().map(this::historyMap).toList();
    }

    private Map<String, Object> historyMap(VerificationReviewHistory entry) {
        AdminUser actor = entry.getAdminUser();
        String name = String.join(" ", List.of(actor.getFirstName() == null ? "" : actor.getFirstName().trim(),
                actor.getLastName() == null ? "" : actor.getLastName().trim())).trim().replaceAll("\\s+", " ");
        return map("id", entry.getId(), "admin_user_id", actor.getId(), "admin_username", actor.getUsername(),
                "admin_name", name.isBlank() ? actor.getUsername() : name, "action", entry.getAction(),
                "section_code", entry.getSectionCode(), "evidence_id", entry.getEvidenceId(),
                "previous_status", entry.getPreviousStatus(), "new_status", entry.getNewStatus(),
                "comments", entry.getComments(), "created_at", entry.getCreatedAt());
    }

    private VerificationSection newSection(long requestId, String code) {
        VerificationSection section = new VerificationSection();
        section.setVerificationRequestId(requestId);
        section.setSectionCode(code);
        return section;
    }

    private void markSectionChangesRequired(long requestId, String code, Long reviewerId, String comments) {
        VerificationSection section = sections.findForUpdate(requestId, code).orElseGet(() -> newSection(requestId, code));
        section.setUserStatus("CHANGES_REQUIRED");
        section.setReviewStatus("CHANGES_REQUIRED");
        section.setReviewedBy(adminUsers.getReferenceById(reviewerId));
        section.setReviewedAt(LocalDateTime.now());
        section.setReviewNotes(comments);
        sections.save(section);
    }

    private void markSectionRejected(long requestId, String code, Long reviewerId, String comments, LocalDateTime reviewedAt) {
        VerificationSection section = sections.findForUpdate(requestId, code).orElseGet(() -> newSection(requestId, code));
        section.setUserStatus("COMPLETED");
        section.setReviewStatus("REJECTED");
        section.setReviewedBy(adminUsers.getReferenceById(reviewerId));
        section.setReviewedAt(reviewedAt);
        section.setReviewNotes(comments);
        sections.save(section);
    }

    private void appendAdminNote(UserVerificationRequest request, String note) {
        String previous = request.getAdminNotes();
        request.setAdminNotes(previous == null || previous.isBlank() ? note : previous + System.lineSeparator() + note);
    }

    private void rejectRequest(UserVerificationRequest request, String reason, LocalDateTime rejectedAt) {
        request.setStatus("REJECTED");
        request.setReviewedAt(rejectedAt);
        request.setRejectionReason(reason);
        requests.save(request);
    }

    private Map<String, Object> detailInternal(long id) { return detailData(id); }

    private void audit(long requestId, String action, String section, Long evidenceId, String previous, String next, String comments) {
        auditAs(requestId, currentAdminId(), action, section, evidenceId, previous, next, comments);
    }

    private void auditAs(long requestId, Long adminId, String action, String section, Long evidenceId, String previous, String next, String comments) {
        VerificationReviewHistory entry = new VerificationReviewHistory();
        entry.setVerificationRequestId(requestId);
        entry.setAdminUser(adminUsers.getReferenceById(adminId));
        entry.setAction(action);
        entry.setSectionCode(section);
        entry.setEvidenceId(evidenceId);
        entry.setPreviousStatus(previous);
        entry.setNewStatus(next);
        entry.setComments(comments);
        history.save(entry);
        adminAudit.recordAs(adminId, action, "VERIFICATION", "USER_VERIFICATION_REQUEST", requestId,
                "section=" + section + ", evidence_id=" + evidenceId + ", previous=" + previous + ", new=" + next + ", comments=" + comments);
    }

    private UserVerificationRequest lockRequest(long id) {
        return requests.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Verification request", "requested record"));
    }

    private UserVerificationRequest lockOpenRequest(long id) {
        UserVerificationRequest request = lockRequest(id);
        if (!"UNDER_REVIEW".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Start review before taking section or evidence actions (current status: " + request.getStatus() + ")");
        }
        return request;
    }

    private String reviewStatus(String action) {
        return switch (action) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            case "REQUEST_CHANGES" -> "CHANGES_REQUIRED";
            default -> throw new IllegalArgumentException("Unsupported review action");
        };
    }

    private void validateCode(String code) {
        if (code == null || !REVIEW_SECTIONS.contains(code)) throw new IllegalArgumentException("Unsupported verification section: " + code);
    }

    private void requireComment(String comment) {
        if (comment == null || comment.isBlank()) throw new IllegalArgumentException("A clear comment is required");
    }

    private void view() { security.requirePermission("VERIFICATION_VIEW"); }

    private void requirePermission(String permission) {
        security.requirePermission(permission);
        currentAdminId();
    }

    private void requireReviewAction(String action) {
        requirePermission(switch (action) {
            case "APPROVE" -> "VERIFICATION_APPROVE";
            case "REJECT" -> "VERIFICATION_REJECT";
            case "REQUEST_CHANGES" -> "VERIFICATION_REQUEST_CHANGES";
            default -> throw new IllegalArgumentException("Unsupported review action");
        });
    }

    private void requireEvidenceAction(String action) { requireReviewAction(action); }

    private Long currentAdminId() {
        Long id = security.getAuthenticatedUserId();
        if (id == null || !adminUsers.existsByIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin account is unavailable");
        }
        return id;
    }

    private String currentAdminUsername() {
        var principal = security.currentPrincipal();
        if (principal == null || principal.username() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin username is unavailable");
        return principal.username();
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
