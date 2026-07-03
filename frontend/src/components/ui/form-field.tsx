import { Label } from './label'
import { cn } from '@/lib/utils'

interface FormFieldProps {
  label?: string
  htmlFor?: string
  required?: boolean
  error?: string | null
  hint?: React.ReactNode
  className?: string
  children: React.ReactNode
}

/** Campo de formulário: rótulo + controle + ajuda/erro inline (padrão do design). */
export function FormField({
  label,
  htmlFor,
  required,
  error,
  hint,
  className,
  children,
}: FormFieldProps) {
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      {label && (
        <Label htmlFor={htmlFor}>
          {label}
          {required && <span className="ml-0.5 text-danger">*</span>}
        </Label>
      )}
      {children}
      {error ? (
        <span className="text-[11.5px] text-danger">{error}</span>
      ) : hint ? (
        <span className="text-[11.5px] leading-snug text-subtle">{hint}</span>
      ) : null}
    </div>
  )
}
