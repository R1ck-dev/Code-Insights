/*
 * Linha de "Atividade recente" — tela C (Dashboard), spec 04 §2.4.
 * Slots: LangChip · título do desafio · AutonomyMeter (sm) · métrica · data.
 *
 * A decisão do slot de métrica (Big-O × calculando × `sem métrica`) é a MESMA da
 * lista de resoluções — vem de `MetricaSlot` (ResolucaoLinha.tsx), não é reescrita aqui.
 */
import { Link } from 'react-router-dom'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { LangChip } from '@/components/domain/badges'
import { MetricaSlot } from '@/components/domain/ResolucaoLinha'
import { cn, formatDate, formatDayMonth } from '@/lib/utils'
import type { LinguagemProgramacao, NivelConfianca } from '@/types/api'

export interface AtividadeLinhaProps {
  /** Destino: `/app/resolucoes/:id`. */
  to: string
  /** Título do desafio a que a resolução pertence. */
  titulo: string
  linguagem: LinguagemProgramacao
  autonomia: number
  analisada: boolean
  /** `k` do colormap (0..7) · `-1` desconhecido · `null` sem dado (`AtividadeRecenteDTO.complexidadeOrdem`). */
  tempoOrdem?: number | null
  /** O `AtividadeRecenteDTO` não traz confiança; sem ela, Big-O de tempo é ≈ ESTIMADO. */
  confiancaTempo?: NivelConfianca | null
  submetidaEm: string
  /** `curta` = `dd/mm` (padrão) · `longa` = `dd/mm/aaaa`. */
  dataFormato?: 'curta' | 'longa'
  className?: string
}

export function AtividadeLinha({
  to,
  titulo,
  linguagem,
  autonomia,
  analisada,
  tempoOrdem,
  confiancaTempo,
  submetidaEm,
  dataFormato = 'curta',
  className,
}: AtividadeLinhaProps) {
  const data = dataFormato === 'longa' ? formatDate(submetidaEm) : formatDayMonth(submetidaEm)

  return (
    <Link
      to={to}
      className={cn(
        'flex items-center gap-3 border-t border-line-soft px-4 py-[11px]',
        'transition-colors first:border-t-0 hover:bg-elevated',
        className,
      )}
    >
      <LangChip lang={linguagem} className="text-[11px]" />

      <span className="min-w-0 flex-1 truncate text-[13px] text-ink">{titulo}</span>

      <AutonomyMeter value={autonomia} size="sm" className="hidden shrink-0 sm:inline-flex" />

      <MetricaSlot
        linguagem={linguagem}
        analisada={analisada}
        tempoOrdem={tempoOrdem}
        confiancaTempo={confiancaTempo}
        compact
      />

      <time
        dateTime={submetidaEm}
        className="tabular shrink-0 text-right font-mono text-[11px] text-soft"
      >
        {data}
      </time>
    </Link>
  )
}
