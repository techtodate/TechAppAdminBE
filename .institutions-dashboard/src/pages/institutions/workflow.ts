import type {DuplicateMatch, InstitutionImport} from '../../types/institution'
export const reviewable = (status?: string) => status === 'READY_FOR_REVIEW' || status === 'VALIDATION_COMPLETED'
export function resumeStep(item: InstitutionImport, requested: number) {
  if (item.status === 'COMPLETED') return 7
  if (item.status === 'IMPORTING' || item.status === 'PROCESSING') return 6
  if (reviewable(item.status)) return requested >= 3 && requested <= 5 ? requested : 3
  if (item.status === 'UPLOADED') return requested === 1 ? 1 : 2
  return 1
}
export function groupMatches(matches: DuplicateMatch[]) { const groups = new Map<number, DuplicateMatch[]>(); for (const match of matches) { const group = groups.get(match.importRecordId) ?? []; group.push(match); groups.set(match.importRecordId, group) } return groups }
export function unresolvedRecords(matches: DuplicateMatch[]) { return [...groupMatches(matches).values()].filter(rows => !rows.some(row => row.resolution === 'MERGED' || row.resolution === 'KEPT_SEPARATE')).length }
