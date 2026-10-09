import {useState} from 'react'
import type {InstitutionImport} from '../../../../types/institution'
import {Details, Notice} from '../Shared'
export default function ImportUploadStep({item, busy, allowed, upload, next}: {item: InstitutionImport; busy: boolean; allowed: boolean; upload: (file: File) => void; next: () => void}) {
  const [file, setFile] = useState<File | null>(null), [error, setError] = useState('')
  const uploaded = item.status !== 'CREATED' && !!item.fileName && item.fileName !== 'pending_upload'
  function choose(candidate?: File) {
    if (!candidate) return
    if (!/\.(csv|txt)$/i.test(candidate.name) || candidate.size === 0) {
      setFile(null); setError('Choose a non-empty CSV or TXT file. Export Excel workbooks as CSV first.'); return
    }
    if (candidate.size > 12 * 1024 * 1024) {
      setFile(null); setError('The file exceeds the 12 MB upload limit.'); return
    }
    setFile(candidate); setError('')
  }
  return <><h2>Upload Source File</h2><p>Selected Source: <strong>{item.source?.sourceName || `Source #${item.sourceId}`}</strong></p>
    <div className="im-drop" onDragOver={e => e.preventDefault()} onDrop={e => {e.preventDefault(); if (!busy && allowed) choose(e.dataTransfer.files[0])}}>
      <p>Drag &amp; drop CSV here</p><label className="im-field">Choose File<input type="file" accept=".csv,.txt,text/csv,text/plain" disabled={busy || !allowed} onChange={e => choose(e.target.files?.[0])}/></label>
    </div>
    <Notice>CSV and TXT are supported, up to 12 MB. Export Excel workbooks as CSV before uploading.</Notice>
    {error && <p role="alert" className="im-error">{error}</p>}
    {file && <Details values={{'File name': file.name, 'File size': `${(file.size / 1024).toFixed(1)} KB`, 'File type': file.type || file.name.split('.').pop()}}/>}
    {uploaded && <Notice>File uploaded: {item.fileName}. {item.totalRecords ? `${item.totalRecords} records detected.` : 'Record count will be available after validation.'}</Notice>}
    <div className="im-actions"><button className="primary" disabled={!file || busy || !allowed} onClick={() => file && upload(file)}>{busy ? 'Uploading…' : 'Upload & Continue'}</button>
      {uploaded && <button disabled={busy || !allowed} onClick={next}>Continue with uploaded file →</button>}
    </div></>
}
