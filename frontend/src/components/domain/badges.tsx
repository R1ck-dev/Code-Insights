/*
 * Chips de domínio do sistema Órbita.
 *
 * Regra 1: a única cor é o colormap de complexidade (BigOChip) e os semânticos
 * (sucesso/atenção/erro). Nada de cor de marca.
 * Regra 3: MEDIDO (marcador CHEIO) vs. ≈ ESTIMADO (marcador VAZADO + prefixo ≈)
 * são sempre distinguíveis — nunca esconder a incerteza.
 * Forma: raio 3px, hairline 1px, mono MEDE (todos os chips são mono).
 */
import { Check, Lock } from 'lucide-react'
import { cn } from '@/lib/utils'
import {
  type Confianca,
  LINGUAGEM_COR_FALLBACK,
  LINGUAGEM_META,
  ROTULO_DESCONHECIDO,
  STATUS_CONTA_META,
  type Tone,
  bordaTonal,
  corDaClasse,
  ehPlotavel,
  fundoTonal,
  rotuloCanonico,
  tintaDaClasse,
} from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'
import type { LinguagemProgramacao, StatusConta } from '@/types/api'

/** Base de todo chip: inline-flex, raio 3px, hairline 1px, mono. */
const CHIP = 'inline-flex shrink-0 items-center rounded-ci border font-mono'

// ---------------------------------------------------------------- linguagem

export function LanguageDot({
  lang,
  size = 7,
}: {
  lang: LinguagemProgramacao
  size?: number
}) {
  return (
    <span
      className="inline-block shrink-0 rounded-full"
      style={{
        width: size,
        height: size,
        background: LINGUAGEM_META[lang]?.color ?? LINGUAGEM_COR_FALLBACK,
      }}
    />
  )
}

/** Chip de linguagem: ponto 7px na cor da linguagem + rótulo mono 12/500. */
export function LangChip({
  lang,
  className,
}: {
  lang: LinguagemProgramacao
  className?: string
}) {
  return (
    <span
      className={cn(CHIP, 'gap-[7px] border-line bg-recess px-2.5 py-1 text-[12px] text-body', className)}
    >
      <LanguageDot lang={lang} />
      {LINGUAGEM_META[lang]?.label ?? lang}
    </span>
  )
}

// ------------------------------------------------------------------- Big-O

export interface BigOChipProps {
  /** Ordem da classe (0..7) vinda do backend. `null`/`-1` → chip neutro `?`. */
  k: number | null | undefined
  confianca: Confianca
  /** Fundo `rgba(cor,.09)` + borda `rgba(cor,.30)` no lugar do chip neutro. */
  tonal?: boolean
  /** Texto do `title` (ex.: "confiança do motor: alta" — o eixo SECUNDÁRIO). */
  title?: string
  className?: string
}

/** Classe de complexidade: quadrado 7×7 na cor da classe + rótulo na tinta da classe. */
export function BigOChip({ k, confianca, tonal, title, className }: BigOChipProps) {
  const { theme } = useTheme()
  const temClasse = ehPlotavel(k)
  const rotulo = temClasse ? rotuloCanonico(k) : ROTULO_DESCONHECIDO
  const estimado = confianca === 'ESTIMADO'

  return (
    <span
      title={title}
      className={cn(CHIP, 'gap-1.5 px-[9px] py-1 text-[12px] font-semibold', className)}
      style={{
        color: tintaDaClasse(k, theme),
        background: tonal && temClasse ? fundoTonal(k, theme) : 'var(--recess)',
        borderColor: tonal && temClasse ? bordaTonal(k, theme) : 'var(--line)',
      }}
    >
      <span
        aria-hidden
        style={{ width: 7, height: 7, background: corDaClasse(k, theme), flex: '0 0 auto' }}
      />
      {estimado ? `≈ ${rotulo}` : rotulo}
    </span>
  )
}

// -------------------------------------------------------------- confiança

export interface ConfidenceChipProps {
  tipo: Confianca
  /** Versão do tile de métrica: mono 9/600, sem preenchimento. */
  compact?: boolean
  className?: string
}

/**
 * MEDIDO = quadrado CHEIO (ink). ≈ ESTIMADO = quadrado VAZADO (1.5px solid mid)
 * + prefixo `≈`. A distinção é semântica e inegociável (regra 3).
 */
export function ConfidenceChip({ tipo, compact, className }: ConfidenceChipProps) {
  const medido = tipo === 'MEDIDO'
  const cor = medido ? 'var(--ink)' : 'var(--mid)'
  const lado = compact ? 6 : 7
  const rotulo = medido ? 'MEDIDO' : compact ? '≈ EST.' : '≈ ESTIMADO'

  return (
    <span
      className={cn(
        CHIP,
        'border-line-strong font-semibold uppercase',
        compact
          ? 'gap-[5px] px-1.5 py-0.5 text-[9px] tracking-[.05em]'
          : 'gap-[7px] bg-recess px-2.5 py-1 text-[11px] tracking-[.06em]',
        className,
      )}
      style={{ color: cor }}
      title={medido ? 'Contagem direta no AST' : 'Inferido por análise estática — pode divergir do pior caso real'}
    >
      <span
        aria-hidden
        style={{
          width: lado,
          height: lado,
          flex: '0 0 auto',
          background: medido ? cor : 'transparent',
          border: medido ? undefined : `1.5px solid ${cor}`,
        }}
      />
      {rotulo}
    </span>
  )
}

// ----------------------------------------------------------------- status

export type StatusResolucao = 'publico' | 'privado' | 'calculando' | 'analisada'

export interface StatusChipProps {
  status: StatusResolucao
  /** `calculando` → `calc.` (usado no card do dashboard). */
  compact?: boolean
  className?: string
}

/** Spinner de 9px do estado "calculando" — o único do sistema (ciSpin .8s). */
function SpinnerChip() {
  return (
    <span
      aria-hidden
      className="ci-spin-chip inline-block shrink-0 rounded-full"
      style={{
        width: 9,
        height: 9,
        border: '1.5px solid currentColor',
        borderTopColor: 'transparent',
      }}
    />
  )
}

export function StatusChip({ status, compact, className }: StatusChipProps) {
  if (status === 'analisada') {
    // Sem caixa: só o check verde + rótulo (spec 04 §1.5).
    return (
      <span
        className={cn(
          'inline-flex shrink-0 items-center gap-[5px] font-mono text-[11px] text-sucesso-ink',
          className,
        )}
      >
        <Check size={12} strokeWidth={2} aria-hidden />
        analisada
      </span>
    )
  }

  if (status === 'calculando') {
    return (
      <span
        className={cn(
          CHIP,
          'gap-1.5 border-line bg-recess px-2 py-0.5 text-[11px] font-semibold text-ink',
          className,
        )}
      >
        <SpinnerChip />
        {compact ? 'calc.' : 'calculando'}
      </span>
    )
  }

  if (status === 'publico') {
    return (
      <span
        className={cn(
          CHIP,
          'gap-[7px] border-sucesso-line bg-sucesso-bg px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[.06em] text-sucesso-ink',
          className,
        )}
      >
        <span
          aria-hidden
          className="shrink-0 rounded-full bg-sucesso"
          style={{ width: 6, height: 6 }}
        />
        Público
      </span>
    )
  }

  return (
    <span
      className={cn(
        CHIP,
        'gap-[7px] border-line-strong bg-recess px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[.06em] text-mid',
        className,
      )}
    >
      <Lock size={11} strokeWidth={2} aria-hidden />
      Privado
    </span>
  )
}

// ------------------------------------------------------- status da conta

const TOM_CONTA: Record<Tone, string> = {
  neutro: 'border-line-strong bg-recess text-mid',
  sucesso: 'border-sucesso-line bg-sucesso-bg text-sucesso-ink',
  atencao: 'border-atencao-line bg-atencao-bg text-atencao-ink',
  erro: 'border-erro-line bg-erro-bg text-erro-texto',
  info: 'border-line-strong bg-recess text-ink',
}

const PONTO_CONTA: Record<Tone, string> = {
  neutro: 'bg-mid',
  sucesso: 'bg-sucesso',
  atencao: 'bg-atencao',
  erro: 'bg-erro-estrutura',
  info: 'bg-ink',
}

/** Chip do status da conta (tela P · Perfil e tela F · Verifique o e-mail). */
export function StatusContaChip({
  status,
  className,
}: {
  status: StatusConta
  className?: string
}) {
  const meta = STATUS_CONTA_META[status]
  return (
    <span
      className={cn(
        CHIP,
        'gap-[7px] px-2.5 py-1 text-[11px] font-semibold',
        TOM_CONTA[meta.tone],
        className,
      )}
    >
      <span
        aria-hidden
        className={cn('shrink-0 rounded-full', PONTO_CONTA[meta.tone])}
        style={{ width: 6, height: 6 }}
      />
      {meta.label}
    </span>
  )
}
