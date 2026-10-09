import {useQuery} from '@tanstack/react-query'
import {Link} from 'react-router-dom'
import {institutionAdminService as api} from '../../../services/institutionAdminService'
import {Badge, Notice, QueryState, root} from './Shared'
import {date} from './formatting'
import ImportHistoryTable from './ImportHistoryTable'
import ValidationSummary from './wizard/ValidationSummary'
const running = (status?: string) => status === 'IMPORTING' || status === 'PROCESSING'
export default function InstitutionDashboard() {
  const overview = useQuery({queryKey: ['institution-overview'], queryFn: api.overview, refetchInterval: query => query.state.data?.recentImports.some(item => running(item.status)) ? 3000 : 30000})
  const data = overview.data, latest = data?.recentImports[0]
  const summary = useQuery({queryKey: ['institution-summary', latest?.id], queryFn: () => api.summary(latest!.id), enabled: !!latest && !overview.isError, refetchInterval: running(latest?.status) ? 3000 : false})
  const refresh = () => { void overview.refetch(); if (latest) void summary.refetch() }
  const cards = [['Institutions', data?.totalInstitutions], ['Configured sources', data?.configuredSources], ['Countries with sources', data?.countriesWithSources], ['Imports', data?.totalImports]] as const
  return <section aria-label="Institutions dashboard"><div className="im-actions"><h2>Overview</h2><span>Across all countries</span><button disabled={overview.isFetching || summary.isFetching} onClick={refresh}>Refresh overview</button></div>
    <div className="im-metrics" aria-live="polite">{cards.map(([label, value]) => <div className="panel" key={label}><span>{label}</span><strong>{overview.isError ? 'Unavailable' : overview.isPending ? '…' : value?.toLocaleString() ?? '—'}</strong></div>)}</div>
    <QueryState pending={overview.isPending} error={overview.error} retry={overview.refetch}/>
    {!overview.isError && !overview.isPending && <section className="panel im-section"><div className="im-actions"><h2>Latest Import</h2><Link to={root + '/history'}>View Import History</Link></div>
      {!latest ? <Notice>No imports yet. Use Import & Sync to load your first source.</Notice> : <><div className="im-actions"><strong>{latest.source?.sourceName || 'Source #' + latest.sourceId}</strong><span>{latest.country?.name || 'Country #' + latest.countryId} · Version {latest.version}</span><Badge value={latest.status}/></div><p>Created {date(latest.createdAt)}</p>
        <QueryState pending={summary.isPending} error={summary.error} retry={summary.refetch}/>
        {summary.data && !summary.isError && <ValidationSummary summary={summary.data.summary} completed={summary.data.status === 'COMPLETED'}/>}
        <div className="im-actions"><Link className="im-button" to={root + '/history/' + latest.id}>View Import Details</Link>{!['COMPLETED','FAILED','CANCELLED'].includes(latest.status) && <Link className="im-button" to={root + '/import/' + latest.id}>Resume Import</Link>}</div></>}
    </section>}
    {!!data?.recentImports.length && !overview.isError && <section className="panel im-section"><h2>Recent Imports</h2><ImportHistoryTable rows={data.recentImports}/></section>}
  </section>
}