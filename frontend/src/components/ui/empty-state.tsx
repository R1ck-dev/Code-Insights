import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface EmptyStateProps {
  icon?: LucideIcon
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}

export function EmptyState({ icon: Icon, title, description, action, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border bg-surface/40 px-6 py-14 text-center',
        className,
      )}
    >
      {Icon && (
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand/10">
          <Icon size={22} className="text-brand-strong" />
        </div>
      )}
      <div className="flex flex-col gap-1.5">
        <span className="text-[15px] font-semibold text-heading">{title}</span>
        {description && (
          <span className="max-w-sm text-[13px] leading-relaxed text-muted">{description}</span>
        )}
      </div>
      {action && <div className="mt-1">{action}</div>}
    </div>
  )
}
