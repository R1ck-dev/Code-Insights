import { forwardRef, useId, useState } from 'react'
import { AlertTriangle, Check, Eye, EyeOff, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Label } from './label'
import { PasswordStrength, type NivelSenha } from './password-strength'

/**
 * Superfície do controle: recuo + hairline forte + raio 3px + anel de foco `ink`.
 * Exportado porque o `Combobox` (que não é um `input` nem um `Select`) tem de vestir EXATAMENTE
 * a mesma pele — dois campos lado a lado com bordas diferentes seriam dois sistemas.
 */
export const controle =
  'ci-foco-input w-full rounded-ci border border-line-strong bg-recess text-ink placeholder:text-soft ' +
  'outline-none transition-[border-color,box-shadow] duration-100 disabled:opacity-60 disabled:cursor-not-allowed'

export const estados =
  'aria-[invalid=true]:border-erro-estrutura data-[valido=true]:border-sucesso'

/** Campos de DADO medem em mono; texto livre lê em Space Grotesk. */
function fonte(mono?: boolean) {
  return mono ? 'font-mono text-[13.5px]' : 'font-sans text-[14px]'
}

interface CampoProps {
  label?: string
  hint?: React.ReactNode
  error?: string | null
  /** Estado válido: borda `#4FB477` + `check`. */
  valid?: boolean
  /** Fonte mono — para dados (e-mail, username, ID). Padrão: Space Grotesk. */
  mono?: boolean
  /** Marca o `*` de obrigatório no rótulo. */
  required?: boolean
}

/** Rótulo + controle + hint/erro. Sem `label`/`hint`/`error`, devolve só o controle. */
export function Campo({
  id,
  label,
  hint,
  error,
  required,
  children,
}: CampoProps & { id: string; children: React.ReactNode }) {
  if (!label && !hint && !error) return <>{children}</>

  return (
    <div className="flex flex-col gap-[6px]">
      {label && (
        <Label htmlFor={id} required={required}>
          {label}
        </Label>
      )}
      {children}
      <Mensagem id={id} error={error} hint={hint} />
    </div>
  )
}

export function Mensagem({
  id,
  error,
  hint,
}: {
  id?: string
  error?: string | null
  hint?: React.ReactNode
}) {
  if (error) {
    return (
      <span
        id={id ? `${id}-erro` : undefined}
        className="flex items-center gap-[6px] text-[11.5px] leading-snug text-erro-texto"
      >
        <AlertTriangle size={12} strokeWidth={2} aria-hidden className="shrink-0" />
        {error}
      </span>
    )
  }
  if (!hint) return null
  return (
    <span
      id={id ? `${id}-hint` : undefined}
      className="text-[11.5px] leading-snug text-soft"
    >
      {hint}
    </span>
  )
}

export interface InputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'>,
    CampoProps {
  /** `md` = 40px (padrão) · `lg` = 42px (cartões de auth). */
  size?: 'md' | 'lg'
  /** Ícone à esquerda (15px, cor `soft`). */
  icon?: LucideIcon
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  {
    id,
    label,
    hint,
    error,
    valid,
    mono,
    required,
    size = 'md',
    icon: Icon,
    className,
    ...props
  },
  ref,
) {
  const auto = useId()
  const inputId = id ?? auto

  return (
    <Campo id={inputId} label={label} hint={hint} error={error} required={required}>
      <div className="relative flex items-center">
        {Icon && (
          <Icon
            size={15}
            strokeWidth={2}
            aria-hidden
            className="pointer-events-none absolute left-3 text-soft"
          />
        )}
        <input
          ref={ref}
          id={inputId}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${inputId}-erro` : hint ? `${inputId}-hint` : undefined}
          data-valido={valid ? true : undefined}
          className={cn(
            controle,
            estados,
            fonte(mono),
            size === 'lg' ? 'h-[42px]' : 'h-[40px]',
            'px-[12px]',
            Icon && 'pl-[36px]',
            valid && 'pr-[40px]',
            className,
          )}
          {...props}
        />
        {valid && (
          <Check
            size={16}
            strokeWidth={2}
            aria-hidden
            className="pointer-events-none absolute right-3 text-sucesso"
          />
        )}
      </div>
    </Campo>
  )
})

export interface TextareaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement>,
    CampoProps {
  /** Altura mínima em px. Padrão: 96. */
  minHeight?: number
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { id, label, hint, error, valid, mono, required, minHeight = 96, className, style, ...props },
  ref,
) {
  const auto = useId()
  const areaId = id ?? auto

  return (
    <Campo id={areaId} label={label} hint={hint} error={error} required={required}>
      <textarea
        ref={ref}
        id={areaId}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${areaId}-erro` : hint ? `${areaId}-hint` : undefined}
        data-valido={valid ? true : undefined}
        style={{ minHeight, ...style }}
        className={cn(
          controle,
          estados,
          mono ? 'font-mono text-[13px]' : 'font-sans text-[13px]',
          'resize-y px-[12px] py-[10px] leading-[1.5]',
          className,
        )}
        {...props}
      />
    </Campo>
  )
})

export interface PasswordInputProps extends Omit<InputProps, 'icon' | 'type'> {
  /** Renderiza o `PasswordStrength` sob o campo. O cálculo do nível é da página. */
  strength?: NivelSenha | null
}

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  function PasswordInput(
    { id, label, hint, error, valid, mono, required, size = 'md', strength, className, ...props },
    ref,
  ) {
    const auto = useId()
    const inputId = id ?? auto
    const [visivel, setVisivel] = useState(false)

    return (
      <Campo id={inputId} label={label} hint={hint} error={error} required={required}>
        <div className="flex flex-col gap-[8px]">
          <div className="relative flex items-center">
            <input
              ref={ref}
              id={inputId}
              type={visivel ? 'text' : 'password'}
              required={required}
              aria-invalid={error ? true : undefined}
              aria-describedby={
                error ? `${inputId}-erro` : hint ? `${inputId}-hint` : undefined
              }
              data-valido={valid ? true : undefined}
              className={cn(
                controle,
                estados,
                fonte(mono),
                size === 'lg' ? 'h-[42px]' : 'h-[40px]',
                'pl-[12px] pr-[42px]',
                className,
              )}
              {...props}
            />
            {/*
             * SEM `tabIndex={-1}`: com ele, quem não usa mouse não conseguia revelar a senha
             * em NENHUMA das 4 telas de senha — e não havia mecanismo alternativo (WCAG 2.1.1).
             * Alvo de 24×24 (WCAG 2.5.8) com o ícone em 16px.
             */}
            <button
              type="button"
              onClick={() => setVisivel((v) => !v)}
              aria-pressed={visivel}
              aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
              className="ci-foco-botao absolute right-2 flex h-6 w-6 cursor-pointer items-center justify-center rounded-ci text-soft transition-colors hover:text-mid"
            >
              {visivel ? (
                <EyeOff size={16} strokeWidth={2} aria-hidden />
              ) : (
                <Eye size={16} strokeWidth={2} aria-hidden />
              )}
            </button>
          </div>
          {strength && <PasswordStrength level={strength} />}
        </div>
      </Campo>
    )
  },
)
