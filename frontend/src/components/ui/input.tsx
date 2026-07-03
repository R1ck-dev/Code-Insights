import { forwardRef, useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { cn } from '@/lib/utils'

const fieldBase =
  'w-full bg-input border border-border rounded-[9px] text-fg placeholder:text-subtle outline-none transition-colors focus-visible:border-brand focus-visible:ring-[3px] focus-visible:ring-brand/25 aria-[invalid=true]:border-danger disabled:opacity-60'

export const Input = forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  function Input({ className, ...props }, ref) {
    return (
      <input
        ref={ref}
        className={cn(fieldBase, 'h-[42px] px-[13px] text-sm', className)}
        {...props}
      />
    )
  },
)

export const Textarea = forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement>
>(function Textarea({ className, ...props }, ref) {
  return (
    <textarea
      ref={ref}
      className={cn(fieldBase, 'min-h-[80px] px-3 py-[11px] text-sm leading-relaxed resize-y', className)}
      {...props}
    />
  )
})

/** Input de senha com botão mostrar/ocultar (olho), como no design. */
export const PasswordInput = forwardRef<
  HTMLInputElement,
  React.InputHTMLAttributes<HTMLInputElement>
>(function PasswordInput({ className, ...props }, ref) {
  const [show, setShow] = useState(false)
  return (
    <div className="relative flex items-center">
      <input
        ref={ref}
        type={show ? 'text' : 'password'}
        className={cn(fieldBase, 'h-[42px] pl-[13px] pr-[42px] text-sm', className)}
        {...props}
      />
      <button
        type="button"
        onClick={() => setShow((s) => !s)}
        className="absolute right-3 text-subtle hover:text-muted cursor-pointer"
        aria-label={show ? 'Ocultar senha' : 'Mostrar senha'}
        tabIndex={-1}
      >
        {show ? <EyeOff size={16} /> : <Eye size={16} />}
      </button>
    </div>
  )
})
