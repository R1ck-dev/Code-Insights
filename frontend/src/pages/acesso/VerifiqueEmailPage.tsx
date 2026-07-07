import { useEffect, useState } from 'react'
import { CheckCircle2, Mail } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Badge } from '@/components/ui/badge'
import { Button, buttonClasses } from '@/components/ui/button'
import { toast } from '@/components/ui/toaster'
import { useReenviarAtivacao } from '@/features/identity/hooks'
import { ouvirContaAtivada } from '@/lib/activationSignal'
import { apiErrorMessage } from '@/lib/api'

export function VerifiqueEmailPage() {
  const location = useLocation()
  const email = (location.state as { email?: string } | null)?.email
  const reenviar = useReenviarAtivacao()
  const [ativada, setAtivada] = useState(false)

  // Reage à ativação feita em outra aba da mesma origem (link do e-mail → /ativar).
  useEffect(() => {
    return ouvirContaAtivada((emailAtivado) => {
      if (!emailAtivado || !email || emailAtivado === email) setAtivada(true)
    })
  }, [email])

  async function handleReenviar() {
    if (!email) return
    try {
      await reenviar.mutateAsync(email)
      toast.success('Se a conta estiver pendente, reenviamos o e-mail de ativação.')
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível reenviar agora. Tente mais tarde.'))
    }
  }

  return (
    <AuthLayout>
      <div className="flex w-[440px] max-w-full flex-col items-center gap-4 rounded-2xl border border-border bg-surface p-9 text-center shadow-[0_30px_70px_-30px_rgba(0,0,0,.85)]">
        {ativada ? (
          <>
            <div className="flex h-[54px] w-[54px] items-center justify-center rounded-[15px] bg-success/[.13]">
              <CheckCircle2 size={26} className="text-success" />
            </div>
            <div className="flex flex-col gap-2">
              <h1 className="text-[21px] font-bold tracking-tight text-heading">Conta ativada!</h1>
              <p className="text-sm leading-relaxed text-muted">
                Sua conta foi confirmada. Você já pode entrar no CodeInsights.
              </p>
            </div>
            <Badge tone="success" dot>
              Conta: Ativada
            </Badge>
            <Link to="/entrar" className={buttonClasses({ className: 'mt-1 w-full' })}>
              Ir para o login
            </Link>
          </>
        ) : (
          <>
            <div className="flex h-[54px] w-[54px] items-center justify-center rounded-[15px] bg-brand/[.12]">
              <Mail size={26} className="text-brand-strong" />
            </div>
            <div className="flex flex-col gap-2">
              <h1 className="text-[21px] font-bold tracking-tight text-heading">
                Verifique seu e-mail
              </h1>
              <p className="text-sm leading-relaxed text-muted">
                Enviamos um link de ativação{' '}
                {email ? (
                  <>
                    para <span className="font-semibold text-fg">{email}</span>
                  </>
                ) : (
                  'para o seu e-mail'
                )}
                . Confirme para ativar sua conta.
              </p>
            </div>
            <Badge tone="warning" dot>
              Conta: Aguardando verificação
            </Badge>
            <div className="mt-1 flex w-full gap-2.5">
              <Button
                variant="secondary"
                className="flex-1"
                loading={reenviar.isPending}
                disabled={!email}
                onClick={handleReenviar}
              >
                Reenviar e-mail
              </Button>
              <Link
                to="/entrar"
                className={buttonClasses({ variant: 'ghost', className: 'flex-1' })}
              >
                Voltar ao login
              </Link>
            </div>
            <span className="text-[12px] text-subtle">
              Não recebeu? Verifique a caixa de spam.
            </span>
          </>
        )}
      </div>
    </AuthLayout>
  )
}
