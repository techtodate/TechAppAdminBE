import type {Named, InstitutionMetadata} from '../../../types/institution'
import {Field} from './Shared'
interface Props { countries: Named[]; country: string; setCountry: (value: string) => void; search: string; setSearch: (value: string) => void; metadata?: InstitutionMetadata; type: string; verification: string; active: string; change: (key: string, value: string) => void }
export default function InstitutionFilters({countries, country, setCountry, search, setSearch, metadata, type, verification, active, change}: Props) {
  return <section className="panel im-section"><div className="im-filters"><Field label="Country"><select value={country} onChange={e => setCountry(e.target.value)}><option value="">All Countries</option>{countries.map(row => <option key={row.id} value={row.id}>{row.name}</option>)}</select></Field>
    <Field label="Institution Type"><select value={type} disabled={!metadata} onChange={e => change('type', e.target.value)}><option value="">All Types</option>{metadata?.institutionTypes.map(value => <option key={value}>{value}</option>)}</select></Field>
    <Field label="Verification"><select value={verification} disabled={!metadata} onChange={e => change('verification', e.target.value)}><option value="">All</option>{metadata?.verificationStatuses.map(value => <option key={value}>{value}</option>)}</select></Field>
    <Field label="Status"><select value={active} onChange={e => change('active', e.target.value)}><option value="true">Active</option><option value="false">Inactive</option><option value="">All statuses</option></select></Field>
    <Field label="Search"><input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search institution…"/></Field></div></section>
}