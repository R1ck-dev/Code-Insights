import { useState, type FormEvent } from 'react'
import { AlertTriangle, Check, Mail } from 'lucide-react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useEsqueciSenha } from '@/features/identity/hooks'
import { apiErrorMessage } from '@/lib/api'
import { cn } from '@/lib/utils'

/*
 * H · Esqueci a senha (spec 03 §H) — cartão 424px sobre o céu.
 *
 * A nota de recuo é PERMANENTE e NEUTRA por design: nunca revela se o e-mail existe
 * (proteção contra enumeração de usuários). Ao enviar, ela só muda de tom — a copy é a mesma.
 */
export function EsqueciSenhaPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const esqueci = useEsqueciSenha()

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await esqueci.mutateAsync(email)
      setSent(true)
    } catch (err) {
      setError(apiErrorMessage(err, 'Não foi possível enviar o link. Tente novamente.'))
    }
  }

  return (
    <AuthLayout width={424}>
      <form onSubmit={onSubmit} className="flex flex-col gap-[16px]">
        <div className="flex flex-col gap-[6px]">
          <h1 className="font-sans text-[21px] leading-tight font-semibold tracking-[-.02em] text-ink">
            Recuperar senha
          </h1>
          <p className="font-sans text-[13px] leading-[1.5] text-mid">
            Informe seu e-mail e enviaremos um link para redefinir a senha.
          </p>
        </div>

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

        {error && (
          <div
            role="alert"
            className="flex items-start gap-[9px] rounded-ci border border-erro-line bg-erro-bg px-[13px] py-[10px] font-sans text-[12.5px] leading-[1.5] text-erro-texto"
          >
            <AlertTriangle size={14} strokeWidth={2} aria-hidden className="mt-[2px] shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <Button
          type="submit"
          variant="primary"
          size="lg"
          font="sans"
          fullWidth
          loading={esqueci.isPending}
        >
          Enviar link
        </Button>

        {/* Nota neutra — a MESMA copy antes e depois do envio (spec 03 §H.4). */}
        <div
          role="status"
          className={cn(
            'flex items-start gap-[9px] rounded-ci border px-[13px] py-[12px]',
            sent ? 'border-sucesso-line bg-sucesso-bg' : 'border-line bg-recess',
          )}
        >
          {sent ? (
            <Check size={15} strokeWidth={2} aria-hidden className="mt-[1px] shrink-0 text-sucesso" />
          ) : (
            <Mail size={15} strokeWidth={2} aria-hidden className="mt-[1px] shrink-0 text-steel" />
          )}
          <span className="font-sans text-[12px] leading-[1.5] text-soft">
            {sent && <span className="font-semibold text-sucesso-ink">Pedido enviado. </span>}
            Se o e-mail estiver cadastrado, você receberá um link em instantes.
          </span>
        </div>

        <Link
          to="/entrar"
          className="ci-foco-botao mx-auto rounded-ci px-2 py-1 text-center font-sans text-[12.5px] font-medium text-steel hover:text-steel-hover"
        >
          Voltar ao login
        </Link>
      </form>
    </AuthLayout>
  )
}
