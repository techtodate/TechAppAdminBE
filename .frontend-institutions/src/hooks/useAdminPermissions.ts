import {createContext, useContext, useSyncExternalStore} from 'react'

export type InstitutionPermission = 'INSTITUTION_VIEW' | 'INSTITUTION_IMPORT' | 'INSTITUTION_MANAGE'
// The current AdminSecurityContext grants these permissions to all admins.
// An authenticated shell can supply its authoritative grants through this context.
export const AdminPermissionsContext = createContext<readonly InstitutionPermission[]>(['INSTITUTION_VIEW', 'INSTITUTION_IMPORT', 'INSTITUTION_MANAGE'])
let denied = false
const listeners = new Set<() => void>()
if (typeof window !== 'undefined') window.addEventListener('institution-permission-denied', () => { denied = true; listeners.forEach(listener => listener()) })
function subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener) } }
export function useAdminPermissions() {
  const grants = useContext(AdminPermissionsContext)
  const forbidden = useSyncExternalStore(subscribe, () => denied, () => false)
  return {canImport: !forbidden && grants.includes('INSTITUTION_IMPORT'), canManage: !forbidden && grants.includes('INSTITUTION_MANAGE'), canView: grants.includes('INSTITUTION_VIEW')}
}
