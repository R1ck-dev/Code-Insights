import { cn } from '@/lib/utils'

/**
 * Painel base do sistema ÓRBITA: fundo `panel` + hairline `line` + raio 3px.
 * Sem sombra por padrão (sombra é só de elemento flutuante: modal, callout).
 */
export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('rounded-ci border border-line bg-panel', className)}
      {...props}
    />
  )
}
