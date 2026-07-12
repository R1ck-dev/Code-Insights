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
import { useMeusDesafios } from '@/features/desafios/hooks'
import { useCriarSnippet } from '@/features/snippets/hooks'
import { apiErrorMessage } from '@/lib/api'
import type { CategoriaConceito } from '@/types/api'

/** Valor sentinela do item "Nenhum" — o Radix Select não aceita `value=""`. */
const SEM_DESAFIO = '__nenhum__'

interface SnippetFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /**
   * Vínculo FIXO: quando informado, o snippet nasce ligado a este desafio e o
   * campo "Desafio" não aparece (o contexto já decidiu). Sem ele, o diálogo
   * oferece o Select opcional de desafios (00-INDICE §6-A, Lacuna 6).
   */
  desafioId?: string
  onCreated?: () => void
}

/**
 * Diálogo de criação de snippet (04 §7.4). Chassi de formulário: cabeçalho com
 * ícone `braces`, corpo rolável e rodapé com as ações.
 */
export function SnippetFormDialog({
  open,
  onOpenChange,
  desafioId,
  onCreated,
}: SnippetFormDialogProps) {
  const criar = useCriarSnippet()
  const vinculoFixo = !!desafioId

  const [codigo, setCodigo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [categoria, setCategoria] = useState<CategoriaConceito | ''>('')
  const [desafioSelecionado, setDesafioSelecionado] = useState(SEM_DESAFIO)
  const [tocado, setTocado] = useState(false)

  // Só busca a lista quando o campo existe (sem vínculo fixo) E o diálogo está aberto. Este
  // diálogo fica MONTADO (fechado) em toda tela de desafio: sem o `enabled`, cada visita
  // disparava um GET de 100 desafios que nunca seria exibido.
  const desafios = useMeusDesafios(0, 100, { enabled: open && !vinculoFixo })
  const opcoesDesafio = vinculoFixo ? [] : (desafios.data?.itens ?? [])

  // Reseta o formulário sempre que o diálogo abre.
  useEffect(() => {
    if (open) {
      setCodigo('')
      setDescricao('')
      setCategoria('')
      setDesafioSelecionado(SEM_DESAFIO)
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

    const vinculo =
      desafioId ?? (desafioSelecionado === SEM_DESAFIO ? null : desafioSelecionado)

    try {
      await criar.mutateAsync({
        codigo: codigoLimpo,
        descricao: descricao.trim() || null,
        categoria,
        desafioId: vinculo,
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
      <DialogContent width={560}>
        <form onSubmit={handleSalvar} className="flex min-h-0 flex-col">
          <DialogHeader icon={Braces}>
            <DialogTitle>Novo snippet</DialogTitle>
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
                lang="java"
                minHeight={160}
                placeholder="Cole aqui o trecho de código…"
              />
            </FormField>

            <Input
              label="Descrição"
              id="snippet-form-descricao"
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

            {!vinculoFixo && (
              <FormField
                label="Desafio"
                hint="Opcional — vincule o trecho ao problema em que ele nasceu."
              >
                <Select value={desafioSelecionado} onValueChange={setDesafioSelecionado}>
                  <SelectTrigger variant="campo">
                    <SelectValue placeholder="Nenhum" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={SEM_DESAFIO}>Nenhum</SelectItem>
                    {opcoesDesafio.map((d) => (
                      <SelectItem key={d.id} value={d.id}>
                        {d.titulo}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormField>
            )}
          </DialogBody>

          <DialogFooter>
            <Button type="button" variant="secondary" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" variant="primary" loading={criar.isPending}>
              Salvar snippet
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
