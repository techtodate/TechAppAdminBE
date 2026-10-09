import {useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {Link, useParams} from 'react-router-dom'
import {institutionAdminService as api} from '../../services/institutionAdminService'
import {Badge, Details, Notice, PageHeader, QueryState, root} from './components/Shared'
import {date} from './components/formatting'
import ValidationSummary from './components/wizard/ValidationSummary'
import ImportRecordsTable from './components/wizard/ImportRecordsTable'
import DuplicateReview from './components/wizard/DuplicateReview'
import './InstitutionMaster.css'
export default function ImportDetailsPage() {
  const {importId} = useParams(), id = Number(importId), valid = Number.isSafeInteger(id) && id > 0, [tab, setTab] = useState('Summary')
  const query = useQuery({queryKey: ['institution-import', id], queryFn: () => api.import(id), enabled: valid, refetchInterval: q => ['PROCESSING','IMPORTING'].includes(q.state.data?.status || '') ? 2000 : false}), item = query.data
  return <div className="im"><PageHeader title="Import Details">{item && !['COMPLETED','CANCELLED','FAILED'].includes(item.status) && <Link className="im-button primary" to={`${root}/import/${id}`}>Resume Import</Link>}</PageHeader>{!valid ? <Notice>Invalid import ID.</Notice> : <QueryState pending={query.isPending} error={query.error} retry={query.refetch}/>} {item && <><section className="panel im-section"><Badge value={item.status}/><Details values={{Source: item.source?.sourceName || item.sourceId, Country: item.country?.name || item.countryId, Version: item.version, File: item.fileName, Started: date(item.startedAt), Completed: date(item.completedAt)}}/></section><ValidationSummary summary={{...item, newCount: item.newCount ?? item.insertedCount}} completed={item.status === 'COMPLETED'}/><div className="im-tabs" role="group" aria-label="Import detail sections">{['Summary','Records','Duplicates','Errors'].map(label => <button key={label} aria-pressed={tab === label} onClick={() => setTab(label)}>{label}</button>)}</div><section className="panel im-section">{tab === 'Summary' && <Details values={{Status: item.status, 'Import Type': item.importType, Created: date(item.createdAt), 'Total Records': item.totalRecords, 'Error Message': item.errorMessage}}/>}{(tab === 'Records' || tab === 'Errors') && <ImportRecordsTable key={tab} importId={id} initial={tab === 'Errors' ? 'ERROR' : ''}/>} {tab === 'Duplicates' && <DuplicateReview importId={id} readOnly/>}</section></>}</div>
}
