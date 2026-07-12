/*
 * ÓRBITAS — a visualização polar do portfólio. É o RELÓGIO do aluno.
 *
 *   RAIO   = autonomia (1 no centro = mais apoio de IA · 5 na borda = mais autônomo)
 *   ÂNGULO = TEMPO (§6-A, Lacuna 3): θᵢ = -90° + i·(360°/n), i = índice cronológico
 *            ascendente. 12h = a resolução MAIS ANTIGA; gira em sentido horário.
 *   COR    = classe de complexidade de tempo (o colormap — a única cor do sistema).
 *
 * Assim cada volta conta a história do aluno no tempo, e a distância ao centro mostra a
 * autonomia crescente: uma espiral que se afasta do centro é exatamente o que a pesquisa
 * quer ver.
 *
 * Geometria conferida contra o protótipo: spec 02 §3 (viewBox 300×300, centro (150,150),
 * r = 26·a). Ângulo e interação vêm do contrato (00-INDICE §6-A) — o protótipo distribuía
 * os ângulos a olho e não tinha seleção.
 *
 * Componente PURO: recebe o dataset e desenha. Cabeçalho, seletor, rodapé de honestidade
 * ("18 de 23 resoluções plotadas") e painel da estrela selecionada são do PainelDeGraficos.
 */
import { useId, useMemo, useState } from 'react'
import { Folder } from 'lucide-react'
import { EmptyState } from '@/components/ui/empty-state'
import {
  CONFIANCA_BIG_O,
  comPrefixoEstimado,
  corDaClasse,
  LINGUAGEM_META,
  rotuloCanonico,
  rotuloConfiancaMotor,
  tintaDaClasse,
} from '@/domain/enums'
import { cn } from '@/lib/utils'
import {
  aneisOrbita,
  dataCompleta,
  ORBITA_CENTRO,
  ORBITA_RAIO_MAX,
  ORBITA_VIEWBOX,
  posicaoCallout,
  posicaoOrbital,
} from './escalas'
import type { PontoPlotavel, PropsGrafico } from './tipos'

// ── Marcas (spec 02 §3.3) ───────────────────────────────────────────────────
const HALO_R = 6
const HALO_OPACIDADE = 0.2
const HALO_R_HOVER = 8
const HALO_OPACIDADE_HOVER = 0.32
const NUCLEO_R = 2.4

const SEL_HALO_R = 11
const SEL_ANEL_R = 7.5 // DECISÃO: o "anel" do vocabulário da Carta, aqui lido como órbita
const SEL_NUCLEO_R = 3.2

/** Alvo de clique invisível: o núcleo de 2.4px é pequeno demais para mouse e para toque. */
const ALVO_R = 12

/**
 * REGRA 3 (inviolável): o núcleo CHEIO é reservado à métrica MEDIDA. A classe de tempo é
 * inferida por análise estática → SEMPRE ≈ ESTIMADA → núcleo VAZADO (o mesmo par cheio/vazado
 * da Carta e da Linha). Antes as Órbitas pintavam TODA estrela como cheia — a incerteza sumia
 * do gráfico e só sobrevivia no callout.
 */
const NUCLEO_MEDIDO = CONFIANCA_BIG_O === 'MEDIDO'
/** Espessura do anel do núcleo vazado. */
const NUCLEO_TRACO = 1.3
const SEL_NUCLEO_TRACO = 1.6

/** O raio-guia do tempo (12h) para na borda do anel externo. */
const GUIA_R = ORBITA_RAIO_MAX

/** Céu decorativo — fixo, NÃO é dado (protótipo, spec 02 §3.2). */
const CEU = [
  { cx: 70, cy: 46, o: 0.6 },
  { cx: 250, cy: 70, o: 0.6 },
  { cx: 60, cy: 240, o: 0.5 },
  { cx: 255, cy: 235, o: 0.5 },
] as const

/** Estrelas-fantasma do estado de carregando (posições fixas, autonomias variadas). */
const FANTASMAS: readonly number[] = [2, 4, 3, 5, 1, 3, 5, 2]

export interface PropsOrbitas extends PropsGrafico {
  /** Enquanto a query não resolve: anéis + centro + estrelas-fantasma pulsando. */
  carregando?: boolean
  className?: string
}

export function Orbitas({
  dataset,
  selecionadoId,
  onSelecionar,
  tema,
  carregando = false,
  className,
}: PropsOrbitas) {
  const [hoverId, setHoverId] = useState<string | null>(null)
  const [focoId, setFocoId] = useState<string | null>(null)
  const tituloId = useId()

  const { largura, altura } = ORBITA_VIEWBOX
  const aneis = useMemo(() => aneisOrbita(), [])

  /** A posição de cada ponto: o índice no array JÁ é a ordem cronológica (contrato do dataset). */
  const estrelas = useMemo(() => {
    const n = dataset.pontos.length
    return dataset.pontos.map((ponto, i) => ({
      ponto,
      ...posicaoOrbital(i, n, ponto.autonomia),
    }))
  }, [dataset.pontos])

  const vazio = !carregando && estrelas.length === 0

  const selecionada = estrelas.find((e) => e.ponto.resolucaoId === selecionadoId) ?? null
  const emFoco = estrelas.find((e) => e.ponto.resolucaoId === (hoverId ?? focoId)) ?? null
  // O callout segue o hover/foco; sem eles, a seleção.
  const doCallout = emFoco ?? selecionada

  return (
    <div className={cn('flex w-full flex-col gap-3', className)}>
      <div className="relative w-full">
        <svg
          viewBox={`0 0 ${largura} ${altura}`}
          width="100%"
          role="group"
          aria-labelledby={tituloId}
          className="block h-auto w-full"
        >
          <title id={tituloId}>
            Órbitas — {estrelas.length} resoluções: raio = autonomia, ângulo = tempo (a mais antiga
            às 12h, girando em sentido horário), cor = classe de complexidade.
          </title>

          {/* ── Cenário: céu decorativo (não é dado) ───────────────────────── */}
          <g aria-hidden="true">
            {CEU.map((estrela) => (
              <circle
                key={`${estrela.cx}-${estrela.cy}`}
                cx={estrela.cx}
                cy={estrela.cy}
                r={tema === 'dark' ? 0.8 : 1.1}
                className="fill-ink"
                opacity={estrela.o}
              />
            ))}
          </g>

          {/* ── Anéis de autonomia (1..5) + rótulos no eixo do tempo ────────── */}
          <g>
            {aneis.map((anel) => (
              <circle
                key={anel.a}
                cx={ORBITA_CENTRO.x}
                cy={ORBITA_CENTRO.y}
                r={anel.r}
                fill="none"
                strokeWidth={1}
                className={anel.externo ? 'stroke-line' : 'stroke-graf-eixo'}
              />
            ))}

            {/* Raio-guia: marca o início do tempo (12h = a resolução mais antiga). */}
            <line
              x1={ORBITA_CENTRO.x}
              y1={ORBITA_CENTRO.y}
              x2={ORBITA_CENTRO.x}
              y2={ORBITA_CENTRO.y - GUIA_R}
              strokeWidth={1}
              className="stroke-graf-eixo"
            />
            <text
              x={ORBITA_CENTRO.x}
              y={12}
              textAnchor="middle"
              fontSize={9}
              className="fill-soft font-mono"
            >
              t₀
            </text>

            {/* Rótulos dos anéis: à esquerda do raio-guia, que vira o eixo da autonomia. */}
            {aneis.map((anel) => (
              <text
                key={`rotulo-${anel.a}`}
                x={ORBITA_CENTRO.x - 12}
                y={anel.rotuloY}
                textAnchor="end"
                fontSize={10}
                className={cn('font-mono tabular', anel.externo ? 'fill-mid' : 'fill-soft')}
              >
                {anel.a}
              </text>
            ))}

            {/* Centro = dependência total (autonomia mínima). */}
            <circle cx={ORBITA_CENTRO.x} cy={ORBITA_CENTRO.y} r={8} className="fill-ink" opacity={0.14} />
            <circle cx={ORBITA_CENTRO.x} cy={ORBITA_CENTRO.y} r={4} className="fill-ink" />
          </g>

          {/* ── Carregando: estrelas-fantasma ──────────────────────────────── */}
          {carregando && (
            <g aria-hidden="true">
              {FANTASMAS.map((autonomia, i) => {
                const p = posicaoOrbital(i, FANTASMAS.length, autonomia)
                return (
                  <circle
                    key={`fantasma-${i}`}
                    cx={p.x}
                    cy={p.y}
                    r={NUCLEO_R}
                    className="ci-pulse fill-line"
                  />
                )
              })}
            </g>
          )}

          {/* ── Estrelas (não selecionadas) ────────────────────────────────── */}
          <g>
            {estrelas.map(({ ponto, x, y }) => {
              if (ponto.resolucaoId === selecionadoId) return null
              const cor = corDaClasse(ponto.k, tema)
              const ativa = ponto.resolucaoId === hoverId || ponto.resolucaoId === focoId
              return (
                <g key={ponto.resolucaoId}>
                  <circle
                    cx={x}
                    cy={y}
                    r={ativa ? HALO_R_HOVER : HALO_R}
                    fill={cor}
                    opacity={ativa ? HALO_OPACIDADE_HOVER : tema === 'dark' ? HALO_OPACIDADE : 0.18}
                    className="transition-[r,opacity] duration-150 motion-reduce:transition-none"
                  />
                  {/* MEDIDO = disco cheio · ≈ ESTIMADO = anel vazado (regra 3). */}
                  {NUCLEO_MEDIDO ? (
                    <circle cx={x} cy={y} r={NUCLEO_R} fill={cor} />
                  ) : (
                    <circle
                      cx={x}
                      cy={y}
                      r={NUCLEO_R + 0.2}
                      fill="none"
                      stroke={cor}
                      strokeWidth={NUCLEO_TRACO}
                    />
                  )}
                  {ponto.resolucaoId === focoId && (
                    <circle
                      cx={x}
                      cy={y}
                      r={ALVO_R}
                      fill="none"
                      strokeWidth={1}
                      className="stroke-ink"
                      opacity={0.55}
                    />
                  )}
                </g>
              )
            })}
          </g>

          {/* ── Estrela selecionada (por cima) + vetor-raio ─────────────────── */}
          {selecionada && (
            <g>
              <line
                x1={ORBITA_CENTRO.x}
                y1={ORBITA_CENTRO.y}
                x2={selecionada.x}
                y2={selecionada.y}
                stroke={corDaClasse(selecionada.ponto.k, tema)}
                strokeWidth={1}
                strokeDasharray="3 3"
                opacity={tema === 'dark' ? 0.5 : 0.6}
              />
              <circle
                cx={selecionada.x}
                cy={selecionada.y}
                r={SEL_HALO_R}
                fill={corDaClasse(selecionada.ponto.k, tema)}
                opacity={tema === 'dark' ? 0.22 : 0.2}
              />
              <circle
                cx={selecionada.x}
                cy={selecionada.y}
                r={SEL_ANEL_R}
                fill="none"
                stroke={corDaClasse(selecionada.ponto.k, tema)}
                strokeWidth={1}
                opacity={tema === 'dark' ? 0.5 : 0.6}
              />
              {/* Selecionada: cheia só se a métrica fosse MEDIDA. Estimada = anel (regra 3). */}
              {NUCLEO_MEDIDO ? (
                <circle
                  cx={selecionada.x}
                  cy={selecionada.y}
                  r={SEL_NUCLEO_R}
                  fill="var(--estrela-nucleo)"
                  style={{ color: corDaClasse(selecionada.ponto.k, tema) }}
                />
              ) : (
                <circle
                  cx={selecionada.x}
                  cy={selecionada.y}
                  r={SEL_NUCLEO_R}
                  fill="none"
                  stroke="var(--estrela-nucleo)"
                  strokeWidth={SEL_NUCLEO_TRACO}
                  style={{ color: corDaClasse(selecionada.ponto.k, tema) }}
                />
              )}
            </g>
          )}

          {/* ── Alvos de clique/foco (por cima de tudo) ─────────────────────── */}
          <g>
            {estrelas.map(({ ponto, x, y }) => (
              <circle
                key={`alvo-${ponto.resolucaoId}`}
                cx={x}
                cy={y}
                r={ALVO_R}
                fill="transparent"
                role="button"
                tabIndex={0}
                aria-label={rotuloAcessivel(ponto)}
                aria-pressed={ponto.resolucaoId === selecionadoId}
                className="cursor-pointer outline-none"
                onPointerEnter={() => setHoverId(ponto.resolucaoId)}
                onPointerLeave={() => setHoverId((id) => (id === ponto.resolucaoId ? null : id))}
                onFocus={(e) => {
                  if (e.currentTarget.matches(':focus-visible')) setFocoId(ponto.resolucaoId)
                }}
                onBlur={() => setFocoId((id) => (id === ponto.resolucaoId ? null : id))}
                onClick={() => onSelecionar?.(ponto.resolucaoId)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    onSelecionar?.(ponto.resolucaoId)
                  }
                }}
              />
            ))}
          </g>
        </svg>

        {/* ── Callout (HTML sobre o SVG — spec 02 §2.9, reusado nas Órbitas) ── */}
        {doCallout && !carregando && (
          <Callout ponto={doCallout.ponto} x={doCallout.x} y={doCallout.y} tema={tema} />
        )}

        {/* ── Vazio: mantém anéis + centro e sobrepõe o bloco do sistema ────── */}
        {vazio && (
          <div className="absolute inset-0 grid place-items-center p-3">
            <EmptyState
              size="sm"
              icon={Folder}
              title="Nenhuma resolução analisada ainda"
              description="Submeta uma resolução em Java para ver sua primeira estrela."
              className="bg-recess/85 backdrop-blur-[1px]"
            />
          </div>
        )}
      </div>

      {/* ── Legenda: um polar sem legenda é indecifrável ────────────────────── */}
      <div className="flex flex-col gap-1">
        <p className="font-mono text-[10.5px] leading-none text-soft">
          raio: autonomia · ângulo: tempo · cor: classe
        </p>
        <p className="font-mono text-[9.5px] leading-none text-soft">
          t₀ = 12h (mais antiga) · sentido horário · centro = mais apoio de IA · núcleo vazado = ≈
          estimado
        </p>
      </div>
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// Callout — mesmo vocabulário da Carta, com a DATA (aqui o ângulo é tempo:
// esconder a data seria esconder a coordenada).
// ════════════════════════════════════════════════════════════════════════════

function Callout({
  ponto,
  x,
  y,
  tema,
}: {
  ponto: PontoPlotavel
  x: number
  y: number
  tema: PropsGrafico['tema']
}) {
  const cor = corDaClasse(ponto.k, tema)
  const tinta = tintaDaClasse(ponto.k, tema)
  const pos = posicaoCallout(x, y, ORBITA_VIEWBOX)
  const motor = rotuloConfiancaMotor(ponto.confiancaTempo)

  return (
    <div
      role="status"
      className="pointer-events-none absolute z-10 flex flex-col gap-[3px] whitespace-nowrap rounded-ci border bg-elevated px-[10px] py-2 shadow-callout"
      style={{ ...pos, borderColor: cor }}
    >
      <span className="font-mono text-[11px] font-semibold text-ink">
        {tema === 'dark' ? '✦ ' : ''}
        {ponto.desafioTitulo} · {LINGUAGEM_META[ponto.linguagem].label}
      </span>

      {/* Uma cor por semântica: só a CLASSE veste o colormap. Ciclomática = contagem (mid),
          autonomia = NEUTRA (ink) — regras 1 e 4. */}
      <span className="tabular flex items-center gap-1.5 font-mono text-[10px]">
        <span style={{ color: tinta }}>
          {comPrefixoEstimado(rotuloCanonico(ponto.k), CONFIANCA_BIG_O)}
        </span>
        {ponto.ciclomatica != null && <span className="text-mid">· M={ponto.ciclomatica}</span>}
        <span className="text-ink">· aut {ponto.autonomia}/5</span>
      </span>

      <span className="tabular font-mono text-[9.5px] text-soft">
        enviada {dataCompleta(ponto.submetidaEm)}
        {motor ? ` · ${motor}` : ''}
      </span>
    </div>
  )
}

function rotuloAcessivel(ponto: PontoPlotavel): string {
  const classe = comPrefixoEstimado(rotuloCanonico(ponto.k), CONFIANCA_BIG_O)
  const natureza = CONFIANCA_BIG_O === 'MEDIDO' ? 'medida' : 'estimada por análise estática'
  const ciclo = ponto.ciclomatica != null ? `, ciclomática ${ponto.ciclomatica} (medida)` : ''
  const motor = rotuloConfiancaMotor(ponto.confiancaTempo)
  return (
    `${ponto.desafioTitulo}, ${LINGUAGEM_META[ponto.linguagem].label}, ` +
    `autonomia ${ponto.autonomia} de 5, tempo ${classe} (${natureza}${motor ? `, ${motor}` : ''})${ciclo}, ` +
    `enviada em ${dataCompleta(ponto.submetidaEm)}`
  )
}
