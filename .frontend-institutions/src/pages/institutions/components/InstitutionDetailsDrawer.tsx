import {useQuery} from '@tanstack/react-query'
import {institutionAdminService as api} from '../../../services/institutionAdminService'
import {Details, Dialog, Notice, QueryState, SafeLink} from './Shared'
import {date} from './formatting'
import {SourceRecords} from './InstitutionSourceRecordModal'
export default function InstitutionDetailsDrawer({id, close}: {id: number; close: () => void}) {
  const query = useQuery({queryKey: ['institution', id], queryFn: () => api.institution(id)}), row = query.data
  return <Dialog title={row?.name || 'Institution Details'} close={close}><QueryState pending={query.isPending} error={query.error} retry={query.refetch}/>{row && <><Details values={{'Institution Name': row.name, 'Short Name': row.shortName, 'Institution Type': row.institutionType, Country: row.country?.name, 'Administrative Area': row.administrativeArea?.name, City: row.city?.name || row.cityName, Address: row.address, Verification: row.verificationStatus, Status: row.active ? 'Active' : 'Inactive', Source: row.source?.sourceName, 'Source Identifier': row.sourceIdentifier, 'Last Seen': date(row.lastSeenAt), 'Created At': date(row.createdAt), 'Updated At': date(row.updatedAt)}}/><p>Website: <SafeLink url={row.website}/></p><h3>Aliases</h3>{row.aliases ? row.aliases.length ? <ul>{row.aliases.map((alias, i) => <li key={i}>{alias.aliasName}</li>)}</ul> : <p>No aliases.</p> : <Notice>Aliases are not available in the institution details response.</Notice>}<SourceRecords institution={row}/></>}</Dialog>
}
