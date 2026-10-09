export interface Named { id: number; name: string }
export interface InstitutionSource {
  id: number; countryId: number; sourceName: string; sourceCode: string; sourceType: string;
  sourceUrl?: string; description?: string; importMethod: string; priority: number; active: boolean;
  country?: Named; lastImportAt?: string;
}
export interface SourceRecord { id: number; source?: InstitutionSource; sourceIdentifier?: string; lastSeenAt?: string; rawData?: Record<string, unknown> }
export interface Institution {
  id: number; name: string; shortName?: string; institutionType?: string; countryId: number;
  country?: Named; administrativeArea?: Named; city?: Named; cityName?: string; address?: string;
  website?: string; verificationStatus: string; active: boolean; source?: InstitutionSource;
  sourceIdentifier?: string; createdAt?: string; updatedAt?: string; lastSeenAt?: string;
  aliases?: {aliasName: string}[]; sourceRecords?: SourceRecord[];
}
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface ImportSummary { totalRecords: number; validRecords: number; newCount: number; updatedCount: number; unchangedCount: number; duplicateCount: number; errorCount: number }
export interface InstitutionImport extends Partial<ImportSummary> {
  id: number; countryId: number; sourceId: number; country?: Named; source?: InstitutionSource;
  version: string; importType: string; fileName?: string; status: string; startedAt?: string;
  completedAt?: string; createdAt?: string; insertedCount?: number; processedRecords?: number; errorMessage?: string;
}
export interface ImportPreview { importId: number; status: string; summary: ImportSummary }
export type Classification = '' | 'NEW' | 'UPDATE' | 'UNCHANGED' | 'DUPLICATE' | 'ERROR';
export interface ImportRecord {
  id: number; rowNumber?: number; rowIndex?: number; sourceIdentifier?: string; action?: Classification;
  classification?: Classification; normalizedData?: Record<string, unknown>; mappedData?: Record<string, unknown>;
  rawData?: Record<string, unknown>; matchConfidence?: number; processingStatus?: string; validationStatus?: string;
  errorMessage?: string; validationErrors?: string[]; institutionId?: number; targetInstitutionId?: number; institution?: Institution;
}
export interface DuplicateMatch { id: number; importRecordId: number; institutionId: number; matchScore: number; resolution: string; matchReason?: Record<string, unknown>; institution?: Institution; importRecord?: ImportRecord }
export interface StandardField { id: number; fieldCode: string; fieldName: string; isRequired: boolean; active: boolean }
export interface SourceMapping { id: number; sourceFieldName: string; standardFieldCode: string; transformationRule?: string; isRequired: boolean; active: boolean }
