import { useEffect } from 'react'
import type { ReactNode } from 'react'
import { AlertTriangle, CheckCircle2 } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { buttonClasses } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { useAtivarConta } from '@/features/identity/hooks'
import { emitirContaAtivada } from '@/lib/activationSignal'
import { cn } from '@/lib/utils'

/*
 * G · Conta ativada (spec 03 §G) — ÓRBITA.
 *
 * Um único cartão de 380px, centralizado, escolhido pelo resultado da ativação do token
 * (o protótipo desenha os dois estados lado a lado só para exibição).
 * Estados: verificando (tile neutro + spinner) · sucesso (verde) · link inválido (erro).
 */

/** Tile de ícone 52×52 do cartão — neutro, sucesso ou erro. */
function TileIcone({
  icon: Icon,
  tom,
  size,
}: {
  icon: LucideIcon
  tom: 'sucesso' | 'erro'
  size: number
}) {
  return (
    <span
      className={cn(
        'flex h-[52px] w-[52px] items-center justify-center rounded-ci border',
        tom === 'sucesso'
          ? 'border-sucesso-line bg-sucesso-bg'
          : 'border-erro-tile-line bg-erro-tile-bg',
      )}
    >
      <Icon
        size={size}
        strokeWidth={2}
        aria-hidden
        className={tom === 'sucesso' ? 'text-sucesso-ink' : 'text-erro-texto'}
      />
    </span>
  )
}

/** Cartão base dos três estados: panel + hairline + raio 3px + sombra flutuante. */
function CartaoResultado({ children }: { children: ReactNode }) {
  return (
    <div
      aria-live="polite"
      className="flex w-full max-w-[380px] flex-col items-center gap-[14px] rounded-ci border border-line bg-panel p-[28px] text-center shadow-float"
    >
      {children}
    </div>
  )
}

function Titulo({ children }: { children: ReactNode }) {
  return <h1 className="text-[18px] font-semibold text-ink">{children}</h1>
}

function Corpo({ children }: { children: ReactNode }) {
  return <p className="text-[13px] leading-[1.5] text-mid">{children}</p>
}

const ACAO = 'mt-0.5 h-[42px]'

export function ContaAtivadaPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const { isPending, isSuccess } = useAtivarConta(token)

  const loading = !!token && isPending

  // Avisa a aba de /verifique-email (mesma origem) que a conta foi ativada.
  useEffect(() => {
    if (isSuccess) emitirContaAtivada()
  }, [isSuccess])

  return (
    <AuthLayout card={false}>
      {loading ? (
        <CartaoResultado>
          <span className="flex h-[52px] w-[52px] items-center justify-center rounded-ci border border-line-strong bg-elevated">
            <Spinner size={22} label="Ativando sua conta" />
          </span>
          <div className="flex flex-col gap-2">
            <Titulo>Ativando sua conta…</Titulo>
            <Corpo>Validando o link de ativação.</Corpo>
          </div>
        </CartaoResultado>
      ) : isSuccess ? (
        <CartaoResultado>
          <TileIcone icon={CheckCircle2} tom="sucesso" size={26} />
          <div className="flex flex-col gap-2">
            <Titulo>Conta ativada!</Titulo>
            <Corpo>Tudo certo. Você já pode entrar no CodeInsights.</Corpo>
          </div>
          <Link
            to="/entrar"
            className={buttonClasses({ font: 'sans', size: 'lg', fullWidth: true, className: ACAO })}
          >
            Ir para o login
          </Link>
        </CartaoResultado>
      ) : (
        <CartaoResultado>
          <TileIcone icon={AlertTriangle} tom="erro" size={25} />
          <div className="flex flex-col gap-2">
            <Titulo>Link inválido ou expirado</Titulo>
            <Corpo>
              {token
                ? 'Este link de ativação não é mais válido. Solicite um novo.'
                : 'Nenhum token de ativação foi informado.'}
            </Corpo>
          </div>
          <Link
            to="/criar-conta"
            className={buttonClasses({
              variant: 'secondary',
              font: 'sans',
              size: 'lg',
              fullWidth: true,
              className: ACAO,
            })}
          >
            Criar nova conta
          </Link>
        </CartaoResultado>
      )}
    </AuthLayout>
  )
}
