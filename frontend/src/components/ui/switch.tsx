import * as SwitchPrimitive from '@radix-ui/react-switch'
import { cn } from '@/lib/utils'

/*
 * Switch / Toggle · ÓRBITA (00-INDICE §3.1 · 04 §8.3)
 * 46×27 · raio 999px — ÚNICA exceção ao raio 3px de todo o sistema (affordance
 * de switch, decisão explícita). Ligado `#4FB477` (sucesso) · desligado `#1B2433`
 * + borda `line-strong` · knob 21px na cor do céu (`bg`).
 * A borda existe nos dois estados (na cor da trilha quando ligado) para que a
 * geometria do knob não mude 1px ao alternar.
 */

export function Switch({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      className={cn(
        'ci-foco-botao relative inline-flex h-[27px] w-[46px] shrink-0 cursor-pointer items-center',
        'rounded-ci-pill border outline-none transition-colors duration-[180ms]',
        'data-[state=checked]:border-sucesso data-[state=checked]:bg-sucesso',
        'data-[state=unchecked]:border-line-strong data-[state=unchecked]:bg-autonomia-off',
        'disabled:cursor-not-allowed disabled:opacity-55',
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb
        className={cn(
          'pointer-events-none block h-[21px] w-[21px] rounded-ci-pill bg-panel shadow-[0_1px_3px_rgba(0,0,0,.35)]',
          'translate-x-[2px] transition-transform duration-[180ms] data-[state=checked]:translate-x-[21px]',
          'dark:bg-bg dark:shadow-[0_1px_3px_rgba(0,0,0,.5)]',
        )}
      />
    </SwitchPrimitive.Root>
  )
}
