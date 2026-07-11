import * as LabelPrimitive from '@radix-ui/react-label'
import { cn } from '@/lib/utils'

export interface LabelProps
  extends React.ComponentProps<typeof LabelPrimitive.Root> {
  /** Marca o campo como obrigatório: asterisco em `erro-texto`. */
  required?: boolean
}

/**
 * Rótulo de campo (§01 §7.2): IBM Plex Mono 11px, `.08em`, MAIÚSCULO, cor `mid`.
 * O texto é passado em caixa normal — o CSS faz o `uppercase`.
 */
export function Label({ className, required, children, ...props }: LabelProps) {
  return (
    <LabelPrimitive.Root
      className={cn(
        'font-mono text-[11px] leading-none font-normal tracking-[.08em] text-mid uppercase',
        className,
      )}
      {...props}
    >
      {children}
      {required && (
        <span aria-hidden className="ml-[3px] text-erro-texto">
          *
        </span>
      )}
    </LabelPrimitive.Root>
  )
}

/** Nome canônico do contrato (00-INDICE §3.1). Mesmo componente que `Label`. */
export const FieldLabel = Label
