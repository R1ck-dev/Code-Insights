import * as TooltipPrimitive from '@radix-ui/react-tooltip'
import { cn } from '@/lib/utils'

/*
 * Tooltip · ÓRBITA — pele do callout (01 §7.8): superfície `elevated`,
 * hairline, raio 3px, padding 8/10 e `shadow-callout` (sombra neutra).
 * O Provider já está montado em `main.tsx`.
 */

export const TooltipProvider = TooltipPrimitive.Provider
export const Tooltip = TooltipPrimitive.Root
export const TooltipTrigger = TooltipPrimitive.Trigger

export function TooltipContent({
  className,
  sideOffset = 6,
  ...props
}: React.ComponentProps<typeof TooltipPrimitive.Content>) {
  return (
    <TooltipPrimitive.Portal>
      <TooltipPrimitive.Content
        sideOffset={sideOffset}
        className={cn(
          'z-50 max-w-[260px] rounded-ci border border-line-strong bg-elevated px-2.5 py-2',
          'text-[12px] leading-[1.45] text-body shadow-callout',
          className,
        )}
        {...props}
      />
    </TooltipPrimitive.Portal>
  )
}
