/*
 * PAINEL "ESTRELA SELECIONADA" (spec 02 §2.12) — o cartão lateral da resolução clicada.
 *
 * Traduz um ponto do gráfico de volta para o que ele é: uma resolução, com data, métricas e
 * autonomia. Fora do SVG, à direita (a página compõe a grade `1.62fr 1fr`, gap 14px).
 *
 * Regras que este cartão carrega inteiras:
 *   3 — MEDIDO (quadrado CHEIO) vs. ≈ ESTIMADO (quadrado VAZADO + prefixo `≈`). O espaço é
 *       estimado por natureza (análise estática); a ciclomática é MEDIDA (contagem exata no
 *       AST) e por isso não tem quadrado nem cor: é `ink` puro.
 *   4 — autonomia é NEUTRA: `AutonomyMeter`, nunca colormap.
 *   6 — "sem métrica" é um estado de primeira classe: `—` em `soft`, jamais um valor inventado.
 */
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { StatusChip } from '@/components/domain/badges'
import { PREFIXO_ESTIMADO, corDaClasse, rotuloCanonico, tintaDaClasse } from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'
import { cn } from '@/lib/utils'
import { dataCompleta } from './escalas'
import type { PontoPlotavel } from './tipos'

export interface PainelEstrelaSelecionadaProps {
  /** `null` → estado de repouso ("clique numa estrela"), com a mesma altura: nada pula. */
  ponto: PontoPlotavel | null | undefined
  /**
   * Rota da resolução. Padrão: a do aluno dono (`/app/resolucoes/:id`). O portfólio público
   * passa a sua (`/u/:usuarioId/desafios/:desafioId/resolucoes/:id`).
   */
  hrefResolucao?: (ponto: PontoPlotavel) => string
  className?: string
}

const CARTAO =
  'flex flex-col gap-[13px] rounded-ci border border-line bg-recess px-4 py-[15px]'

const CABECALHO = 'font-mono text-[10.5px] uppercase tracking-[.1em] text-mid'

function hrefPadrao(ponto: PontoPlotavel): string {
  return `/app/resolucoes/${ponto.resolucaoId}`
}

export function PainelEstrelaSelecionada({
  ponto,
  hrefResolucao = hrefPadrao,
  className,
}: PainelEstrelaSelecionadaProps) {
  const { theme } = useTheme()

  if (!ponto) {
    return (
      <aside className={cn(CARTAO, 'justify-center', className)}>
        <span className={CABECALHO}>Estrela selecionada</span>
        <p className="text-[12.5px] leading-[1.5] text-soft">
          Clique numa estrela para ver a resolução por trás dela: complexidade, autonomia e código.
        </p>
      </aside>
    )
  }

  const escuro = theme === 'dark'
  const estimadoTempo = ponto.confiancaTempo === 'ESTIMADO'

  return (
    <aside className={cn(CARTAO, className)} aria-live="polite">
      {/* ── Identificação ───────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-2">
        <span className={CABECALHO}>Estrela selecionada</span>
        <StatusChip status={ponto.visibilidade === 'PUBLICO' ? 'publico' : 'privado'} />
      </div>

      <div className="flex flex-col gap-1">
        <h3 className="text-[17px] font-semibold leading-[1.3] text-ink">
          {escuro && (
            <span aria-hidden className="mr-1.5 text-mid">
              ✦
            </span>
          )}
          {ponto.desafioTitulo}
        </h3>
        <span className="tabular font-mono text-[11px] text-soft">
          enviada {dataCompleta(ponto.submetidaEm)}
        </span>
      </div>

      {/* ── Métricas ────────────────────────────────────────────────────────── */}
      <dl className="flex flex-col gap-[9px] border-t border-line pt-3">
        <LinhaMetrica rotulo="tempo">
          <ValorClasse
            k={ponto.k}
            texto={rotuloCanonico(ponto.k)}
            estimado={estimadoTempo}
            tema={theme}
          />
        </LinhaMetrica>

        <LinhaMetrica rotulo="espaço">
          {ponto.kEspaco === null ? (
            <SemMetrica />
          ) : (
            /* O espaço sai da mesma análise estática do tempo: é ESTIMADO por natureza. */
            <ValorClasse
              k={ponto.kEspaco}
              texto={rotuloCanonico(ponto.kEspaco)}
              estimado
              tema={theme}
            />
          )}
        </LinhaMetrica>

        <LinhaMetrica rotulo="ciclomática">
          {ponto.ciclomatica === null ? (
            <SemMetrica />
          ) : (
            /* Contagem exata (McCabe) — MEDIDA: sem `≈`, sem cor, sem quadrado de classe. */
            <span className="tabular font-mono text-[15px] font-semibold text-ink">
              M = {ponto.ciclomatica}
            </span>
          )}
        </LinhaMetrica>
      </dl>

      {/* ── Autonomia (neutra) + ação ───────────────────────────────────────── */}
      <div className="flex flex-col gap-2 border-t border-line pt-3">
        <span className={CABECALHO}>Autonomia IA</span>
        <AutonomyMeter value={ponto.autonomia} size="md" />
      </div>

      <Link
        to={hrefResolucao(ponto)}
        className="ci-foco-botao -mx-1 inline-flex items-center gap-1 self-start rounded-ci px-1 py-0.5 font-mono text-[12px] text-steel transition-colors hover:text-steel-hover"
      >
        Ver resolução
        <ChevronRight size={14} strokeWidth={2} />
      </Link>
    </aside>
  )
}

// ── Peças ─────────────────────────────────────────────────────────────────────

function LinhaMetrica({ rotulo, children }: { rotulo: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="font-mono text-[11px] text-mid">{rotulo}</dt>
      <dd className="flex items-center gap-2">{children}</dd>
    </div>
  )
}

/** `—` em `soft`: a ausência de métrica é dita, nunca disfarçada de zero. */
function SemMetrica() {
  return <span className="font-mono text-[15px] font-semibold text-soft">—</span>
}

/**
 * Valor de classe do colormap: quadrado 8×8 + rótulo na tinta da classe.
 * CHEIO = MEDIDO · VAZADO + `≈` = ESTIMADO (regra 3, a mesma gramática do `ConfidenceChip`).
 */
function ValorClasse({
  k,
  texto,
  estimado,
  tema,
}: {
  k: number
  texto: string
  estimado: boolean
  tema: 'dark' | 'light'
}) {
  const cor = corDaClasse(k, tema)

  return (
    <span
      className="flex items-center gap-2 font-mono text-[15px] font-semibold"
      style={{ color: tintaDaClasse(k, tema) }}
      title={estimado ? 'Estimado por análise estática' : 'Medido no AST'}
    >
      <span
        aria-hidden
        style={{
          width: 8,
          height: 8,
          flex: '0 0 auto',
          background: estimado ? 'transparent' : cor,
          border: estimado ? `1.5px solid ${cor}` : undefined,
        }}
      />
      {estimado ? `${PREFIXO_ESTIMADO}${texto}` : texto}
    </span>
  )
}
