import * as SelectPrimitive from '@radix-ui/react-select'
import { Check, ChevronDown } from 'lucide-react'
import { cn } from '@/lib/utils'

export const Select = SelectPrimitive.Root
export const SelectValue = SelectPrimitive.Value

export function SelectTrigger({
  className,
  children,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Trigger>) {
  return (
    <SelectPrimitive.Trigger
      className={cn(
        'inline-flex h-10 items-center justify-between gap-2 rounded-[9px] border border-border bg-input px-3 text-[13.5px] text-fg outline-none transition-colors',
        'hover:bg-surface-2 focus-visible:border-brand focus-visible:ring-[3px] focus-visible:ring-brand/25 data-[placeholder]:text-subtle',
        className,
      )}
      {...props}
    >
      {children}
      <SelectPrimitive.Icon asChild>
        <ChevronDown size={15} className="text-subtle" />
      </SelectPrimitive.Icon>
    </SelectPrimitive.Trigger>
  )
}

export function SelectContent({
  className,
  children,
  position = 'popper',
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Content>) {
  return (
    <SelectPrimitive.Portal>
      <SelectPrimitive.Content
        position={position}
        sideOffset={6}
        className={cn(
          'z-50 max-h-[320px] min-w-[var(--radix-select-trigger-width)] overflow-hidden rounded-xl border border-border-strong bg-surface p-1.5 shadow-[0_20px_50px_-20px_rgba(0,0,0,.8)]',
          className,
        )}
        {...props}
      >
        <SelectPrimitive.Viewport className="flex flex-col gap-0.5">
          {children}
        </SelectPrimitive.Viewport>
      </SelectPrimitive.Content>
    </SelectPrimitive.Portal>
  )
}

export function SelectItem({
  className,
  children,
  ...props
}: React.ComponentProps<typeof SelectPrimitive.Item>) {
  return (
    <SelectPrimitive.Item
      className={cn(
        'relative flex cursor-pointer select-none items-center gap-2 rounded-lg py-2 pl-2.5 pr-8 text-[13.5px] text-fg outline-none',
        'focus:bg-surface-2 data-[state=checked]:text-brand-strong data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
        className,
      )}
      {...props}
    >
      <SelectPrimitive.ItemText>{children}</SelectPrimitive.ItemText>
      <SelectPrimitive.ItemIndicator className="absolute right-2.5">
        <Check size={15} />
      </SelectPrimitive.ItemIndicator>
    </SelectPrimitive.Item>
  )
}
