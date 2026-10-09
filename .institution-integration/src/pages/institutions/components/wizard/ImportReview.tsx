import type {InstitutionImport, ImportSummary} from '../../../../types/institution'
import ValidationSummary from './ValidationSummary'
import ImportRecordsTable from './ImportRecordsTable'
import {Details, Notice} from '../Shared'
export default function ImportReview({item, summary, unresolved, ready, busy, allowed, start, cancel}: {item: InstitutionImport; summary: ImportSummary; unresolved: number; ready: boolean; busy: boolean; allowed: boolean; start: () => void; cancel: () => void}) {
  return <><h2>Review Import</h2><Details values={{Source: item.source?.sourceName || item.sourceId, Country: item.country?.name || item.countryId, Version: item.version, File: item.fileName}}/><ValidationSummary summary={summary}/><ImportRecordsTable importId={item.id}/>{unresolved > 0 && <Notice>{unresolved} duplicate records require review.</Notice>}{summary.errorCount > 0 && <Notice>{summary.errorCount} errors must be corrected before importing. Cancel this import and upload a corrected file.</Notice>}<div className="im-actions"><button disabled={busy || !allowed} onClick={cancel}>Cancel Import</button><button className="primary" disabled={!ready || busy || !allowed || unresolved > 0 || summary.errorCount > 0} onClick={start}>Start Import</button></div></>
}
