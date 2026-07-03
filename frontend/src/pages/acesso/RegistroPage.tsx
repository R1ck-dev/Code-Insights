import { useMemo, useState, type FormEvent } from 'react'
import { Check } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Button, buttonClasses } from '@/components/ui/button'
import { Input, PasswordInput } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'
import { useRegistrar } from '@/features/identity/hooks'
import { apiErrorMessage } from '@/lib/api'

const USERNAME_RE = /^[A-Za-z0-9._-]+$/

function passwordStrength(pw: string): { score: number; label: string; color: string } {
  let score = 0
  if (pw.length >= 8) score++
  if (pw.length >= 12) score++
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++
  if (/\d/.test(pw) || /[^A-Za-z0-9]/.test(pw)) score++
  const meta = [
    { label: 'Fraca', color: '#F0563F' },
    { label: 'Fraca', color: '#F0563F' },
    { label: 'Média', color: '#E0A21E' },
    { label: 'Boa', color: '#9CC15A' },
    { label: 'Forte', color: '#2FB863' },
  ][score]
  return { score, ...meta }
}

export function RegistroPage() {
  const navigate = useNavigate()
  const registrar = useRegistrar()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const usernameValid = username.length >= 3 && username.length <= 50 && USERNAME_RE.test(username)
  const strength = useMemo(() => passwordStrength(password), [password])

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
    <AuthLayout topRight={<Link to="/entrar" className={buttonClasses({ variant: 'secondary', size: 'sm' })}>Entrar</Link>}>
      <form
        onSubmit={onSubmit}
        className="flex w-[424px] max-w-full flex-col gap-[18px] rounded-2xl border border-border bg-surface p-8 shadow-[0_30px_70px_-30px_rgba(0,0,0,.85)]"
      >
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[22px] font-bold tracking-tight text-heading">Criar sua conta</h1>
          <span className="text-[13.5px] text-muted">Comece a montar seu portfólio de código.</span>
        </div>

        <FormField
          label="Nome de usuário"
          htmlFor="username"
          hint={
            <>
              3–50 · letras, números e <span className="font-mono text-muted">. _ -</span>
            </>
          }
        >
          <div className="relative flex items-center">
            <Input
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={usernameValid ? 'border-success pr-10' : undefined}
              autoComplete="username"
              required
            />
            {usernameValid && <Check size={16} className="absolute right-3 text-success" />}
          </div>
        </FormField>

        <FormField label="E-mail" htmlFor="email">
          <Input
            id="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </FormField>

        <div className="flex flex-col gap-2">
          <label htmlFor="senha" className="text-[12.5px] font-semibold text-label">
            Senha
          </label>
          <PasswordInput
            id="senha"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required
          />
          {password && (
            <div className="flex items-center gap-2.5">
              <div className="flex flex-1 gap-1.5">
                {[0, 1, 2, 3].map((i) => (
                  <span
                    key={i}
                    className="h-[5px] flex-1 rounded-full"
                    style={{ background: i < strength.score ? strength.color : 'var(--border-strong)' }}
                  />
                ))}
              </div>
              <span className="text-[11.5px] font-semibold" style={{ color: strength.color }}>
                {strength.label}
              </span>
            </div>
          )}
          <span className="text-[11.5px] text-subtle">Mínimo de 8 caracteres.</span>
        </div>

        {error && (
          <div className="rounded-lg border border-danger/30 bg-danger/10 px-3 py-2.5 text-[12.5px] text-danger">
            {error}
          </div>
        )}

        <Button type="submit" size="lg" loading={registrar.isPending} className="w-full">
          Criar conta
        </Button>

        <div className="text-center text-[13px] text-muted">
          Já tem conta?{' '}
          <Link to="/entrar" className="font-semibold text-brand-strong hover:underline">
            Entrar
          </Link>
        </div>
      </form>
    </AuthLayout>
  )
}
