import { cn } from '@/lib/utils'

/** Área de conteúdo padrão das páginas do app (padding + largura confortável). */
export function PageContainer({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('mx-auto flex w-full max-w-6xl flex-col gap-5 p-6 md:px-7 md:py-6', className)}
      {...props}
    />
  )
}
