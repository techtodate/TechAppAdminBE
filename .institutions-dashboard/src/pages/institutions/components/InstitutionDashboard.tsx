import {useQuery} from '@tanstack/react-query'
import {Link} from 'react-router-dom'
import {institutionAdminService as api} from '../../../services/institutionAdminService'
import {Badge, Notice, QueryState, root} from './Shared'
import {date} from './formatting'
import ImportHistoryTable from './ImportHistoryTable'
import ValidationSummary from './wizard/ValidationSummary'

const running = (status?: string) => status === 'IMPORTING' || status === 'PROCESSING'

export default function InstitutionDashboard() {
  const institutions = useQuery({queryKey: ['institution-overview'], queryFn: api.overview, refetchInterval: 30000})
  const sources = useQuery({queryKey: ['institution-sources', ''], queryFn: () => api.sources()})
  const history = useQuery({queryKey: ['institution-history', 0], queryFn: () => api.history(0), refetchInterval: query => query.state.data?.content.some(item => running(item.status)) ? 3000 : 30000})
  const latest = history.data?.content[0]
  const summary = useQuery({queryKey: ['institution-summary', latest?.id], queryFn: () => api.summary(latest!.id), enabled: !!latest, refetchInterval: running(latest?.status) ? 3000 : false})
  const refreshing = institutions.isFetching || sources.isFetching || history.isFetching || summary.isFetching
  const refresh = () => { void institutions.refetch(); void sources.refetch(); void history.refetch(); if (latest) void summary.refetch() }
  const cards = [
    {label: 'Institutions', value: institutions.data?.totalElements, pending: institutions.isPending, error: institutions.isError},
    {label: 'Configured sources', value: sources.data?.length, pending: sources.isPending, error: sources.isError},
    {label: 'Countries with sources', value: sources.data ? new Set(sources.data.map(source => source.countryId)).size : undefined, pending: sources.isPending, error: sources.isError},
    {label: 'Imports', value: history.data?.totalElements, pending: history.isPending, error: history.isError},
  ]
  return <section aria-label="Institutions dashboard">
    <div className="im-actions"><h2>Overview</h2><span>Across all countries</span><button disabled={refreshing} onClick={refresh}>{refreshing ? 'Refreshing…' : 'Refresh overview'}</button></div>
    <div className="im-metrics" aria-live="polite">{cards.map(card => <div className="panel" key={card.label}><span>{card.label}</span><strong>{card.error ? 'Unavailable' : card.pending ? '…' : card.value?.toLocaleString() ?? '—'}</strong></div>)}</div>
    {(institutions.error || sources.error) && <QueryState pending={false} error={institutions.error || sources.error} retry={refresh}/>}
    <section className="panel im-section"><div className="im-actions"><h2>Latest Import</h2><Link to={`${root}/history`}>View Import History</Link></div>
      <QueryState pending={history.isPending} error={history.error} retry={history.refetch}/>
      {!history.isPending && !history.isError && !latest && <Notice>No imports yet. Use Import & Sync to load your first source.</Notice>}
      {latest && <><div className="im-actions"><strong>{latest.source?.sourceName || `Source #${latest.sourceId}`}</strong><span>{latest.country?.name || `Country #${latest.countryId}`} · Version {latest.version}</span><Badge value={latest.status}/></div><p>Created {date(latest.createdAt)}</p>
        <QueryState pending={summary.isPending} error={summary.error} retry={summary.refetch}/>
        {summary.data && !summary.isError && <ValidationSummary summary={summary.data.summary} completed={summary.data.status === 'COMPLETED'}/>}
        <div className="im-actions"><Link className="im-button" to={`${root}/history/${latest.id}`}>View Import Details</Link>{!['COMPLETED','FAILED','CANCELLED'].includes(latest.status) && <Link className="im-button" to={`${root}/import/${latest.id}`}>Resume Import</Link>}</div>
      </>}
    </section>
    {!!history.data?.content.length && !history.isError && <section className="panel im-section"><h2>Recent Imports</h2><ImportHistoryTable rows={history.data.content.slice(0, 5)}/></section>}
  </section>
}
