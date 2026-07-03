import { cn } from '@/lib/utils'

interface CodeEditorProps {
  value: string
  onChange: (value: string) => void
  label?: string
  placeholder?: string
  minHeight?: number
  className?: string
  id?: string
}

/**
 * Editor de código simples: textarea monospace com cabeçalho e contador.
 * (Highlight ao vivo — via CodeMirror — fica como evolução futura.)
 */
export function CodeEditor({
  value,
  onChange,
  label = 'Editor',
  placeholder,
  minHeight = 300,
  className,
  id,
}: CodeEditorProps) {
  return (
    <div className={cn('overflow-hidden rounded-[10px] border border-border bg-[#0E0F14] dark:bg-[#0E0F14]', className)}>
      <div className="flex h-[38px] items-center justify-between border-b border-border bg-surface px-3">
        <span className="font-mono text-[12px] font-medium text-muted">{label}</span>
        <span className="font-mono text-[11px] text-subtle">{value.length} caracteres</span>
      </div>
      <textarea
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        spellCheck={false}
        className="block w-full resize-y bg-transparent px-4 py-3 font-mono text-[13px] leading-[1.7] text-[#D7DAE3] outline-none placeholder:text-subtle"
        style={{ minHeight, tabSize: 2 }}
      />
    </div>
  )
}
