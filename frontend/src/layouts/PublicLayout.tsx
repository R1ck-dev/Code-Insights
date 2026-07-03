import { Link, Outlet } from 'react-router-dom'
import { Logo } from '@/components/Logo'
import { ThemeToggle } from '@/components/ThemeToggle'
import { buttonClasses } from '@/components/ui/button'
import { useAuth } from '@/auth/useAuth'

/** Chrome público (landing, portfólio): top nav com logo + acesso. */
export function PublicLayout() {
  const { status } = useAuth()
  const authed = status === 'authenticated'

  return (
    <div className="flex min-h-screen flex-col bg-bg">
      <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-border-subtle bg-bg/85 px-5 backdrop-blur sm:px-8">
        <Link to="/" aria-label="Início">
          <Logo />
        </Link>
        <div className="flex items-center gap-2.5">
          <ThemeToggle />
          {authed ? (
            <Link to="/app" className={buttonClasses({ size: 'sm' })}>
              Ir para o app
            </Link>
          ) : (
            <>
              <Link to="/entrar" className={buttonClasses({ variant: 'ghost', size: 'sm' })}>
                Entrar
              </Link>
              <Link to="/criar-conta" className={buttonClasses({ size: 'sm' })}>
                Criar conta
              </Link>
            </>
          )}
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  )
}
