import {httpClient} from '../api/httpClient'
import type {InstitutionDashboard, InstitutionFilters, InstitutionMetadata, ImportHistoryFilters} from '../types/institution'
import type {Classification, DuplicateMatch, ImportPreview, ImportRecord, Institution, InstitutionImport, InstitutionSource, Named, Page, SourceMapping, StandardField} from '../types/institution'

// Normalize Java's SNAKE_CASE responses at this boundary; retain camelCase UI types.
export function normalizeInstitutionResponse(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(normalizeInstitutionResponse)
  if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([key, item]) => [key.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase()), key === 'raw_data' || key === 'rawData' ? item : normalizeInstitutionResponse(item)]))
  return value
}
export function institutionError(error: unknown): string {
  const message = error instanceof Error ? error.message : ''
  if (/HTTP 403/.test(message)) return 'You do not have permission to perform this action.'
  if (/HTTP 401/.test(message)) return 'Your session has expired. Sign in again.'
  if (/HTTP 409/.test(message)) {
    // Preserve the backend's conflict detail (for example a database
    // constraint) so the operator can correct the request instead of seeing
    // an unrelated generic message.
    const detail = message.replace(/\s*\(HTTP 409\)\s*$/, '').trim()
    if (detail && !/^(Error|Exception)(:|$)/i.test(detail)) return detail
    return 'The import conflicts with the current server data. Refresh and try again.'
  }
  if (/timeout|network/i.test(message)) return 'The server could not be reached. Refresh the import status before retrying an action.'
  if (/Exception|\bat [\w.$]+\(|<html|HTTP 5\d\d|trace/i.test(message)) return 'The server could not complete this request. Please try again or contact your administrator.'
  return message.slice(0, 300) || 'Unable to complete this request. Please try again.'
}
async function get<T>(path: string, params?: object): Promise<T> {
  try { return normalizeInstitutionResponse((await httpClient.get(path, {params})).data) as T }
  catch (error) { throw new Error(institutionError(error)) }
}
export function institutionPage<T>(value: unknown): Page<T> {
  if (!value || typeof value !== 'object' || !('content' in value) || !Array.isArray(value.content)) throw new Error('The server returned an invalid list response. Please contact your administrator.')
  const data = value as Record<string, unknown>
  // Spring can serialize Page directly, or put its metadata inside a page object.
  const metadata = data.page && typeof data.page === 'object' ? data.page as Record<string, unknown> : data
  const {totalElements, totalPages, number, size} = metadata
  if (![totalElements, totalPages, number, size].every(item => typeof item === 'number' && Number.isInteger(item) && item >= 0)) throw new Error('The server returned invalid pagination details. Please contact your administrator.')
  return {content: data.content as T[], totalElements: totalElements as number, totalPages: totalPages as number, number: number as number, size: size as number}
}
async function getPage<T>(path: string, params?: object): Promise<Page<T>> { return institutionPage<T>(await get<unknown>(path, params)) }
async function post<T>(path: string, body?: unknown): Promise<T> {
  try { return normalizeInstitutionResponse((await httpClient.post(path, body, {timeout: 0})).data) as T }
  catch (error) {
    if (error instanceof Error && /HTTP 403/.test(error.message)) window.dispatchEvent(new Event('institution-permission-denied'))
    throw new Error(institutionError(error))
  }
}
const imports = '/admin/institution-imports'
export const institutionAdminService = {
  countries: () => get<Named[]>('/admin/institution-countries'),
  overview: () => get<InstitutionDashboard>('/admin/institutions/summary'),
  metadata: () => get<InstitutionMetadata>('/admin/institutions/metadata'),
  list: (countryId: string, q: string, page: number, filters: InstitutionFilters = {}) => getPage<Institution>(`/admin/institutions${q.trim() ? '/search' : ''}`, {countryId: countryId || undefined, q: q.trim() || undefined, ...filters, page, size: 20, sort: 'name,asc'}),
  institution: (id: number) => get<Institution>(`/admin/institutions/${id}`),
  sources: (countryId?: string) => get<InstitutionSource[]>('/admin/institution-sources', {countryId: countryId || undefined}),
  fields: () => get<StandardField[]>('/admin/institution-standard-fields'),
  mappings: (sourceId: number) => get<SourceMapping[]>('/admin/institution-source-mappings', {sourceId}),
  saveMappings: async (sourceId: number, rows: SourceMapping[]) => {
    try { return normalizeInstitutionResponse((await httpClient.put('/admin/institution-source-mappings', rows.map(row => ({source_field_name: row.sourceFieldName.trim(), standard_field_code: row.standardFieldCode, transformation_rule: row.transformationRule || null})), {params: {sourceId}})).data) as SourceMapping[] }
    catch (error) { throw new Error(institutionError(error)) }
  },
  create: (countryId: number, sourceId: number, version: string, importType: string) => post<InstitutionImport>(imports, {country_id: countryId, source_id: sourceId, version, import_type: importType}),
  upload: (id: number, file: File) => { const body = new FormData(); body.append('file', file); return post<InstitutionImport>(`${imports}/${id}/upload`, body) },
  validate: (id: number) => post<ImportPreview>(`${imports}/${id}/validate`),
  import: (id: number) => get<InstitutionImport>(`${imports}/${id}`),
  summary: (id: number) => get<ImportPreview>(`${imports}/${id}/summary`),
  records: (id: number, page: number, classification: Classification = '', q = '') => getPage<ImportRecord>(`${imports}/${id}/records`, {page, size: 20, classification: classification || undefined, q: q.trim() || undefined}),
  record: (id: number, recordId: number) => get<ImportRecord>(`${imports}/${id}/records/${recordId}`),
  duplicates: (id: number) => get<DuplicateMatch[]>(`${imports}/${id}/duplicates`),
  resolve: (id: number, matchId: number, action: 'merge' | 'keep-separate') => post<DuplicateMatch>(`${imports}/${id}/duplicates/${matchId}/${action}`),
  apply: (id: number) => post<InstitutionImport>(`${imports}/${id}/apply`),
  cancel: (id: number) => post<InstitutionImport>(`${imports}/${id}/cancel`),
  history: (page: number, filters: ImportHistoryFilters = {}) => getPage<InstitutionImport>('/admin/institution-import-history', {page, size: 20, ...Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''))}),
}
