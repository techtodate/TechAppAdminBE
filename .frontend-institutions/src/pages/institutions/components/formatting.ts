export const display = (value: unknown): string => value == null || value === '' ? '—' : typeof value === 'object' ? ('name' in value ? String(value.name) : 'See details') : String(value)
export const date = (value?: string) => value && !Number.isNaN(Date.parse(value)) ? new Date(value).toLocaleString() : '—'
