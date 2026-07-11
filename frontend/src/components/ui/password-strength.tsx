import { cn } from '@/lib/utils'

export type NivelSenha = 'fraca' | 'media' | 'forte' | 'muito-forte'

const NIVEIS: Record<NivelSenha, { preenchidos: number; rotulo: string; barra: string; texto: string }> = {
  fraca: { preenchidos: 1, rotulo: 'Fraca', barra: 'bg-erro-estrutura', texto: 'text-erro-texto' },
  media: { preenchidos: 2, rotulo: 'Média', barra: 'bg-atencao', texto: 'text-atencao-ink' },
  forte: { preenchidos: 3, rotulo: 'Forte', barra: 'bg-sucesso', texto: 'text-sucesso-ink' },
  'muito-forte': {
    preenchidos: 4,
    rotulo: 'Muito forte',
    barra: 'bg-sucesso',
    texto: 'text-sucesso-ink',
  },
}

const SEGMENTOS = [0, 1, 2, 3]

export interface PasswordStrengthProps {
  level: NivelSenha
  className?: string
}

/**
 * Medidor de força de senha (spec 03 §Medidor): 4 segmentos `h5 · raio 2px`,
 * vazio = `autonomia-off` (#1B2433 no escuro), rótulo mono 11/600 na cor do nível.
 * O CÁLCULO do nível é da página — este componente só desenha.
 */
export function PasswordStrength({ level, className }: PasswordStrengthProps) {
  const { preenchidos, rotulo, barra, texto } = NIVEIS[level]

  return (
    <div
      className={cn('flex items-center gap-[9px]', className)}
      role="status"
      aria-label={`Força da senha: ${rotulo}`}
    >
      <div className="flex flex-1 gap-[5px]">
        {SEGMENTOS.map((i) => (
          <span
            key={i}
            aria-hidden
            className={cn(
              'h-[5px] flex-1 rounded-ci-sm',
              i < preenchidos ? barra : 'bg-autonomia-off',
            )}
          />
        ))}
      </div>
      <span
        className={cn(
          'font-mono text-[11px] leading-none font-semibold whitespace-nowrap',
          texto,
        )}
      >
        {rotulo}
      </span>
    </div>
  )
}
