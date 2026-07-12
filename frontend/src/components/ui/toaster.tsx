import { Toaster as SonnerToaster } from 'sonner'
import { useTheme } from '@/theme/ThemeProvider'

export { toast } from 'sonner'

/** Toasts em `panel` + hairline + raio 3px. Sem cor: o feedback aqui é neutro. */
export function Toaster() {
  const { theme } = useTheme()

  return (
    <SonnerToaster
      theme={theme}
      position="bottom-right"
      gap={10}
      toastOptions={{
        style: {
          background: 'var(--panel)',
          border: '1px solid var(--line-strong)',
          borderRadius: '3px',
          boxShadow: 'var(--sombra-modal-funda)',
          color: 'var(--ink)',
          fontFamily: 'var(--font-sans)',
          fontSize: '13px',
        },
      }}
    />
  )
}
