import { useState } from 'react'
import {
  Copy,
  FlaskConical,
  KeyRound,
  RefreshCw,
  Search,
  ShieldCheck,
  UserCheck,
  Users,
} from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
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
  useReanalisarMetricas,
  useUsuariosAdmin,
} from '@/features/admin/hooks'
import { apiErrorMessage } from '@/lib/api'
import { formatDate, formatDateTime, pluralPt } from '@/lib/utils'
import type {
  LinkRedefinicaoResponse,
  RelatorioReanaliseDTO,
  UsuarioAdminDTO,
} from '@/types/api'

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
 * A reanálise de métricas ficou de fora numa primeira versão, para não deixar uma reescrita em massa
 * de dado de pesquisa a um clique de distância. Ela voltou porque a alternativa era pior: sem tela,
 * a operação só acontecia colando JavaScript no console do navegador, e ensinar isso num sistema que
 * guarda dado de pesquisa é um risco maior do que o botão. O perigo mora no automatismo, não no
 * acesso — por isso ela mora numa seção separada, atrás de um diálogo que diz o que vai acontecer.
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

      <SecaoDeMetricas />

      <DialogDoLink link={linkGerado} onFechar={() => setLinkGerado(null)} />
    </PageContainer>
  )
}

/* -------------------------------------------------------------- métricas --- */

/**
 * Seção separada da tabela de contas de propósito: as ações de conta agem sobre uma pessoa e são
 * reversíveis; esta reescreve o corpus inteiro e não é. Misturá-las na mesma faixa de leitura
 * transformaria "reanalisar" em mais um botão de rotina.
 */
function SecaoDeMetricas() {
  const [confirmando, setConfirmando] = useState(false)
  const [relatorio, setRelatorio] = useState<RelatorioReanaliseDTO | null>(null)
  const reanalisar = useReanalisarMetricas()

  const executar = () =>
    reanalisar.mutate(undefined, {
      onSuccess: (resultado) => {
        setRelatorio(resultado)
        setConfirmando(false)
      },
      onError: (err) => {
        setConfirmando(false)
        toast.error(apiErrorMessage(err, 'Não foi possível reanalisar o corpus.'))
      },
    })

  return (
    <Card className="flex flex-col gap-[13px] p-[22px]">
      <div className="flex flex-col gap-[6px]">
        <h2 className="text-[15px] font-semibold text-ink">Métricas do corpus</h2>
        <p className="max-w-[62ch] text-[12.5px] leading-[1.6] text-body">
          Cada resolução guarda as métricas calculadas pelo motor em uso no dia da submissão. Quando o
          motor evolui, o que já está gravado não muda sozinho — reanalisar recalcula tudo com o motor
          atual, para que a plataforma não compare medições de versões diferentes.
        </p>
      </div>

      <div>
        <Button
          variant="secondary"
          icon={RefreshCw}
          loading={reanalisar.isPending}
          onClick={() => setConfirmando(true)}
        >
          Reanalisar o corpus
        </Button>
      </div>

      {relatorio && <ResultadoDaReanalise relatorio={relatorio} />}

      <ConfirmDialog
        open={confirmando}
        onOpenChange={setConfirmando}
        icon={RefreshCw}
        destructive
        title="Reanalisar o corpus inteiro?"
        description="As métricas de todas as resoluções da plataforma serão recalculadas e regravadas com o motor atual. Os valores antigos não ficam guardados, e não há como desfazer."
        confirmLabel="Reanalisar"
        loading={reanalisar.isPending}
        onConfirm={executar}
      />
    </Card>
  )
}

/**
 * `comMudanca` é o número que importa: reprocessar tudo e não mudar nada é o resultado esperado
 * quando o motor não mudou desde a última passada, e sem dizer isso o relatório parece um fracasso.
 * As falhas aparecem sempre que existirem — resolução que o analisador não conseguiu ler continua
 * com a métrica antiga, e isso precisa ser visível.
 */
function ResultadoDaReanalise({ relatorio }: { relatorio: RelatorioReanaliseDTO }) {
  return (
    <div className="flex flex-col gap-[6px] rounded-ci border border-line bg-recess p-[13px]">
      <p className="font-mono text-[11.5px] text-ink">
        {pluralPt(relatorio.total, 'resolução percorrida', 'resoluções percorridas')} ·{' '}
        {relatorio.reprocessadas} reprocessada(s) · {relatorio.comMudanca} com mudança
        {relatorio.puladas > 0 && ` · ${relatorio.puladas} pulada(s)`}
      </p>

      <p className="text-[12px] leading-[1.55] text-soft">
        {relatorio.comMudanca === 0
          ? 'Nenhuma classificação mudou: o motor atual concorda com o que já estava gravado.'
          : 'As telas de métrica já refletem os novos valores.'}
      </p>

      {relatorio.falhas > 0 && (
        <p className="text-[12px] leading-[1.55] text-erro-texto">
          {relatorio.falhas} resolução(ões) falharam na análise e continuam com a métrica anterior.
        </p>
      )}
    </div>
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
