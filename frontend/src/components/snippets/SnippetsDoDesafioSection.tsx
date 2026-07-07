import { useState } from 'react'
import { Braces, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { CodeBlock } from '@/components/CodeBlock'
import { SnippetFormDialog } from '@/components/snippets/SnippetFormDialog'
import { CATEGORIA_META } from '@/domain/enums'
import { useSnippetsDoDesafio } from '@/features/snippets/hooks'
import type { CategoriaConceito, SnippetDTO } from '@/types/api'

/**
 * Seção "Snippets deste desafio": lista os snippets vinculados ao desafio e
 * permite adicionar novos já vinculados. Reutilizada na submissão de resolução
 * e no detalhe do desafio.
 */
export function SnippetsDoDesafioSection({ desafioId }: { desafioId: string }) {
  const [formAberto, setFormAberto] = useState(false)
  const query = useSnippetsDoDesafio(desafioId)
  const snippets = query.data?.itens ?? []

  return (
    <section className="flex flex-col gap-3.5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <h3 className="text-base font-semibold text-heading">Snippets deste desafio</h3>
          {query.data && (
            <span className="rounded-md border border-border bg-surface px-2 py-0.5 font-mono text-[12px] text-subtle tabular-nums">
              {query.data.totalItens}
            </span>
          )}
        </div>
        <Button variant="secondary" size="sm" onClick={() => setFormAberto(true)}>
          <Plus size={15} />
          Adicionar snippet
        </Button>
      </div>

      {query.isPending ? (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {[0, 1].map((k) => (
            <Skeleton key={k} className="h-24 w-full rounded-xl" />
          ))}
        </div>
      ) : snippets.length === 0 ? (
        <Card className="flex flex-col items-start gap-1 p-4">
          <span className="flex items-center gap-2 text-[13.5px] font-medium text-muted">
            <Braces size={15} className="text-subtle" />
            Nenhum snippet vinculado a este desafio.
          </span>
          <span className="text-[12px] text-subtle">
            Guarde trechos reutilizáveis que nasceram deste problema — eles também aparecem na sua
            biblioteca de snippets.
          </span>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {snippets.map((s) => (
            <SnippetMini key={s.id} snippet={s} />
          ))}
        </div>
      )}

      <SnippetFormDialog open={formAberto} onOpenChange={setFormAberto} desafioId={desafioId} />
    </section>
  )
}

function SnippetMini({ snippet }: { snippet: SnippetDTO }) {
  return (
    <Card className="flex flex-col gap-2 p-3">
      <CodeBlock code={snippet.codigo} lang="java" lines={false} maxHeight={120} showCopy={false} />
      <div className="flex items-center gap-2 px-0.5">
        <CategoriaTag categoria={snippet.categoria} />
        {snippet.descricao && (
          <span className="min-w-0 truncate text-[12px] text-muted">{snippet.descricao}</span>
        )}
      </div>
    </Card>
  )
}

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
