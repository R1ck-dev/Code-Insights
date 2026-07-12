import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface EmptyStateProps {
  icon?: LucideIcon
  title: string
  description?: string
  action?: React.ReactNode
  /** `sm` = tile 38px (dentro de seções) · `md` = tile 52px (estado de página inteira). */
  size?: 'sm' | 'md'
  className?: string
}

/** Estado vazio (01 §7.11): recuo + hairline **tracejado**, tile elevado, ação opcional. */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  size = 'md',
  className,
}: EmptyStateProps) {
  const grande = size === 'md'

  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-[9px] rounded-ci border border-dashed border-line-strong bg-recess text-center',
        grande ? 'px-5 py-10' : 'p-5',
        className,
      )}
    >
      {Icon && (
        <div
          className="flex items-center justify-center rounded-ci border border-line-strong bg-elevated"
          style={{ width: grande ? 52 : 38, height: grande ? 52 : 38 }}
        >
          <Icon size={grande ? 22 : 18} strokeWidth={2} className="text-soft" />
        </div>
      )}
      <div className="flex flex-col gap-1.5">
        <span className={cn('font-medium text-body', grande ? 'text-[14px]' : 'text-[13px]')}>
          {title}
        </span>
        {description && (
          <span className="mx-auto max-w-[46ch] text-[12px] leading-[1.5] text-soft">
            {description}
          </span>
        )}
      </div>
      {action && <div className="mt-1">{action}</div>}
    </div>
  )
}
