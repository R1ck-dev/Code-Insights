import { Fragment } from 'react'
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { cn } from '@/lib/utils'

export interface Crumb {
  label: string
  to?: string
}

/** Trilha da topbar variante (c) (04 §1.2): mono 12px, elo corrente em `ink`. */
export function Breadcrumb({ items, className }: { items: Crumb[]; className?: string }) {
  return (
    <nav
      aria-label="Trilha"
      className={cn('flex min-w-0 items-center gap-2 font-mono text-[12px] text-soft', className)}
    >
      {items.map((item, i) => {
        const last = i === items.length - 1

        return (
          <Fragment key={i}>
            {item.to && !last ? (
              <Link to={item.to} className="truncate text-mid transition-colors hover:text-ink">
                {item.label}
              </Link>
            ) : (
              <span
                aria-current={last ? 'page' : undefined}
                className={cn('truncate', last ? 'text-ink' : 'text-mid')}
              >
                {item.label}
              </span>
            )}
            {!last && <ChevronRight size={13} strokeWidth={2} className="shrink-0 text-soft/70" />}
          </Fragment>
        )
      })}
    </nav>
  )
}
