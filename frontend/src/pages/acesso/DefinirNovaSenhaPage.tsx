import { useState, type FormEvent } from 'react'
import { AlertTriangle, Check, KeyRound } from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { AuthCard, AuthLayout } from '@/layouts/AuthLayout'
import { Button, buttonClasses } from '@/components/ui/button'
import { PasswordInput } from '@/components/ui/input'
import type { NivelSenha } from '@/components/ui/password-strength'
import { toast } from '@/components/ui/toaster'
import { useRedefinirSenha } from '@/features/identity/hooks'
import { apiErrorMessage } from '@/lib/api'

const MIN_SENHA = 8

/**
 * Força da senha (o cálculo é da página; o `PasswordStrength` só desenha).
 * Abaixo do mínimo a senha é sempre "fraca" — nenhuma variedade compensa o tamanho.
 */
function nivelDaSenha(senha: string): NivelSenha | null {
  if (!senha) return null
  if (senha.length < MIN_SENHA) return 'fraca'

  let pontos = 0
  if (senha.length >= 12) pontos++
  if (/[a-z]/.test(senha) && /[A-Z]/.test(senha)) pontos++
  if (/\d/.test(senha)) pontos++
  if (/[^A-Za-z0-9]/.test(senha)) pontos++

  if (pontos >= 4) return 'muito-forte'
  if (pontos >= 3) return 'forte'
  if (pontos >= 1) return 'media'
  return 'fraca'
}

/** Tela I · Definir nova senha — lê o `?token` do e-mail e redefine a senha. */
export function DefinirNovaSenhaPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [senha, setSenha] = useState('')
  const [confirma, setConfirma] = useState('')
  const [error, setError] = useState<string | null>(null)
  const redefinir = useRedefinirSenha()

  const match = confirma.length > 0 && senha === confirma
  const strong = senha.length >= MIN_SENHA

  // Feedback local, ao vivo — a mesma verdade que os guardas do submit checam.
  const erroSenha =
    senha.length > 0 && !strong ? `A senha deve ter no mínimo ${MIN_SENHA} caracteres.` : null
  const erroConfirma = confirma.length > 0 && !match ? 'As senhas não coincidem.' : null
  // O banner fica só com o que não tem campo próprio (falha da API, link expirado).
  const erroGeral = error && error !== erroSenha && error !== erroConfirma ? error : null

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!token) return setError('Link inválido ou incompleto. Solicite um novo link de redefinição.')
    if (!strong) return setError(`A senha deve ter no mínimo ${MIN_SENHA} caracteres.`)
    if (!match) return setError('As senhas não coincidem.')
    setError(null)
    try {
      await redefinir.mutateAsync({ token, novaSenha: senha })
      toast.success('Senha alterada! Você já pode entrar.')
      navigate('/entrar')
    } catch (err) {
      setError(apiErrorMessage(err, 'Não foi possível redefinir a senha. O link pode ter expirado.'))
    }
  }

  // Sem token não há o que redefinir: cartão de link inválido (padrão da tela G).
  if (!token) {
    return (
      <AuthLayout card={false}>
        <AuthCard
          width={380}
          className="items-center gap-[14px] p-[28px] text-center shadow-float"
        >
          <span
            aria-hidden
            className="flex size-[52px] items-center justify-center rounded-ci border border-erro-tile-line bg-erro-tile-bg"
          >
            <AlertTriangle size={25} strokeWidth={2} className="text-erro-texto" />
          </span>
          <div className="flex flex-col gap-[8px]">
            <h1 className="font-sans text-[18px] leading-tight font-semibold text-ink">
              Link inválido ou expirado
            </h1>
            <p className="text-[13px] leading-[1.5] text-mid">
              Este link de redefinição não é mais válido. Solicite um novo.
            </p>
          </div>
          <Link
            to="/recuperar-senha"
            className={buttonClasses({
              variant: 'secondary',
              size: 'lg',
              font: 'sans',
              fullWidth: true,
              className: 'mt-[2px] h-[42px]',
            })}
          >
            Solicitar novo link
          </Link>
        </AuthCard>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout width={424}>
      <form onSubmit={onSubmit} className="flex flex-col gap-[15px]" noValidate>
        <div className="flex flex-col gap-[6px]">
          <h1 className="font-sans text-[21px] leading-tight font-semibold tracking-[-.02em] text-ink">
            Definir nova senha
          </h1>
          <p className="text-[13px] text-mid">
            Escolha uma senha forte com no mínimo {MIN_SENHA} caracteres.
          </p>
        </div>

        <PasswordInput
          id="nova-senha"
          label="Nova senha"
          size="lg"
          autoComplete="new-password"
          autoFocus
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          strength={nivelDaSenha(senha)}
          error={erroSenha}
          required
        />

        <div className="flex flex-col gap-[7px]">
          <PasswordInput
            id="confirmar-senha"
            label="Confirmar senha"
            size="lg"
            autoComplete="new-password"
            value={confirma}
            onChange={(e) => setConfirma(e.target.value)}
            valid={match}
            error={erroConfirma}
            required
          />
          {match && (
            <span className="flex items-center gap-[6px] text-[11.5px] leading-snug text-sucesso-ink">
              <Check size={13} strokeWidth={2} aria-hidden className="shrink-0" />
              As senhas coincidem.
            </span>
          )}
        </div>

        {erroGeral && (
          <p
            role="alert"
            className="flex gap-[9px] rounded-ci border border-erro-card-line bg-erro-card-bg px-[13px] py-[11px] text-[12.5px] leading-[1.5] text-erro-texto"
          >
            <AlertTriangle size={15} strokeWidth={2} aria-hidden className="mt-[1px] shrink-0" />
            {erroGeral}
          </p>
        )}

        <Button
          type="submit"
          size="lg"
          font="sans"
          fullWidth
          icon={KeyRound}
          loading={redefinir.isPending}
        >
          Salvar senha
        </Button>
      </form>
    </AuthLayout>
  )
}
