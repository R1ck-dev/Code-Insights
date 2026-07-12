import type { CSSProperties } from 'react'
import { LANG_LABEL, type CodeLang } from '@/components/CodeBlock'
import { cn } from '@/lib/utils'

/*
 * Editor de código: textarea monospace com cabeçalho (ponto de linguagem + rótulo) e contador
 * de caracteres.
 *
 * ⚠ NÃO existe aqui uma tabela de cores. Antes, um objeto `SKIN` hardcodava 9 hex × 2 temas —
 * uma SEGUNDA CÓPIA da tabela de tokens, invisível a um find/replace e destinada a driftar no
 * primeiro token que mudasse. O CodeEditor não é paleta de sintaxe (essa é a única exceção do
 * sistema, e vive no CodeBlock): é cromo de input, e cromo de input sai dos tokens.
 * O anel de foco vem de `.ci-foco-input` — não se reconstrói `--anel-input` à mão.
 */

interface CodeEditorProps {
  value: string
  onChange: (value: string) => void
  /** Rótulo do cabeçalho. Na tela de submeter resolução: `Editor · {Linguagem}`. */
  label?: string
  /** Quando informado, mostra o ponto de linguagem 9px no cabeçalho (mesma cor do CodeBlock). */
  lang?: CodeLang
  placeholder?: string
  minHeight?: number
  className?: string
  id?: string
  /** Campo com erro — o `FormField` que o embrulha já desenha a mensagem. */
  'aria-invalid'?: boolean
  /** `id` da mensagem de erro/hint (ex.: `codigo-erro`). */
  'aria-describedby'?: string
}

/**
 * (Realce ao vivo — via CodeMirror — fica como evolução futura.)
 */
export function CodeEditor({
  value,
  onChange,
  label = 'Editor',
  lang,
  placeholder,
  minHeight = 300,
  className,
  id,
  'aria-invalid': invalid,
  'aria-describedby': describedBy,
}: CodeEditorProps) {
  const langColor = lang ? LANG_LABEL[lang]?.[1] : undefined

  return (
    <div
      className={cn(
        'overflow-hidden rounded-ci border border-line-strong bg-recess transition-colors',
        'focus-within:border-ink focus-within:shadow-anel-input',
        className,
      )}
    >
      <div className="flex h-[38px] items-center justify-between border-b border-line bg-panel px-3">
        <div className="flex min-w-0 items-center gap-2">
          {langColor && (
            <span
              aria-hidden
              className="h-[9px] w-[9px] shrink-0 rounded-full"
              style={{ background: langColor }}
            />
          )}
          <span className="truncate font-mono text-[12px] font-medium text-mid">{label}</span>
        </div>
        <span className="tabular shrink-0 font-mono text-[11px] text-soft">
          {value.length} caracteres
        </span>
      </div>
      <textarea
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        spellCheck={false}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy}
        className={cn(
          'block w-full resize-y bg-transparent px-4 py-3 font-mono text-[13px] leading-[1.7]',
          'text-ink caret-ink placeholder:text-soft outline-none',
        )}
        style={{ minHeight, tabSize: 2 } as CSSProperties}
      />
    </div>
  )
}
