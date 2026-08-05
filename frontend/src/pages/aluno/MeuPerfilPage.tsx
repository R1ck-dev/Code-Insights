/*
 * P · Meu perfil (spec 04 §8) — identidade ÓRBITA.
 *
 * Dados da conta (username editável + e-mail/papel/status em somente-leitura),
 * visibilidade do portfólio (Switch — a ÚNICA exceção ao raio 3px do sistema)
 * e o card "Conta" com Sair.
 *
 * Sem métrica nesta tela: nenhuma superfície de complexidade, nenhum colormap.
 * A única cor aqui é a semântica (sucesso/erro) — regra 1.
 */
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Compass, ExternalLink, LogOut, Trash2 } from 'lucide-react'
import { useAuth } from '@/auth/useAuth'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogIconTile,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input, Mensagem } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { toast } from '@/components/ui/toaster'
import { Avatar } from '@/components/Avatar'
import { StatusContaChip } from '@/components/domain/badges'
import { ROLE_LABEL, VISIBILIDADE_ICONE, VISIBILIDADE_META } from '@/domain/enums'
import { formatDate } from '@/lib/utils'
import { apiErrorMessage } from '@/lib/api'
import {
  useAlterarVisibilidadePerfil,
  useAtualizarPerfil,
  useExcluirMinhaConta,
} from '@/features/identity/hooks'

const USERNAME_RE = /^[A-Za-z0-9._-]+$/

/** Linha somente-leitura: chave mono à esquerda (o dado é medido), valor à direita. */
function LinhaInfo({ chave, children }: { chave: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1.5 border-t border-line-soft py-3">
      <span className="font-mono text-[12px] tracking-[.04em] text-soft">{chave}</span>
      <span className="flex min-w-0 items-center gap-[9px] text-[13.5px] text-ink">
        {children}
      </span>
    </div>
  )
}

/** Selo neutro de campo imutável (mono 10px, recuo + hairline). */
function SeloSomenteLeitura() {
  return (
    <span className="shrink-0 rounded-ci border border-line bg-recess px-[7px] py-[2px] font-mono text-[10px] text-soft">
      somente leitura
    </span>
  )
}

export function MeuPerfilPage() {
  const { user, refresh, logout } = useAuth()
  const navigate = useNavigate()
  const atualizarPerfil = useAtualizarPerfil()
  const alterarVisibilidade = useAlterarVisibilidadePerfil()
  const [username, setUsername] = useState(user?.username ?? '')
  const [erroServidor, setErroServidor] = useState<string | null>(null)
  const [excluindo, setExcluindo] = useState(false)

  // O guard de rota garante um usuário autenticado nesta página.
  if (!user) return null

  const publico = user.visibilidadePerfil === 'PUBLICO'
  const IconeVisibilidade = VISIBILIDADE_ICONE[user.visibilidadePerfil]
  const rotuloVisibilidade = VISIBILIDADE_META[user.visibilidadePerfil].label

  const usernameValido =
    username.length >= 3 && username.length <= 50 && USERNAME_RE.test(username)
  const inalterado = username === user.username
  const podeSalvar = usernameValido && !inalterado && !atualizarPerfil.isPending

  // Erro local só aparece depois que o campo foi tocado (≠ do valor salvo).
  const erroLocal =
    inalterado || usernameValido
      ? null
      : username.length < 3
        ? 'Use pelo menos 3 caracteres.'
        : 'Use apenas letras, números e . _ -'
  const erroUsername = erroLocal ?? erroServidor

  function editarUsername(valor: string) {
    setUsername(valor)
    setErroServidor(null)
  }

  async function salvarUsername() {
    try {
      await atualizarPerfil.mutateAsync({ username })
      await refresh()
      setErroServidor(null)
      toast.success('Nome de usuário atualizado.')
    } catch (err) {
      const msg = apiErrorMessage(err, 'Não foi possível atualizar o nome de usuário.')
      setErroServidor(msg)
      toast.error(msg)
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

      <div className="grid grid-cols-1 items-start gap-[18px] lg:grid-cols-[1.15fr_1fr]">
        {/* ---------------------------------------- dados da conta (esquerda) */}
        <Card className="flex flex-col gap-[18px] p-[22px]">
          <div className="flex items-center gap-4">
            <Avatar name={user.username} size={60} className="text-[22px]! font-bold" />
            <div className="flex min-w-0 flex-col gap-[3px]">
              <span className="truncate text-[19px] font-bold leading-tight text-ink">
                @{user.username}
              </span>
              <span className="text-[13px] text-mid">
                {ROLE_LABEL[user.role]} · membro desde{' '}
                <span className="tabular">{formatDate(user.criadoEm)}</span>
              </span>
            </div>
          </div>

          <div className="flex flex-col gap-[6px] border-t border-line-soft pt-4">
            <Label htmlFor="username">Nome de usuário</Label>
            <div className="flex items-start gap-[9px]">
              <Input
                id="username"
                mono
                value={username}
                onChange={(e) => editarUsername(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && podeSalvar) void salvarUsername()
                }}
                autoComplete="off"
                spellCheck={false}
                maxLength={50}
                disabled={atualizarPerfil.isPending}
                valid={usernameValido && !inalterado && !erroServidor}
                aria-invalid={erroUsername ? true : undefined}
                aria-describedby={erroUsername ? 'username-erro' : 'username-hint'}
                className="flex-1"
              />
              <Button
                variant="secondary"
                onClick={salvarUsername}
                disabled={!podeSalvar}
                loading={atualizarPerfil.isPending}
              >
                {atualizarPerfil.isPending ? 'Salvando…' : 'Salvar'}
              </Button>
            </div>
            <Mensagem
              id="username"
              error={erroUsername}
              hint={
                <>
                  3–50 · letras, números e{' '}
                  <span className="font-mono text-mid">. _ -</span>
                </>
              }
            />
          </div>

          <div className="flex flex-col">
            <LinhaInfo chave="e-mail">
              <span className="truncate">{user.email}</span>
              <SeloSomenteLeitura />
            </LinhaInfo>
            <LinhaInfo chave="papel">
              <span>{ROLE_LABEL[user.role]}</span>
              <SeloSomenteLeitura />
            </LinhaInfo>
            <LinhaInfo chave="status da conta">
              <StatusContaChip status={user.status} />
            </LinhaInfo>
          </div>
        </Card>

        {/* -------------------------------- visibilidade + conta (direita) */}
        <div className="flex flex-col gap-[18px]">
          <Card className="flex flex-col gap-[15px] p-[22px]">
            <h3 className="text-[14px] font-semibold text-ink">
              Visibilidade do portfólio
            </h3>

            <div className="flex items-center justify-between gap-[14px]">
              <span
                id="visibilidade-estado"
                className="inline-flex items-center gap-[7px] text-[13.5px] font-semibold text-ink"
              >
                <IconeVisibilidade
                  size={15}
                  strokeWidth={2}
                  aria-hidden
                  className={publico ? 'text-sucesso-ink' : 'text-mid'}
                />
                {rotuloVisibilidade}
              </span>
              <Switch
                checked={publico}
                onCheckedChange={alternarVisibilidade}
                disabled={alterarVisibilidade.isPending}
                aria-label="Tornar o portfólio público"
                aria-describedby="visibilidade-efeito"
              />
            </div>

            <p
              id="visibilidade-efeito"
              className="text-[12.5px] leading-[1.55] text-soft"
            >
              {publico
                ? 'Qualquer pessoa pode ver seu portfólio e os seus desafios públicos. Desafios privados continuam ocultos.'
                : 'Seu portfólio está oculto. Somente você enxerga seus desafios e resoluções enquanto estiver privado.'}
            </p>

            {/* O efeito que ninguém adivinha: público = listado na Explorar. */}
            <div className="flex items-start gap-[9px] rounded-ci border border-line bg-recess px-[11px] py-[9px]">
              <Compass
                size={14}
                strokeWidth={2}
                aria-hidden
                className="mt-[2px] shrink-0 text-soft"
              />
              {/* Prosa lê em Space Grotesk (regra 5): esta frase explica, não mede. */}
              <span className="text-[11.5px] leading-[1.6] text-soft">
                Portfólios públicos ficam listados em{' '}
                <span className="text-mid">Explorar</span>, o diretório aberto de portfólios da
                plataforma.
              </span>
            </div>

            <Link
              to={`/u/${user.id}`}
              className="inline-flex w-fit items-center gap-[7px] rounded-ci font-mono text-[12.5px] font-semibold text-steel no-underline hover:text-steel-hover"
            >
              Ver meu portfólio público
              <ExternalLink size={14} strokeWidth={2} aria-hidden />
            </Link>
          </Card>

          <Card className="flex flex-col gap-[13px] p-[22px]">
            <h3 className="text-[14px] font-semibold text-ink">Conta</h3>
            <Button variant="secondary" icon={LogOut} onClick={sair} fullWidth>
              Sair da conta
            </Button>

            <div className="flex flex-col gap-[9px] border-t border-line-soft pt-[13px]">
              <p className="text-[12.5px] leading-[1.55] text-soft">
                Excluir a conta apaga também seus desafios, resoluções, métricas, snippets e o
                consentimento de pesquisa. Não há como desfazer.
              </p>
              <Button
                variant="destructive"
                icon={Trash2}
                onClick={() => setExcluindo(true)}
                fullWidth
              >
                Excluir minha conta
              </Button>
            </div>
          </Card>
        </div>
      </div>

      <DialogoDeExclusao
        aberto={excluindo}
        onFechar={() => setExcluindo(false)}
        username={user.username}
      />
    </PageContainer>
  )
}

/**
 * Exclusão definitiva. Pede a senha em vez de um "tem certeza?": o JWT prova quem é, mas não prova
 * quem está diante do teclado, e o ato não tem volta — não há anonimização nem cópia.
 *
 * O texto lista o que some junto porque ninguém associa "excluir conta" a "perder as métricas da
 * pesquisa". Quem clica precisa saber o tamanho do que está apagando antes de digitar a senha.
 */
function DialogoDeExclusao({
  aberto,
  onFechar,
  username,
}: {
  aberto: boolean
  onFechar: () => void
  username: string
}) {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const excluir = useExcluirMinhaConta()
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  function fechar(proximo: boolean) {
    if (proximo) return
    setSenha('')
    setErro(null)
    onFechar()
  }

  async function confirmar() {
    try {
      await excluir.mutateAsync(senha)
      // logout antes de navegar: a rota de destino é pública, e um token de conta inexistente
      // faria a próxima chamada autenticada devolver 401 numa tela que não sabe tratar isso.
      logout()
      navigate('/')
      toast.success('Conta excluída. Seus dados foram removidos.')
    } catch (err) {
      setErro(apiErrorMessage(err, 'Não foi possível excluir a conta.'))
    }
  }

  return (
    <Dialog open={aberto} onOpenChange={fechar}>
      <DialogContent width={440} className="gap-3.5 p-6">
        <DialogIconTile icon={Trash2} variant="destructive" />
        <div className="flex flex-col gap-[7px]">
          <DialogTitle className="text-[18px]">Excluir a conta @{username}?</DialogTitle>
          <DialogDescription>
            Isto apaga definitivamente seus desafios, resoluções, métricas, snippets e o
            consentimento de pesquisa. Não há como desfazer e não guardamos cópia.
          </DialogDescription>
        </div>

        <div className="flex flex-col gap-[6px]">
          <Label htmlFor="senha-exclusao">Confirme com sua senha</Label>
          <Input
            id="senha-exclusao"
            type="password"
            value={senha}
            onChange={(e) => {
              setSenha(e.target.value)
              setErro(null)
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && senha && !excluir.isPending) void confirmar()
            }}
            autoComplete="current-password"
            disabled={excluir.isPending}
            aria-invalid={erro ? true : undefined}
            aria-describedby={erro ? 'senha-exclusao-erro' : undefined}
          />
          <Mensagem id="senha-exclusao" error={erro} />
        </div>

        <div className="mt-1 flex justify-end gap-2.5">
          <Button variant="secondary" onClick={() => fechar(false)} disabled={excluir.isPending}>
            Cancelar
          </Button>
          <Button
            variant="destructive-solid"
            onClick={confirmar}
            disabled={!senha || excluir.isPending}
            loading={excluir.isPending}
          >
            Excluir definitivamente
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
