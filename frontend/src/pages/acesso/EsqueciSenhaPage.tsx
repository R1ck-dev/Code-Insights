import { useState, type FormEvent } from 'react'
import { Mail } from 'lucide-react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'

/**
 * Fluxo previsto (ainda sem endpoint no backend — ver lacuna nº 3 da API).
 * Mostra sempre a mensagem neutra, sem revelar se o e-mail existe.
 */
export function EsqueciSenhaPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setSent(true)
  }

  return (
    <AuthLayout>
      <form
        onSubmit={onSubmit}
        className="flex w-[424px] max-w-full flex-col gap-[18px] rounded-2xl border border-border bg-surface p-8 shadow-[0_30px_70px_-30px_rgba(0,0,0,.85)]"
      >
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[21px] font-bold tracking-tight text-heading">Recuperar senha</h1>
          <span className="text-[13.5px] leading-relaxed text-muted">
            Informe seu e-mail e enviaremos um link para redefinir a senha.
          </span>
        </div>

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

        <Button type="submit" size="lg" className="w-full">
          Enviar link
        </Button>

        {sent && (
          <div className="flex gap-2.5 rounded-lg border border-border bg-input px-3 py-3">
            <Mail size={15} className="mt-0.5 shrink-0 text-brand-strong" />
            <span className="text-[12px] leading-relaxed text-subtle">
              Se o e-mail estiver cadastrado, você receberá um link em instantes.
            </span>
          </div>
        )}

        <Link to="/entrar" className="text-center text-[13px] font-semibold text-brand-strong hover:underline">
          Voltar ao login
        </Link>
      </form>
    </AuthLayout>
  )
}
