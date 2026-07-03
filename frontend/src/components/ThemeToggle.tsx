import { Moon, Sun } from 'lucide-react'
import { useTheme } from '@/theme/ThemeProvider'
import { cn } from '@/lib/utils'

export function ThemeToggle({ className }: { className?: string }) {
  const { theme, toggleTheme } = useTheme()
  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={theme === 'dark' ? 'Ativar modo claro' : 'Ativar modo escuro'}
      className={cn(
        'flex h-[38px] w-[38px] cursor-pointer items-center justify-center rounded-[9px] border border-border text-muted transition-colors hover:bg-surface-2 hover:text-fg',
        className,
      )}
    >
      {theme === 'dark' ? <Sun size={17} /> : <Moon size={17} />}
    </button>
  )
}
