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

  return (
    <nav
      aria-label="Paginação"
      className={cn('mt-1.5 flex items-center justify-center gap-2.5', className)}
    >
      <PageButton disabled={page <= 0} onClick={() => onChange(page - 1)} label="Página anterior">
        <ArrowLeft size={16} strokeWidth={2} />
      </PageButton>

      <span className="tabular font-mono text-[12.5px] text-mid">
        Página <span className="font-semibold text-ink">{page + 1}</span> de {totalPages}
      </span>

      <PageButton
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
        label="Próxima página"
      >
        <ArrowRight size={16} strokeWidth={2} />
      </PageButton>
    </nav>
  )
}

function PageButton({
  disabled,
  onClick,
  label,
  children,
}: {
  disabled: boolean
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
        'ci-foco-botao flex h-9 w-9 items-center justify-center rounded-ci border transition-colors',
        disabled
          ? 'cursor-not-allowed border-line text-mid opacity-40'
          : 'cursor-pointer border-line-strong text-ink hover:bg-elevated',
      )}
    >
      {children}
    </button>
  )
}
