import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Button } from '@/components/ui/button'
import { Input, PasswordInput } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'
import { useAuth } from '@/auth/useAuth'
import { apiErrorMessage } from '@/lib/api'
import { Avatar } from '@/components/Avatar'

/**
 * Contas semeadas no banco para a fase de desenvolvimento. O bloco de "acesso rápido"
 * que as usa só é renderizado em dev (`import.meta.env.DEV`) — não vai para o build de produção.
 */
const SENHA_TESTE = 'senha12345'
const CONTAS_TESTE = [
  { nome: 'Ana Souza', email: 'ana@codeinsights.dev', descricao: 'Aluna · portfólio público completo' },
  { nome: 'Bruno Lima', email: 'bruno@codeinsights.dev', descricao: 'Aluno · portfólio público enxuto' },
  { nome: 'Carla Mendes', email: 'carla@codeinsights.dev', descricao: 'Aluna · perfil privado' },
]

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/app'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err, 'E-mail ou senha inválidos.'))
    } finally {
      setLoading(false)
    }
  }

  // Atalho da fase de teste: entra direto com uma conta semeada (renderizado só em dev).
  async function entrarComo(emailConta: string) {
    setError(null)
    setLoading(true)
    try {
      await login(emailConta, SENHA_TESTE)
      navigate(from, { replace: true })
    } catch (err) {
      setError(apiErrorMessage(err, 'Não foi possível entrar com a conta de teste.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
      <form
        onSubmit={onSubmit}
        className="flex w-[424px] max-w-full flex-col gap-5 rounded-2xl border border-border bg-surface p-8 shadow-[0_30px_70px_-30px_rgba(0,0,0,.85)]"
      >
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[22px] font-bold tracking-tight text-heading">Bem-vindo de volta</h1>
          <span className="text-[13.5px] text-muted">Entre para acessar seu portfólio.</span>
        </div>

        <FormField label="E-mail" htmlFor="email">
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="voce@exemplo.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </FormField>

        <div className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between">
            <label htmlFor="senha" className="text-[12.5px] font-semibold text-label">
              Senha
            </label>
            <Link to="/recuperar-senha" className="text-[12px] font-medium text-brand-strong hover:underline">
              Esqueci minha senha
            </Link>
          </div>
          <PasswordInput
            id="senha"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        {error && (
          <div className="rounded-lg border border-danger/30 bg-danger/10 px-3 py-2.5 text-[12.5px] text-danger">
            {error}
          </div>
        )}

        <Button type="submit" size="lg" loading={loading} className="w-full">
          Entrar
        </Button>

        {import.meta.env.DEV && (
          <div className="flex flex-col gap-2 rounded-xl border border-dashed border-border-strong bg-bg-deep/60 p-3">
            <span className="text-[10.5px] font-semibold uppercase tracking-wider text-subtle">
              Acesso rápido · ambiente de teste
            </span>
            <div className="flex flex-col gap-2">
              {CONTAS_TESTE.map((conta) => (
                <button
                  key={conta.email}
                  type="button"
                  disabled={loading}
                  onClick={() => entrarComo(conta.email)}
                  className="flex items-center justify-between gap-3 rounded-lg border border-border bg-surface-2 px-3 py-2 text-left transition-colors hover:border-brand-strong/50 hover:bg-surface disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <span className="flex items-center gap-2.5">
                    <Avatar name={conta.nome} size={28} />
                    <span className="flex flex-col leading-tight">
                      <span className="text-[13px] font-semibold text-heading">{conta.nome}</span>
                      <span className="text-[11px] text-muted">{conta.descricao}</span>
                    </span>
                  </span>
                  <span className="font-mono text-[11px] text-subtle">entrar →</span>
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="border-t border-border pt-4 text-center text-[13px] text-muted">
          Não tem conta?{' '}
          <Link to="/criar-conta" className="font-semibold text-brand-strong hover:underline">
            Criar conta
          </Link>
        </div>
      </form>
    </AuthLayout>
  )
}
