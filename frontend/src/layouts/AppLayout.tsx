import {
  Braces,
  ChevronDown,
  Compass,
  FileText,
  FlaskConical,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldCheck,
  Target,
  User,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
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
import { useMeuConsentimento } from '@/features/pesquisa/hooks'
import { ROLE_LABEL } from '@/domain/enums'
import { cn } from '@/lib/utils'
import type { Role } from '@/types/api'

/*
 * Shell autenticado · ÓRBITA (spec 04 §1).
 *
 * Sidebar 236px (`recess` + hairline `line-soft` à direita) · topbar 60px (hairline embaixo).
 * Nav em IBM Plex Mono 13/500 — mono MEDE, e a nav é instrumento.
 * Item ativo: fundo `elevated` + `box-shadow: inset 2px 0 0 ink` (a barra de luz à esquerda).
 *
 * Decisões de produto (00-INDICE §6-A) aplicadas aqui:
 *  - SEM sino de notificações (não há backend de notificações; chrome inerte é pior que nada).
 *  - SEM busca na topbar. Não existe busca global; a Explorar tem campo próprio no corpo da página,
 *    como as demais listas (Desafios, Snippets) já fazem. O shell não expõe slot de busca.
 *  - SEM breadcrumb na topbar: as telas D/M/N usam `<Breadcrumb>` (components/page) no corpo.
 */

interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  /** Prefixos extra que mantêm o item aceso (ex.: /app/resolucoes/:id acende "Desafios"). */
  tambemEm?: string[]
  end?: boolean
  /** Papéis que enxergam o item. Ausente = todos. */
  papeis?: readonly Role[]
}

const NAV: NavItem[] = [
  { to: '/app', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/app/explorar', label: 'Explorar', icon: Compass },
  { to: '/app/desafios', label: 'Desafios', icon: Target, tambemEm: ['/app/resolucoes'] },
  { to: '/app/snippets', label: 'Snippets', icon: Braces },
  { to: '/app/perfil', label: 'Perfil', icon: User },
  { to: '/app/consentimento', label: 'Consentimento', icon: FileText },
  {
    to: '/app/pesquisa',
    label: 'Pesquisa',
    icon: FlaskConical,
    papeis: ['PESQUISADOR', 'ADMIN'],
  },
  { to: '/app/admin', label: 'Administração', icon: ShieldCheck, papeis: ['ADMIN'] },
]

/**
 * Esconder o item é cortesia com o aluno, não segurança: quem digitar a URL bate no `RequireRole`,
 * e quem chamar a API bate no `SecurityConfig`. São três camadas com propósitos distintos.
 */
function navDoPapel(role: Role | undefined): NavItem[] {
  return NAV.filter((item) => !item.papeis || (role !== undefined && item.papeis.includes(role)))
}

function itemAtivo(pathname: string, item: NavItem): boolean {
  const casa = (base: string) => pathname === base || pathname.startsWith(`${base}/`)
  if (item.end) return pathname === item.to
  return casa(item.to) || (item.tambemEm?.some(casa) ?? false)
}

export function AppLayout() {
  const { user } = useAuth()
  const { pathname } = useLocation()

  return (
    <div className="flex min-h-screen bg-bg">
      {/* Pular para o conteúdo (WCAG 2.4.1): a sidebar + topbar se repetem em TODAS as telas;
          sem isto, quem navega por teclado tabula a nav inteira em cada página. */}
      <a
        href="#conteudo"
        className="ci-foco-botao sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-50 focus:rounded-ci focus:border focus:border-line-strong focus:bg-panel focus:px-3 focus:py-2 focus:font-mono focus:text-[12.5px] focus:text-ink"
      >
        Pular para o conteúdo
      </a>

      {/* Sidebar (≥ md) */}
      <aside className="sticky top-0 hidden h-screen w-[236px] shrink-0 flex-col border-r border-line-soft bg-recess px-3 py-[18px] md:flex">
        <Link to="/app" aria-label="Início" className="ci-foco-botao rounded-ci px-1.5 pb-5">
          <Logo size={24} />
        </Link>

        <nav className="flex flex-col gap-[3px]" aria-label="Navegação principal">
          {navDoPapel(user?.role).map((item) => {
            const { to, label, icon: Icon, end } = item
            const ativo = itemAtivo(pathname, item)
            return (
              <NavLink
                key={to}
                to={to}
                end={end}
                aria-current={ativo ? 'page' : undefined}
                className={cn(
                  'ci-foco-botao flex h-[38px] items-center gap-[11px] rounded-ci px-3',
                  'font-mono text-[13px] font-medium transition-colors',
                  ativo
                    ? 'bg-elevated text-ink shadow-[inset_2px_0_0_var(--ink)]'
                    : 'text-mid hover:bg-elevated/55 hover:text-ink',
                )}
              >
                <Icon
                  size={17}
                  strokeWidth={2}
                  aria-hidden
                  className={cn('shrink-0', ativo ? 'text-ink' : 'text-soft')}
                />
                {label}
              </NavLink>
            )
          })}
        </nav>

        <div className="flex-1" />

        {/* Rodapé: bloco do usuário */}
        <div className="flex flex-col gap-2.5 border-t border-line-soft pt-3.5">
          <div className="flex items-center gap-2.5 px-1.5">
            <Avatar name={user?.username} size={32} />
            <div className="flex min-w-0 flex-col gap-px leading-[1.3]">
              <span className="truncate font-mono text-[12.5px] font-medium text-ink">
                {user?.username}
              </span>
              <span className="font-mono text-[10.5px] text-soft">
                {user ? ROLE_LABEL[user.role].toLowerCase() : ''}
              </span>
            </div>
          </div>
          {user && (
            <Link
              to={`/u/${user.id}`}
              className="ci-foco-botao rounded-ci px-1.5 font-mono text-[11px] text-steel transition-colors hover:text-steel-hover"
            >
              Ver meu portfólio
            </Link>
          )}
        </div>
      </aside>

      {/* Conteúdo */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex h-[60px] shrink-0 items-center justify-between border-b border-line-soft bg-bg/85 px-4 backdrop-blur md:px-6">
          <div className="flex items-center gap-2.5 md:hidden">
            <MobileNav pathname={pathname} />
            <Link to="/app" aria-label="Início">
              <Logo size={24} wordmark={false} />
            </Link>
          </div>
          <div className="hidden md:block" />
          <div className="flex items-center gap-2">
            <ThemeToggle size={36} />
            <span aria-hidden="true" className="mx-[3px] h-[22px] w-px bg-line" />
            <UserMenu />
          </div>
        </header>

        <main id="conteudo" tabIndex={-1} className="min-w-0 flex-1 outline-none">
          <ConviteDeConsentimento />
          <Outlet />
        </main>
      </div>
    </div>
  )
}

/**
 * Faixa que convida a responder ao TCLE — some assim que a pessoa responde, aceitando OU recusando.
 *
 * É convite, e não portão: não bloqueia nada, não tem modal e não volta depois de respondida. Um
 * aviso que só desaparece com "sim" seria pressão, e pressão invalida o consentimento. Também não
 * aparece enquanto a consulta carrega — piscar "responda ao termo" para quem já respondeu seria
 * ruído em toda navegação.
 */
function ConviteDeConsentimento() {
  const { data } = useMeuConsentimento()
  const { pathname } = useLocation()

  if (!data || data.decisao !== null || pathname === '/app/consentimento') return null

  return (
    <div className="border-b border-line-soft bg-recess px-4 py-[11px] md:px-6">
      <div className="flex flex-wrap items-center gap-x-[11px] gap-y-[7px]">
        <FlaskConical size={14} strokeWidth={2} aria-hidden className="shrink-0 text-soft" />
        <p className="text-[12.5px] text-body">
          Esta plataforma é objeto de uma pesquisa. Você decide se suas submissões podem ser usadas
          nela.
        </p>
        <Link
          to="/app/consentimento"
          className="ci-foco-botao rounded-ci font-mono text-[12px] font-medium text-steel underline-offset-2 transition-colors hover:text-steel-hover hover:underline"
        >
          Ler o termo e responder
        </Link>
      </div>
    </div>
  )
}

/** Menu do usuário (topbar): avatar 27 + username mono + chevron. */
function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label="Menu do usuário"
        className="ci-foco-botao flex h-[36px] cursor-pointer items-center gap-[9px] rounded-ci border border-transparent pl-1.5 pr-2 outline-none transition-colors hover:bg-elevated"
      >
        <Avatar name={user?.username} size={27} />
        <span className="hidden font-mono text-[12.5px] font-medium text-ink sm:block">
          {user?.username}
        </span>
        <ChevronDown size={14} strokeWidth={2} aria-hidden className="text-soft" />
      </DropdownMenuTrigger>

      <DropdownMenuContent>
        <DropdownMenuLabel>@{user?.username}</DropdownMenuLabel>
        <DropdownMenuItem asChild>
          <Link to="/app/perfil">
            <User size={15} strokeWidth={2} aria-hidden className="text-soft" />
            Meu perfil
          </Link>
        </DropdownMenuItem>
        {user && (
          <DropdownMenuItem asChild>
            <Link to={`/u/${user.id}`}>
              <Compass size={15} strokeWidth={2} aria-hidden className="text-soft" />
              Ver portfólio público
            </Link>
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="danger"
          onSelect={() => {
            logout()
            navigate('/')
          }}
        >
          <LogOut size={15} strokeWidth={2} aria-hidden />
          Sair
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

/** < md: a sidebar vira menu (mesmo comportamento de antes, pele nova). */
function MobileNav({ pathname }: { pathname: string }) {
  const { user } = useAuth()
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label="Abrir menu"
        className="ci-foco-botao flex h-9 w-9 cursor-pointer items-center justify-center rounded-ci border border-line text-mid outline-none transition-colors hover:border-line-strong hover:text-ink"
      >
        <Menu size={18} strokeWidth={2} aria-hidden />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        {navDoPapel(user?.role).map((item) => {
          const { to, label, icon: Icon, end } = item
          const ativo = itemAtivo(pathname, item)
          return (
            <DropdownMenuItem key={to} asChild>
              <NavLink
                to={to}
                end={end}
                aria-current={ativo ? 'page' : undefined}
                className={cn(ativo && 'bg-elevated text-ink')}
              >
                <Icon
                  size={16}
                  strokeWidth={2}
                  aria-hidden
                  className={ativo ? 'text-ink' : 'text-soft'}
                />
                {label}
              </NavLink>
            </DropdownMenuItem>
          )
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
