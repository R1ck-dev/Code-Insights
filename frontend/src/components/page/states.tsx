import type { UseQueryResult } from '@tanstack/react-query'
import { AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { apiErrorMessage } from '@/lib/api'
import { cn } from '@/lib/utils'

export function LoadingSection({ className }: { className?: string }) {
  return (
    <div className={cn('flex items-center justify-center py-20', className)}>
      <Spinner size={26} color="var(--brand)" />
    </div>
  )
}

export function ErrorState({
  message,
  onRetry,
  className,
}: {
  message: string
  onRetry?: () => void
  className?: string
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 rounded-xl border border-border bg-surface px-6 py-14 text-center',
        className,
      )}
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-danger/10">
        <AlertTriangle size={22} className="text-danger" />
      </div>
      <span className="max-w-sm text-[13.5px] text-muted">{message}</span>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Tentar novamente
        </Button>
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
