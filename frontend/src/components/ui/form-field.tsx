import { cn } from '@/lib/utils'
import { Label } from './label'
import { Mensagem } from './input'

export interface FormFieldProps {
  label?: string
  htmlFor?: string
  required?: boolean
  error?: string | null
  hint?: React.ReactNode
  /** Ação à direita do rótulo (ex.: "esqueci a senha"). */
  action?: React.ReactNode
  className?: string
  children: React.ReactNode
}

/**
 * Anatomia do campo (§01 §7.2): rótulo mono + controle + hint/erro, gap 6px.
 * Use com controles que não têm rótulo próprio (Select, Toggle, AutonomyInput).
 * `Input`/`Textarea`/`PasswordInput` já montam essa anatomia sozinhos via `label`.
 */
export function FormField({
  label,
  htmlFor,
  required,
  error,
  hint,
  action,
  className,
  children,
}: FormFieldProps) {
  return (
    <div className={cn('flex flex-col gap-[6px]', className)}>
      {(label || action) && (
        <div className="flex items-center justify-between gap-3">
          {label && (
            <Label htmlFor={htmlFor} required={required}>
              {label}
            </Label>
          )}
          {action}
        </div>
      )}
      {children}
      <Mensagem id={htmlFor} error={error} hint={hint} />
    </div>
  )
}
