import {useState} from 'react'
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {institutionAdminService as api} from '../../../../services/institutionAdminService'
import {useAdminPermissions} from '../../../../hooks/useAdminPermissions'
import {useSnackbar} from '../../../../hooks/useSnackbar'
import type {SourceMapping, StandardField} from '../../../../types/institution'
import {Notice, QueryState} from '../Shared'
interface Props { sourceId: number; busy: boolean; allowed: boolean; validate: () => void; back: () => void }
export default function FieldMappingTable(props: Props) {
  const fields = useQuery({queryKey: ['institution-standard-fields'], queryFn: api.fields}), mappings = useQuery({queryKey: ['institution-mappings', props.sourceId], queryFn: () => api.mappings(props.sourceId)})
  return <><QueryState pending={fields.isPending || mappings.isPending} error={fields.error || mappings.error} retry={() => {void fields.refetch(); void mappings.refetch()}}/>{fields.data && mappings.data && <MappingEditor key={props.sourceId + ':' + mappings.dataUpdatedAt} {...props} fields={fields.data} initial={mappings.data.filter(row => row.active)}/>}</>
}
function MappingEditor({sourceId, fields, initial, busy, allowed, validate, back}: Props & {fields: StandardField[]; initial: SourceMapping[]}) {
  const [rows, setRows] = useState(initial), client = useQueryClient(), {show} = useSnackbar(), {canManage} = useAdminPermissions()
  const dirty = JSON.stringify(rows) !== JSON.stringify(initial)
  const destinations = rows.map(row => row.standardFieldCode), sourceNames = rows.map(row => row.sourceFieldName.trim().toLowerCase())
  const missing = [...new Set(['SOURCE_IDENTIFIER', 'NAME', ...fields.filter(field => field.active && field.isRequired).map(field => field.fieldCode)])].filter(code => !destinations.includes(code))
  const invalid = missing.length > 0 || !rows.length || rows.some(row => !row.sourceFieldName.trim() || !row.standardFieldCode) || new Set(destinations).size !== rows.length || new Set(sourceNames).size !== rows.length
  const save = useMutation({mutationFn: () => api.saveMappings(sourceId, rows), onSuccess: saved => {client.setQueryData(['institution-mappings', sourceId], saved); show('Source mappings saved.')}, onError: (error: Error) => show(error.message, 'error')})
  const disabled = busy || save.isPending
  const change = (id: number, key: 'sourceFieldName' | 'standardFieldCode' | 'transformationRule', value: string) => setRows(current => current.map(row => row.id === id ? {...row, [key]: value} : row))
  return <><h2>Map Source Fields</h2><Notice>Use the exact column names from your source file. Save mapping changes before validation.</Notice>{missing.length > 0 && <p role="alert" className="im-error">Required fields are unmapped: {missing.join(', ')}.</p>}{invalid && !missing.length && <p role="alert" className="im-error">Source and destination fields must be non-empty and unique.</p>}
    <div className="tablewrap"><table className="im-table"><thead><tr>{['Source Field','Destination Field','Transformation','Required','Actions'].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{rows.map(row => <tr key={row.id}>
      <td data-label="Source Field"><input aria-label="Source field" maxLength={200} disabled={disabled || !canManage} value={row.sourceFieldName} onChange={e => change(row.id, 'sourceFieldName', e.target.value)}/></td>
      <td data-label="Destination Field"><select aria-label="Destination field" disabled={disabled || !canManage} value={row.standardFieldCode} onChange={e => change(row.id, 'standardFieldCode', e.target.value)}><option value="">Choose field</option>{fields.filter(field => field.active).map(field => <option value={field.fieldCode} key={field.id}>{field.fieldName}</option>)}</select></td>
      <td data-label="Transformation"><select aria-label="Transformation" disabled={disabled || !canManage} value={row.transformationRule || ''} onChange={e => change(row.id, 'transformationRule', e.target.value)}>{['','TRIM','NORMALIZE_NAME','NORMALIZE_TYPE','NORMALIZE_LOCATION','NORMALIZE_URL'].map(value => <option value={value} key={value}>{value || 'None'}</option>)}</select></td>
      <td data-label="Required">{fields.find(field => field.fieldCode === row.standardFieldCode)?.isRequired ? 'Yes' : 'No'}</td>
      <td data-label="Actions"><button disabled={disabled || !canManage} onClick={() => setRows(current => current.filter(item => item.id !== row.id))}>Remove</button></td>
    </tr>)}</tbody></table></div>
    <div className="im-actions"><button disabled={disabled} onClick={back}>Back</button><button disabled={disabled || !canManage} onClick={() => setRows(current => [...current, {id: Math.min(0, ...current.map(row => row.id)) - 1, sourceFieldName: '', standardFieldCode: '', isRequired: false, active: true}])}>Add Field</button><button disabled={disabled || !canManage || invalid || !dirty} onClick={() => save.mutate()}>Save Mapping</button><button className="primary" disabled={disabled || !allowed || invalid || dirty} onClick={validate}>{busy ? 'Validating…' : 'Validate & Continue →'}</button></div>
  </>
}