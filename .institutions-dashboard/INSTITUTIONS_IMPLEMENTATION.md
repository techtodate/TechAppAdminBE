# Institution Master

## Run and verify

```sh
npm install
npm run dev
npm run build
npm run lint
node --test tests/institutions.test.cjs
```

Uses the existing Axios `VITE_API_BASE_URL` (default `http://localhost:8082/api`), React Query, admin shell, notification provider and loading/error components. No additional framework or dependency is added. Dialogs use native accessible modal dialogs with focus restoration. Data changes remain subject to backend authorization.

## Routes

- `/master-data/institutions`: server-paginated list, country filter and debounced server search; details, source information and editing availability dialog.
- `/master-data/institutions/sources`: country-based source browsing and read-only details.
- `/master-data/institutions/import`: create an import using server-supplied countries/sources.
- `/master-data/institutions/import/:importId`: resumable eight-step workflow. The server owns import state; the URL holds the ID and current review step. Reload recovers source, version, uploaded filename, mappings and results. A file not uploaded yet must be reselected after reload.
- `/master-data/institutions/history`: paginated import history.
- `/master-data/institutions/history/:importId`: summary, records, duplicates, errors and resume action.

## Institutions dashboard

The main Institutions page now includes an overview and latest-import dashboard:

- Institution total from `GET /api/admin/institutions?page=0&size=1` (server `totalElements`, not the current page length).
- Configured source count and countries with sources from `GET /api/admin/institution-sources`.
- Import total and five recent imports from `GET /api/admin/institution-import-history?page=0&size=20`.
- Latest import validation cards from `GET /api/admin/institution-imports/{id}/summary`.
- Links to import details and resumable imports, explicit empty/error states and a refresh action.

Overview counts span all countries; country/search controls below apply to the institution list only. History refreshes every three seconds while a returned import is processing, otherwise every 30 seconds. Institution totals also refresh every 30 seconds and are invalidated after import mutations. No unimplemented `/institutions/summary` endpoint is called. Paginated APIs accept direct Spring Page serialization or nested `page` metadata and reject invalid payloads instead of showing misleading zero totals.

New component: `src/pages/institutions/components/InstitutionDashboard.tsx`. Dashboard request adapters, rendering, pagination, empty/error states, recovery and apply guards are covered by `tests/institutions.test.cjs`.

## Files

Updated existing integration files: `src/App.tsx`, `src/components/layout/AdminLayout.tsx`.

Implemented the formerly empty `src/types/institution.ts`, `src/services/institutionAdminService.ts`, `src/hooks/useAdminPermissions.ts`, all six files in `src/pages/institutions/`, all six direct components and all ten wizard components in that directory.

Added `src/pages/institutions/components/Shared.tsx` for navigation, pagination, fields, badges, dialogs, safe external links, notices and query states; `components/formatting.ts` for display helpers; `src/pages/institutions/workflow.ts` for recovery and duplicate-resolution rules; this document and `tests/institutions.test.cjs`.

`institutionAdminService` integrates all endpoints in the requested prompt plus individual import-record lookup. It uses the existing HTTP client, converts snake_case responses to UI types, preserves raw source keys, submits snake_case create payloads and multipart uploads, and sanitizes server errors. Long-running mutation calls are not automatically retried. Apply rechecks status, errors and duplicates immediately before submission.

## Backend dependencies / remaining prompt gaps

These reflect the checked-in Java controllers and services, not assumptions that APIs exist:

1. Institution list supports country and name search only. Type, verification and active filters are disabled and labeled unavailable. Status defaults to **All statuses**, since defaulting to Active would falsely describe unfiltered results. Requires server filter parameters and type options to meet the full prompt.
2. No institution update endpoint exists. Edit explains the limitation; no unsupported PUT is sent.
3. Institution responses do not expose alias/source-record collections or last-seen dates. Components can render these when supplied; today they explicitly indicate missing data. No fabricated records or timestamps.
4. Mapping and standard-field APIs are GET-only. Saved mappings display as read-only; Save Mapping is disabled. Required unmapped fields block validation. An authorized save contract and source-header/sample endpoint are needed for editable mapping and upload preview.
5. Upload accepts CSV/TXT, not XLSX. UI validates these formats and explains CSV conversion. Record counts are available after validation, not upload. XLSX requires backend support or a separately agreed conversion library.
6. No Ignore duplicate endpoint exists. Ignore is disabled. Merge / Keep Separate use the real endpoints, with candidate selection and confirmation. A record is resolved when any candidate has a saved resolution; unselected candidates need not be merged.
7. History endpoint has pagination but no filters. Its country/source/status/date filters explicitly apply to the current page. Record classification is server-side; record text search explicitly applies to the current page. Full-dataset history/text filters need backend support.
8. Source `lastImportAt` is not provided. UI says Not provided instead of inventing a date.
9. Current backend `AdminSecurityContext` defaults all admins to VIEW/IMPORT/MANAGE, and the frontend has no authenticated identity/permission API. The permission context mirrors that model, can accept authoritative grants from a future authenticated shell, and disables mutations after a 403. It is not a security boundary and does not fabricate permission headers. A real identity/grants integration remains needed for role-specific permissions before the first request.
10. Validation/apply are synchronous transactions. UI polls status and shows indeterminate progress until real processed counts are supplied. It never simulates percentages. Per-record live counts need a backend progress contract.
11. The backend stores uploaded file path via a no-op setter and reads it via a getter returning null in the inspected source. This can block validation regardless of frontend behavior; requires backend verification. Import count aliases and duplicate summary recomputation also need backend confirmation before counts can be described as actual applied totals.

## Manual acceptance checks

- List: verify request paging and country/search parameters; empty/error/retry states; inspect details and source data.
- Wizard: create, upload CSV, refresh URL, verify saved mapping, validate, inspect classifications and changed fields, select and confirm duplicate candidate, review and confirm apply. Ensure Start Import is disabled for errors/pending duplicate records and while applying.
- Test FAILED/CANCELLED/COMPLETED/PROCESSING states, reload during long calls, permission denial and interrupted requests. Check history links and status refresh.
- Check desktop, tablet row expansion and mobile cards, keyboard modal focus/Escape, safe external URLs and no stack traces in errors.
- Use a disposable test source before executing apply; frontend validation cannot guarantee correctness of backend persistence and counters.
