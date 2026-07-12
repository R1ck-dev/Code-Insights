import { useMemo, useState, type FormEvent } from 'react'
import { AlertTriangle } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard, AuthLayout } from '@/layouts/AuthLayout'
import { Button, buttonClasses } from '@/components/ui/button'
import { Input, PasswordInput } from '@/components/ui/input'
import type { NivelSenha } from '@/components/ui/password-strength'
import { ThemeToggle } from '@/components/ThemeToggle'
import { useRegistrar } from '@/features/identity/hooks'
import { apiErrorMessage } from '@/lib/api'

/*
 * E · Registro (spec 03 §E) — cartão 424px, padding 28, gap 16.
 * Nebulosa `auth` + starfield esparso vêm do AuthLayout; a tela é só o miolo.
 */

const USERNAME_RE = /^[A-Za-z0-9._-]+$/

/**
 * Nível de força da senha. O CÁLCULO é da página; o desenho é do `PasswordStrength`
 * (4 segmentos, cores dos tokens semânticos) — nenhum hex mora aqui.
 */
function nivelSenha(pw: string): NivelSenha {
  let score = 0
  if (pw.length >= 8) score++
  if (pw.length >= 12) score++
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++
  if (/\d/.test(pw) || /[^A-Za-z0-9]/.test(pw)) score++

  if (score <= 1) return 'fraca'
  if (score === 2) return 'media'
  if (score === 3) return 'forte'
  return 'muito-forte'
}

export function RegistroPage() {
  const navigate = useNavigate()
  const registrar = useRegistrar()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const usernameValid = username.length >= 3 && username.length <= 50 && USERNAME_RE.test(username)
  const usernameErro =
    username.length > 0 && !USERNAME_RE.test(username)
      ? 'Use apenas letras, números e os símbolos . _ -'
      : null
  const nivel = useMemo(() => nivelSenha(password), [password])

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await registrar.mutateAsync({ username, email, password })
      navigate('/verifique-email', { state: { email } })
    } catch (err) {
      setError(apiErrorMessage(err, 'Não foi possível criar a conta.'))
    }
  }

  return (
    <AuthLayout
      card={false}
      topRight={
        <div className="flex items-center gap-[9px]">
          <ThemeToggle size={36} />
          <Link
            to="/entrar"
            className={buttonClasses({
              variant: 'secondary',
              size: 'sm',
              className: 'h-[34px] text-[12px]',
            })}
          >
            entrar
          </Link>
        </div>
      }
    >
      <AuthCard width={424} className="p-[28px]">
        <form onSubmit={onSubmit} className="flex flex-col gap-[16px]">
          <div className="flex flex-col gap-[6px]">
            <h1 className="text-[21px] leading-tight font-semibold tracking-[-.02em] text-ink">
              Criar sua conta
            </h1>
            <p className="text-[13px] text-mid">Comece a montar seu portfólio de código.</p>
          </div>

          <Input
            id="username"
            label="Nome de usuário"
            mono
            size="lg"
            autoComplete="username"
            autoFocus
            placeholder="ana.dev"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            valid={usernameValid}
            error={usernameErro}
            hint={
              <>
                3–50 · letras, números e <span className="font-mono text-mid">. _ -</span>
              </>
            }
            required
          />

          <Input
            id="email"
            label="E-mail"
            type="email"
            mono
            size="lg"
            autoComplete="email"
            placeholder="ana@exemplo.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <PasswordInput
            id="senha"
            label="Senha"
            mono
            size="lg"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            strength={password ? nivel : null}
            hint="Mínimo de 8 caracteres."
            required
          />

          {error && (
            <div
              role="alert"
              className="flex items-start gap-[9px] rounded-ci border border-erro-line bg-erro-bg px-[13px] py-[10px] text-[12.5px] leading-snug text-erro-texto"
            >
              <AlertTriangle size={14} strokeWidth={2} aria-hidden className="mt-[1px] shrink-0" />
              {error}
            </div>
          )}

          <Button
            type="submit"
            variant="primary"
            size="lg"
            font="sans"
            fullWidth
            loading={registrar.isPending}
          >
            Criar conta
          </Button>

          <p className="text-center text-[12.5px] text-mid">
            Já tem conta?{' '}
            <Link to="/entrar" className="font-medium">
              Entrar
            </Link>
          </p>
        </form>
      </AuthCard>
    </AuthLayout>
  )
}
