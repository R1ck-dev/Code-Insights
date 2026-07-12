/*
 * M · Detalhe do desafio (04 §5) — sistema ÓRBITA.
 *
 * Cabeçalho com ações (Editar · Visibilidade · Remover) · Enunciado + Detalhes ·
 * lista paginada de resoluções (<ResolucaoLinha>, com o estado "calculando") ·
 * snippets do desafio · diálogo de edição + 2 confirmações.
 *
 * Decisões que a tela carrega:
 *  - Editar o DESAFIO existe (o enunciado não é dado de medição). Editar RESOLUÇÃO não
 *    existe (00-INDICE §4.1): a linha de resolução leva ao detalhe, e a evolução se faz
 *    submetendo uma NOVA resolução ao mesmo desafio.
 *  - Métrica só existe para Java (§4.4). O slot de métrica da linha resolve os 3 estados
 *    (Big-O · calculando · sem métrica) — a incerteza (≈ ESTIMADO) nunca é escondida.
 *  - `ResolucaoResumoDTO` não traz complexidade; o `k` do colormap vem da carta celeste
 *    (`useCartaCeleste`, já em cache pelo dashboard), casada por `resolucaoId`. Sem ela,
 *    a linha degrada para o estado honesto "sem métrica" — nunca um valor inventado.
 */
import { useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ExternalLink, FileCode2, Globe, Lock, Pencil, Plus, Trash2 } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { Breadcrumb } from '@/components/page/Breadcrumb'
import { QueryBoundary } from '@/components/page/states'
import { Card } from '@/components/ui/card'
import { Button, buttonClasses } from '@/components/ui/button'
import { Chip } from '@/components/ui/badge'
import { Input, Textarea } from '@/components/ui/input'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import { EmptyState } from '@/components/ui/empty-state'
import { Pagination } from '@/components/ui/pagination'
import { Skeleton } from '@/components/ui/skeleton'
import { toast } from '@/components/ui/toaster'
import { StatusChip } from '@/components/domain/badges'
import { ResolucaoLinha } from '@/components/domain/ResolucaoLinha'
import { SnippetsDoDesafioSection } from '@/components/snippets/SnippetsDoDesafioSection'
import { apiErrorMessage } from '@/lib/api'
import { formatDayMonth, pluralPt } from '@/lib/utils'
import {
  useAlterarVisibilidadeDesafio,
  useAtualizarDesafio,
  useDesafioDetalhe,
  useRemoverDesafio,
} from '@/features/desafios/hooks'
import { useResolucoesDoDesafio } from '@/features/resolucoes/hooks'
import { useCartaCeleste } from '@/features/metricas/hooks'
import type { DesafioDetalheDTO, PontoCartaDTO } from '@/types/api'

export function DesafioDetalhePage() {
  const { desafioId } = useParams<{ desafioId: string }>()
  const query = useDesafioDetalhe(desafioId)

  return (
    <PageContainer>
      <QueryBoundary query={query}>
        {(desafio) => <DesafioDetalheConteudo desafio={desafio} />}
      </QueryBoundary>
    </PageContainer>
  )
}

function DesafioDetalheConteudo({ desafio }: { desafio: DesafioDetalheDTO }) {
  const navigate = useNavigate()
  const id = desafio.id
  const isPublico = desafio.visibilidade === 'PUBLICO'

  const [editarAberto, setEditarAberto] = useState(false)
  const [visibilidadeAberta, setVisibilidadeAberta] = useState(false)
  const [removerAberto, setRemoverAberto] = useState(false)

  const alterarVisibilidade = useAlterarVisibilidadeDesafio(id)
  const remover = useRemoverDesafio()

  async function confirmarVisibilidade() {
    try {
      await alterarVisibilidade.mutateAsync(!isPublico)
      toast.success(isPublico ? 'Desafio tornado privado.' : 'Desafio tornado público.')
      setVisibilidadeAberta(false)
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível alterar a visibilidade.'))
    }
  }

  async function confirmarRemocao() {
    try {
      await remover.mutateAsync(id)
      toast.success('Desafio removido.')
      navigate('/app/desafios')
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível remover o desafio.'))
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'desafios', to: '/app/desafios' }, { label: desafio.titulo }]} />

      {/* ---------------------------------------------------------- cabeçalho */}
      <header className="flex flex-wrap items-start justify-between gap-5">
        <div className="flex min-w-0 flex-col gap-[11px]">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-[25px] font-semibold leading-tight tracking-[-.02em] text-ink">
              {desafio.titulo}
            </h1>
            <StatusChip status={isPublico ? 'publico' : 'privado'} />
          </div>

          <div className="flex flex-wrap items-center gap-[9px]">
            {desafio.plataformaOrigem && (
              <Chip className="py-[3px] text-[11px]">{desafio.plataformaOrigem}</Chip>
            )}
            {desafio.identificadorExterno && (
              <Chip className="tabular py-[3px] text-[11px]">#{desafio.identificadorExterno}</Chip>
            )}
            {desafio.urlExterna && (
              <a
                href={desafio.urlExterna}
                target="_blank"
                rel="noreferrer"
                className="ci-foco-botao inline-flex items-center gap-1.5 rounded-ci font-mono text-[11px] text-steel transition-colors hover:text-steel-hover"
              >
                abrir link
                <ExternalLink size={12} strokeWidth={2} aria-hidden />
                <span className="sr-only">(abre em nova aba)</span>
              </a>
            )}
            <span className="tabular font-mono text-[11px] text-soft">
              criado {formatDayMonth(desafio.criadoEm)} · atualizado{' '}
              {formatDayMonth(desafio.atualizadoEm)}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap gap-[9px]">
          <Button variant="secondary" size="sm" icon={Pencil} onClick={() => setEditarAberto(true)}>
            Editar
          </Button>
          <Button
            variant="secondary"
            size="sm"
            icon={isPublico ? Lock : Globe}
            onClick={() => setVisibilidadeAberta(true)}
          >
            Visibilidade
          </Button>
          <Button
            variant="destructive"
            size="sm"
            icon={Trash2}
            onClick={() => setRemoverAberto(true)}
          >
            Remover
          </Button>
        </div>
      </header>

      {/* ------------------------------------------------- enunciado + detalhes */}
      <div className="grid grid-cols-1 items-start gap-[18px] lg:grid-cols-[1.5fr_1fr]">
        <Card className="flex flex-col gap-[13px] p-5">
          <h2 className="text-[14px] font-semibold text-ink">Enunciado</h2>
          {desafio.enunciado ? (
            <p className="whitespace-pre-wrap text-[14px] leading-[1.65] text-body">
              {desafio.enunciado}
            </p>
          ) : (
            <p className="text-[13px] leading-[1.55] text-soft">
              Sem enunciado. Você pode adicionar um em <span className="text-mid">Editar</span>.
            </p>
          )}
        </Card>

        <Card className="flex flex-col gap-3 p-[18px]">
          <h2 className="font-mono text-[11px] uppercase tracking-[.08em] text-mid">Detalhes</h2>

          <DetalheLinha rotulo="plataforma">
            {desafio.plataformaOrigem ? (
              <span className="text-[12.5px] text-ink">{desafio.plataformaOrigem}</span>
            ) : (
              <Vazio />
            )}
          </DetalheLinha>

          <DetalheLinha rotulo="identificador">
            {desafio.identificadorExterno ? (
              <span className="tabular font-mono text-[12px] text-ink">
                #{desafio.identificadorExterno}
              </span>
            ) : (
              <Vazio />
            )}
          </DetalheLinha>

          <DetalheLinha rotulo="resoluções">
            <span className="tabular font-mono text-[12px] text-ink">{desafio.qtdResolucoes}</span>
          </DetalheLinha>

          <DetalheLinha rotulo="visibilidade">
            <StatusChip status={isPublico ? 'publico' : 'privado'} />
          </DetalheLinha>
        </Card>
      </div>

      {/* ------------------------------------------------------------ resoluções */}
      <section className="flex flex-col gap-[13px]">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <h2 className="text-[16px] font-semibold text-ink">Resoluções</h2>
            <span className="tabular rounded-ci border border-line bg-panel px-2 py-0.5 font-mono text-[12px] text-soft">
              {desafio.qtdResolucoes}
            </span>
          </div>
          <Link
            to={`/app/desafios/${id}/submeter`}
            className={buttonClasses({ size: 'sm' })}
          >
            <Plus size={14} strokeWidth={2} aria-hidden />
            Submeter resolução
          </Link>
        </div>

        <ListaResolucoes desafioId={id} />
      </section>

      {/* --------------------------------------------- snippets deste desafio */}
      <SnippetsDoDesafioSection desafioId={id} />

      {/* -------------------------------------------------------------- diálogos */}
      {editarAberto && (
        <Dialog open={editarAberto} onOpenChange={setEditarAberto}>
          <DialogContent width={560}>
            <EditarDesafioForm desafio={desafio} onClose={() => setEditarAberto(false)} />
          </DialogContent>
        </Dialog>
      )}

      <ConfirmDialog
        open={visibilidadeAberta}
        onOpenChange={setVisibilidadeAberta}
        icon={isPublico ? Lock : Globe}
        title={isPublico ? 'Tornar privado?' : 'Tornar público?'}
        description={
          isPublico
            ? 'Este desafio deixará de aparecer no seu portfólio público.'
            : 'Qualquer pessoa poderá ver este desafio e suas resoluções públicas.'
        }
        confirmLabel={isPublico ? 'Tornar privado' : 'Tornar público'}
        onConfirm={confirmarVisibilidade}
        loading={alterarVisibilidade.isPending}
      />

      <ConfirmDialog
        open={removerAberto}
        onOpenChange={setRemoverAberto}
        icon={Trash2}
        destructive
        title="Remover desafio?"
        description="Esta ação não pode ser desfeita. As resoluções deste desafio também serão removidas."
        confirmLabel="Remover"
        onConfirm={confirmarRemocao}
        loading={remover.isPending}
      />
    </>
  )
}

/** Linha chave→valor do card Detalhes: chave mono `soft`, valor à direita. */
function DetalheLinha({ rotulo, children }: { rotulo: string; children: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="font-mono text-[12px] text-soft">{rotulo}</span>
      {children}
    </div>
  )
}

/** Ausência de dado — nunca um zero fingindo ser dado. */
function Vazio() {
  return <span className="font-mono text-[12px] text-soft">—</span>
}

// --------------------------------------------------------------- resoluções

function ListaResolucoes({ desafioId }: { desafioId: string }) {
  const [pagina, setPagina] = useState(0)
  const query = useResolucoesDoDesafio(desafioId, pagina)

  /*
   * A carta celeste é a única fonte de `tempoOrdem`/`confiança` por resolução (o
   * `ResolucaoResumoDTO` não os traz). Consulta em cache — o dashboard já a usa.
   *
   * ⚠ ENQUANTO ELA NÃO CHEGA (ou se falhar), a linha NÃO pode dizer "sem métrica": esse rótulo
   * é reservado à linguagem sem analisador (§4.4). "Ainda não carreguei" vira esqueleto;
   * "não consegui carregar" vira `—`. Afirmar ausência de métrica onde há métrica é pior do
   * que não mostrar nada.
   */
  const carta = useCartaCeleste()
  const metricas = useMemo(() => {
    const mapa = new Map<string, PontoCartaDTO>()
    for (const ponto of carta.data ?? []) mapa.set(ponto.resolucaoId, ponto)
    return mapa
  }, [carta.data])

  return (
    <QueryBoundary query={query} loading={<ListaEsqueleto />}>
      {(lista) =>
        lista.itens.length === 0 ? (
          <EmptyState
            icon={FileCode2}
            title="Nenhuma resolução ainda."
            description="Submeta a primeira resolução: a análise estática mede a complexidade e a autonomia autodeclarada entra na sua carta."
            action={
              <Link
                to={`/app/desafios/${desafioId}/submeter`}
                className={buttonClasses({ size: 'sm' })}
              >
                <Plus size={14} strokeWidth={2} aria-hidden />
                Submeter resolução
              </Link>
            }
          />
        ) : (
          <div className="flex flex-col gap-4">
            <Card className="overflow-hidden">
              <h3 className="sr-only">
                {pluralPt(lista.totalItens, 'resolução', 'resoluções')} deste desafio
              </h3>
              {lista.itens.map((r) => {
                const ponto = metricas.get(r.id)
                return (
                  <ResolucaoLinha
                    key={r.id}
                    to={`/app/resolucoes/${r.id}`}
                    linguagem={r.linguagem}
                    autonomia={r.indiceAutonomiaIA}
                    analisada={r.analisada}
                    // `undefined` = não sei (carta com erro) · `null` = a carta respondeu e
                    // esta resolução não tem classe de tempo. São coisas diferentes.
                    tempoOrdem={carta.isSuccess ? (ponto?.tempoOrdem ?? null) : undefined}
                    confiancaTempo={ponto?.confiancaTempo}
                    carregandoMetrica={carta.isPending}
                    submetidaEm={r.submetidaEm}
                  />
                )
              })}
            </Card>

            <Pagination
              page={lista.paginaAtual}
              totalPages={lista.totalPaginas}
              onChange={setPagina}
            />
          </div>
        )
      }
    </QueryBoundary>
  )
}

/** Esqueleto da lista: mesma moldura, 3 linhas pulsando (04 §5.6). */
function ListaEsqueleto() {
  return (
    <Card className="overflow-hidden" role="status" aria-busy aria-label="Carregando resoluções">
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          className="flex items-center gap-3.5 border-t border-line-soft px-4 py-[13px] first:border-t-0"
        >
          <Skeleton className="h-[26px] w-[74px]" />
          <Skeleton className="h-[13px] flex-1" />
          <Skeleton className="h-[13px] w-[90px]" />
          <Skeleton className="h-[13px] w-[56px]" />
        </div>
      ))}
    </Card>
  )
}

// ------------------------------------------------------------------ diálogo

function EditarDesafioForm({
  desafio,
  onClose,
}: {
  desafio: DesafioDetalheDTO
  onClose: () => void
}) {
  const atualizar = useAtualizarDesafio(desafio.id)
  const [titulo, setTitulo] = useState(desafio.titulo)
  const [enunciado, setEnunciado] = useState(desafio.enunciado ?? '')
  const [plataforma, setPlataforma] = useState(desafio.plataformaOrigem ?? '')
  const [identificador, setIdentificador] = useState(desafio.identificadorExterno ?? '')
  const [urlExterna, setUrlExterna] = useState(desafio.urlExterna ?? '')

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!titulo.trim()) return
    try {
      await atualizar.mutateAsync({
        titulo: titulo.trim(),
        enunciado: enunciado.trim() || null,
        plataformaOrigem: plataforma.trim() || null,
        identificadorExterno: identificador.trim() || null,
        urlExterna: urlExterna.trim() || null,
      })
      toast.success('Desafio atualizado.')
      onClose()
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível salvar as alterações.'))
    }
  }

  return (
    <form onSubmit={onSubmit} className="flex min-h-0 flex-col">
      <DialogHeader icon={Pencil}>
        <DialogTitle>Editar desafio</DialogTitle>
      </DialogHeader>

      <DialogBody>
        <Input
          id="editar-titulo"
          label="Título"
          required
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          maxLength={255}
        />

        <Textarea
          id="editar-enunciado"
          label="Enunciado"
          minHeight={76}
          value={enunciado}
          onChange={(e) => setEnunciado(e.target.value)}
          placeholder="Dado um array e um alvo, retorne os índices…"
        />

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Input
            id="editar-plataforma"
            label="Plataforma"
            value={plataforma}
            onChange={(e) => setPlataforma(e.target.value)}
            maxLength={100}
            placeholder="LeetCode"
          />
          <Input
            id="editar-identificador"
            label="Identificador"
            mono
            value={identificador}
            onChange={(e) => setIdentificador(e.target.value)}
            maxLength={100}
            placeholder="1"
          />
        </div>

        <Input
          id="editar-url"
          label="URL externa"
          type="url"
          mono
          value={urlExterna}
          onChange={(e) => setUrlExterna(e.target.value)}
          maxLength={500}
          placeholder="https://…"
        />

        <div className="flex items-start gap-[9px] rounded-ci border border-line bg-recess p-3">
          <FileCode2 size={14} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-steel" />
          <p className="text-[12px] leading-[1.45] text-mid">
            O enunciado não é dado de medição — pode ser corrigido à vontade. As{' '}
            <span className="font-semibold text-ink">resoluções</span> já submetidas continuam
            imutáveis.
          </p>
        </div>
      </DialogBody>

      <DialogFooter>
        <Button type="button" variant="ghost" onClick={onClose} disabled={atualizar.isPending}>
          Cancelar
        </Button>
        <Button type="submit" loading={atualizar.isPending} disabled={!titulo.trim()}>
          Salvar
        </Button>
      </DialogFooter>
    </form>
  )
}
