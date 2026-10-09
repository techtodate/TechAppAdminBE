import {useEffect, useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {Link, useSearchParams} from 'react-router-dom'
import {institutionAdminService as api} from '../../services/institutionAdminService'
import {useAdminPermissions} from '../../hooks/useAdminPermissions'
import InstitutionFilters from './components/InstitutionFilters'
import InstitutionTable from './components/InstitutionTable'
import InstitutionDetailsDrawer from './components/InstitutionDetailsDrawer'
import InstitutionSourceRecordModal from './components/InstitutionSourceRecordModal'
import InstitutionEditModal from './components/InstitutionEditModal'
import InstitutionDashboard from './components/InstitutionDashboard'
import {PageHeader, Pager, QueryState, root} from './components/Shared'
import './InstitutionMaster.css'

export default function InstitutionsPage() {
  const [params, setParams] = useSearchParams(), country = params.get('country') || '', q = params.get('q') || ''
  const page = Math.max(0, Math.floor(Number(params.get('page')) || 0)), [debouncedSearch, setDebouncedSearch] = useState(q)
  const [selected, setSelected] = useState<{id: number; mode: 'view' | 'source' | 'edit'} | null>(null)
  const {canImport} = useAdminPermissions()
  useEffect(() => { const timer = setTimeout(() => setDebouncedSearch(q), 350); return () => clearTimeout(timer) }, [q])
  const countries = useQuery({queryKey: ['institution-countries'], queryFn: api.countries})
  const query = useQuery({queryKey: ['institutions', country, debouncedSearch, page], queryFn: () => api.list(country, debouncedSearch, page)})
  const update = (key: string, value: string) => setParams(current => {const next = new URLSearchParams(current); next.set(key, value); if (key !== 'page') next.delete('page'); return next})
  return <div className="im"><PageHeader title="Institution Master" description="Manage and synchronize institution data from trusted sources.">{canImport && <Link className="primary im-button" to={`${root}/import`}>+ Import & Sync</Link>}<Link className="im-button" to={`${root}/sources`}>Sources</Link></PageHeader>
    <InstitutionDashboard/>
    <h2>Institution List</h2>
    <QueryState pending={countries.isPending} error={countries.error} retry={countries.refetch}/>
    <InstitutionFilters countries={countries.data ?? []} country={country} setCountry={value => update('country', value)} search={q} setSearch={value => update('q', value)}/>
    <QueryState pending={query.isPending} error={query.error} retry={query.refetch}/>
    {query.data && !query.isError && <div className="panel"><InstitutionTable rows={query.data.content} select={(id, mode) => setSelected({id, mode})}/><Pager data={query.data} page={page} change={value => update('page', String(value))}/></div>}
    {selected?.mode === 'view' && <InstitutionDetailsDrawer id={selected.id} close={() => setSelected(null)}/>}
    {selected?.mode === 'source' && <InstitutionSourceRecordModal id={selected.id} close={() => setSelected(null)}/>}
    {selected?.mode === 'edit' && <InstitutionEditModal close={() => setSelected(null)}/>}
  </div>
}
