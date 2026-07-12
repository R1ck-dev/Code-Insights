/*
 * Tela O · Snippets (04 §7) — biblioteca de trechos do aluno.
 *
 * Filtro por categoria server-side (chips) · grade de `SnippetCard` · diálogo de
 * criação (`SnippetFormDialog`, que já traz o campo opcional "Desafio") · diálogo
 * de detalhe (CodeBlock + editar/remover) · edição (diálogo local, porque o
 * `SnippetFormDialog` compartilhado só cria) · ConfirmDialog de remoção.
 *
 * Não há métrica de complexidade nesta tela — snippet não é resolução.
 */
import { useEffect, useState } from 'react'
import { Braces, Maximize2, Pencil, Plus, SearchX, Target, Trash2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Button } from '@/components/ui/button'
import { Chip } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'
import { EmptyState } from '@/components/ui/empty-state'
import { Pagination } from '@/components/ui/pagination'
import { Skeleton } from '@/components/ui/skeleton'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { toast } from '@/components/ui/toaster'
import { CodeBlock } from '@/components/CodeBlock'
import { CodeEditor } from '@/components/CodeEditor'
import { SnippetCard } from '@/components/domain/SnippetCard'
import { SnippetFormDialog } from '@/components/snippets/SnippetFormDialog'
import { CATEGORIAS, CATEGORIA_META, SNIPPET_FALLBACK_ICON } from '@/domain/enums'
import { useMeusDesafios } from '@/features/desafios/hooks'
import { useAtualizarSnippet, useMeusSnippets, useRemoverSnippet } from '@/features/snippets/hooks'
import { apiErrorMessage } from '@/lib/api'
import { cn, formatDate, pluralPt } from '@/lib/utils'
import type { CategoriaConceito, SnippetDTO } from '@/types/api'

type FiltroCat = CategoriaConceito | 'TODAS'

export function SnippetsPage() {
  const [pagina, setPagina] = useState(0)
  const [cat, setCat] = useState<FiltroCat>('TODAS')
  // Filtro por categoria server-side: o backend recebe `categoria` e a paginação
  // reflete a categoria selecionada (sem "sumir" com itens de outras páginas).
  const query = useMeusSnippets(pagina, cat === 'TODAS' ? null : cat)

  // Títulos dos desafios do autor: o SnippetDTO só traz `desafioId`. Mesma query
  // (e mesmo cache) que o SnippetFormDialog usa para o Select de vínculo.
  const desafios = useMeusDesafios(0, 100)
  const tituloDoDesafio = (id: string | null) =>
    id ? (desafios.data?.itens.find((d) => d.id === id)?.titulo ?? null) : null

  const remover = useRemoverSnippet()

  const [criando, setCriando] = useState(false)
  const [editando, setEditando] = useState<SnippetDTO | null>(null)
  const [detalhe, setDetalhe] = useState<SnippetDTO | null>(null)
  const [removendo, setRemovendo] = useState<SnippetDTO | null>(null)

  function selecionarCategoria(c: FiltroCat) {
    setCat(c)
    setPagina(0)
  }

  function abrirEditar(snippet: SnippetDTO) {
    setDetalhe(null)
    setEditando(snippet)
  }

  async function handleRemover() {
    if (!removendo) return
    try {
      await remover.mutateAsync(removendo.id)
      toast.success('Snippet removido.')
      setRemovendo(null)
      setDetalhe(null)
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível remover o snippet.'))
    }
  }

  const botaoNovo = (
    <Button icon={Plus} onClick={() => setCriando(true)}>
      Novo snippet
    </Button>
  )

  const total = query.data?.totalItens
  const subtitulo =
    total == null
      ? undefined
      : cat === 'TODAS'
        ? pluralPt(total, 'trecho', 'trechos')
        : `${pluralPt(total, 'trecho', 'trechos')} em ${CATEGORIA_META[cat].label}`

  return (
    <PageContainer>
      <PageHeader title="Snippets" subtitle={subtitulo} actions={botaoNovo} />

      <QueryBoundary query={query} loading={<GradeEsqueleto />}>
        {(dados) => {
          // Sem nenhum snippet e sem filtro ativo: onboarding (os chips não fazem sentido).
          if (dados.itens.length === 0 && cat === 'TODAS') {
            return (
              <EmptyState
                icon={Braces}
                title="Nenhum snippet ainda."
                description="Guarde trechos de código reutilizáveis, categorizados por conceito, para consultar depois."
                action={botaoNovo}
              />
            )
          }

          return (
            <div className="flex flex-col gap-[18px]">
              <div className="flex flex-wrap gap-2">
                <FiltroCategoria
                  ativa={cat === 'TODAS'}
                  onClick={() => selecionarCategoria('TODAS')}
                >
                  Todas
                </FiltroCategoria>
                {CATEGORIAS.map((c) => (
                  <FiltroCategoria
                    key={c.value}
                    ativa={cat === c.value}
                    icon={c.icon}
                    onClick={() => selecionarCategoria(c.value)}
                  >
                    {c.label}
                  </FiltroCategoria>
                ))}
              </div>

              {dados.itens.length === 0 ? (
                <EmptyState
                  icon={SearchX}
                  title="Nenhum snippet nesta categoria."
                  description="Você ainda não guardou trechos da categoria selecionada."
                  action={
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => selecionarCategoria('TODAS')}
                    >
                      Ver todas
                    </Button>
                  }
                />
              ) : (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {dados.itens.map((snippet) => (
                    <SnippetCard
                      key={snippet.id}
                      snippet={snippet}
                      desafioTitulo={tituloDoDesafio(snippet.desafioId)}
                      desafioHref={
                        snippet.desafioId ? `/app/desafios/${snippet.desafioId}` : undefined
                      }
                      actions={
                        <AcoesDoSnippet
                          onAbrir={() => setDetalhe(snippet)}
                          onEditar={() => abrirEditar(snippet)}
                          onRemover={() => setRemovendo(snippet)}
                        />
                      }
                    />
                  ))}
                </div>
              )}

              {dados.totalPaginas > 1 && (
                <Pagination page={pagina} totalPages={dados.totalPaginas} onChange={setPagina} />
              )}
            </div>
          )
        }}
      </QueryBoundary>

      {/* Criar — o diálogo compartilhado já traz o campo opcional "Desafio". */}
      <SnippetFormDialog open={criando} onOpenChange={setCriando} />

      {/* Editar — diálogo local: o compartilhado só cria. */}
      {editando && (
        <EditarSnippetDialog
          key={editando.id}
          snippet={editando}
          onFechar={() => setEditando(null)}
        />
      )}

      <DetalheSnippetDialog
        snippet={detalhe}
        desafioTitulo={tituloDoDesafio(detalhe?.desafioId ?? null)}
        onFechar={() => setDetalhe(null)}
        onEditar={() => detalhe && abrirEditar(detalhe)}
        onRemover={() => detalhe && setRemovendo(detalhe)}
      />

      <ConfirmDialog
        open={!!removendo}
        onOpenChange={(aberto) => !aberto && setRemovendo(null)}
        icon={Trash2}
        title="Remover snippet?"
        description="Este trecho será removido permanentemente da sua biblioteca. Essa ação não pode ser desfeita."
        confirmLabel="Remover"
        onConfirm={handleRemover}
        loading={remover.isPending}
        destructive
      />
    </PageContainer>
  )
}

/* ---------------------------------------------------------------- filtro --- */

/**
 * Chip de filtro (04 §7.2) — é um **botão**, não o `Chip` de dado do sistema
 * (esse mora dentro do `SnippetCard`). Ativo: `ink`/`ink-on`. Inativo: `panel`
 * + hairline, com hover elevando a borda para `line-strong`.
 */
function FiltroCategoria({
  ativa,
  icon: Icone,
  onClick,
  children,
}: {
  ativa: boolean
  icon?: React.ComponentType<{ size?: number; strokeWidth?: number; className?: string }>
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={ativa}
      className={cn(
        'ci-foco-botao inline-flex cursor-pointer items-center gap-[7px] rounded-ci px-3 py-[6px]',
        'font-mono text-[12px] leading-none transition-colors',
        ativa
          ? 'border border-transparent bg-ink font-semibold text-ink-on'
          : 'border border-line bg-panel font-medium text-mid hover:border-line-strong hover:text-ink',
      )}
    >
      {Icone && <Icone size={13} strokeWidth={2} className="shrink-0" aria-hidden />}
      {children}
    </button>
  )
}

/* ----------------------------------------------------------------- ações --- */

function AcaoIcone({
  label,
  icon: Icone,
  onClick,
  destrutiva,
}: {
  label: string
  icon: typeof Pencil
  onClick: () => void
  destrutiva?: boolean
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant={destrutiva ? 'destructive' : 'secondary'}
          size="sm"
          iconOnly
          icon={Icone}
          aria-label={label}
          onClick={onClick}
        />
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  )
}

function AcoesDoSnippet({
  onAbrir,
  onEditar,
  onRemover,
}: {
  onAbrir: () => void
  onEditar: () => void
  onRemover: () => void
}) {
  return (
    <>
      <AcaoIcone label="Abrir snippet" icon={Maximize2} onClick={onAbrir} />
      <AcaoIcone label="Editar snippet" icon={Pencil} onClick={onEditar} />
      <AcaoIcone label="Remover snippet" icon={Trash2} onClick={onRemover} destrutiva />
    </>
  )
}

/* --------------------------------------------------------------- detalhe --- */

function DetalheSnippetDialog({
  snippet,
  desafioTitulo,
  onFechar,
  onEditar,
  onRemover,
}: {
  snippet: SnippetDTO | null
  desafioTitulo: string | null
  onFechar: () => void
  onEditar: () => void
  onRemover: () => void
}) {
  const meta = snippet ? CATEGORIA_META[snippet.categoria] : null
  const titulo = snippet?.descricao?.trim() || meta?.label || 'Snippet'

  return (
    <Dialog open={!!snippet} onOpenChange={(aberto) => !aberto && onFechar()}>
      <DialogContent width={560} aria-describedby={undefined}>
        {snippet && (
          <>
            <DialogHeader icon={meta?.icon ?? SNIPPET_FALLBACK_ICON}>
              <DialogTitle>{titulo}</DialogTitle>
              <div className="flex flex-wrap items-center gap-2">
                {meta && (
                  <Chip icon={meta.icon} className="px-2 py-[3px] text-[11px]">
                    {meta.label}
                  </Chip>
                )}
                {snippet.desafioId && desafioTitulo && (
                  <Link
                    to={`/app/desafios/${snippet.desafioId}`}
                    className="flex min-w-0 items-center gap-1.5 font-mono text-[11px] text-steel hover:text-steel-hover"
                  >
                    <Target size={12} strokeWidth={2} aria-hidden className="shrink-0" />
                    <span className="truncate">{desafioTitulo}</span>
                  </Link>
                )}
              </div>
            </DialogHeader>

            <DialogBody>
              <CodeBlock code={snippet.codigo} lang="java" label={titulo} maxHeight={360} />
              <time
                dateTime={snippet.criadoEm}
                className="tabular font-mono text-[11.5px] text-soft"
              >
                criado em {formatDate(snippet.criadoEm)}
              </time>
            </DialogBody>

            <DialogFooter>
              <Button variant="destructive" icon={Trash2} onClick={onRemover}>
                Remover
              </Button>
              <Button variant="secondary" icon={Pencil} onClick={onEditar}>
                Editar
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

/* ---------------------------------------------------------------- edição --- */

/**
 * Edição de snippet. O `SnippetFormDialog` compartilhado só cria, e o
 * `AtualizarSnippetRequest` do backend não aceita `desafioId` — por isso o
 * campo "Desafio" não aparece aqui (o vínculo se define na criação).
 */
function EditarSnippetDialog({
  snippet,
  onFechar,
}: {
  snippet: SnippetDTO
  onFechar: () => void
}) {
  const atualizar = useAtualizarSnippet(snippet.id)

  const [codigo, setCodigo] = useState(snippet.codigo)
  const [descricao, setDescricao] = useState(snippet.descricao ?? '')
  const [categoria, setCategoria] = useState<CategoriaConceito | ''>(snippet.categoria)
  const [tocado, setTocado] = useState(false)

  // O diálogo é remontado por `key={snippet.id}`; este efeito cobre a troca de
  // snippet sem desmontar (defensivo) e mantém o formulário fiel ao dado.
  useEffect(() => {
    setCodigo(snippet.codigo)
    setDescricao(snippet.descricao ?? '')
    setCategoria(snippet.categoria)
    setTocado(false)
  }, [snippet])

  const codigoInvalido = tocado && codigo.trim().length === 0
  const categoriaInvalida = tocado && !categoria

  async function handleSalvar(event: React.FormEvent) {
    event.preventDefault()
    setTocado(true)
    const codigoLimpo = codigo.trim()
    if (!codigoLimpo || !categoria) return

    try {
      await atualizar.mutateAsync({
        codigo: codigoLimpo,
        descricao: descricao.trim() || null,
        categoria,
      })
      toast.success('Snippet atualizado.')
      onFechar()
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível salvar o snippet.'))
    }
  }

  return (
    <Dialog open onOpenChange={(aberto) => !aberto && onFechar()}>
      <DialogContent width={560}>
        <form onSubmit={handleSalvar} className="flex min-h-0 flex-col">
          <DialogHeader icon={Braces}>
            <DialogTitle>Editar snippet</DialogTitle>
          </DialogHeader>

          <DialogBody>
            <FormField
              label="Código"
              htmlFor="snippet-editar-codigo"
              required
              error={codigoInvalido ? 'Informe o código do snippet.' : undefined}
            >
              <CodeEditor
                id="snippet-editar-codigo"
                value={codigo}
                onChange={setCodigo}
                label="snippet.java"
                lang="java"
                minHeight={160}
                placeholder="Cole aqui o trecho de código…"
              />
            </FormField>

            <Input
              label="Descrição"
              id="snippet-editar-descricao"
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              placeholder="Ex.: MDC pelo algoritmo de Euclides"
            />

            <FormField
              label="Categoria"
              required
              error={categoriaInvalida ? 'Selecione uma categoria.' : undefined}
            >
              <Select
                value={categoria || undefined}
                onValueChange={(v) => setCategoria(v as CategoriaConceito)}
              >
                <SelectTrigger variant="campo" valid={!!categoria}>
                  <SelectValue placeholder="Selecione uma categoria" />
                </SelectTrigger>
                <SelectContent>
                  {CATEGORIAS.map((c) => (
                    <SelectItem key={c.value} value={c.value}>
                      {c.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FormField>
          </DialogBody>

          <DialogFooter>
            <Button type="button" variant="secondary" onClick={onFechar}>
              Cancelar
            </Button>
            <Button type="submit" loading={atualizar.isPending}>
              Salvar snippet
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/* ------------------------------------------------------------ carregando --- */

/** 6 células-esqueleto com a moldura do card (04 §7.6). */
function GradeEsqueleto() {
  return (
    <div
      role="status"
      aria-busy
      aria-label="Carregando snippets"
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
    >
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} className="flex flex-col gap-2.5 rounded-ci border border-line bg-panel p-3.5">
          <Skeleton className="h-[128px] w-full" />
          <Skeleton className="h-[22px] w-[45%]" />
          <div className="border-t border-line-soft pt-2.5">
            <Skeleton className="h-[11px] w-[38%]" />
          </div>
        </div>
      ))}
    </div>
  )
}
