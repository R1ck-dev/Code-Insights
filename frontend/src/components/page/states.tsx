import type { UseQueryResult } from '@tanstack/react-query'
import { AlertTriangle, RefreshCw } from 'lucide-react'
import { Spinner } from '@/components/ui/spinner'
import { apiErrorMessage } from '@/lib/api'
import { cn } from '@/lib/utils'

/** Carregamento de uma seção/página inteira: spinner neutro centralizado. */
export function LoadingSection({ className, label }: { className?: string; label?: string }) {
  return (
    <div
      role="status"
      aria-busy
      aria-label="Carregando"
      className={cn('flex flex-col items-center justify-center gap-3 py-20', className)}
    >
      <Spinner size={22} />
      {label && <span className="font-mono text-[11px] text-soft">{label}</span>}
    </div>
  )
}

interface ErrorStateProps {
  message: string
  /** Cabeçalho do cartão. Padrão: "Não foi possível carregar". */
  title?: string
  onRetry?: () => void
  className?: string
}

/** Cartão de erro (01 §7.11): tonalizado em erro, alinhado à esquerda, retry mono 31px. */
export function ErrorState({
  message,
  title = 'Não foi possível carregar',
  onRetry,
  className,
}: ErrorStateProps) {
  return (
    <div
      role="alert"
      className={cn(
        'flex flex-col gap-[9px] rounded-ci border border-erro-card-line bg-erro-card-bg p-[18px]',
        className,
      )}
    >
      <div className="flex items-center gap-2">
        <AlertTriangle size={15} strokeWidth={2} className="shrink-0 text-erro-texto" />
        <span className="text-[12.5px] font-semibold text-erro-texto">{title}</span>
      </div>

      <p className="text-[12px] leading-[1.5] text-mid">{message}</p>

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="ci-foco-botao mt-0.5 inline-flex h-[31px] cursor-pointer items-center gap-[7px] self-start rounded-ci border border-line-strong px-[11px] font-mono text-[12px] font-medium text-ink transition-colors hover:bg-elevated"
        >
          <RefreshCw size={13} strokeWidth={2} />
          Tentar novamente
        </button>
      )}
    </div>
  )
}

/** Renderiza loading/erro/dado de uma query com um só componente. */
export function QueryBoundary<T>({
  query,
  loading,
  children,
}: {
  query: UseQueryResult<T>
  loading?: React.ReactNode
  children: (data: T) => React.ReactNode
}) {
  if (query.isPending) return <>{loading ?? <LoadingSection />}</>
  if (query.isError) {
    return <ErrorState message={apiErrorMessage(query.error)} onRetry={() => void query.refetch()} />
  }
  return <>{children(query.data)}</>
}
