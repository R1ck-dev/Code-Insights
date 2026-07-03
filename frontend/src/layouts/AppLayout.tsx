import { Braces, ChevronDown, LayoutDashboard, LogOut, Menu, Target, User } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Logo } from '@/components/Logo'
import { Avatar } from '@/components/Avatar'
import { ThemeToggle } from '@/components/ThemeToggle'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useAuth } from '@/auth/useAuth'
import { ROLE_LABEL } from '@/domain/enums'
import { cn } from '@/lib/utils'

interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
}

const NAV: NavItem[] = [
  { to: '/app', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/app/desafios', label: 'Desafios', icon: Target },
  { to: '/app/snippets', label: 'Snippets', icon: Braces },
  { to: '/app/perfil', label: 'Perfil', icon: User },
]

function navItemClasses(active: boolean) {
  return cn(
    'flex h-10 items-center gap-[11px] rounded-[9px] px-3 text-sm transition-colors',
    active
      ? 'bg-brand/[.13] font-semibold text-brand-on'
      : 'font-medium text-muted hover:bg-surface-2 hover:text-fg',
  )
}

export function AppLayout() {
  const { user } = useAuth()

  return (
    <div className="flex min-h-screen bg-bg">
      {/* Sidebar (desktop) */}
      <aside className="sticky top-0 hidden h-screen w-[236px] shrink-0 flex-col border-r border-border-subtle bg-bg-deep px-3.5 py-[18px] md:flex">
        <Link to="/app" className="mb-5 px-1.5">
          <Logo size="sm" />
        </Link>
        <nav className="flex flex-col gap-1">
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => navItemClasses(isActive)}>
              {({ isActive }) => (
                <>
                  <Icon size={18} className={isActive ? 'text-brand-strong' : 'text-subtle'} />
                  {label}
                </>
              )}
            </NavLink>
          ))}
        </nav>
        <div className="flex-1" />
        <div className="flex flex-col gap-2.5 border-t border-border-subtle pt-3.5">
          <div className="flex items-center gap-2.5">
            <Avatar name={user?.username} size={34} />
            <div className="flex min-w-0 flex-col leading-tight">
              <span className="truncate text-[13px] font-semibold text-fg">{user?.username}</span>
              <span className="text-[11.5px] text-subtle">{user ? ROLE_LABEL[user.role] : ''}</span>
            </div>
          </div>
          {user && (
            <Link
              to={`/u/${user.id}`}
              className="px-0.5 text-[12px] font-medium text-brand-strong hover:underline"
            >
              Ver meu portfólio
            </Link>
          )}
        </div>
      </aside>

      {/* Conteúdo */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex h-[62px] items-center justify-between border-b border-border-subtle bg-bg/85 px-5 backdrop-blur">
          <div className="flex items-center gap-2 md:hidden">
            <MobileNav />
            <Link to="/app">
              <Logo size="sm" />
            </Link>
          </div>
          <div className="hidden md:block" />
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <UserMenu />
          </div>
        </header>
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex h-[38px] cursor-pointer items-center gap-2 rounded-[9px] pl-1.5 pr-2 outline-none hover:bg-surface-2">
        <Avatar name={user?.username} size={28} />
        <span className="hidden text-[13.5px] font-semibold text-fg sm:block">{user?.username}</span>
        <ChevronDown size={15} className="text-subtle" />
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        <DropdownMenuLabel>@{user?.username}</DropdownMenuLabel>
        <DropdownMenuItem asChild>
          <Link to="/app/perfil">
            <User size={15} className="text-subtle" />
            Meu perfil
          </Link>
        </DropdownMenuItem>
        {user && (
          <DropdownMenuItem asChild>
            <Link to={`/u/${user.id}`}>
              <Target size={15} className="text-subtle" />
              Ver portfólio público
            </Link>
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem
          data-variant="danger"
          onSelect={() => {
            logout()
            navigate('/')
          }}
        >
          <LogOut size={15} />
          Sair
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

function MobileNav() {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label="Abrir menu"
        className="flex h-9 w-9 items-center justify-center rounded-lg border border-border text-muted outline-none hover:bg-surface-2"
      >
        <Menu size={18} />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        {NAV.map(({ to, label, icon: Icon, end }) => (
          <DropdownMenuItem key={to} asChild>
            <NavLink to={to} end={end}>
              <Icon size={16} className="text-subtle" />
              {label}
            </NavLink>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
