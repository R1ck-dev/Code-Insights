import { ArrowLeft, ArrowRight } from 'lucide-react'
import { cn } from '@/lib/utils'

interface PaginationProps {
  /** página atual (0-based, como o backend). */
  page: number
  totalPages: number
  onChange: (page: number) => void
  className?: string
}

export function Pagination({ page, totalPages, onChange, className }: PaginationProps) {
  if (totalPages <= 1) return null
  const prevDisabled = page <= 0
  const nextDisabled = page >= totalPages - 1

  return (
    <div className={cn('flex items-center justify-center gap-2.5', className)}>
      <PageButton
        disabled={prevDisabled}
        onClick={() => onChange(page - 1)}
        label="Página anterior"
      >
        <ArrowLeft size={16} />
      </PageButton>
      <span className="font-mono text-[12.5px] text-muted">
        Página <span className="font-semibold text-fg">{page + 1}</span> de {totalPages}
      </span>
      <PageButton disabled={nextDisabled} onClick={() => onChange(page + 1)} label="Próxima página">
        <ArrowRight size={16} />
      </PageButton>
    </div>
  )
}

function PageButton({
  disabled,
  onClick,
  label,
  children,
}: {
  disabled?: boolean
  onClick: () => void
  label: string
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className={cn(
        'flex h-9 w-9 items-center justify-center rounded-lg border text-fg transition-colors',
        disabled
          ? 'cursor-not-allowed border-border opacity-40'
          : 'cursor-pointer border-border-strong hover:bg-surface-2',
      )}
    >
      {children}
    </button>
  )
}
