package com.app.admin.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read-side projections for verification and Main App profile tables with deployment-dependent columns. */
@Repository
public class VerificationReadRepository {
    private static final Map<String, List<ProfileLink>> PROFILE_LINKS = Map.of(
            "user_addresses", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles")),
            "user_professional_profile", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles")),
            "user_education", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles"),
                    new ProfileLink("professional_profile_id", "user_professional_profile"), new ProfileLink("user_professional_profile_id", "user_professional_profile")),
            "user_work_experience", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles"),
                    new ProfileLink("professional_profile_id", "user_professional_profile"), new ProfileLink("user_professional_profile_id", "user_professional_profile")),
            "training_profiles", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles")),
            "trainer_profiles", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles")),
            "user_training_profiles", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles")),
            "user_trainer_profiles", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles")),
            "trainer_profile", List.of(new ProfileLink("profile_id", "user_profiles"), new ProfileLink("user_profile_id", "user_profiles")));
    private static final List<String> TRAINER_PROFILE_TABLES = List.of(
            "training_profiles", "trainer_profiles", "user_training_profiles", "user_trainer_profiles", "trainer_profile");
    private static final List<String> IDENTITY_TABLES = List.of(
            "verification_identity", "user_identity_verifications", "identity_verifications", "user_identity_verification", "identity_verification");
    private static final Set<String> SENSITIVE_COLUMN_PARTS = Set.of(
            "password", "secret", "token", "otp", "credential", "ssn", "aadhaar", "aadhar",
            "pan_number", "passport_number", "document_number", "storage_key");
    private final JdbcTemplate jdbc;

    public VerificationReadRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public QueueRows queueRows(int page, int size, String search, String status, String type, Long reviewer, String sortBy, String direction) {
        Map<String, String> columns = columns("users");
        String userName = userNameExpression("u", columns);
        String reviewerName = "COALESCE(NULLIF(concat_ws(' ',r.first_name,r.last_name),''),r.username)";
        String email = columns.containsKey("email") ? "u.\"" + quote(columns.get("email")) + "\"" : "CAST(u.id AS TEXT)";
        StringBuilder where = new StringBuilder(" WHERE v.status IN ('SUBMITTED','UNDER_REVIEW','CHANGES_REQUIRED')");
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            where.append(" AND (CAST(v.id AS TEXT) ILIKE ? OR CAST(u.id AS TEXT) ILIKE ? OR ").append(userName).append(" ILIKE ? OR ").append(email).append(" ILIKE ?)");
            String query = "%" + search.trim() + "%";
            args.add(query); args.add(query); args.add(query); args.add(query);
        }
        if (status != null && !status.isBlank()) { where.append(" AND v.status=?"); args.add(status); }
        if (type != null && !type.isBlank()) { where.append(" AND v.verification_type=?"); args.add(type); }
        if (reviewer != null) { where.append(" AND v.assigned_reviewer_id=?"); args.add(reviewer); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM public.user_verification_requests v JOIN public.users u ON u.id=v.user_id" + where, Long.class, args.toArray());
        String sortColumn = switch (sortBy) {
            case "id" -> "v.id"; case "status" -> "v.status"; case "type", "verificationType" -> "v.verification_type";
            case "progress", "progressPercentage" -> "v.progress_percentage"; case "updatedAt" -> "v.updated_at";
            case "submittedAt" -> "v.submitted_at"; case "user", "userName" -> userName; case "reviewer", "reviewerName" -> reviewerName;
            default -> throw new IllegalArgumentException("Unsupported sort field");
        };
        args.add(size); args.add((long) page * size);
        String sql = "SELECT v.id,v.user_id,v.verification_type,v.status,v.current_step,v.progress_percentage,v.submitted_at,v.assigned_reviewer_id,v.created_at,v.updated_at, "
                + userName + " AS user_name," + reviewerName + " AS reviewer_name,r.username AS reviewer_username FROM public.user_verification_requests v "
                + "JOIN public.users u ON u.id=v.user_id LEFT JOIN public.admin_users r ON r.id=v.assigned_reviewer_id" + where
                + " ORDER BY " + sortColumn + " " + direction + " NULLS LAST,v.id DESC LIMIT ? OFFSET ?";
        return new QueueRows(jdbc.queryForList(sql, args.toArray()), total == null ? 0 : total);
    }

    public Map<String, Long> counts() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT status,COUNT(*) AS count FROM public.user_verification_requests WHERE status IN ('SUBMITTED','UNDER_REVIEW','CHANGES_REQUIRED','VERIFIED','REJECTED') GROUP BY status");
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of("SUBMITTED", "UNDER_REVIEW", "CHANGES_REQUIRED", "VERIFIED", "REJECTED")) counts.put(status, 0L);
        rows.forEach(row -> counts.put(String.valueOf(row.get("status")), ((Number) row.get("count")).longValue()));
        return counts;
    }

    public Map<String, Object> requestDetail(long id) {
        Map<String, String> userColumns = columns("users");
        String name = userNameExpression("u", userColumns);
        String email = userColumns.containsKey("email") ? "u.\"" + quote(userColumns.get("email")) + "\"" : "NULL::TEXT";
        String reviewerName = "COALESCE(NULLIF(concat_ws(' ',r.first_name,r.last_name),''),r.username)";
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT v.id,v.user_id,v.verification_type,v.status,v.current_step,v.progress_percentage,v.submitted_at,v.reviewed_at,v.verified_at,v.expires_at,v.changes_requested_at,v.admin_notes,v.rejection_reason,v.assigned_reviewer_id,v.created_at,v.updated_at,u.id AS subject_id,"
                + name + " AS user_name," + email + " AS user_email," + reviewerName + " AS reviewer_name,r.username AS reviewer_username FROM public.user_verification_requests v "
                + "JOIN public.users u ON u.id=v.user_id LEFT JOIN public.admin_users r ON r.id=v.assigned_reviewer_id WHERE v.id=?", id);
        return rows.stream().findFirst().orElse(null);
    }

    public List<Map<String, Object>> profileRows(String table, long userId) {
        if ("training_profiles".equals(table)) {
            for (String candidate : trainerProfileTables()) {
                List<Map<String, Object>> rows = profileRowsFromTable(candidate, userId);
                if (!rows.isEmpty()) return rows;
            }
            return List.of();
        }
        return profileRowsFromTable(table, userId);
    }

    private List<Map<String, Object>> profileRowsFromTable(String table, long userId) {
        if (!Set.of("users", "user_profiles", "user_addresses", "user_professional_profile", "user_education", "user_work_experience",
                "training_profiles", "trainer_profiles", "user_training_profiles", "user_trainer_profiles", "trainer_profile").contains(table)
                && !table.matches("(?i)(?=.*(?:trainer|training))(?=.*profile)[a-z0-9_]+")) return List.of();
        Map<String, String> availableColumns = columns(table);
        if (availableColumns.isEmpty()) return List.of();
        List<String> selected = availableColumns.values().stream()
                .filter(column -> !sensitiveColumn(column)).toList();
        if (selected.isEmpty()) return List.of();
        String projection = String.join(",", selected.stream().map(column -> "t.\"" + quote(column) + "\"").toList());
        if ("users".equals(table)) return jdbc.queryForList("SELECT " + projection + " FROM public.users t WHERE t.id=?", userId);
        if (availableColumns.containsKey("user_id")) return jdbc.queryForList("SELECT " + projection + " FROM public.\"" + quote(table) + "\" t WHERE t.user_id=?", userId);
        List<ProfileLink> links = new ArrayList<>(PROFILE_LINKS.getOrDefault(table, List.of()));
        if (table.matches("(?i)(?=.*(?:trainer|training))(?=.*profile)[a-z0-9_]+")) {
            links.add(new ProfileLink("profile_id", "user_profiles"));
            links.add(new ProfileLink("user_profile_id", "user_profiles"));
            links.add(new ProfileLink("professional_profile_id", "user_professional_profile"));
            links.add(new ProfileLink("user_professional_profile_id", "user_professional_profile"));
        }
        for (ProfileLink link : links) {
            if (!availableColumns.containsKey(link.foreignKey())) continue;
            Map<String, String> parentColumns = columns(link.parentTable());
            if (parentColumns.isEmpty()) continue;
            String parentAlias = "p";
            String join = " JOIN public.\"" + quote(link.parentTable()) + "\" " + parentAlias
                    + " ON " + parentAlias + ".id=t.\"" + quote(availableColumns.get(link.foreignKey())) + "\"";
            if (parentColumns.containsKey("user_id")) {
                return jdbc.queryForList("SELECT " + projection + " FROM public.\"" + quote(table) + "\" t" + join
                        + " WHERE " + parentAlias + ".\"" + quote(parentColumns.get("user_id")) + "\"=?", userId);
            }
            for (String parentLink : List.of("user_profile_id", "profile_id")) {
                if (parentColumns.containsKey(parentLink) && columns("user_profiles").containsKey("user_id")) {
                    return jdbc.queryForList("SELECT " + projection + " FROM public.\"" + quote(table) + "\" t" + join
                            + " JOIN public.user_profiles up ON up.id=" + parentAlias + ".\"" + quote(parentColumns.get(parentLink))
                            + "\" WHERE up.user_id=?", userId);
                }
            }
        }
        return List.of();
    }

    public List<Map<String, Object>> identityRows(long requestId, long userId) {
        for (String table : identityTables()) {
            Map<String, String> available = columns(table);
            if (available.isEmpty()) continue;
            String key = available.containsKey("verification_request_id") ? "verification_request_id"
                    : available.containsKey("user_id") ? "user_id" : null;
            if (key == null) continue;
            List<String> selected = available.values().stream()
                    .filter(column -> !sensitiveColumn(column) && !isDocumentStorageKey(column)).toList();
            List<String> projectionParts = new ArrayList<>(selected.stream().map(column -> "i.\"" + quote(column) + "\"").toList());
            available.values().stream().filter(this::isDocumentStorageKey).forEach(column ->
                    projectionParts.add("i.\"" + quote(column) + "\" AS \"_download_key_" + quote(column) + "\""));
            if (projectionParts.isEmpty()) continue;
            String projection = String.join(",", projectionParts);
            String reviewerColumn = available.containsKey("reviewed_by") ? available.get("reviewed_by")
                    : available.get("reviewed_by_user_id");
            String reviewer = reviewerColumn == null ? "" : ",a.username AS reviewed_by_username";
            String join = reviewerColumn == null ? "" : " LEFT JOIN public.admin_users a ON a.id=i.\"" + quote(reviewerColumn) + "\"";
            long value = "verification_request_id".equals(key) ? requestId : userId;
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + projection + reviewer + " FROM public.\"" + quote(table) + "\" i" + join
                    + " WHERE i.\"" + quote(available.get(key)) + "\"=?", value);
            if (!rows.isEmpty()) return rows;
        }
        return List.of();
    }

    public int updateIdentityReview(long requestId, long userId, String reviewStatus,
            java.time.LocalDateTime reviewedAt, String reviewNotes) {
        for (String table : identityTables()) {
            Map<String, String> available = columns(table);
            String requestColumn = available.get("verification_request_id");
            String userColumn = available.get("user_id");
            String keyColumn = requestColumn != null ? requestColumn : userColumn;
            if (keyColumn == null) continue;
            String reviewColumn = firstColumn(available, "review_status", "admin_review_status", "identity_review_status");
            if (reviewColumn == null) continue;

            List<String> assignments = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            assignments.add("\"" + quote(reviewColumn) + "\"=?");
            args.add(reviewStatus);
            String notesColumn = firstColumn(available, "review_notes", "admin_review_notes", "reviewer_notes");
            if (notesColumn != null) { assignments.add("\"" + quote(notesColumn) + "\"=?"); args.add(reviewNotes); }
            String reviewedAtColumn = firstColumn(available, "reviewed_at", "admin_reviewed_at");
            if (reviewedAtColumn != null) { assignments.add("\"" + quote(reviewedAtColumn) + "\"=?"); args.add(reviewedAt); }
            String updatedAtColumn = available.get("updated_at");
            if (updatedAtColumn != null) { assignments.add("\"" + quote(updatedAtColumn) + "\"=?"); args.add(reviewedAt); }
            args.add(requestColumn != null ? requestId : userId);
            int updated = jdbc.update("UPDATE public.\"" + quote(table) + "\" SET " + String.join(",", assignments)
                    + " WHERE \"" + quote(keyColumn) + "\"=?", args.toArray());
            if (updated > 0) return updated;
        }
        return 0;
    }

    private List<String> trainerProfileTables() {
        return matchingTables(TRAINER_PROFILE_TABLES, "(?i)(?=.*(?:trainer|training))(?=.*profile)[a-z0-9_]+");
    }

    private List<String> identityTables() {
        return matchingTables(IDENTITY_TABLES, "(?i)[a-z0-9_]*(identity[a-z0-9_]*verif|verif[a-z0-9_]*identity)[a-z0-9_]*");
    }

    private List<String> matchingTables(List<String> preferred, String pattern) {
        Set<String> available = new java.util.LinkedHashSet<>();
        jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name")
                .forEach(row -> available.add(String.valueOf(row.get("table_name"))));
        List<String> result = new ArrayList<>();
        preferred.stream().filter(available::contains).forEach(result::add);
        available.stream().filter(name -> name.matches(pattern) && !result.contains(name)).forEach(result::add);
        return result;
    }

    public List<Map<String, Object>> sectionRows(long requestId) {
        return jdbc.queryForList("SELECT s.id,s.section_code,s.user_status,s.review_status,s.review_notes,s.reviewed_by,a.username AS reviewed_by_username,s.reviewed_at FROM verification_sections s LEFT JOIN public.admin_users a ON a.id=s.reviewed_by WHERE s.verification_request_id=? ORDER BY s.id", requestId);
    }

    public List<Map<String, Object>> evidenceRows(long requestId) {
        String table = columns("user_verification_evidence").isEmpty()
                ? "verification_evidence" : "user_verification_evidence";
        Map<String, String> available = columns(table);
        List<String> selected = available.values().stream()
                .filter(column -> !sensitiveColumn(column) && !isDocumentStorageKey(column)
                        && !Set.of("review_status", "review_notes", "reviewed_by", "reviewed_at").contains(column.toLowerCase(Locale.ROOT))).toList();
        List<String> projectionParts = new ArrayList<>(selected.stream().map(column -> "e.\"" + quote(column) + "\"").toList());
        available.values().stream().filter(this::isDocumentStorageKey).forEach(column ->
                projectionParts.add("e.\"" + quote(column) + "\" AS \"_download_key_" + quote(column) + "\""));
        if (projectionParts.isEmpty()) return List.of();
        String projection = String.join(",", projectionParts);
        Map<String, String> reviewColumns = columns("verification_evidence_reviews");
        String reviewer = reviewColumns.isEmpty() ? ",CAST('PENDING' AS TEXT) AS review_status,NULL::TEXT AS review_notes,NULL::BIGINT AS reviewed_by,NULL::TIMESTAMP AS reviewed_at,NULL::TEXT AS reviewed_by_username"
                : ",COALESCE(er.review_status,'PENDING') AS review_status,er.review_notes,er.reviewed_by,er.reviewed_at,a.username AS reviewed_by_username";
        String reviewJoin = reviewColumns.isEmpty() ? ""
                : " LEFT JOIN public.verification_evidence_reviews er ON er.source_evidence_id=e.\"" + quote(available.getOrDefault("id", "id"))
                    + "\" AND er.verification_request_id=e.\"" + quote(available.getOrDefault("verification_request_id", "verification_request_id"))
                    + "\" LEFT JOIN public.admin_users a ON a.id=er.reviewed_by";
        String legacyReviewer = !reviewColumns.isEmpty() || !available.containsKey("reviewed_by") ? ""
                : ",a.username AS reviewed_by_username";
        String legacyJoin = !reviewColumns.isEmpty() || !available.containsKey("reviewed_by") ? ""
                : " LEFT JOIN public.admin_users a ON a.id=e.\"" + quote(available.get("reviewed_by")) + "\"";
        String requestColumn = available.getOrDefault("verification_request_id", "verification_request_id");
        String idColumn = available.getOrDefault("id", "id");
        return jdbc.queryForList("SELECT " + projection + reviewer + legacyReviewer + " FROM public.\"" + quote(table) + "\" e" + reviewJoin + legacyJoin
                + " WHERE e.\"" + quote(requestColumn) + "\"=? ORDER BY e.\"" + quote(idColumn) + "\"", requestId);
    }

    public Map<String, Object> evidenceItem(long requestId, long evidenceId) {
        return evidenceRows(requestId).stream()
                .filter(row -> row.get("id") instanceof Number id && id.longValue() == evidenceId)
                .findFirst().orElse(null);
    }

    public List<Map<String, Object>> historyRows(long requestId) {
        return jdbc.queryForList("SELECT h.id,h.admin_user_id,u.username AS admin_username,COALESCE(NULLIF(concat_ws(' ',u.first_name,u.last_name),''),u.username) AS admin_name,h.action,h.section_code,h.evidence_id,h.previous_status,h.new_status,h.comments,h.created_at FROM verification_review_history h LEFT JOIN public.admin_users u ON u.id=h.admin_user_id WHERE h.verification_request_id=? ORDER BY h.created_at,h.id", requestId);
    }

    public Map<String, Object> evidenceFile(long requestId, long evidenceId) {
        String table = columns("user_verification_evidence").isEmpty()
                ? "verification_evidence" : "user_verification_evidence";
        Map<String, String> available = columns(table);
        String keyColumn = available.get("storage_key");
        String statusColumn = available.containsKey("status") ? available.get("status") : available.get("upload_status");
        String containerColumn = available.get("storage_container");
        String requestColumn = available.get("verification_request_id");
        String idColumn = available.get("id");
        if (keyColumn == null || statusColumn == null || requestColumn == null || idColumn == null) return null;
        String containerProjection = containerColumn == null ? "NULL AS storage_container"
                : "\"" + quote(containerColumn) + "\" AS storage_container";
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT \"" + quote(keyColumn) + "\" AS storage_key,\""
                + quote(statusColumn) + "\" AS upload_status," + containerProjection + " FROM public.\"" + quote(table) + "\" WHERE \""
                + quote(requestColumn) + "\"=? AND \"" + quote(idColumn) + "\"=?", requestId, evidenceId);
        return rows.stream().findFirst().orElse(null);
    }

    private Map<String, String> columns(String table) {
        Map<String, String> columns = new HashMap<>();
        jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name=? ORDER BY ordinal_position", table)
                .forEach(row -> { String name = String.valueOf(row.get("column_name")); columns.put(name.toLowerCase(Locale.ROOT), name); });
        return Collections.unmodifiableMap(columns);
    }

    private String userNameExpression(String alias, Map<String, String> columns) {
        List<String> candidates = List.of("name", "full_name", "display_name", "username", "first_name", "last_name");
        List<String> present = candidates.stream().filter(columns::containsKey).map(columns::get).toList();
        if (present.isEmpty()) return "CAST(" + alias + ".id AS TEXT)";
        List<String> selected = present.stream().filter(column -> Set.of("name", "full_name", "display_name", "username").contains(column.toLowerCase(Locale.ROOT))).toList();
        if (selected.isEmpty()) selected = present;
        List<String> expressions = selected.stream().map(column -> alias + ".\"" + quote(column) + "\"").toList();
        return "COALESCE(NULLIF(CONCAT_WS(' '," + String.join(",", expressions) + "),''),CAST(" + alias + ".id AS TEXT))";
    }

    private String quote(String value) { return value.replace("\"", "\"\""); }

    private boolean sensitiveColumn(String column) {
        String normalized = column.toLowerCase(Locale.ROOT);
        return SENSITIVE_COLUMN_PARTS.stream().anyMatch(normalized::contains);
    }

    private boolean isDocumentStorageKey(String column) {
        String normalized = column.toLowerCase(Locale.ROOT);
        boolean pathField = normalized.endsWith("_key") || normalized.endsWith("_path") || normalized.endsWith("_blob_name");
        return pathField && (normalized.contains("document") || normalized.contains("image")
                || normalized.contains("file") || normalized.contains("storage") || normalized.contains("blob"));
    }

    private String firstColumn(Map<String, String> columns, String... candidates) {
        for (String candidate : candidates) if (columns.containsKey(candidate)) return columns.get(candidate);
        return null;
    }

    private record ProfileLink(String foreignKey, String parentTable) { }

    public record QueueRows(List<Map<String, Object>> rows, long total) { }
}
