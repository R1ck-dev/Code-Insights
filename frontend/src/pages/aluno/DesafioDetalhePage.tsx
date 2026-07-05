import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ExternalLink, FileCode2, Globe, Pencil, Plus, Trash2 } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { Breadcrumb } from '@/components/page/Breadcrumb'
import { QueryBoundary } from '@/components/page/states'
import { Card } from '@/components/ui/card'
import { Button, buttonClasses } from '@/components/ui/button'
import { Chip } from '@/components/ui/badge'
import { Input, Textarea } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'
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
import { toast } from '@/components/ui/toaster'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { AnalysisStatus, LanguageBadge, VisibilityBadge } from '@/components/domain/badges'
import { LINGUAGEM_META } from '@/domain/enums'
import { apiErrorMessage } from '@/lib/api'
import { formatDate } from '@/lib/utils'
import {
  useAlterarVisibilidadeDesafio,
  useAtualizarDesafio,
  useDesafioDetalhe,
  useRemoverDesafio,
} from '@/features/desafios/hooks'
import { useResolucoesDoDesafio } from '@/features/resolucoes/hooks'
import type { DesafioDetalheDTO, ResolucaoResumoDTO } from '@/types/api'

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
      <Breadcrumb items={[{ label: 'Desafios', to: '/app/desafios' }, { label: desafio.titulo }]} />

      {/* Cabeçalho */}
      <div className="flex flex-wrap items-start justify-between gap-5">
        <div className="flex min-w-0 flex-col gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-[25px] font-bold tracking-tight text-heading">{desafio.titulo}</h1>
            <VisibilityBadge visibilidade={desafio.visibilidade} />
          </div>
          <div className="flex flex-wrap items-center gap-2.5">
            {desafio.plataformaOrigem && <Chip>{desafio.plataformaOrigem}</Chip>}
            {desafio.identificadorExterno && <Chip mono>#{desafio.identificadorExterno}</Chip>}
            {desafio.urlExterna && (
              <a
                href={desafio.urlExterna}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 text-[11.5px] font-medium text-brand-strong hover:underline"
              >
                Abrir link
                <ExternalLink size={12} />
              </a>
            )}
            <span className="font-mono text-[11.5px] text-subtle">
              criado {formatDate(desafio.criadoEm)} · atualizado {formatDate(desafio.atualizadoEm)}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap gap-2.5">
          <Button variant="secondary" size="sm" onClick={() => setEditarAberto(true)}>
            <Pencil size={15} />
            Editar
          </Button>
          <Button variant="secondary" size="sm" onClick={() => setVisibilidadeAberta(true)}>
            <Globe size={15} />
            Visibilidade
          </Button>
          <Button variant="destructive" size="sm" onClick={() => setRemoverAberto(true)}>
            <Trash2 size={15} />
            Remover
          </Button>
        </div>
      </div>

      {/* Enunciado + Detalhes */}
      <div className="grid grid-cols-1 items-start gap-5 md:grid-cols-[1.5fr_1fr]">
        <Card className="flex flex-col gap-3 p-5">
          <span className="text-sm font-semibold text-heading">Enunciado</span>
          {desafio.enunciado ? (
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-fg">{desafio.enunciado}</p>
          ) : (
            <p className="text-sm text-muted">Sem enunciado.</p>
          )}
        </Card>

        <Card className="flex flex-col gap-3 p-[18px]">
          <span className="text-[13px] font-semibold text-muted">Detalhes</span>
          <DetalheLinha rotulo="Plataforma" valor={desafio.plataformaOrigem ?? '—'} />
          <DetalheLinha
            rotulo="Identificador"
            valor={desafio.identificadorExterno ? `#${desafio.identificadorExterno}` : '—'}
            mono
          />
          <DetalheLinha rotulo="Resoluções" valor={String(desafio.qtdResolucoes)} mono />
          <div className="flex items-center justify-between">
            <span className="text-[12.5px] text-muted">Visibilidade</span>
            <VisibilityBadge visibilidade={desafio.visibilidade} />
          </div>
        </Card>
      </div>

      {/* Resoluções */}
      <section className="flex flex-col gap-3.5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <h3 className="text-base font-semibold text-heading">Resoluções</h3>
            <span className="rounded-md border border-border bg-surface px-2 py-0.5 font-mono text-[12px] text-subtle tabular-nums">
              {desafio.qtdResolucoes}
            </span>
          </div>
          <Link to={`/app/desafios/${id}/submeter`} className={buttonClasses({ size: 'sm' })}>
            <Plus size={15} />
            Submeter resolução
          </Link>
        </div>

        <ListaResolucoes desafioId={id} />
      </section>

      {/* Diálogos */}
      {editarAberto && (
        <Dialog open={editarAberto} onOpenChange={setEditarAberto}>
          <DialogContent>
            <EditarDesafioForm desafio={desafio} onClose={() => setEditarAberto(false)} />
          </DialogContent>
        </Dialog>
      )}

      <ConfirmDialog
        open={visibilidadeAberta}
        onOpenChange={setVisibilidadeAberta}
        icon={Globe}
        title={isPublico ? 'Tornar privado?' : 'Tornar público?'}
        description={
          isPublico
            ? 'Este desafio deixará de ser visível para outras pessoas.'
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
        description="Esta ação não pode ser desfeita. As resoluções também serão removidas."
        confirmLabel="Remover"
        onConfirm={confirmarRemocao}
        loading={remover.isPending}
      />
    </>
  )
}

function DetalheLinha({ rotulo, valor, mono }: { rotulo: string; valor: string; mono?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-[12.5px] text-muted">{rotulo}</span>
      <span className={mono ? 'font-mono text-[12px] text-fg tabular-nums' : 'text-[12.5px] text-fg'}>
        {valor}
      </span>
    </div>
  )
}

function ListaResolucoes({ desafioId }: { desafioId: string }) {
  const [pagina, setPagina] = useState(0)
  const query = useResolucoesDoDesafio(desafioId, pagina)

  return (
    <QueryBoundary query={query}>
      {(lista) =>
        lista.itens.length === 0 ? (
          <EmptyState
            icon={FileCode2}
            title="Nenhuma resolução ainda"
            description="Submeta sua primeira resolução e acompanhe as métricas de complexidade e autonomia."
            action={
              <Link to={`/app/desafios/${desafioId}/submeter`} className={buttonClasses({ size: 'sm' })}>
                <Plus size={15} />
                Submeter resolução
              </Link>
            }
          />
        ) : (
          <div className="flex flex-col gap-4">
            <Card className="divide-y divide-border-subtle overflow-hidden">
              {lista.itens.map((r) => (
                <ResolucaoLinha key={r.id} resolucao={r} />
              ))}
            </Card>
            {lista.totalPaginas > 1 && (
              <Pagination page={lista.paginaAtual} totalPages={lista.totalPaginas} onChange={setPagina} />
            )}
          </div>
        )
      }
    </QueryBoundary>
  )
}

function ResolucaoLinha({ resolucao }: { resolucao: ResolucaoResumoDTO }) {
  return (
    <Link
      to={`/app/resolucoes/${resolucao.id}`}
      className="flex items-center gap-3.5 px-4 py-3 transition-colors hover:bg-surface-2"
    >
      <LanguageBadge linguagem={resolucao.linguagem} />
      <span className="min-w-0 flex-1 truncate text-[13.5px] text-fg">
        {LINGUAGEM_META[resolucao.linguagem].label}
      </span>
      <AutonomyMeter value={resolucao.indiceAutonomiaIA} size="sm" className="shrink-0" />
      <AnalysisStatus analisada={resolucao.analisada} className="shrink-0" />
      <span className="hidden shrink-0 sm:inline-flex">
        <VisibilityBadge visibilidade={resolucao.visibilidade} />
      </span>
      <span className="w-[66px] shrink-0 text-right font-mono text-[11.5px] text-subtle">
        {formatDate(resolucao.submetidaEm)}
      </span>
    </Link>
  )
}

function EditarDesafioForm({ desafio, onClose }: { desafio: DesafioDetalheDTO; onClose: () => void }) {
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
    <form onSubmit={onSubmit} className="flex flex-col">
      <DialogHeader>
        <DialogTitle>Editar desafio</DialogTitle>
      </DialogHeader>
      <DialogBody>
        <FormField label="Título" htmlFor="editar-titulo" required>
          <Input
            id="editar-titulo"
            value={titulo}
            onChange={(e) => setTitulo(e.target.value)}
            maxLength={255}
            required
          />
        </FormField>
        <FormField label="Enunciado" htmlFor="editar-enunciado">
          <Textarea
            id="editar-enunciado"
            value={enunciado}
            onChange={(e) => setEnunciado(e.target.value)}
            placeholder="Descreva o problema…"
          />
        </FormField>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label="Plataforma" htmlFor="editar-plataforma">
            <Input
              id="editar-plataforma"
              value={plataforma}
              onChange={(e) => setPlataforma(e.target.value)}
              maxLength={100}
              placeholder="LeetCode"
            />
          </FormField>
          <FormField label="Identificador" htmlFor="editar-identificador">
            <Input
              id="editar-identificador"
              value={identificador}
              onChange={(e) => setIdentificador(e.target.value)}
              maxLength={100}
              className="font-mono"
              placeholder="1"
            />
          </FormField>
        </div>
        <FormField label="URL externa" htmlFor="editar-url">
          <Input
            id="editar-url"
            type="url"
            value={urlExterna}
            onChange={(e) => setUrlExterna(e.target.value)}
            maxLength={500}
            className="font-mono text-[12.5px]"
            placeholder="https://…"
          />
        </FormField>
      </DialogBody>
      <DialogFooter>
        <Button type="button" variant="secondary" onClick={onClose} disabled={atualizar.isPending}>
          Cancelar
        </Button>
        <Button type="submit" loading={atualizar.isPending} disabled={!titulo.trim()}>
          Salvar
        </Button>
      </DialogFooter>
    </form>
  )
}
