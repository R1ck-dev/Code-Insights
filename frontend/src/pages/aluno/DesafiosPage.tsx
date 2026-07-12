/*
 * L · Meus desafios (04 §4) — ÓRBITA.
 *
 * Cabeçalho + barra de filtros + grade de <DesafioCard> + paginação + diálogo "Novo desafio".
 * Nenhuma métrica aparece aqui (o DTO de lista não traz nenhuma) — logo, nenhuma cor:
 * o colormap não tem o que dizer nesta tela.
 *
 * ⚠ Filtros e ordenação são LOCAIS À PÁGINA CARREGADA: `GET /api/desafios` só aceita
 * `pagina`/`tamanho` (features/desafios/api.ts). Refinam o que está à vista; a paginação
 * continua navegando o conjunto do backend.
 */
import { useMemo, useState } from 'react'
import { Filter, Lock, Plus, Target } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Button } from '@/components/ui/button'
import { Input, Textarea } from '@/components/ui/input'
import { EmptyState } from '@/components/ui/empty-state'
import { Pagination } from '@/components/ui/pagination'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { toast } from '@/components/ui/toaster'
import { DesafioCard } from '@/components/domain/DesafioCard'
import { useCriarDesafio, useMeusDesafios } from '@/features/desafios/hooks'
import { apiErrorMessage } from '@/lib/api'
import { pluralPt } from '@/lib/utils'
import type { DesafioResumoDTO, Visibilidade } from '@/types/api'

/** Sentinela do "sem filtro" — o Radix Select não aceita `value=""`. */
const TODAS = 'TODAS'

type FiltroVisibilidade = typeof TODAS | Visibilidade
type Ordenacao = 'recentes' | 'antigos' | 'titulo'

const ORDENACOES: { value: Ordenacao; label: string }[] = [
  { value: 'recentes', label: 'Mais recentes' },
  { value: 'antigos', label: 'Mais antigos' },
  { value: 'titulo', label: 'Título (A–Z)' },
]

const VISIBILIDADES: { value: FiltroVisibilidade; label: string }[] = [
  { value: TODAS, label: 'Todas' },
  { value: 'PUBLICO', label: 'Público' },
  { value: 'PRIVADO', label: 'Privado' },
]

export function DesafiosPage() {
  const [pagina, setPagina] = useState(0)
  const query = useMeusDesafios(pagina)

  const [plataforma, setPlataforma] = useState<string>(TODAS)
  const [visibilidade, setVisibilidade] = useState<FiltroVisibilidade>(TODAS)
  const [ordenacao, setOrdenacao] = useState<Ordenacao>('recentes')

  const criar = useCriarDesafio()
  const [dialogAberto, setDialogAberto] = useState(false)
  const [titulo, setTitulo] = useState('')
  const [enunciado, setEnunciado] = useState('')
  const [plataformaNova, setPlataformaNova] = useState('')
  const [identificador, setIdentificador] = useState('')
  const [urlExterna, setUrlExterna] = useState('')
  const [tituloTocado, setTituloTocado] = useState(false)

  const itens = useMemo(() => query.data?.itens ?? [], [query.data])
  const temFiltro = plataforma !== TODAS || visibilidade !== TODAS

  /** Plataformas da página carregada + a selecionada (para o trigger nunca ficar sem rótulo). */
  const plataformas = useMemo(() => {
    const nomes = new Set<string>()
    for (const d of itens) if (d.plataformaOrigem) nomes.add(d.plataformaOrigem)
    if (plataforma !== TODAS) nomes.add(plataforma)
    return [...nomes].sort((a, b) => a.localeCompare(b, 'pt-BR'))
  }, [itens, plataforma])

  const visiveis = useMemo(() => {
    const filtrados = itens.filter(
      (d) =>
        (plataforma === TODAS || d.plataformaOrigem === plataforma) &&
        (visibilidade === TODAS || d.visibilidade === visibilidade),
    )
    return ordenar(filtrados, ordenacao)
  }, [itens, plataforma, visibilidade, ordenacao])

  /** Um filtro novo volta para a primeira página — filtrar a página 3 e ver nada é hostil. */
  function aplicar(muda: () => void) {
    muda()
    setPagina(0)
  }

  function limparFiltros() {
    setPlataforma(TODAS)
    setVisibilidade(TODAS)
    setPagina(0)
  }

  function abrirDialogo() {
    setTitulo('')
    setEnunciado('')
    setPlataformaNova('')
    setIdentificador('')
    setUrlExterna('')
    setTituloTocado(false)
    setDialogAberto(true)
  }

  const tituloInvalido = tituloTocado && titulo.trim().length === 0

  async function handleSalvar(event: React.FormEvent) {
    event.preventDefault()
    setTituloTocado(true)
    const tituloLimpo = titulo.trim()
    if (!tituloLimpo) return

    try {
      await criar.mutateAsync({
        titulo: tituloLimpo,
        enunciado: enunciado.trim() || null,
        plataformaOrigem: plataformaNova.trim() || null,
        identificadorExterno: identificador.trim() || null,
        urlExterna: urlExterna.trim() || null,
      })
      toast.success('Desafio criado.')
      setDialogAberto(false)
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível criar o desafio.'))
    }
  }

  const botaoNovo = (
    <Button icon={Plus} onClick={abrirDialogo}>
      Novo desafio
    </Button>
  )

  return (
    <PageContainer className="gap-[18px]">
      <PageHeader title="Meus desafios" subtitle={legenda(query.data)} actions={botaoNovo} />

      <QueryBoundary query={query} loading={<GradeEsqueleto />}>
        {(dados) =>
          dados.itens.length === 0 ? (
            <EmptyState
              icon={Target}
              title="Nenhum desafio ainda."
              description="Registre seu primeiro desafio para começar a submeter resoluções e acompanhar a evolução das métricas."
              action={botaoNovo}
            />
          ) : (
            <div className="flex flex-col gap-[18px]">
              <div className="flex flex-wrap items-center gap-[9px]">
                <Select
                  value={plataforma}
                  onValueChange={(v) => aplicar(() => setPlataforma(v))}
                >
                  <SelectTrigger aria-label="Filtrar por plataforma" ativo={plataforma !== TODAS}>
                    <SelectValue placeholder="Plataforma" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={TODAS}>Plataforma · todas</SelectItem>
                    {plataformas.map((p) => (
                      <SelectItem key={p} value={p}>
                        {p}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <Select
                  value={visibilidade}
                  onValueChange={(v) => aplicar(() => setVisibilidade(v as FiltroVisibilidade))}
                >
                  <SelectTrigger
                    aria-label="Filtrar por visibilidade"
                    ativo={visibilidade !== TODAS}
                  >
                    <SelectValue placeholder="Visibilidade" />
                  </SelectTrigger>
                  <SelectContent>
                    {VISIBILIDADES.map((v) => (
                      <SelectItem key={v.value} value={v.value}>
                        {v.value === TODAS ? 'Visibilidade · todas' : v.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                {temFiltro && (
                  <Button variant="ghost" size="sm" onClick={limparFiltros}>
                    Limpar filtros
                  </Button>
                )}

                <div className="flex-1" />

                <Select
                  value={ordenacao}
                  onValueChange={(v) => setOrdenacao(v as Ordenacao)}
                >
                  <SelectTrigger aria-label="Ordenar desafios" ativo={ordenacao !== 'recentes'}>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ORDENACOES.map((o) => (
                      <SelectItem key={o.value} value={o.value}>
                        {o.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {visiveis.length === 0 ? (
                <EmptyState
                  icon={Filter}
                  title="Nenhum desafio com esses filtros."
                  description="Os filtros valem para os desafios desta página. Limpe-os ou vá para outra página."
                  action={
                    <Button variant="ghost" size="sm" onClick={limparFiltros}>
                      Limpar filtros
                    </Button>
                  }
                />
              ) : (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {visiveis.map((desafio) => (
                    <DesafioCard
                      key={desafio.id}
                      to={`/app/desafios/${desafio.id}`}
                      titulo={desafio.titulo}
                      plataforma={desafio.plataformaOrigem}
                      visibilidade={desafio.visibilidade}
                      criadoEm={desafio.criadoEm}
                    />
                  ))}
                </div>
              )}

              <Pagination page={pagina} totalPages={dados.totalPaginas} onChange={setPagina} />
            </div>
          )
        }
      </QueryBoundary>

      <Dialog open={dialogAberto} onOpenChange={setDialogAberto}>
        <DialogContent width={560}>
          <form onSubmit={handleSalvar} className="flex min-h-0 flex-col">
            <DialogHeader icon={Plus}>
              <DialogTitle>Novo desafio</DialogTitle>
            </DialogHeader>

            <DialogBody>
              <Input
                id="desafio-titulo"
                label="Título"
                required
                autoFocus
                maxLength={255}
                placeholder="Ex.: Two Sum"
                value={titulo}
                onChange={(e) => setTitulo(e.target.value)}
                error={tituloInvalido ? 'Informe um título.' : null}
              />

              <Textarea
                id="desafio-enunciado"
                label="Enunciado"
                minHeight={76}
                maxLength={5000}
                placeholder="Dado um array e um alvo, retorne os índices…"
                value={enunciado}
                onChange={(e) => setEnunciado(e.target.value)}
              />

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <Input
                  id="desafio-plataforma"
                  label="Plataforma"
                  maxLength={100}
                  placeholder="Ex.: LeetCode"
                  value={plataformaNova}
                  onChange={(e) => setPlataformaNova(e.target.value)}
                />

                <Input
                  id="desafio-identificador"
                  label="Identificador"
                  mono
                  maxLength={100}
                  placeholder="Ex.: #1"
                  value={identificador}
                  onChange={(e) => setIdentificador(e.target.value)}
                />
              </div>

              <Input
                id="desafio-url"
                label="URL externa"
                type="url"
                mono
                maxLength={500}
                placeholder="https://…"
                value={urlExterna}
                onChange={(e) => setUrlExterna(e.target.value)}
              />

              <div className="flex gap-[9px] rounded-ci border border-line bg-recess px-3 py-[11px]">
                <Lock size={14} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-steel" />
                <p className="text-[12px] leading-[1.45] text-mid">
                  O desafio nasce <strong className="font-semibold text-ink">privado</strong>. Você
                  pode publicá-lo depois.
                </p>
              </div>
            </DialogBody>

            <DialogFooter>
              <Button type="button" variant="ghost" onClick={() => setDialogAberto(false)}>
                Cancelar
              </Button>
              <Button type="submit" loading={criar.isPending}>
                Salvar
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </PageContainer>
  )
}

/** `12 desafios · 4 públicos` — a contagem de públicos só é honesta quando cabe tudo numa página. */
function legenda(dados: { itens: DesafioResumoDTO[]; totalItens: number; totalPaginas: number } | undefined) {
  if (!dados) return undefined
  const total = pluralPt(dados.totalItens, 'desafio', 'desafios')
  if (dados.totalPaginas > 1) return total
  const publicos = dados.itens.filter((d) => d.visibilidade === 'PUBLICO').length
  return `${total} · ${pluralPt(publicos, 'público', 'públicos')}`
}

function ordenar(itens: DesafioResumoDTO[], ordenacao: Ordenacao): DesafioResumoDTO[] {
  const copia = [...itens]
  if (ordenacao === 'titulo') {
    return copia.sort((a, b) => a.titulo.localeCompare(b.titulo, 'pt-BR'))
  }
  const sinal = ordenacao === 'antigos' ? -1 : 1
  return copia.sort(
    (a, b) => sinal * (new Date(b.criadoEm).getTime() - new Date(a.criadoEm).getTime()),
  )
}

/** 6 cartões-esqueleto na mesma moldura do <DesafioCard> (04 §4.5). */
function GradeEsqueleto() {
  return (
    <div
      role="status"
      aria-busy
      aria-label="Carregando desafios"
      className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3"
    >
      {Array.from({ length: 6 }, (_, i) => (
        <div
          key={i}
          className="flex flex-col gap-3 rounded-ci border border-line bg-panel p-[17px]"
        >
          <div className="flex items-start justify-between gap-2.5">
            <Skeleton className="h-[15px] w-1/2 bg-line" />
            <Skeleton className="h-[19px] w-[68px]" />
          </div>
          <Skeleton className="h-[19px] w-[84px]" />
          <div className="mt-2 flex items-center justify-between border-t border-line-soft pt-[11px]">
            <Skeleton className="h-[11px] w-[78px]" />
            <Skeleton className="h-[11px] w-[62px]" />
          </div>
        </div>
      ))}
    </div>
  )
}
