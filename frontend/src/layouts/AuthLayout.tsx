import { Link } from 'react-router-dom'
import { Logo } from '@/components/Logo'
import { ThemeToggle } from '@/components/ThemeToggle'

interface AuthLayoutProps {
  children: React.ReactNode
  /** ação no canto superior direito (ex.: botão "Entrar"). Default: alternador de tema. */
  topRight?: React.ReactNode
}

/** Moldura das telas de acesso: fundo com glow índigo + grade, logo e card central. */
export function AuthLayout({ children, topRight }: AuthLayoutProps) {
  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden bg-bg-deep">
      {/* glow + grade sutil */}
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            'radial-gradient(70% 55% at 50% -8%, rgba(110,95,246,.16), transparent 62%)',
        }}
      />
      <div
        className="pointer-events-none absolute inset-0 opacity-60"
        style={{
          backgroundImage: 'radial-gradient(circle at 1px 1px, rgba(139,133,255,.05) 1px, transparent 0)',
          backgroundSize: '26px 26px',
        }}
      />

      <header className="relative flex h-[60px] items-center justify-between px-6 sm:px-8">
        <Link to="/" aria-label="Início">
          <Logo size="sm" />
        </Link>
        {topRight ?? <ThemeToggle />}
      </header>

      <main className="relative flex flex-1 items-center justify-center px-4 pb-16">
        {children}
      </main>
    </div>
  )
}
