import {Link} from 'react-router-dom'
import type {InstitutionImport} from '../../../../types/institution'
import ValidationSummary from './ValidationSummary'
import {Details, root} from '../Shared'
export default function ImportComplete({item}: {item: InstitutionImport}) { return <><h2>✓ Import Completed</h2><Details values={{Source: item.source?.sourceName || item.sourceId, Country: item.country?.name || item.countryId, Version: item.version}}/><p>{item.totalRecords ?? 0} records processed</p><ValidationSummary summary={{...item, newCount: item.insertedCount ?? item.newCount}} completed/><div className="im-actions"><Link className="im-button" to={`${root}/history/${item.id}`}>View Import Details</Link><Link className="im-button primary" to={root}>View Institution Master</Link><Link className="im-button" to={`${root}/import`}>Import Another Source</Link></div></> }
