import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AlertTriangle, ArrowRight } from 'lucide-react'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Button } from '@/components/ui/button'
import { Input, PasswordInput } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'
import { Avatar } from '@/components/Avatar'
import { useAuth } from '@/auth/useAuth'
import { apiErrorMessage } from '@/lib/api'

/*
 * A · Login (spec 03 §A) — cartão 424px sobre o céu.
 * O `AuthLayout` traz a nebulosa dupla (`login`), o starfield (10 estrelas), o header
 * com a marca e o alternador de tema, e o próprio cartão. Aqui é só o miolo.
 */

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
    <AuthLayout width={424} nebula="login" estrelas="media">
      <BarraDeInstrumento />

      <div className="flex flex-col gap-[6px]">
        <h1 className="text-[22px] leading-tight font-semibold tracking-[-.02em] text-ink">
          Bem-vindo de volta
        </h1>
        <p className="text-[13px] text-mid">Entre para acessar seu portfólio.</p>
      </div>

      <form onSubmit={onSubmit} className="flex flex-col gap-[18px]">
        <Input
          id="email"
          type="email"
          label="E-mail"
          mono
          size="lg"
          autoComplete="email"
          placeholder="ana@exemplo.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <FormField
          label="Senha"
          htmlFor="senha"
          action={
            <Link
              to="/recuperar-senha"
              className="font-mono text-[11px] text-steel hover:text-steel-hover"
            >
              esqueci a senha
            </Link>
          }
        >
          <PasswordInput
            id="senha"
            mono
            size="lg"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </FormField>

        {error && (
          <p
            role="alert"
            className="flex items-start gap-[7px] rounded-ci border border-erro-line bg-erro-bg px-[11px] py-[9px] text-[12px] leading-snug text-erro-texto"
          >
            <AlertTriangle size={13} strokeWidth={2} aria-hidden className="mt-[1px] shrink-0" />
            {error}
          </p>
        )}

        <Button type="submit" size="lg" font="sans" fullWidth loading={loading}>
          Entrar
        </Button>
      </form>

      {import.meta.env.DEV && <AcessoRapido loading={loading} onEntrar={entrarComo} />}

      <p className="border-t border-line pt-[15px] text-center text-[12.5px] text-mid">
        Não tem conta?{' '}
        <Link to="/criar-conta" className="font-medium text-steel hover:text-steel-hover">
          Criar conta
        </Link>
      </p>
    </AuthLayout>
  )
}

/** Barra de instrumento do cartão: rótulo `ACESSO` + LED vivo (spec 03 §A, item 1). */
function BarraDeInstrumento() {
  return (
    <div className="flex items-center justify-between">
      <span className="font-mono text-[10px] tracking-[.14em] text-soft uppercase">Acesso</span>
      <span aria-hidden className="ci-blink size-[6px] shrink-0 rounded-full bg-ink" />
    </div>
  )
}

/**
 * Ferramenta do pesquisador (só em `import.meta.env.DEV`): entra com uma das contas semeadas.
 * Bloco de recuo (`recess`) com hairline tracejada — lê como instrumento, não como parte do produto.
 */
function AcessoRapido({
  loading,
  onEntrar,
}: {
  loading: boolean
  onEntrar: (email: string) => void
}) {
  return (
    <section
      aria-label="Acesso rápido do ambiente de teste"
      className="flex flex-col gap-[10px] rounded-ci border border-dashed border-line bg-recess p-[12px]"
    >
      <span className="font-mono text-[10px] tracking-[.14em] text-soft uppercase">
        Acesso rápido · ambiente de teste
      </span>

      <div className="flex flex-col gap-[7px]">
        {CONTAS_TESTE.map((conta) => (
          <button
            key={conta.email}
            type="button"
            disabled={loading}
            onClick={() => onEntrar(conta.email)}
            className="ci-foco-botao flex cursor-pointer items-center justify-between gap-3 rounded-ci border border-line bg-panel px-[11px] py-[8px] text-left transition-colors duration-100 hover:border-line-strong hover:bg-elevated disabled:cursor-not-allowed disabled:opacity-55"
          >
            <span className="flex items-center gap-[10px]">
              <Avatar name={conta.nome} size={28} />
              <span className="flex flex-col gap-[2px] leading-none">
                <span className="text-[13px] font-medium text-ink">{conta.nome}</span>
                <span className="text-[11px] text-mid">{conta.descricao}</span>
              </span>
            </span>
            <span className="flex shrink-0 items-center gap-[5px] font-mono text-[11px] text-steel">
              entrar
              <ArrowRight size={12} strokeWidth={2} aria-hidden />
            </span>
          </button>
        ))}
      </div>
    </section>
  )
}
