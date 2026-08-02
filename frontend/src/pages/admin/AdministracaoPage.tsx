import { useState } from 'react'
import { Copy, FlaskConical, KeyRound, Search, ShieldCheck, UserCheck, Users } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { EmptyState } from '@/components/ui/empty-state'
import { Input } from '@/components/ui/input'
import { Pagination } from '@/components/ui/pagination'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeaderCell,
  TableRow,
} from '@/components/ui/table'
import { toast } from '@/components/ui/toaster'
import { ROLE_LABEL, STATUS_CONTA_META } from '@/domain/enums'
import {
  useAtivarConta,
  useGerarLinkDeRedefinicao,
  usePromoverParaPesquisador,
  useUsuariosAdmin,
} from '@/features/admin/hooks'
import { apiErrorMessage } from '@/lib/api'
import { formatDate, formatDateTime, pluralPt } from '@/lib/utils'
import type { LinkRedefinicaoResponse, UsuarioAdminDTO } from '@/types/api'

/*
 * A · Administração de contas — ÓRBITA.
 *
 * As três ações já existiam na API e só eram alcançáveis pelo Swagger, o que exigia saber o e-mail
 * da pessoa de cor. A tela não acrescenta poder nenhum: ela troca "digite o e-mail certo" por
 * "encontre a pessoa na lista", que é o atrito real quando um participante está travado esperando.
 *
 * As ações aparecem condicionadas ao estado da linha — ativar só para quem está pendente, promover
 * só para quem ainda não é pesquisador. Mostrar botão que a API vai recusar ensina a ignorar erro.
 *
 * O que NÃO está aqui: reanalisar métricas. É reescrita em massa de dado de pesquisa, e um botão
 * numa tela de rotina a torna fácil demais de apertar sem querer. Continua em POST
 * /api/admin/metricas/reanalisar, onde o esforço de chegar é proporcional ao estrago possível.
 */

export function AdministracaoPage() {
  const [busca, setBusca] = useState('')
  const [pagina, setPagina] = useState(0)
  const [linkGerado, setLinkGerado] = useState<LinkRedefinicaoResponse | null>(null)

  const query = useUsuariosAdmin(busca, pagina)

  const buscar = (termo: string) => {
    setBusca(termo)
    // Voltar à primeira página: sem isto, buscar estando na página 3 devolve vazio e parece que a
    // pessoa procurada não existe.
    setPagina(0)
  }

  return (
    <PageContainer className="gap-[18px]">
      <PageHeader
        title="Administração"
        subtitle="Contas da plataforma: papel, status e as ações manuais para quando o e-mail falha."
      />

      <div className="flex flex-wrap items-center gap-[9px]">
        <Input
          id="busca-usuarios"
          icon={Search}
          placeholder="Nome de usuário ou e-mail"
          aria-label="Buscar por nome de usuário ou e-mail"
          value={busca}
          onChange={(e) => buscar(e.target.value)}
          className="w-[280px]"
        />
      </div>

      <QueryBoundary query={query}>
        {(pagina_) =>
          pagina_.itens.length === 0 ? (
            <EmptyState
              icon={Users}
              title={busca ? 'Ninguém corresponde a essa busca.' : 'Nenhuma conta cadastrada.'}
              description={
                busca
                  ? 'A busca cobre nome de usuário e e-mail, em qualquer parte do texto.'
                  : 'As contas aparecem aqui assim que alguém se cadastrar.'
              }
            />
          ) : (
            <div className="flex flex-col gap-[15px]">
              <p className="font-mono text-[11.5px] text-soft">
                {pluralPt(pagina_.totalItens, 'conta', 'contas')}
              </p>

              <TabelaDeUsuarios usuarios={pagina_.itens} onLinkGerado={setLinkGerado} />

              <Pagination
                page={pagina_.paginaAtual}
                totalPages={pagina_.totalPaginas}
                onChange={setPagina}
              />
            </div>
          )
        }
      </QueryBoundary>

      <DialogDoLink link={linkGerado} onFechar={() => setLinkGerado(null)} />
    </PageContainer>
  )
}

/* --------------------------------------------------------------- tabela --- */

function TabelaDeUsuarios({
  usuarios,
  onLinkGerado,
}: {
  usuarios: UsuarioAdminDTO[]
  onLinkGerado: (link: LinkRedefinicaoResponse) => void
}) {
  return (
    <Table aria-label="Contas da plataforma">
      <TableHead>
        <TableRow>
          <TableHeaderCell>Usuário</TableHeaderCell>
          <TableHeaderCell>E-mail</TableHeaderCell>
          <TableHeaderCell>Papel</TableHeaderCell>
          <TableHeaderCell>Status</TableHeaderCell>
          <TableHeaderCell>Cadastro</TableHeaderCell>
          <TableHeaderCell>Ações</TableHeaderCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {usuarios.map((usuario) => (
          <TableRow key={usuario.id}>
            <TableCell className="font-mono text-ink">{usuario.username}</TableCell>
            <TableCell className="text-mid">{usuario.email}</TableCell>
            <TableCell>
              <Badge tom={usuario.role === 'ALUNO' ? 'neutro' : 'info'}>
                {ROLE_LABEL[usuario.role]}
              </Badge>
            </TableCell>
            <TableCell>
              <Badge tom={STATUS_CONTA_META[usuario.status].tone} dot>
                {STATUS_CONTA_META[usuario.status].label}
              </Badge>
            </TableCell>
            <TableCell className="text-soft">{formatDate(usuario.criadoEm)}</TableCell>
            <TableCell>
              <Acoes usuario={usuario} onLinkGerado={onLinkGerado} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

/* ---------------------------------------------------------------- ações --- */

function Acoes({
  usuario,
  onLinkGerado,
}: {
  usuario: UsuarioAdminDTO
  onLinkGerado: (link: LinkRedefinicaoResponse) => void
}) {
  const promover = usePromoverParaPesquisador()
  const ativar = useAtivarConta()
  const gerarLink = useGerarLinkDeRedefinicao()

  const erro = (padrao: string) => (err: unknown) => toast.error(apiErrorMessage(err, padrao))

  return (
    <div className="flex flex-wrap gap-[7px]">
      {/* ADMIN não aparece: promovê-lo seria rebaixamento sem volta, e o domínio recusa. Esconder
          o botão evita oferecer uma ação que só pode terminar em erro. */}
      {usuario.role === 'ALUNO' && (
        <Button
          size="sm"
          variant="secondary"
          icon={FlaskConical}
          loading={promover.isPending}
          onClick={() =>
            promover.mutate(usuario.email, {
              onSuccess: (papel) =>
                toast.success(
                  `${papel.username} agora é pesquisador. Ele precisa sair e entrar de novo — o papel viaja dentro do token.`,
                ),
              onError: erro('Não foi possível promover.'),
            })
          }
        >
          Tornar pesquisador
        </Button>
      )}

      {usuario.role === 'PESQUISADOR' && (
        <span className="flex items-center gap-[5px] font-mono text-[11px] text-soft">
          <ShieldCheck size={13} strokeWidth={2} aria-hidden />
          acesso à pesquisa
        </span>
      )}

      {usuario.status === 'PENDENTE_VERIFICACAO' && (
        <Button
          size="sm"
          variant="secondary"
          icon={UserCheck}
          loading={ativar.isPending}
          onClick={() =>
            ativar.mutate(usuario.email, {
              onSuccess: (conta) => toast.success(`Conta de ${conta.username} ativada.`),
              onError: erro('Não foi possível ativar a conta.'),
            })
          }
        >
          Ativar conta
        </Button>
      )}

      <Button
        size="sm"
        variant="ghost"
        icon={KeyRound}
        loading={gerarLink.isPending}
        onClick={() =>
          gerarLink.mutate(usuario.email, {
            onSuccess: onLinkGerado,
            onError: erro('Não foi possível gerar o link.'),
          })
        }
      >
        Link de senha
      </Button>
    </div>
  )
}

/* ----------------------------------------------------------- link gerado --- */

/**
 * O link aparece num diálogo, e não num toast: é um token de uso único que precisa ser copiado
 * inteiro, e toast some sozinho. Perder o link significa gerar outro e invalidar o primeiro.
 */
function DialogDoLink({
  link,
  onFechar,
}: {
  link: LinkRedefinicaoResponse | null
  onFechar: () => void
}) {
  const copiar = async () => {
    if (!link) return
    try {
      await navigator.clipboard.writeText(link.link)
      toast.success('Link copiado.')
    } catch {
      // clipboard exige contexto seguro (https ou localhost) e permissão; falhar em silêncio faria
      // o admin achar que copiou.
      toast.error('O navegador não permitiu copiar. Selecione o texto e copie à mão.')
    }
  }

  return (
    <Dialog open={link !== null} onOpenChange={(aberto) => !aberto && onFechar()}>
      <DialogContent width={520}>
        <DialogHeader icon={KeyRound}>
          <DialogTitle>Link de redefinição de senha</DialogTitle>
        </DialogHeader>
        <DialogBody>
          {link && (
            <div className="flex flex-col gap-[13px]">
              <p className="text-[12.5px] leading-[1.6] text-body">
                Entregue este link a <strong className="font-semibold text-ink">{link.username}</strong>{' '}
                por outro canal. Ele vale até {formatDateTime(link.expiraEm)} e serve uma vez só.
              </p>

              <p className="rounded-ci border border-line bg-recess p-[11px] font-mono text-[11.5px] leading-[1.5] break-all text-ink">
                {link.link}
              </p>

              <Button variant="secondary" size="sm" icon={Copy} onClick={copiar}>
                Copiar link
              </Button>
            </div>
          )}
        </DialogBody>
      </DialogContent>
    </Dialog>
  )
}
