import {Dialog, Notice} from './Shared'
export default function InstitutionEditModal({close}: {close: () => void}) { return <Dialog title="Edit Institution" close={close}><Notice>Manual editing is not available yet. Use Import & Sync to update institution data from a trusted source.</Notice><button onClick={close}>Close</button></Dialog> }
