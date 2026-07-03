import { useState, type FormEvent } from 'react'
import { Check } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { AuthLayout } from '@/layouts/AuthLayout'
import { Button } from '@/components/ui/button'
import { PasswordInput } from '@/components/ui/input'
import { toast } from '@/components/ui/toaster'

/** Fluxo previsto (ainda sem endpoint no backend — ver lacuna nº 3 da API). */
export function DefinirNovaSenhaPage() {
  const navigate = useNavigate()
  const [senha, setSenha] = useState('')
  const [confirma, setConfirma] = useState('')
  const [error, setError] = useState<string | null>(null)

  const match = confirma.length > 0 && senha === confirma
  const strong = senha.length >= 8

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!strong) return setError('A senha deve ter no mínimo 8 caracteres.')
    if (!match) return setError('As senhas não coincidem.')
    setError(null)
    toast.success('Senha alterada! Você já pode entrar.')
    navigate('/entrar')
  }

  return (
    <AuthLayout>
      <form
        onSubmit={onSubmit}
        className="flex w-[424px] max-w-full flex-col gap-4 rounded-2xl border border-border bg-surface p-8 shadow-[0_30px_70px_-30px_rgba(0,0,0,.85)]"
      >
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[21px] font-bold tracking-tight text-heading">Definir nova senha</h1>
          <span className="text-[13.5px] text-muted">Escolha uma senha forte com no mínimo 8 caracteres.</span>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="nova" className="text-[12.5px] font-semibold text-label">
            Nova senha
          </label>
          <PasswordInput
            id="nova"
            autoComplete="new-password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            minLength={8}
            required
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="confirma" className="text-[12.5px] font-semibold text-label">
            Confirmar senha
          </label>
          <div className="relative flex items-center">
            <PasswordInput
              id="confirma"
              autoComplete="new-password"
              value={confirma}
              onChange={(e) => setConfirma(e.target.value)}
              className={match ? 'border-success pr-10' : undefined}
              required
            />
          </div>
          {match && (
            <span className="flex items-center gap-1.5 text-[11.5px] text-success">
              <Check size={13} /> As senhas coincidem.
            </span>
          )}
        </div>

        {error && (
          <div className="rounded-lg border border-danger/30 bg-danger/10 px-3 py-2.5 text-[12.5px] text-danger">
            {error}
          </div>
        )}

        <Button type="submit" size="lg" className="w-full">
          Salvar senha
        </Button>
      </form>
    </AuthLayout>
  )
}
