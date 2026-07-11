import { cn } from '@/lib/utils'

/**
 * Área de conteúdo das páginas do app (04 §1.3): `padding 24px 28px`, `gap 20px`.
 * A largura é do shell — este container não centraliza nem limita.
 */
export function PageContainer({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('flex w-full min-w-0 flex-col gap-5 px-6 py-5 md:px-7 md:py-6', className)}
      {...props}
    />
  )
}
