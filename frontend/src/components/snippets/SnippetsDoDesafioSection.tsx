import { useState } from 'react'
import { Braces, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Chip } from '@/components/ui/badge'
import { EmptyState } from '@/components/ui/empty-state'
import { Skeleton } from '@/components/ui/skeleton'
import { CodeBlock } from '@/components/CodeBlock'
import { SnippetFormDialog } from '@/components/snippets/SnippetFormDialog'
import { CATEGORIA_META } from '@/domain/enums'
import { useSnippetsDoDesafio } from '@/features/snippets/hooks'
import type { CategoriaConceito, SnippetDTO } from '@/types/api'

/**
 * Seção "Snippets deste desafio" (telas M e N): lista os snippets vinculados ao
 * desafio e permite adicionar novos já vinculados.
 */
export function SnippetsDoDesafioSection({ desafioId }: { desafioId: string }) {
  const [formAberto, setFormAberto] = useState(false)
  const query = useSnippetsDoDesafio(desafioId)
  const snippets = query.data?.itens ?? []

  return (
    <section className="flex flex-col gap-3.5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <h3 className="text-[16px] font-semibold text-ink">Snippets deste desafio</h3>
          {query.data && (
            <span className="tabular rounded-ci border border-line bg-recess px-2 py-0.5 font-mono text-[12px] text-mid">
              {query.data.totalItens}
            </span>
          )}
        </div>
        <Button variant="secondary" size="sm" icon={Plus} onClick={() => setFormAberto(true)}>
          Adicionar snippet
        </Button>
      </div>

      {query.isPending ? (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {[0, 1].map((i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      ) : snippets.length === 0 ? (
        <EmptyState
          size="sm"
          icon={Braces}
          title="Nenhum snippet vinculado a este desafio."
          description="Guarde trechos reutilizáveis que nasceram deste problema — eles também aparecem na sua biblioteca de snippets."
        />
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
    <Card className="flex flex-col gap-2.5 p-3">
      <CodeBlock code={snippet.codigo} lang="java" lines={false} maxHeight={120} showCopy={false} />
      <div className="flex min-w-0 items-center gap-2 px-0.5">
        <CategoriaChip categoria={snippet.categoria} />
        {snippet.descricao && (
          <span className="min-w-0 truncate text-[12px] text-mid">{snippet.descricao}</span>
        )}
      </div>
    </Card>
  )
}

function CategoriaChip({ categoria }: { categoria: CategoriaConceito }) {
  const meta = CATEGORIA_META[categoria]
  return (
    <Chip icon={meta.icon} className="py-[3px] text-[11.5px]">
      {meta.label}
    </Chip>
  )
}
