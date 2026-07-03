import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ExternalLink, Globe, Lock, LogOut } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Chip } from '@/components/ui/badge'
import { toast } from '@/components/ui/toaster'
import { Avatar } from '@/components/Avatar'
import { StatusContaBadge } from '@/components/domain/badges'
import { ROLE_LABEL } from '@/domain/enums'
import { formatDate } from '@/lib/utils'
import { apiErrorMessage } from '@/lib/api'
import {
  useAlterarVisibilidadePerfil,
  useAtualizarPerfil,
} from '@/features/identity/hooks'

const USERNAME_RE = /^[A-Za-z0-9._-]+$/

/** Linha somente-leitura (rótulo à esquerda, valor à direita) das configurações de conta. */
function InfoRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 border-t border-border-subtle py-3">
      <span className="text-[13px] text-muted">{label}</span>
      <span className="flex items-center gap-2.5 text-[13.5px] text-fg">{children}</span>
    </div>
  )
}

export function MeuPerfilPage() {
  const { user, refresh, logout } = useAuth()
  const navigate = useNavigate()
  const atualizarPerfil = useAtualizarPerfil()
  const alterarVisibilidade = useAlterarVisibilidadePerfil()
  const [username, setUsername] = useState(user?.username ?? '')

  // O guard de rota garante um usuário autenticado nesta página.
  if (!user) return null

  const publico = user.visibilidadePerfil === 'PUBLICO'
  const usernameValido =
    username.length >= 3 && username.length <= 50 && USERNAME_RE.test(username)
  const inalterado = username === user.username
  const podeSalvar = usernameValido && !inalterado && !atualizarPerfil.isPending

  async function salvarUsername() {
    try {
      await atualizarPerfil.mutateAsync({ username })
      await refresh()
      toast.success('Nome de usuário atualizado.')
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível atualizar o nome de usuário.'))
    }
  }

  async function alternarVisibilidade(proximo: boolean) {
    try {
      await alterarVisibilidade.mutateAsync(proximo)
      await refresh()
      toast.success(
        proximo ? 'Portfólio agora está público.' : 'Portfólio agora está privado.',
      )
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível alterar a visibilidade.'))
    }
  }

  function sair() {
    logout()
    navigate('/')
  }

  return (
    <PageContainer>
      <PageHeader
        title="Perfil & configurações"
        subtitle="Seus dados de conta e a visibilidade do portfólio."
      />

      <div className="grid grid-cols-1 items-start gap-[18px] md:grid-cols-[1.15fr_1fr]">
        {/* Coluna esquerda — identidade + dados da conta */}
        <Card className="flex flex-col gap-[18px] p-[22px]">
          <div className="flex items-center gap-4">
            <Avatar size={60} name={user.username} />
            <div className="flex flex-col gap-0.5">
              <span className="text-[19px] font-bold text-heading">@{user.username}</span>
              <span className="text-[13px] text-muted">
                {ROLE_LABEL[user.role]} · membro desde {formatDate(user.criadoEm)}
              </span>
            </div>
          </div>

          <div className="flex flex-col gap-1.5 border-t border-border-subtle pt-4">
            <Label htmlFor="username">Nome de usuário</Label>
            <div className="flex gap-[9px]">
              <Input
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="off"
                spellCheck={false}
                maxLength={50}
                className="flex-1"
              />
              <Button
                variant="secondary"
                onClick={salvarUsername}
                disabled={!podeSalvar}
                loading={atualizarPerfil.isPending}
              >
                Salvar
              </Button>
            </div>
            <span className="text-[11.5px] text-subtle">
              3–50 · letras, números e <span className="font-mono text-muted">. _ -</span>
            </span>
          </div>

          <div className="flex flex-col">
            <InfoRow label="E-mail">
              <span>{user.email}</span>
              <Chip mono>somente leitura</Chip>
            </InfoRow>
            <InfoRow label="Papel">{ROLE_LABEL[user.role]}</InfoRow>
            <InfoRow label="Status da conta">
              <StatusContaBadge status={user.status} />
            </InfoRow>
          </div>
        </Card>

        {/* Coluna direita — visibilidade + conta */}
        <div className="flex flex-col gap-[18px]">
          <Card className="flex flex-col gap-[15px] p-[22px]">
            <span className="text-[14px] font-semibold text-heading">
              Visibilidade do portfólio
            </span>

            <div className="flex items-center justify-between gap-[14px]">
              <span className="inline-flex items-center gap-2 text-[13.5px] font-semibold text-fg">
                {publico ? (
                  <Globe size={15} className="text-success" />
                ) : (
                  <Lock size={15} className="text-muted" />
                )}
                {publico ? 'Público' : 'Privado'}
              </span>
              <Switch
                checked={publico}
                onCheckedChange={alternarVisibilidade}
                disabled={alterarVisibilidade.isPending}
                aria-label="Alternar visibilidade do portfólio"
              />
            </div>

            <span className="text-[12.5px] leading-relaxed text-subtle">
              {publico
                ? 'Qualquer pessoa pode ver seu portfólio e os seus desafios públicos. Desafios privados continuam ocultos.'
                : 'Seu portfólio está oculto. Somente você enxerga seus desafios e resoluções enquanto estiver privado.'}
            </span>

            <Link
              to={`/u/${user.id}`}
              className="inline-flex items-center gap-1.5 text-[13px] font-semibold text-brand-strong hover:underline"
            >
              Ver meu portfólio público
              <ExternalLink size={14} />
            </Link>
          </Card>

          <Card className="flex flex-col gap-[13px] p-[22px]">
            <span className="text-[14px] font-semibold text-heading">Conta</span>
            <Button variant="secondary" onClick={sair} className="w-full">
              <LogOut size={16} />
              Sair da conta
            </Button>
          </Card>
        </div>
      </div>
    </PageContainer>
  )
}
