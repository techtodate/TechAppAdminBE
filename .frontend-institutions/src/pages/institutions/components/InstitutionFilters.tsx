import type {Named} from '../../../types/institution'
import {Field, Notice} from './Shared'
export default function InstitutionFilters({countries, country, setCountry, search, setSearch}: {countries: Named[]; country: string; setCountry: (value: string) => void; search: string; setSearch: (value: string) => void}) {
  return <section className="panel im-section"><div className="im-filters"><Field label="Country"><select value={country} onChange={e => setCountry(e.target.value)}><option value="">All Countries</option>{countries.map(row => <option key={row.id} value={row.id}>{row.name}</option>)}</select></Field>
    <Field label="Institution Type"><select disabled aria-describedby="im-filter-note"><option>All Types</option></select></Field><Field label="Verification"><select disabled aria-describedby="im-filter-note"><option>All</option></select></Field><Field label="Status"><select disabled aria-describedby="im-filter-note"><option>All statuses</option></select></Field>
    <Field label="Search"><input type="search" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search institution…"/></Field></div><div id="im-filter-note"><Notice>Country and search filter all institutions. Type, verification and status filters are not yet available.</Notice></div></section>
}
