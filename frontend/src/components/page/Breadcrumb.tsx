import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Fragment } from 'react'

interface Crumb {
  label: string
  to?: string
}

export function Breadcrumb({ items }: { items: Crumb[] }) {
  return (
    <nav className="flex items-center gap-2 font-mono text-[12.5px] text-subtle" aria-label="Trilha">
      {items.map((item, i) => {
        const last = i === items.length - 1
        return (
          <Fragment key={i}>
            {item.to && !last ? (
              <Link to={item.to} className="text-muted hover:text-fg">
                {item.label}
              </Link>
            ) : (
              <span className={last ? 'text-label' : 'text-muted'}>{item.label}</span>
            )}
            {!last && <ChevronRight size={13} className="text-border-strong" />}
          </Fragment>
        )
      })}
    </nav>
  )
}
