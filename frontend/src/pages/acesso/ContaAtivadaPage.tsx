import { useEffect } from 'react'
import { AlertTriangle, CheckCircle2 } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { buttonClasses } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { useAtivarConta } from '@/features/identity/hooks'
import { emitirContaAtivada } from '@/lib/activationSignal'

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
    <AuthLayout>
      <div className="flex w-[440px] max-w-full flex-col items-center gap-4 rounded-2xl border border-border bg-surface p-8 text-center shadow-[0_24px_60px_-30px_rgba(0,0,0,.85)]">
        {loading ? (
          <>
            <Spinner size={28} color="var(--brand)" />
            <span className="text-sm text-muted">Ativando sua conta…</span>
          </>
        ) : isSuccess ? (
          <>
            <div className="flex h-[52px] w-[52px] items-center justify-center rounded-[15px] bg-success/[.13]">
              <CheckCircle2 size={26} className="text-success" />
            </div>
            <div className="flex flex-col gap-1.5">
              <h1 className="text-[18px] font-bold text-heading">Conta ativada!</h1>
              <p className="text-[13px] leading-relaxed text-muted">
                Tudo certo. Você já pode entrar no CodeInsights.
              </p>
            </div>
            <Link to="/entrar" className={buttonClasses({ className: 'mt-1 w-full' })}>
              Ir para o login
            </Link>
          </>
        ) : (
          <>
            <div className="flex h-[52px] w-[52px] items-center justify-center rounded-[15px] bg-danger/[.12]">
              <AlertTriangle size={25} className="text-danger" />
            </div>
            <div className="flex flex-col gap-1.5">
              <h1 className="text-[18px] font-bold text-heading">Link inválido ou expirado</h1>
              <p className="text-[13px] leading-relaxed text-muted">
                {token
                  ? 'Este link de ativação não é mais válido. Solicite um novo.'
                  : 'Nenhum token de ativação foi informado.'}
              </p>
            </div>
            <Link to="/criar-conta" className={buttonClasses({ variant: 'secondary', className: 'mt-1 w-full' })}>
              Criar nova conta
            </Link>
          </>
        )}
      </div>
    </AuthLayout>
  )
}
