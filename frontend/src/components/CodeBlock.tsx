import { useState } from 'react'
import { useTheme } from '@/theme/ThemeProvider'
import { cn } from '@/lib/utils'

export type CodeLang = 'java' | 'python' | 'cpp' | 'javascript' | 'c'

interface Token {
  t: string
  c: string
}

const KW_CFAM =
  'if else for while do switch case default break continue return new delete class struct enum union public private protected static final const void int long double float char bool boolean byte short unsigned signed namespace using template typename auto this true false null nullptr try catch finally throw throws import package extends implements interface abstract synchronized volatile transient instanceof super sizeof typedef inline virtual operator friend explicit mutable goto'
const KW_JS =
  KW_CFAM +
  ' function var let of in typeof yield export from as require module undefined NaN Infinity console async await'
const KW_PY =
  'def class if elif else for while return import from as with try except finally raise lambda yield pass break continue global nonlocal in is not and or None True False self async await del assert print range len enumerate int str float bool list dict set tuple'

function kwset(lang: CodeLang): Set<string> {
  const map: Record<CodeLang, string> = {
    java: KW_CFAM,
    cpp: KW_CFAM,
    c: KW_CFAM,
    javascript: KW_JS,
    python: KW_PY,
  }
  return new Set(map[lang].split(/\s+/))
}

function tokenize(code: string, lang: CodeLang): Token[] {
  const kw = kwset(lang)
  const out: Token[] = []
  const isIdS = (c: string) => /[A-Za-z_$]/.test(c)
  const isId = (c: string) => /[A-Za-z0-9_$]/.test(c)
  let i = 0
  const n = code.length
  const push = (t: string, c: string) => out.push({ t, c })
  while (i < n) {
    const ch = code[i]
    if (ch === '\n') {
      push('\n', 'ws')
      i++
      continue
    }
    if (/[^\S\n]/.test(ch)) {
      let j = i
      while (j < n && /[^\S\n]/.test(code[j])) j++
      push(code.slice(i, j), 'ws')
      i = j
      continue
    }
    if ((ch === '/' && code[i + 1] === '/') || (ch === '#' && lang === 'python')) {
      let j = i
      while (j < n && code[j] !== '\n') j++
      push(code.slice(i, j), 'comment')
      i = j
      continue
    }
    if (ch === '/' && code[i + 1] === '*') {
      let j = i + 2
      while (j < n && !(code[j] === '*' && code[j + 1] === '/')) j++
      j = Math.min(n, j + 2)
      push(code.slice(i, j), 'comment')
      i = j
      continue
    }
    if (lang === 'python' && (code.startsWith('"""', i) || code.startsWith("'''", i))) {
      const q = code.substr(i, 3)
      let j = i + 3
      while (j < n && !code.startsWith(q, j)) j++
      j = Math.min(n, j + 3)
      push(code.slice(i, j), 'string')
      i = j
      continue
    }
    if (ch === '"' || ch === "'" || ch === '`') {
      let j = i + 1
      while (j < n) {
        if (code[j] === '\\') {
          j += 2
          continue
        }
        if (code[j] === ch) {
          j++
          break
        }
        if (code[j] === '\n' && ch !== '`') break
        j++
      }
      push(code.slice(i, j), 'string')
      i = j
      continue
    }
    if (ch === '@' && isIdS(code[i + 1] || '')) {
      let j = i + 1
      while (j < n && isId(code[j])) j++
      push(code.slice(i, j), 'annotation')
      i = j
      continue
    }
    if (/[0-9]/.test(ch) || (ch === '.' && /[0-9]/.test(code[i + 1] || ''))) {
      let j = i
      while (j < n && /[0-9a-fA-FxXbBoLlUu._]/.test(code[j])) j++
      push(code.slice(i, j), 'number')
      i = j
      continue
    }
    if (isIdS(ch)) {
      let j = i
      while (j < n && isId(code[j])) j++
      const w = code.slice(i, j)
      let k = j
      while (k < n && /[^\S\n]/.test(code[k])) k++
      let c = 'ident'
      if (kw.has(w)) c = 'keyword'
      else if (/^[A-Z]/.test(w)) c = 'type'
      else if (code[k] === '(') c = 'function'
      push(w, c)
      i = j
      continue
    }
    if (/[{}()[\];,.]/.test(ch)) {
      push(ch, 'punct')
      i++
      continue
    }
    if (/[+\-*/%=<>!&|^~?:]/.test(ch)) {
      let j = i
      while (j < n && /[+\-*/%=<>!&|^~?:]/.test(code[j])) j++
      push(code.slice(i, j), 'operator')
      i = j
      continue
    }
    push(ch, 'ident')
    i++
  }
  return out
}

/**
 * Paleta de sintaxe fria dessaturada (Órbita).
 * Regra 6 do sistema: o código NUNCA compete com o colormap de complexidade — ele é o único
 * acento cromático da interface. Nada de índigo/roxo/saturado aqui.
 */
const COLORS = {
  // Órbita escuro (protótipo: theme="field")
  dark: {
    ws: 'inherit',
    ident: '#D4DBE8',
    keyword: '#8FA6C9',
    type: '#D3AE6A',
    function: '#C7D2E6',
    string: '#8FC79E',
    number: '#E0A57C',
    comment: '#5A6779',
    annotation: '#C9AE6A',
    punct: '#7C8AA3',
    operator: '#A6B2C6',
  } as Record<string, string>,
  // Órbita claro (protótipo: theme="fieldlight")
  light: {
    ws: 'inherit',
    ident: '#1F2A3D',
    keyword: '#3A63B0',
    type: '#8A5A16',
    function: '#1F2A3D',
    string: '#3E7A4A',
    number: '#9A5A1E',
    comment: '#8A94A8',
    annotation: '#8A5A16',
    punct: '#55617A',
    operator: '#55617A',
  } as Record<string, string>,
}

/** Cromo do card: fundo, cabeçalho, hairline, rótulo, botão em repouso e gutter (cor `grid`). */
const PANEL = {
  dark: { bg: '#0B0F17', header: '#121826', border: '#24303F', hd: '#9AA7BD', hdSoft: '#5F6C82', gutter: '#33415A' },
  light: { bg: '#F5F7FB', header: '#EAEFF6', border: '#D3DAE6', hd: '#55617A', hdSoft: '#8A94A8', gutter: '#AEB8CC' },
}

/** Verde de sucesso do sistema — nunca #2FB863 (verde fora do colormap). */
const COPIADO = { dark: '#4FB477', light: '#3E9E63' }

/** Rótulo default + cor do ponto de linguagem (§2.4 do índice). Reusado pelo CodeEditor. */
export const LANG_LABEL: Record<CodeLang, [string, string]> = {
  java: ['Java', '#E76F00'],
  python: ['Python', '#4B8BBE'],
  cpp: ['C++', '#4C93D6'],
  javascript: ['JavaScript', '#E9C500'],
  c: ['C', '#659AD2'],
}

/** Fallback (linguagem desconhecida em runtime): rótulo neutro + steel. */
const LANG_FALLBACK: [string, string] = ['Código', '#8FA6C9']

interface CodeBlockProps {
  code: string
  lang?: CodeLang
  label?: string
  lines?: boolean
  maxHeight?: number | string
  className?: string
  /** Exibe o botão "Copiar" no cabeçalho. Desligue em previews clicáveis (evita interativo aninhado). */
  showCopy?: boolean
}

/** Bloco de código com syntax highlighting, numeração e botão copiar. */
export function CodeBlock({
  code,
  lang = 'java',
  label,
  lines = true,
  maxHeight,
  className,
  showCopy = true,
}: CodeBlockProps) {
  const { theme } = useTheme()
  const [copied, setCopied] = useState(false)
  const COL = COLORS[theme]
  const P = PANEL[theme]
  const [langName, langColor] = LANG_LABEL[lang] ?? LANG_FALLBACK

  const toks = tokenize(code, lang)
  const rows: Token[][] = [[]]
  for (const tk of toks) {
    const parts = tk.t.split('\n')
    parts.forEach((p, idx) => {
      if (idx > 0) rows.push([])
      if (p !== '') rows[rows.length - 1].push({ t: p, c: tk.c })
    })
  }

  const copy = async () => {
    try {
      await navigator.clipboard?.writeText(code)
      setCopied(true)
      setTimeout(() => setCopied(false), 1200)
    } catch {
      /* clipboard indisponível (contexto inseguro/sem permissão): não confirma cópia */
    }
  }

  return (
    <div
      className={cn('overflow-hidden rounded-[3px] border', className)}
      style={{ background: P.bg, borderColor: P.border }}
    >
      <div
        className="flex h-[38px] items-center justify-between border-b pl-3 pr-2.5"
        style={{ background: P.header, borderColor: P.border }}
      >
        <div className="flex min-w-0 items-center gap-2">
          <span className="h-[9px] w-[9px] shrink-0 rounded-full" style={{ background: langColor }} />
          <span className="truncate font-mono text-[12px] font-medium" style={{ color: P.hd }}>
            {label ?? langName}
          </span>
        </div>
        {showCopy && (
          <button
            type="button"
            onClick={copy}
            className="cursor-pointer rounded-[3px] px-[7px] py-1 font-mono text-[11.5px] font-medium transition-colors hover:bg-[rgba(127,127,127,0.16)]"
            style={{ color: copied ? COPIADO[theme] : P.hdSoft }}
          >
            {copied ? 'Copiado' : 'Copiar'}
          </button>
        )}
      </div>
      <div className="overflow-auto py-3.5 pl-1 pr-4" style={{ maxHeight }}>
        <div className="font-mono" style={{ fontSize: 13, lineHeight: 1.7 }}>
          {rows.map((ln, li) => (
            <div key={li} className="flex" style={{ minHeight: '1.7em' }}>
              {lines ? (
                <span
                  className="shrink-0 select-none pr-[18px] text-right"
                  style={{ width: '2.8em', color: P.gutter }}
                >
                  {li + 1}
                </span>
              ) : (
                <span className="w-3.5 shrink-0" />
              )}
              <span style={{ whiteSpace: 'pre' }}>
                {ln.length
                  ? ln.map((tk, ti) => (
                      <span
                        key={ti}
                        style={{
                          color: COL[tk.c] || COL.ident,
                          fontStyle: tk.c === 'comment' ? 'italic' : 'normal',
                        }}
                      >
                        {tk.t}
                      </span>
                    ))
                  : '​'}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
