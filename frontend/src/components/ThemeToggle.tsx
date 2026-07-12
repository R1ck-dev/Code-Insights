import { Moon, Sun } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useTheme } from '@/theme/ThemeProvider'

interface ThemeToggleProps {
  /** 36 = topbar do app e telas de acesso · 38 = nav pública (spec 03 §1). */
  size?: 36 | 38
  className?: string
}

/**
 * Botão-ícone de tema (spec 01 §7.10 · spec 03 §1): quadrado sem preenchimento,
 * hairline `line` sempre visível, raio 3px, ícone lucide 17px `strokeWidth 2`.
 * Repouso `mid` → hover `ink` com a borda subindo para `line-strong`.
 */
export function ThemeToggle({ size = 36, className }: ThemeToggleProps) {
  const { theme, toggleTheme } = useTheme()
  const isDark = theme === 'dark'

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={isDark ? 'Ativar modo claro' : 'Ativar modo escuro'}
      title={isDark ? 'Modo claro' : 'Modo escuro'}
      className={cn(
        'ci-foco-botao flex shrink-0 cursor-pointer items-center justify-center rounded-ci border border-line bg-transparent text-mid transition-colors hover:border-line-strong hover:text-ink',
        className,
      )}
      style={{ width: size, height: size }}
    >
      {isDark ? <Sun size={17} strokeWidth={2} /> : <Moon size={17} strokeWidth={2} />}
    </button>
  )
}
