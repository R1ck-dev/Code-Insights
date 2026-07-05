import { useState } from 'react'
import { Braces, Clock, Pencil, Plus, SearchX, Trash2 } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'
import { EmptyState } from '@/components/ui/empty-state'
import { Pagination } from '@/components/ui/pagination'
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
import { CATEGORIAS, CATEGORIA_META } from '@/domain/enums'
import {
  useAtualizarSnippet,
  useCriarSnippet,
  useMeusSnippets,
  useRemoverSnippet,
} from '@/features/snippets/hooks'
import { apiErrorMessage } from '@/lib/api'
import { cn, formatDate, pluralPt } from '@/lib/utils'
import type { CategoriaConceito, SnippetDTO } from '@/types/api'

export function SnippetsPage() {
  const [pagina, setPagina] = useState(0)
  const [cat, setCat] = useState<CategoriaConceito | 'TODAS'>('TODAS')
  // Filtro por categoria server-side: o backend recebe `categoria` e a paginação
  // reflete a categoria selecionada (sem "sumir" com itens de outras páginas).
  const query = useMeusSnippets(pagina, cat === 'TODAS' ? null : cat)

  const criar = useCriarSnippet()
  const remover = useRemoverSnippet()

  // Diálogo criar/editar. Quando editandoId é null, o formulário está em modo criação.
  const [formAberto, setFormAberto] = useState(false)
  const [editandoId, setEditandoId] = useState<string | null>(null)
  const atualizar = useAtualizarSnippet(editandoId ?? '')

  const [codigo, setCodigo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [categoria, setCategoria] = useState<CategoriaConceito | ''>('')
  const [tocado, setTocado] = useState(false)

  // Diálogo de detalhe e confirmação de remoção.
  const [detalhe, setDetalhe] = useState<SnippetDTO | null>(null)
  const [removendo, setRemovendo] = useState<SnippetDTO | null>(null)

  function selecionarCategoria(c: CategoriaConceito | 'TODAS') {
    setCat(c)
    setPagina(0)
  }

  function abrirCriar() {
    setEditandoId(null)
    setCodigo('')
    setDescricao('')
    setCategoria('')
    setTocado(false)
    setFormAberto(true)
  }

  function abrirEditar(snippet: SnippetDTO) {
    setDetalhe(null)
    setEditandoId(snippet.id)
    setCodigo(snippet.codigo)
    setDescricao(snippet.descricao ?? '')
    setCategoria(snippet.categoria)
    setTocado(false)
    setFormAberto(true)
  }

  const codigoInvalido = tocado && codigo.trim().length === 0
  const categoriaInvalida = tocado && !categoria
  const salvando = editandoId ? atualizar.isPending : criar.isPending

  async function handleSalvar(event: React.FormEvent) {
    event.preventDefault()
    setTocado(true)
    const codigoLimpo = codigo.trim()
    if (!codigoLimpo || !categoria) return

    const body = {
      codigo: codigoLimpo,
      descricao: descricao.trim() || null,
      categoria,
    }
    try {
      if (editandoId) {
        await atualizar.mutateAsync(body)
        toast.success('Snippet atualizado.')
      } else {
        await criar.mutateAsync(body)
        toast.success('Snippet criado.')
      }
      setFormAberto(false)
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível salvar o snippet.'))
    }
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
    <Button variant="primary" onClick={abrirCriar}>
      <Plus size={16} />
      Novo snippet
    </Button>
  )

  return (
    <PageContainer>
      <PageHeader
        title="Snippets"
        subtitle={
          query.data ? pluralPt(query.data.totalItens, 'trecho', 'trechos') : undefined
        }
        actions={botaoNovo}
      />

      <QueryBoundary query={query}>
        {(dados) => {
          // Sem nenhum snippet e sem filtro ativo: estado de onboarding (esconde os chips).
          if (dados.itens.length === 0 && cat === 'TODAS') {
            return (
              <EmptyState
                icon={Braces}
                title="Nenhum snippet ainda"
                description="Guarde trechos de código reutilizáveis, categorizados por conceito, para consultar depois."
                action={botaoNovo}
              />
            )
          }

          return (
            <div className="flex flex-col gap-5">
              <div className="flex flex-wrap gap-2">
                <CategoriaChip ativa={cat === 'TODAS'} onClick={() => selecionarCategoria('TODAS')}>
                  Todas
                </CategoriaChip>
                {CATEGORIAS.map((c) => {
                  const Icone = c.icon
                  return (
                    <CategoriaChip
                      key={c.value}
                      ativa={cat === c.value}
                      onClick={() => selecionarCategoria(c.value)}
                    >
                      <Icone size={13} />
                      {c.label}
                    </CategoriaChip>
                  )
                })}
              </div>

              {dados.itens.length === 0 ? (
                <EmptyState
                  icon={SearchX}
                  title="Nenhum snippet nesta categoria"
                  description="Você ainda não tem trechos da categoria selecionada."
                />
              ) : (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                  {dados.itens.map((snippet) => (
                    <SnippetCard
                      key={snippet.id}
                      snippet={snippet}
                      onOpen={() => setDetalhe(snippet)}
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

      {/* Diálogo criar / editar */}
      <Dialog open={formAberto} onOpenChange={setFormAberto}>
        <DialogContent showClose={false}>
          <form onSubmit={handleSalvar}>
            <DialogHeader>
              <DialogTitle>{editandoId ? 'Editar snippet' : 'Novo snippet'}</DialogTitle>
              <Braces size={18} className="text-subtle" />
            </DialogHeader>

            <DialogBody>
              <FormField
                label="Código"
                htmlFor="snippet-codigo"
                required
                error={codigoInvalido ? 'Informe o código do snippet.' : undefined}
              >
                <CodeEditor
                  id="snippet-codigo"
                  value={codigo}
                  onChange={setCodigo}
                  label="snippet.java"
                  minHeight={160}
                  placeholder="Cole aqui o trecho de código…"
                />
              </FormField>

              <FormField label="Descrição" htmlFor="snippet-descricao">
                <Input
                  id="snippet-descricao"
                  value={descricao}
                  onChange={(e) => setDescricao(e.target.value)}
                  placeholder="Ex.: MDC pelo algoritmo de Euclides"
                />
              </FormField>

              <FormField
                label="Categoria"
                required
                error={categoriaInvalida ? 'Selecione uma categoria.' : undefined}
              >
                <Select
                  value={categoria || undefined}
                  onValueChange={(v) => setCategoria(v as CategoriaConceito)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Selecione uma categoria" />
                  </SelectTrigger>
                  <SelectContent>
                    {CATEGORIAS.map((c) => {
                      const Icone = c.icon
                      return (
                        <SelectItem key={c.value} value={c.value}>
                          <span className="inline-flex items-center gap-2">
                            <Icone size={14} className="text-brand-strong" />
                            {c.label}
                          </span>
                        </SelectItem>
                      )
                    })}
                  </SelectContent>
                </Select>
              </FormField>
            </DialogBody>

            <DialogFooter>
              <Button type="button" variant="ghost" onClick={() => setFormAberto(false)}>
                Cancelar
              </Button>
              <Button type="submit" variant="primary" loading={salvando}>
                Salvar
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Diálogo de detalhe */}
      <Dialog open={!!detalhe} onOpenChange={(aberto) => !aberto && setDetalhe(null)}>
        <DialogContent aria-describedby={undefined}>
          {detalhe && (
            <div className="flex flex-col gap-4 p-5">
              <div className="flex items-start justify-between gap-3 pr-6">
                <div className="flex min-w-0 flex-col gap-2">
                  <CategoriaTag categoria={detalhe.categoria} />
                  {detalhe.descricao ? (
                    <DialogTitle className="text-[15px] font-semibold leading-snug text-heading">
                      {detalhe.descricao}
                    </DialogTitle>
                  ) : (
                    <DialogTitle className="sr-only">Detalhe do snippet</DialogTitle>
                  )}
                </div>
                <div className="flex shrink-0 gap-2">
                  <Button
                    variant="secondary"
                    size="icon"
                    aria-label="Editar snippet"
                    onClick={() => abrirEditar(detalhe)}
                  >
                    <Pencil size={15} />
                  </Button>
                  <Button
                    variant="destructive"
                    size="icon"
                    aria-label="Remover snippet"
                    onClick={() => setRemovendo(detalhe)}
                  >
                    <Trash2 size={15} />
                  </Button>
                </div>
              </div>

              <CodeBlock code={detalhe.codigo} lang="java" maxHeight={360} />

              <div className="flex items-center gap-2 border-t border-border-subtle pt-3">
                <Clock size={14} className="text-subtle" />
                <span className="font-mono text-[11.5px] tabular-nums text-subtle">
                  Criado em {formatDate(detalhe.criadoEm)}
                </span>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Confirmação de remoção */}
      <ConfirmDialog
        open={!!removendo}
        onOpenChange={(aberto) => !aberto && setRemovendo(null)}
        icon={Trash2}
        title="Remover snippet"
        description="Este trecho será removido permanentemente do seu portfólio. Essa ação não pode ser desfeita."
        confirmLabel="Remover"
        onConfirm={handleRemover}
        loading={remover.isPending}
        destructive
      />
    </PageContainer>
  )
}

function SnippetCard({
  snippet,
  onOpen,
}: {
  snippet: SnippetDTO
  onOpen: () => void
}) {
  return (
    <Card
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onOpen()
        }
      }}
      className="flex cursor-pointer flex-col gap-2.5 p-3 transition-colors hover:border-border-strong hover:bg-surface-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60"
    >
      {/* Preview sem botão "Copiar" para não aninhar interativo dentro do card clicável. */}
      <CodeBlock code={snippet.codigo} lang="java" lines={false} maxHeight={128} showCopy={false} />
      <div className="flex items-center gap-2 px-0.5">
        <CategoriaTag categoria={snippet.categoria} />
        {snippet.descricao && (
          <span className="min-w-0 truncate text-[12px] text-muted">{snippet.descricao}</span>
        )}
      </div>
    </Card>
  )
}

/** Pílula com ícone + rótulo da categoria do snippet. */
function CategoriaTag({ categoria }: { categoria: CategoriaConceito }) {
  const meta = CATEGORIA_META[categoria]
  const Icone = meta.icon
  return (
    <span className="inline-flex shrink-0 items-center gap-1.5 rounded-[7px] border border-border bg-surface-2 px-2 py-1 text-[11.5px] font-medium text-label">
      <Icone size={12} className="text-brand-strong" />
      {meta.label}
    </span>
  )
}

/** Chip de filtro por categoria (estado ativo em brand). */
function CategoriaChip({
  ativa,
  onClick,
  children,
}: {
  ativa: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-[12.5px] transition-colors',
        ativa
          ? 'bg-brand font-semibold text-brand-on'
          : 'border border-border bg-input font-medium text-muted hover:bg-surface-2 hover:text-fg',
      )}
    >
      {children}
    </button>
  )
}
