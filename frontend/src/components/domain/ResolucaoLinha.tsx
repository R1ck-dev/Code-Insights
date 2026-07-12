/*
 * Linha de resolução — lista da tela M (detalhe do desafio) e da tela K (desafio público).
 *
 * A linha inteira é um <Link> de verdade. Slots, na ordem do protótipo:
 *   LangChip · rótulo · AutonomyMeter (sm) · métrica · analisada · data
 *
 * Regras que o componente carrega para nenhuma tela reinventar:
 *  - MÉTRICA SÓ EXISTE PARA JAVA (§4.4): outra linguagem → `sem métrica` em mono 10.5 soft.
 *    Nunca um valor vazio, nunca um `—` solto.
 *  - `analisada === false` (Java) → chip `calculando` com spinner.
 *  - `tempoOrdem === -1` → o motor rodou e NÃO classificou: BigOChip neutro `?` (≠ sem métrica).
 *  - `tempoOrdem === undefined` → A LISTA NÃO CARREGOU A MÉTRICA (não é o mesmo que não haver
 *    métrica!): `—` neutro. Dizer "sem métrica" aqui seria mentir sobre o dado.
 *  - Big-O é ≈ ESTIMADO por natureza (`TIPO_METRICA_META`) — nunca derivado do `NivelConfianca`.
 *  - Autonomia é NEUTRA (osso/tinta), nunca colormap.
 */
import { Link } from 'react-router-dom'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { BigOChip, LangChip, StatusChip } from '@/components/domain/badges'
import { Skeleton } from '@/components/ui/skeleton'
import {
  CONFIANCA_BIG_O,
  LINGUAGEM_COM_METRICAS,
  ROTULO_SEM_METRICA,
  rotuloConfiancaMotor,
} from '@/domain/enums'
import { cn, formatDate, formatDayMonth } from '@/lib/utils'
import type { LinguagemProgramacao, NivelConfianca } from '@/types/api'

/** Texto canônico do estado "sem métrica" (mono 10.5px `soft`). */
export function SemMetrica({ className }: { className?: string }) {
  return (
    <span
      className={cn('shrink-0 font-mono text-[10.5px] text-soft', className)}
      title="Esta linguagem não tem analisador de complexidade"
    >
      {ROTULO_SEM_METRICA}
    </span>
  )
}

/**
 * "A métrica existe, mas esta lista não a carregou" — estado honesto de DESCONHECIMENTO.
 * A lista de resoluções do backend não traz complexidade; quem tem o dado é a carta celeste
 * (e o portfólio público não tem acesso a ela). Renderizar `sem métrica` aqui afirmaria uma
 * ausência que não existe — o visitante clica na linha e vê `≈ O(n²)` na tela seguinte.
 */
function MetricaNaoCarregada() {
  return (
    <span
      className="shrink-0 font-mono text-[10.5px] text-soft"
      title="Métrica não exibida nesta lista — abra a resolução para ver o retrato completo"
    >
      —
    </span>
  )
}

export interface MetricaSlotProps {
  linguagem: LinguagemProgramacao
  analisada: boolean
  /**
   * `k` do colormap (0..7) · `-1` o motor não classificou · `null` não há métrica ·
   * `undefined` NÃO SABEMOS (o dado não veio nesta lista).
   */
  tempoOrdem?: number | null
  /** Confiança do MOTOR (eixo secundário) — vira `title`, nunca preenche marcador. */
  confiancaTempo?: NivelConfianca | null
  /** A métrica está sendo buscada agora (ex.: a carta celeste ainda carregando). */
  carregandoMetrica?: boolean
  /** Chip Big-O tonalizado (fundo/borda na cor da classe). */
  tonal?: boolean
  /** `calculando` → `calc.` (dashboard). */
  compact?: boolean
}

/**
 * O slot de métrica de qualquer lista de resoluções. Reusado pela AtividadeLinha —
 * a decisão "chip Big-O × calculando × sem métrica × não sei" mora aqui, e só aqui.
 * A ordem dos testes é a ordem das CAUSAS.
 */
export function MetricaSlot({
  linguagem,
  analisada,
  tempoOrdem,
  confiancaTempo,
  carregandoMetrica,
  tonal,
  compact,
}: MetricaSlotProps) {
  // 1. Linguagem sem analisador: não haverá métrica, ponto final (§4.4).
  if (linguagem !== LINGUAGEM_COM_METRICAS) return <SemMetrica />
  // 2. Java, análise assíncrona em curso.
  if (!analisada) return <StatusChip status="calculando" compact={compact} />
  // 3. Java, analisada — mas o valor ainda está sendo buscado: esqueleto, jamais "sem métrica".
  if (carregandoMetrica) return <Skeleton className="h-[22px] w-[74px] shrink-0" />
  // 4. O dado não veio nesta lista: `—`. Não sabemos ≠ não existe.
  if (tempoOrdem === undefined) return <MetricaNaoCarregada />
  // 5. Analisada em Java e ainda assim sem classe de tempo: aí sim é ausência real.
  if (tempoOrdem === null) return <SemMetrica />
  // 6. Tem valor (inclusive `-1` → chip neutro `?`). Big-O é sempre ≈ ESTIMADO.
  return (
    <BigOChip
      k={tempoOrdem}
      confianca={CONFIANCA_BIG_O}
      tonal={tonal}
      title={rotuloConfiancaMotor(confiancaTempo) ?? undefined}
    />
  )
}

export interface ResolucaoLinhaProps {
  /** Destino: `/app/resolucoes/:id` (tela M) ou `/u/:autorId/…/resolucoes/:id` (tela K). */
  to: string
  linguagem: LinguagemProgramacao
  /** Índice de Autonomia IA autodeclarado (1–5). */
  autonomia: number
  analisada: boolean
  /**
   * `k` (0..7) · `-1` não classificado · `null` sem métrica · `undefined` não carregado aqui.
   */
  tempoOrdem?: number | null
  confiancaTempo?: NivelConfianca | null
  /** A fonte da métrica ainda está carregando → esqueleto no slot (nunca "sem métrica"). */
  carregandoMetrica?: boolean
  /** ISO da submissão. */
  submetidaEm: string
  /** Rótulo curto da tentativa (`Força bruta`, `HashMap otimizado`). Sem ele, cai no fallback mono do ID curto. */
  rotulo?: string | null
  /** `lista` (tela M): linha dentro do painel, hairline no topo a partir da 2ª. `cartao` (tela K): card solto. */
  variant?: 'lista' | 'cartao'
  /** `curta` = `dd/mm` (tela M) · `longa` = `dd/mm/aaaa` (tela K). */
  dataFormato?: 'curta' | 'longa'
  className?: string
}

export function ResolucaoLinha({
  to,
  linguagem,
  autonomia,
  analisada,
  tempoOrdem,
  confiancaTempo,
  carregandoMetrica,
  submetidaEm,
  rotulo,
  variant = 'lista',
  dataFormato = 'curta',
  className,
}: ResolucaoLinhaProps) {
  const cartao = variant === 'cartao'
  const data = dataFormato === 'longa' ? formatDate(submetidaEm) : formatDayMonth(submetidaEm)

  return (
    <Link
      to={to}
      className={cn(
        'group flex items-center gap-3.5 px-4 py-[13px] transition-colors',
        cartao
          ? 'rounded-ci border border-line bg-panel hover:border-line-strong'
          : 'border-t border-line-soft first:border-t-0 hover:bg-elevated',
        className,
      )}
    >
      <LangChip lang={linguagem} className="text-[11px]" />

      <span className="min-w-0 flex-1 truncate text-[13.5px] text-ink">
        {rotulo || <span className="text-mid">Resolução</span>}
      </span>

      <AutonomyMeter value={autonomia} size="sm" className="hidden shrink-0 sm:inline-flex" />

      <MetricaSlot
        linguagem={linguagem}
        analisada={analisada}
        tempoOrdem={tempoOrdem}
        confiancaTempo={confiancaTempo}
        carregandoMetrica={carregandoMetrica}
        tonal={cartao}
      />

      {analisada && <StatusChip status="analisada" className="hidden lg:inline-flex" />}

      <time
        dateTime={submetidaEm}
        className="tabular shrink-0 text-right font-mono text-[11px] text-soft"
      >
        {data}
      </time>
    </Link>
  )
}
