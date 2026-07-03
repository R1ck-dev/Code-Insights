import * as SwitchPrimitive from '@radix-ui/react-switch'
import { cn } from '@/lib/utils'

export function Switch({
  className,
  ...props
}: React.ComponentProps<typeof SwitchPrimitive.Root>) {
  return (
    <SwitchPrimitive.Root
      className={cn(
        'relative inline-flex h-[27px] w-[46px] shrink-0 cursor-pointer items-center rounded-full ring-1 ring-inset ring-white/10 transition-colors outline-none',
        'focus-visible:ring-2 focus-visible:ring-ring/60',
        'data-[state=checked]:bg-brand data-[state=unchecked]:bg-border-strong',
        className,
      )}
      {...props}
    >
      <SwitchPrimitive.Thumb className="pointer-events-none block h-[21px] w-[21px] translate-x-[3px] rounded-full bg-white shadow-[0_1px_3px_rgba(0,0,0,.4)] transition-transform data-[state=checked]:translate-x-[22px]" />
    </SwitchPrimitive.Root>
  )
}
