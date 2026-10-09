import {httpClient} from '../api/httpClient'
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
  if (/HTTP 409/.test(message)) return 'This import has changed or is already completed. Refresh its status before continuing.'
  if (/timeout|network/i.test(message)) return 'The server could not be reached. Refresh the import status before retrying an action.'
  if (/Exception|\bat [\w.$]+\(|<html|HTTP 5\d\d|trace/i.test(message)) return 'The server could not complete this request. Please try again or contact your administrator.'
  return message.slice(0, 300) || 'Unable to complete this request. Please try again.'
}
async function get<T>(path: string, params?: object): Promise<T> {
  try { return normalizeInstitutionResponse((await httpClient.get(path, {params})).data) as T }
  catch (error) { throw new Error(institutionError(error)) }
}
async function post<T>(path: string, body?: unknown): Promise<T> {
  try { return normalizeInstitutionResponse((await httpClient.post(path, body, {timeout: 0})).data) as T }
  catch (error) {
    if (error instanceof Error && /HTTP 403/.test(error.message)) window.dispatchEvent(new Event('institution-permission-denied'))
    throw new Error(institutionError(error))
  }
}
const imports = '/admin/institution-imports'
export const institutionAdminService = {
  countries: () => get<Named[]>('/countries'),
  list: (countryId: string, q: string, page: number) => get<Page<Institution>>(`/admin/institutions${q.trim() ? '/search' : ''}`, {countryId: countryId || undefined, q: q.trim() || undefined, page, size: 20, sort: 'name,asc'}),
  institution: (id: number) => get<Institution>(`/admin/institutions/${id}`),
  sources: (countryId?: string) => get<InstitutionSource[]>('/admin/institution-sources', {countryId: countryId || undefined}),
  fields: () => get<StandardField[]>('/admin/institution-standard-fields'),
  mappings: (sourceId: number) => get<SourceMapping[]>('/admin/institution-source-mappings', {sourceId}),
  create: (countryId: number, sourceId: number, version: string, importType: string) => post<InstitutionImport>(imports, {country_id: countryId, source_id: sourceId, version, import_type: importType}),
  upload: (id: number, file: File) => { const body = new FormData(); body.append('file', file); return post<InstitutionImport>(`${imports}/${id}/upload`, body) },
  validate: (id: number) => post<ImportPreview>(`${imports}/${id}/validate`),
  import: (id: number) => get<InstitutionImport>(`${imports}/${id}`),
  summary: (id: number) => get<ImportPreview>(`${imports}/${id}/summary`),
  records: (id: number, page: number, classification: Classification = '') => get<Page<ImportRecord>>(`${imports}/${id}/records`, {page, size: 20, classification: classification || undefined}),
  record: (id: number, recordId: number) => get<ImportRecord>(`${imports}/${id}/records/${recordId}`),
  duplicates: (id: number) => get<DuplicateMatch[]>(`${imports}/${id}/duplicates`),
  resolve: (id: number, matchId: number, action: 'merge' | 'keep-separate') => post<DuplicateMatch>(`${imports}/${id}/duplicates/${matchId}/${action}`),
  apply: (id: number) => post<InstitutionImport>(`${imports}/${id}/apply`),
  cancel: (id: number) => post<InstitutionImport>(`${imports}/${id}/cancel`),
  history: (page: number) => get<Page<InstitutionImport>>('/admin/institution-import-history', {page, size: 20}),
}
