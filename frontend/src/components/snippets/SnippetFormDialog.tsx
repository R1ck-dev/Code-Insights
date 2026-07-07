import { useEffect, useState } from 'react'
import { Braces } from 'lucide-react'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormField } from '@/components/ui/form-field'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { CodeEditor } from '@/components/CodeEditor'
import { toast } from '@/components/ui/toaster'
import { CATEGORIAS } from '@/domain/enums'
import { useCriarSnippet } from '@/features/snippets/hooks'
import { apiErrorMessage } from '@/lib/api'
import type { CategoriaConceito } from '@/types/api'

interface SnippetFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Quando informado, o snippet criado é vinculado a este desafio. */
  desafioId?: string
  onCreated?: () => void
}

/**
 * Diálogo reutilizável de criação de snippet. Aceita um `desafioId` opcional para
 * vincular o trecho a um desafio (usado na submissão de resolução e no detalhe do
 * desafio). A tela de Snippets mantém seu próprio formulário (criar + editar).
 */
export function SnippetFormDialog({
  open,
  onOpenChange,
  desafioId,
  onCreated,
}: SnippetFormDialogProps) {
  const criar = useCriarSnippet()
  const [codigo, setCodigo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [categoria, setCategoria] = useState<CategoriaConceito | ''>('')
  const [tocado, setTocado] = useState(false)

  // Reseta o formulário sempre que o diálogo abre.
  useEffect(() => {
    if (open) {
      setCodigo('')
      setDescricao('')
      setCategoria('')
      setTocado(false)
    }
  }, [open])

  const codigoInvalido = tocado && codigo.trim().length === 0
  const categoriaInvalida = tocado && !categoria

  async function handleSalvar(event: React.FormEvent) {
    event.preventDefault()
    setTocado(true)
    const codigoLimpo = codigo.trim()
    if (!codigoLimpo || !categoria) return
    try {
      await criar.mutateAsync({
        codigo: codigoLimpo,
        descricao: descricao.trim() || null,
        categoria,
        desafioId: desafioId ?? null,
      })
      toast.success('Snippet criado.')
      onOpenChange(false)
      onCreated?.()
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível salvar o snippet.'))
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent showClose={false}>
        <form onSubmit={handleSalvar}>
          <DialogHeader>
            <DialogTitle>Novo snippet</DialogTitle>
            <Braces size={18} className="text-subtle" />
          </DialogHeader>

          <DialogBody>
            <FormField
              label="Código"
              htmlFor="snippet-form-codigo"
              required
              error={codigoInvalido ? 'Informe o código do snippet.' : undefined}
            >
              <CodeEditor
                id="snippet-form-codigo"
                value={codigo}
                onChange={setCodigo}
                label="snippet.java"
                minHeight={160}
                placeholder="Cole aqui o trecho de código…"
              />
            </FormField>

            <FormField label="Descrição" htmlFor="snippet-form-descricao">
              <Input
                id="snippet-form-descricao"
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
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" variant="primary" loading={criar.isPending}>
              Salvar
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
