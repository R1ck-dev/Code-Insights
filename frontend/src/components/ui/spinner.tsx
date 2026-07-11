import { cn } from '@/lib/utils'

interface SpinnerProps {
  /**
   * `botao` — 13px, traço 2px sobre `line-strong`, `ciSpin .7s` (padrão).
   * `chip`  — 9px, traço 1.5px `ink` com topo transparente, `ciSpin .8s`
   *           (é o spinner do chip CALCULANDO, único no sistema).
   */
  variant?: 'botao' | 'chip'
  /** Diâmetro em px. Padrão: 13 (`botao`) / 9 (`chip`). */
  size?: number
  className?: string
  /** Rótulo acessível. Padrão: "Carregando". */
  label?: string
}

/**
 * Spinner **neutro**. No Órbita "calculando" é estado neutro (osso/tinta), não
 * info azul: não existe spinner colorido — regra inviolável nº 1.
 */
export function Spinner({ variant = 'botao', size, className, label = 'Carregando' }: SpinnerProps) {
  const chip = variant === 'chip'
  const px = size ?? (chip ? 9 : 13)

  return (
    <span
      role="status"
      aria-label={label}
      className={cn('inline-block shrink-0 rounded-full', chip ? 'ci-spin-chip' : 'ci-spin', className)}
      style={{
        width: px,
        height: px,
        border: chip ? '1.5px solid var(--ink)' : '2px solid var(--line-strong)',
        borderTopColor: chip ? 'transparent' : 'var(--ink)',
      }}
    />
  )
}

/** Tela cheia enquanto a sessão é resolvida (`RequireAuth` / `RequireGuest`). */
export function FullScreenLoader() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg">
      <Spinner size={26} />
    </div>
  )
}
