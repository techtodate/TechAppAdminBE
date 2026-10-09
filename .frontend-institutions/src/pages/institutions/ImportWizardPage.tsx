import {useEffect, useRef, useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {Link, useNavigate, useParams, useSearchParams} from 'react-router-dom'
import {institutionAdminService as api} from '../../services/institutionAdminService'
import {useAdminPermissions} from '../../hooks/useAdminPermissions'
import {useSnackbar} from '../../hooks/useSnackbar'
import {reviewable, resumeStep, unresolvedRecords} from './workflow'
import {Confirmation, Notice, PageHeader, QueryState, root} from './components/Shared'
import ImportWizardSteps from './components/wizard/ImportWizardSteps'
import ImportSourceStep from './components/wizard/ImportSourceStep'
import ImportUploadStep from './components/wizard/ImportUploadStep'
import FieldMappingTable from './components/wizard/FieldMappingTable'
import ValidationSummary from './components/wizard/ValidationSummary'
import ImportRecordsTable from './components/wizard/ImportRecordsTable'
import DuplicateReview from './components/wizard/DuplicateReview'
import ImportReview from './components/wizard/ImportReview'
import ImportProgress from './components/wizard/ImportProgress'
import ImportComplete from './components/wizard/ImportComplete'
import './InstitutionMaster.css'

export default function ImportWizardPage() {
  const {importId} = useParams(), id = Number(importId), validId = Number.isSafeInteger(id) && id > 0
  const [params, setParams] = useSearchParams(), navigate = useNavigate(), client = useQueryClient(), {show} = useSnackbar(), {canImport} = useAdminPermissions()
  const [confirmation, setConfirmation] = useState<'apply' | 'cancel' | null>(null), [operation, setOperation] = useState(''), [uncertain, setUncertain] = useState(false), lock = useRef(false)
  const query = useQuery({queryKey: ['institution-import', id], queryFn: () => api.import(id), enabled: validId, refetchInterval: q => operation || ['PROCESSING','IMPORTING'].includes(q.state.data?.status || '') ? 2000 : false})
  const item = query.data, ready = reviewable(item?.status)
  const summary = useQuery({queryKey: ['institution-summary', id], queryFn: () => api.summary(id), enabled: validId && ready})
  const duplicates = useQuery({queryKey: ['institution-duplicates', id], queryFn: () => api.duplicates(id), enabled: validId && ready})
  const go = (step: number) => { if (window.location.pathname === `${root}/import/${id}`) setParams({step: String(step)}, {replace: true}) }
  const refresh = async () => { await Promise.all([client.invalidateQueries({queryKey: ['institution-import', id]}), client.invalidateQueries({queryKey: ['institution-summary', id]}), client.invalidateQueries({queryKey: ['institution-records', id]}), client.invalidateQueries({queryKey: ['institution-duplicates', id]}), client.invalidateQueries({queryKey: ['institution-history']}), client.invalidateQueries({queryKey: ['institutions']})]) }
  const mutation = useMutation({mutationFn: async (task: () => Promise<unknown>) => task(), onError: (error: Error) => {show(error.message, 'error'); setUncertain(/could not be reached|could not complete/.test(error.message))}, onSettled: async () => {lock.current = false; setOperation(''); setConfirmation(null); await refresh()}})
  function run(name: string, task: () => Promise<unknown>) { if (lock.current || !canImport) return; lock.current = true; setOperation(name); mutation.mutate(task) }
  useEffect(() => { if (!operation) return; const preventClose = (event: BeforeUnloadEvent) => event.preventDefault(); window.addEventListener('beforeunload', preventClose); return () => window.removeEventListener('beforeunload', preventClose) }, [operation])
  const busy = mutation.isPending, requested = Number(params.get('step')), step = operation === 'apply' || operation === 'validate' ? 6 : item ? resumeStep(item, requested) : 0
  const terminal = item && ['FAILED','CANCELLED'].includes(item.status)
  return <div className="im"><PageHeader title="Import & Sync" description="Prepare, validate and review institution data before importing."/><ImportWizardSteps step={step}/>{!canImport && <Notice>You do not have permission to import or apply changes.</Notice>}{importId && !validId ? <Notice>Invalid import ID. <Link to={`${root}/import`}>Start a new import</Link>.</Notice> : <>
    {validId && <QueryState pending={query.isPending} error={query.error} retry={query.refetch}/>}
    {uncertain && <Notice>The last request could not be confirmed. Check the server status before retrying. {validId ? <button disabled={query.isFetching} onClick={async () => {const result = await query.refetch(); if (result.isSuccess) setUncertain(false)}}>Refresh status</button> : <Link to={`${root}/history`}>Check Import History for the new import</Link>}</Notice>}
    <section className="panel im-section">
      {!validId && <ImportSourceStep busy={busy} canImport={canImport && !uncertain} submit={(country, source, version, type) => run('create', async () => {const created = await api.create(country, source, version, type); client.setQueryData(['institution-import', created.id], created); if (window.location.pathname === `${root}/import`) navigate(`${root}/import/${created.id}`, {replace: true})})}/>}
      {terminal && <><h2>Import {item.status.toLowerCase()}</h2><Notice>{item.errorMessage || 'This import cannot be continued. Start a new import with corrected data.'}</Notice><Link to={`${root}/history/${id}`}>View Import Details</Link><p><Link to={`${root}/import`}>Import Another Source</Link></p></>}
      {item && !terminal && <>
        {step === 1 && <ImportUploadStep item={item} busy={busy} allowed={canImport && !uncertain} upload={file => run('upload', async () => {const saved = await api.upload(id, file); client.setQueryData(['institution-import', id], saved); go(2)})} next={() => go(2)}/>}
        {step === 2 && <FieldMappingTable sourceId={item.sourceId} busy={busy} allowed={canImport && !uncertain} back={() => go(1)} validate={() => run('validate', async () => {await api.validate(id); go(3)})}/>}
        {(step === 3 || step === 5) && <QueryState pending={summary.isPending || duplicates.isPending} error={summary.error || duplicates.error} retry={() => {void summary.refetch(); void duplicates.refetch()}}/>}
        {step === 3 && summary.data && <><h2>Validation Results</h2><ValidationSummary summary={summary.data.summary}/><ImportRecordsTable importId={id}/><div className="im-actions"><button onClick={() => go(4)}>Continue to Duplicates →</button></div></>}
        {step === 4 && <><DuplicateReview importId={id}/><div className="im-actions"><button onClick={() => go(3)}>Back</button><button onClick={() => go(5)}>Continue to Review →</button></div></>}
        {step === 5 && summary.data && <><ImportReview item={item} summary={summary.data.summary} unresolved={unresolvedRecords(duplicates.data ?? [])} ready={ready && duplicates.isSuccess && !duplicates.isFetching && summary.isSuccess && !summary.isFetching && !uncertain} busy={busy} allowed={canImport} start={() => setConfirmation('apply')} cancel={() => setConfirmation('cancel')}/><button disabled={busy} onClick={() => go(4)}>Back to Duplicates</button></>}
        {step === 6 && <ImportProgress item={item} validating={operation === 'validate' || item.status === 'PROCESSING'}/>}
        {step === 7 && <ImportComplete item={item}/>}
        {step < 5 && <div className="im-actions"><Link to={`${root}/history/${id}`}>Save & Exit</Link><button disabled={busy || !canImport} onClick={() => setConfirmation('cancel')}>Cancel Import</button></div>}
      </>}
    </section>
    {confirmation && <Confirmation title={confirmation === 'apply' ? 'Start this import?' : 'Cancel this import?'} busy={busy} close={() => setConfirmation(null)} confirm={() => {if (confirmation === 'apply') run('apply', async () => {const latest = await api.import(id); const [preview, matches] = await Promise.all([api.summary(id), api.duplicates(id)]); if (!reviewable(latest.status) || preview.summary.errorCount > 0 || unresolvedRecords(matches) > 0) throw new Error('Import cannot be applied. Refresh and resolve all errors and duplicate records first.'); const saved = await api.apply(id); client.setQueryData(['institution-import', id], saved); go(7)}); else run('cancel', async () => {const saved = await api.cancel(id); client.setQueryData(['institution-import', id], saved)})}}>{confirmation === 'apply' ? 'This will write the reviewed institution records to the master. Do not submit the import again while it is processing.' : 'This ends the current import. Its history will remain available.'}</Confirmation>}
  </>}</div>
}
