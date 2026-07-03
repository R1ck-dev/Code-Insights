import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Lock, Plus, Target } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Chip } from '@/components/ui/badge'
import { Input, Textarea } from '@/components/ui/input'
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
import { toast } from '@/components/ui/toaster'
import { VisibilityBadge } from '@/components/domain/badges'
import { useCriarDesafio, useMeusDesafios } from '@/features/desafios/hooks'
import { apiErrorMessage } from '@/lib/api'
import { formatDate, pluralPt } from '@/lib/utils'
import type { DesafioResumoDTO } from '@/types/api'

export function DesafiosPage() {
  const navigate = useNavigate()
  const [pagina, setPagina] = useState(0)
  const query = useMeusDesafios(pagina)

  const criar = useCriarDesafio()
  const [dialogAberto, setDialogAberto] = useState(false)
  const [titulo, setTitulo] = useState('')
  const [enunciado, setEnunciado] = useState('')
  const [plataforma, setPlataforma] = useState('')
  const [identificador, setIdentificador] = useState('')
  const [urlExterna, setUrlExterna] = useState('')
  const [tituloTocado, setTituloTocado] = useState(false)

  function abrirDialogo() {
    setTitulo('')
    setEnunciado('')
    setPlataforma('')
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
        plataformaOrigem: plataforma.trim() || null,
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
    <Button variant="primary" onClick={abrirDialogo}>
      <Plus size={16} />
      Novo desafio
    </Button>
  )

  return (
    <PageContainer>
      <PageHeader
        title="Meus desafios"
        subtitle={
          query.data ? pluralPt(query.data.totalItens, 'desafio', 'desafios') : undefined
        }
        actions={botaoNovo}
      />

      <QueryBoundary query={query}>
        {(dados) =>
          dados.itens.length === 0 ? (
            <EmptyState
              icon={Target}
              title="Nenhum desafio ainda"
              description="Cadastre seu primeiro desafio para começar a registrar resoluções e acompanhar métricas."
              action={botaoNovo}
            />
          ) : (
            <div className="flex flex-col gap-5">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
                {dados.itens.map((desafio) => (
                  <DesafioCard
                    key={desafio.id}
                    desafio={desafio}
                    onOpen={() => navigate(`/app/desafios/${desafio.id}`)}
                  />
                ))}
              </div>
              <Pagination
                page={pagina}
                totalPages={dados.totalPaginas}
                onChange={setPagina}
              />
            </div>
          )
        }
      </QueryBoundary>

      <Dialog open={dialogAberto} onOpenChange={setDialogAberto}>
        <DialogContent showClose={false}>
          <form onSubmit={handleSalvar}>
            <DialogHeader>
              <DialogTitle>Novo desafio</DialogTitle>
              <Plus size={18} className="text-subtle" />
            </DialogHeader>

            <DialogBody>
              <FormField
                label="Título"
                htmlFor="desafio-titulo"
                required
                error={tituloInvalido ? 'Informe um título.' : undefined}
              >
                <Input
                  id="desafio-titulo"
                  value={titulo}
                  onChange={(e) => setTitulo(e.target.value)}
                  maxLength={255}
                  placeholder="Ex.: Two Sum"
                  autoFocus
                />
              </FormField>

              <FormField label="Enunciado" htmlFor="desafio-enunciado">
                <Textarea
                  id="desafio-enunciado"
                  value={enunciado}
                  onChange={(e) => setEnunciado(e.target.value)}
                  rows={3}
                  placeholder="Descrição do problema (opcional)…"
                />
              </FormField>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <FormField label="Plataforma" htmlFor="desafio-plataforma">
                  <Input
                    id="desafio-plataforma"
                    value={plataforma}
                    onChange={(e) => setPlataforma(e.target.value)}
                    maxLength={100}
                    placeholder="Ex.: LeetCode"
                  />
                </FormField>

                <FormField label="Identificador" htmlFor="desafio-identificador">
                  <Input
                    id="desafio-identificador"
                    value={identificador}
                    onChange={(e) => setIdentificador(e.target.value)}
                    maxLength={100}
                    className="font-mono"
                    placeholder="Ex.: 1"
                  />
                </FormField>
              </div>

              <FormField label="URL externa" htmlFor="desafio-url">
                <Input
                  id="desafio-url"
                  type="url"
                  value={urlExterna}
                  onChange={(e) => setUrlExterna(e.target.value)}
                  maxLength={500}
                  className="font-mono text-[12.5px]"
                  placeholder="https://…"
                />
              </FormField>

              <div className="flex gap-2.5 rounded-[10px] border border-info/25 bg-info/[.06] px-3 py-2.5">
                <Lock size={14} className="mt-0.5 shrink-0 text-info" />
                <p className="text-[12px] leading-snug text-muted">
                  O desafio nasce <span className="font-semibold text-info">privado</span>. Você pode
                  publicá-lo depois.
                </p>
              </div>
            </DialogBody>

            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                onClick={() => setDialogAberto(false)}
              >
                Cancelar
              </Button>
              <Button type="submit" variant="primary" loading={criar.isPending}>
                Salvar
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </PageContainer>
  )
}

function DesafioCard({
  desafio,
  onOpen,
}: {
  desafio: DesafioResumoDTO
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
      className="flex cursor-pointer flex-col gap-3 p-[17px] transition-colors hover:border-border-strong hover:bg-surface-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60"
    >
      <div className="flex items-start justify-between gap-2.5">
        <span className="min-w-0 text-[15px] font-semibold leading-snug text-heading">
          {desafio.titulo}
        </span>
        <span className="shrink-0">
          <VisibilityBadge visibilidade={desafio.visibilidade} />
        </span>
      </div>

      {desafio.plataformaOrigem && (
        <div className="flex items-center gap-2">
          <Chip>{desafio.plataformaOrigem}</Chip>
        </div>
      )}

      <div className="mt-auto flex items-center justify-between border-t border-border-subtle pt-[11px]">
        <span className="text-[12px] text-muted">Criado em</span>
        <span className="font-mono text-[11px] tabular-nums text-subtle">
          {formatDate(desafio.criadoEm)}
        </span>
      </div>
    </Card>
  )
}
