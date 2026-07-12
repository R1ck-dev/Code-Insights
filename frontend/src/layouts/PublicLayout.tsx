import { Link, Outlet } from 'react-router-dom'
import { Logo } from '@/components/Logo'
import { ThemeToggle } from '@/components/ThemeToggle'
import { buttonClasses } from '@/components/ui/button'
import { useAuth } from '@/auth/useAuth'

/*
 * Chrome público (landing, portfólio, desafio/resolução pública, Explorar pública) — spec 03 §B.1.
 *
 * Nav 64px sticky sobre `bg`, hairline `line-soft` embaixo. Botões em IBM Plex Mono 13px
 * (a nav pública é instrumento, não prosa). SEM starfield/nebulosa aqui: a atmosfera é
 * responsabilidade da tela (o hero da landing acende a sua) — este layout é moldura de conteúdo.
 */
export function PublicLayout() {
  const { status } = useAuth()
  const autenticado = status === 'authenticated'

  return (
    <div className="flex min-h-screen flex-col bg-bg">
      <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-line-soft bg-bg/85 px-5 backdrop-blur sm:px-8">
        <Link to="/" aria-label="Início" className="ci-foco-botao rounded-ci">
          <Logo size={26} />
        </Link>

        <div className="flex items-center gap-[9px]">
          <ThemeToggle size={38} />
          {autenticado ? (
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

      <main className="min-w-0 flex-1">
        <Outlet />
      </main>
    </div>
  )
}
