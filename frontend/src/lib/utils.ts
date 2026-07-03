import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Combina classes Tailwind resolvendo conflitos (padrão shadcn). */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}

const dateFmt = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})
const dateTimeFmt = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})
const dayMonthFmt = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' })

function parse(iso: string | null | undefined): Date | null {
  if (!iso) return null
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? null : d
}

/** ISO → "dd/mm/aaaa". */
export function formatDate(iso: string | null | undefined): string {
  const d = parse(iso)
  return d ? dateFmt.format(d) : '—'
}

/** ISO → "dd/mm/aaaa · HH:MM". */
export function formatDateTime(iso: string | null | undefined): string {
  const d = parse(iso)
  return d ? dateTimeFmt.format(d).replace(', ', ' · ') : '—'
}

/** ISO → "dd/mm". */
export function formatDayMonth(iso: string | null | undefined): string {
  const d = parse(iso)
  return d ? dayMonthFmt.format(d) : '—'
}

/** "ana.dev" → "AD"; "maria" → "MA". Até 2 caracteres. */
export function initials(name: string | null | undefined): string {
  if (!name) return '?'
  const parts = name.split(/[^A-Za-z0-9]+/).filter(Boolean)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  return name.slice(0, 2).toUpperCase()
}

/** Pluralização simples pt-BR: pluralPt(3, "resolução", "resoluções"). */
export function pluralPt(n: number, singular: string, plural: string): string {
  return `${n} ${n === 1 ? singular : plural}`
}
