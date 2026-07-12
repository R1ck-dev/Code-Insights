import { useEffect, useState } from 'react'
import type { LucideIcon } from 'lucide-react'
import { Check, CheckCircle2, Mail } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Badge } from '@/components/ui/badge'
import { Button, buttonClasses } from '@/components/ui/button'
import { toast } from '@/components/ui/toaster'
import { useReenviarAtivacao } from '@/features/identity/hooks'
import { ouvirContaAtivada } from '@/lib/activationSignal'
import { apiErrorMessage } from '@/lib/api'
import { cn } from '@/lib/utils'

/*
 * F · Verifique e-mail (spec 03 §F) — cartão 440px, conteúdo centralizado.
 *
 * A tela muda de pele sozinha: `ouvirContaAtivada` avisa quando o usuário abre o link
 * do e-mail em OUTRA aba (BroadcastChannel/storage, same-origin). O cartão vira então o
 * estado de sucesso da tela G — por isso o wrapper é `aria-live="polite"`.
 */

/** Tile 54×54 do cabeçalho do cartão. Neutro (aguardando) ou sucesso (ativada). */
function TileIcone({ icon: Icon, tom }: { icon: LucideIcon; tom: 'neutro' | 'sucesso' }) {
  return (
    <span
      aria-hidden
      className={cn(
        'flex size-[54px] shrink-0 items-center justify-center rounded-ci border',
        tom === 'sucesso'
          ? 'border-sucesso-line bg-sucesso-bg text-sucesso'
          : 'border-line-strong bg-elevated text-steel',
      )}
    >
      <Icon size={26} strokeWidth={2} />
    </span>
  )
}

export function VerifiqueEmailPage() {
  const location = useLocation()
  const email = (location.state as { email?: string } | null)?.email
  const reenviar = useReenviarAtivacao()
  const [ativada, setAtivada] = useState(false)
  const [reenviado, setReenviado] = useState(false)

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
      setReenviado(true)
      toast.success('Se a conta estiver pendente, reenviamos o e-mail de ativação.')
    } catch (err) {
      setReenviado(false)
      toast.error(apiErrorMessage(err, 'Não foi possível reenviar agora. Tente mais tarde.'))
    }
  }

  return (
    <AuthLayout width={440}>
      <div
        aria-live="polite"
        className="flex flex-col items-center gap-4 text-center"
      >
        {ativada ? (
          <>
            <TileIcone icon={CheckCircle2} tom="sucesso" />

            <div className="flex flex-col gap-2">
              <h1 className="text-[21px] font-semibold tracking-[-.02em] text-ink">
                Conta ativada!
              </h1>
              <p className="text-[14px] leading-[1.55] text-mid">
                Tudo certo. Você já pode entrar no CodeInsights.
              </p>
            </div>

            <Link
              to="/entrar"
              className={buttonClasses({
                font: 'sans',
                size: 'lg',
                fullWidth: true,
                className: 'mt-1 h-[42px]',
              })}
            >
              Ir para o login
            </Link>
          </>
        ) : (
          <>
            <TileIcone icon={Mail} tom="neutro" />

            <div className="flex flex-col gap-2">
              <h1 className="text-[21px] font-semibold tracking-[-.02em] text-ink">
                Verifique seu e-mail
              </h1>
              <p className="text-[14px] leading-[1.55] text-mid">
                Enviamos um link de ativação{' '}
                {email ? (
                  <>
                    para <span className="font-semibold text-ink">{email}</span>
                  </>
                ) : (
                  'para o seu e-mail'
                )}
                . Confirme para ativar sua conta.
              </p>
            </div>

            <Badge tom="atencao" dot>
              Aguardando verificação
            </Badge>

            <div className="mt-1 flex w-full gap-[10px]">
              <Button
                variant="secondary"
                className="h-[42px] flex-1"
                loading={reenviar.isPending}
                disabled={!email}
                onClick={handleReenviar}
              >
                Reenviar e-mail
              </Button>
              <Link
                to="/entrar"
                className={buttonClasses({
                  variant: 'ghost',
                  className: 'h-[42px] flex-1',
                })}
              >
                Voltar ao login
              </Link>
            </div>

            {reenviado ? (
              <span className="inline-flex items-center gap-[6px] text-[12px] text-sucesso-ink">
                <Check size={13} strokeWidth={2} aria-hidden className="shrink-0" />
                E-mail reenviado. Confira sua caixa de entrada.
              </span>
            ) : (
              <span className="text-[12px] text-soft">
                Não recebeu? Verifique a caixa de spam.
              </span>
            )}
          </>
        )}
      </div>
    </AuthLayout>
  )
}
