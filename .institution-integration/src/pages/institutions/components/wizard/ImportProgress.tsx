import type {InstitutionImport} from '../../../../types/institution'
import ValidationSummary from './ValidationSummary'
import {Notice} from '../Shared'
export default function ImportProgress({item, validating = false}: {item: InstitutionImport; validating?: boolean}) {
  const known = item.processedRecords != null && !!item.totalRecords
  return <section aria-live="polite"><h2>{validating ? 'Validating source records…' : 'Importing institutions…'}</h2><progress aria-label={validating ? 'Validation progress' : 'Import progress'} max={item.totalRecords || 1} value={known ? item.processedRecords : undefined}/><p>{known ? `Processing: ${item.processedRecords} / ${item.totalRecords}` : 'Waiting for the server to finish processing.'}</p><ValidationSummary summary={item}/><Notice>Status refreshes automatically. The server may publish counts only after processing completes.</Notice></section>
}
