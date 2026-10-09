import {display} from './formatting'
import {useEffect, useId, useRef, type ReactNode} from 'react'
import {Link} from 'react-router-dom'
import {ErrorState, LoadingState} from '../../../components/common/LoadingState'
import type {Page} from '../../../types/institution'
import {institutionError} from '../../../services/institutionAdminService'
export const root = '/master-data/institutions'
export function Badge({value}: {value?: string}) { return <span className={`im-badge im-${value?.toLowerCase()}`}>{value?.replaceAll('_', ' ') || '—'}</span> }
export function PageHeader({title, description, children}: {title: string; description?: string; children?: ReactNode}) { return <><div className="pagehead"><div><p>MASTER DATA / INSTITUTIONS</p><h1>{title}</h1><span>{description}</span></div><div className="im-actions">{children}</div></div><nav className="im-nav" aria-label="Institutions"><Link to={root}>Institution List</Link><Link to={`${root}/sources`}>Sources</Link><Link to={`${root}/history`}>Import History</Link></nav></> }
export function QueryState({pending, error, retry}: {pending: boolean; error: Error | null; retry: () => unknown}) { return pending ? <LoadingState/> : error ? <ErrorState message={institutionError(error)} retry={retry}/> : null }
export function Notice({children}: {children: ReactNode}) { return <p className="im-notice" role="status">{children}</p> }
export function Field({label, children}: {label: string; children: ReactNode}) { return <label className="im-field"><span>{label}</span>{children}</label> }
export function Details({values}: {values: Record<string, unknown>}) { return <dl className="im-details">{Object.entries(values).map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{display(value)}</dd></div>)}</dl> }
export function SafeLink({url}: {url?: string}) { return url && /^https?:\/\//i.test(url) ? <a href={url} target="_blank" rel="noopener noreferrer">{url}</a> : <span>{url || '—'}</span> }
export function Pager({data, page, change}: {data?: Page<unknown>; page: number; change: (page: number) => void}) { return <div className="bottom-pagination"><span>{data?.totalElements ?? 0} records</span><div><button disabled={page === 0} onClick={() => change(page - 1)}>Previous</button><strong>Page {page + 1} of {Math.max(1, data?.totalPages ?? 1)}</strong><button disabled={!data || page + 1 >= data.totalPages} onClick={() => change(page + 1)}>Next</button></div></div> }
export function Dialog({title, children, close, busy = false}: {title: string; children: ReactNode; close: () => void; busy?: boolean}) {
  const id = useId(), ref = useRef<HTMLDialogElement>(null)
  useEffect(() => { const dialog = ref.current, previous = document.activeElement as HTMLElement | null; dialog?.showModal(); return () => { dialog?.close(); previous?.focus() } }, [])
  return <dialog className="im-dialog" ref={ref} aria-labelledby={id} onCancel={event => {event.preventDefault(); if (!busy) close()}}><div className="im-dialog-head"><h2 id={id}>{title}</h2><button aria-label="Close dialog" disabled={busy} onClick={close}>×</button></div>{children}</dialog>
}
export function Confirmation({title, children, busy, close, confirm}: {title: string; children: ReactNode; busy: boolean; close: () => void; confirm: () => void}) { return <Dialog title={title} close={close} busy={busy}><p>{children}</p><div className="im-actions"><button disabled={busy} onClick={close}>Back</button><button className="primary" disabled={busy} onClick={confirm}>{busy ? 'Saving…' : 'Confirm'}</button></div></Dialog> }
