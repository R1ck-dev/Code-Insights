import { useState, type CSSProperties } from 'react'
import { LANG_LABEL, type CodeLang } from '@/components/CodeBlock'
import { useTheme } from '@/theme/ThemeProvider'
import { cn } from '@/lib/utils'

/** Pele de input do Órbita: corpo em `recess`, hairline `line-strong`, foco = borda `ink` + anel .3/.14. */
const SKIN = {
  dark: {
    bg: '#05060B',
    header: '#0B0F1A',
    border: '#2A3658',
    divider: '#1E2740',
    focusBorder: '#EDF0FA',
    focusRing: 'rgba(237,240,250,.3)',
    hd: '#A6AFC9',
    soft: '#6B738F',
    ink: '#EDF0FA',
  },
  light: {
    bg: '#EAEFF6',
    header: '#FBFCFE',
    border: '#C3CAD8',
    divider: '#D3DAE6',
    focusBorder: '#1A2436',
    focusRing: 'rgba(26,36,54,.14)',
    hd: '#55617A',
    soft: '#8A94A8',
    ink: '#1A2436',
  },
}

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
}

/**
 * Editor de código: textarea monospace com cabeçalho (ponto de linguagem + rótulo) e contador
 * de caracteres. Tema vem de `useTheme()`.
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
}: CodeEditorProps) {
  const { theme } = useTheme()
  const [focused, setFocused] = useState(false)
  const S = SKIN[theme]
  const langColor = lang ? LANG_LABEL[lang]?.[1] : undefined

  return (
    <div
      className={cn('overflow-hidden rounded-[3px] border transition-colors', className)}
      style={{
        background: S.bg,
        borderColor: focused ? S.focusBorder : S.border,
        boxShadow: focused ? `0 0 0 2px ${S.focusRing}` : undefined,
      }}
    >
      <div
        className="flex h-[38px] items-center justify-between border-b px-3"
        style={{ background: S.header, borderColor: S.divider }}
      >
        <div className="flex min-w-0 items-center gap-2">
          {langColor && (
            <span className="h-[9px] w-[9px] shrink-0 rounded-full" style={{ background: langColor }} />
          )}
          <span className="truncate font-mono text-[12px] font-medium" style={{ color: S.hd }}>
            {label}
          </span>
        </div>
        <span
          className="shrink-0 font-mono text-[11px] tabular-nums"
          style={{ color: S.soft }}
        >
          {value.length} caracteres
        </span>
      </div>
      <textarea
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        placeholder={placeholder}
        spellCheck={false}
        className="block w-full resize-y bg-transparent px-4 py-3 font-mono text-[13px] leading-[1.7] outline-none placeholder:text-[var(--ci-editor-soft)]"
        style={
          {
            minHeight,
            tabSize: 2,
            color: S.ink,
            caretColor: S.ink,
            '--ci-editor-soft': S.soft,
          } as CSSProperties
        }
      />
    </div>
  )
}
