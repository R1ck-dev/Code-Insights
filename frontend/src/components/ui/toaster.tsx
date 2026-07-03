import { Toaster as SonnerToaster } from 'sonner'
import { useTheme } from '@/theme/ThemeProvider'

export { toast } from 'sonner'

export function Toaster() {
  const { theme } = useTheme()
  return (
    <SonnerToaster
      theme={theme}
      position="bottom-right"
      toastOptions={{
        style: {
          background: 'var(--surface)',
          border: '1px solid var(--border-strong)',
          color: 'var(--fg)',
          fontFamily: 'var(--font-sans)',
        },
      }}
    />
  )
}
