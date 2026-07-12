/*
 * ESPIRAL DO TEMPO (arquivo/export continuam `Orbitas` para não quebrar imports).
 *
 * REDESENHO — o gráfico antigo (raio = autonomia, ângulo = tempo) foi rodado no app e NÃO ERA
 * LEGÍVEL: ângulo não tem zero natural, nada dizia "o tempo anda nesta direção", e a autonomia,
 * virando distância ao centro, brigava com a leitura de trajetória. A leitura nova é a de um
 * DISCO DE ÁRVORE, de dentro para fora:
 *
 *   RAIO    = TEMPO      — centro = a resolução mais ANTIGA · borda = a mais RECENTE.
 *   TRILHA  = a ordem    — uma espiral única e contínua ligando as resoluções em ordem.
 *   TAMANHO = AUTONOMIA  — 1..5 (`raioPorAutonomia_TAMANHO`). ⚠ REGRA 4: autonomia é NEUTRA;
 *                          ela vira TAMANHO, jamais cor de classe.
 *   COR     = CLASSE de complexidade (o colormap — a única cor do sistema, regra 1).
 *
 * A pergunta que o gráfico responde de bate-pronto: "conforme o tempo passa (para fora), meus
 * pontos ficam MAIORES (mais autônomo) e mais VERDES (menos custosos)?"
 *
 * ⚠ NENHUM `<title>` dentro do SVG: o navegador desenha o tooltip NATIVO por cima do callout —
 * dois pop-ups, e o nativo roubando o clique (bug visto no app). Acessibilidade vai por
 * `aria-label`/`role`; o tooltip visual é só o callout deste componente.
 *
 * Componente PURO: recebe o dataset e desenha. Rodapé de honestidade ("18 de 23 plotadas") e
 * painel da resolução selecionada são do PainelDeGraficos.
 */
import { useId, useMemo, useState } from 'react'
import { Folder } from 'lucide-react'
import { EmptyState } from '@/components/ui/empty-state'
import {
  CLASSES,
  CONFIANCA_BIG_O,
  comPrefixoEstimado,
  corDaClasse,
  LINGUAGEM_META,
  NOTA_METRICAS_SO_JAVA,
  rotuloCanonico,
  rotuloConfiancaMotor,
  tintaDaClasse,
} from '@/domain/enums'
import { cn, pluralPt } from '@/lib/utils'
import {
  ESPIRAL_ANEIS,
  ESPIRAL_CENTRO,
  ESPIRAL_R_MAX,
  ESPIRAL_R_MIN,
  ESPIRAL_VIEWBOX,
  aneisDeTempo,
  caminhoDaEspiral,
  dataCompleta,
  dataCurta,
  haloPorAutonomia_TAMANHO,
  limitar,
  posicaoCallout,
  posicaoEspiral,
  raioDoTempo,
  raioPorAutonomia_TAMANHO,
} from './escalas'
import type { PontoPlotavel, PropsGrafico } from './tipos'

// ── Marcas ──────────────────────────────────────────────────────────────────

/**
 * REGRA 3 (inviolável): marcador CHEIO é reservado ao que foi CONTADO. A classe de tempo sai de
 * inferência estática → é SEMPRE `≈ ESTIMADA` → núcleo VAZADO (o mesmo par cheio/vazado da Carta).
 */
const NUCLEO_MEDIDO = CONFIANCA_BIG_O === 'MEDIDO'
const NUCLEO_TRACO = 1.3
const SEL_NUCLEO_TRACO = 1.7

/** Folga do anel de seleção/foco em torno do núcleo (que varia com a autonomia). */
const SEL_ANEL_FOLGA = 4.5
const FOCO_FOLGA = 6.5

/** Alvo de clique/toque: cresce com o ponto, mas nunca menor que o mínimo tocável. */
const ALVO_MIN = 8
const ALVO_MAX = 11.5

/** Espessura do halo do texto (o "recorte" que deixa o rótulo legível sobre a trilha). */
const HALO_TEXTO = 3

/** Estado *carregando*: autonomias fixas — o esqueleto tem a forma do gráfico, sem cor. */
const FANTASMAS: readonly number[] = [2, 4, 3, 5, 1, 3, 5, 2]

/** Espiral-fantasma do vazio/carregando: a forma aparece mesmo sem dado. */
const TRILHA_FANTASMA = caminhoDaEspiral(FANTASMAS.length)

export interface PropsOrbitas extends PropsGrafico {
  /** Enquanto a query não resolve: trilha + anéis + pontos-fantasma pulsando. */
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
  const rotuloId = useId()

  const escuro = tema === 'dark'
  const clicavel = typeof onSelecionar === 'function'
  const { largura, altura } = ESPIRAL_VIEWBOX
  const { pontos } = dataset
  const n = pontos.length

  /** O índice em `dataset.pontos` JÁ é a ordem cronológica (contrato do dataset). */
  const marcadores = useMemo(
    () =>
      pontos.map((ponto, i) => ({
        ponto,
        i,
        ...posicaoEspiral(i, n),
        nucleo: raioPorAutonomia_TAMANHO(ponto.autonomia),
        halo: haloPorAutonomia_TAMANHO(ponto.autonomia),
      })),
    [pontos, n],
  )

  const trilha = useMemo(() => caminhoDaEspiral(n), [n])
  const aneis = useMemo(() => aneisDeTempo(pontos, ESPIRAL_ANEIS), [pontos])

  /** A ponta da trilha, extrapolada 8px na tangente: a seta que diz "o tempo vai para fora". */
  const seta = useMemo(() => {
    if (n < 2) return null
    const fim = posicaoEspiral(n - 1, n)
    const antes = posicaoEspiral(n - 1.25, n)
    const ang = Math.atan2(fim.y - antes.y, fim.x - antes.x)
    return { x: fim.x + Math.cos(ang) * 9, y: fim.y + Math.sin(ang) * 9, graus: (ang * 180) / Math.PI }
  }, [n])

  const primeiro = marcadores[0] ?? null
  const ultimo = marcadores[n - 1] ?? null

  // Hover e foco de teclado revelam a MESMA informação (paridade de entrada); a seleção é o
  // estado persistente e sobrevive ao mouse sair.
  const destacadoId = hoverId ?? focoId ?? selecionadoId ?? null
  const destacado = marcadores.find((m) => m.ponto.resolucaoId === destacadoId) ?? null
  const selecionado = marcadores.find((m) => m.ponto.resolucaoId === selecionadoId) ?? null

  const vazio = !carregando && n === 0
  const soUma = !carregando && n === 1

  return (
    <div className={cn('flex w-full flex-col gap-3', className)}>
      {/* ── O gráfico se apresenta: o que é e como se lê, em uma frase ───────── */}
      <header className="flex flex-col gap-0.5">
        <h4 id={rotuloId} className="text-[12.5px] font-semibold text-ink">
          Espiral do tempo
        </h4>
        <p className="text-[11.5px] leading-[1.45] text-soft">
          Leia de dentro para fora: cada ponto é uma resolução, da mais <b>antiga</b> (centro) à
          mais <b>recente</b> (borda).
        </p>
      </header>

      <div className="grid gap-x-5 gap-y-4 md:grid-cols-[minmax(0,1fr)_212px] md:items-center">
        {/* ── A espiral ─────────────────────────────────────────────────────── */}
        <div className="relative mx-auto w-full max-w-[430px]">
          <svg
            viewBox={`0 0 ${largura} ${altura}`}
            width="100%"
            role="group"
            aria-label={rotuloDoGrafico(marcadores.map((m) => m.ponto))}
            className="block h-auto w-full"
          >
            {/* ── Anéis-guia do TEMPO (o eixo radial) ──────────────────────── */}
            <g aria-hidden className="pointer-events-none">
              {/* Sem dado, os anéis extremos ainda declaram a forma do gráfico. */}
              {(vazio || carregando || soUma) && (
                <>
                  <circle
                    cx={ESPIRAL_CENTRO.x}
                    cy={ESPIRAL_CENTRO.y}
                    r={ESPIRAL_R_MIN}
                    fill="none"
                    strokeWidth={1}
                    strokeDasharray="2 4"
                    className="stroke-graf-eixo"
                  />
                  <circle
                    cx={ESPIRAL_CENTRO.x}
                    cy={ESPIRAL_CENTRO.y}
                    r={ESPIRAL_R_MAX}
                    fill="none"
                    strokeWidth={1}
                    strokeDasharray="2 4"
                    className="stroke-graf-eixo"
                  />
                </>
              )}

              {aneis.map((anel) => (
                <circle
                  key={`anel-${anel.i}`}
                  cx={ESPIRAL_CENTRO.x}
                  cy={ESPIRAL_CENTRO.y}
                  r={anel.r}
                  fill="none"
                  strokeWidth={1}
                  className={anel.externo ? 'stroke-line' : 'stroke-graf-eixo'}
                />
              ))}
            </g>

            {/* ── A TRILHA: a ordem cronológica desenhada por baixo de tudo ─── */}
            <g aria-hidden className="pointer-events-none">
              {(carregando || vazio) && (
                <path
                  d={TRILHA_FANTASMA}
                  fill="none"
                  strokeWidth={1}
                  className="stroke-graf-eixo"
                  opacity={0.7}
                />
              )}
              {!carregando && trilha && (
                <path d={trilha} fill="none" strokeWidth={1} className="stroke-graf-eixo" />
              )}
              {!carregando && seta && (
                <path
                  d="M0,0 L-5,-2.6 L-5,2.6 Z"
                  transform={`translate(${seta.x} ${seta.y}) rotate(${seta.graus})`}
                  className="fill-graf-eixo"
                />
              )}
            </g>

            {/* ── Rótulos do eixo do tempo (com halo: legíveis sobre a trilha) ─ */}
            {!carregando && !vazio && (
              <g aria-hidden className="pointer-events-none">
                {/* Anéis intermediários: só a data. O 1º e o último têm rótulo próprio. */}
                {aneis.slice(1, -1).map((anel) => (
                  <TextoComHalo
                    key={`data-${anel.i}`}
                    x={ESPIRAL_CENTRO.x}
                    y={anel.rotuloY}
                    anchor="middle"
                    size={8.5}
                    className="tabular fill-soft font-mono"
                  >
                    {anel.rotulo}
                  </TextoComHalo>
                ))}

                {/* O CENTRO é o começo do tempo — e o miolo do disco está sempre livre. */}
                {primeiro && !soUma && (
                  <>
                    <TextoComHalo
                      x={ESPIRAL_CENTRO.x}
                      y={ESPIRAL_CENTRO.y - 3}
                      anchor="middle"
                      size={8.5}
                      className="fill-soft font-mono"
                    >
                      mais antiga
                    </TextoComHalo>
                    <TextoComHalo
                      x={ESPIRAL_CENTRO.x}
                      y={ESPIRAL_CENTRO.y + 8}
                      anchor="middle"
                      size={9.5}
                      className="tabular fill-mid font-mono"
                    >
                      {dataCurta(primeiro.ponto.submetidaEm)}
                    </TextoComHalo>
                  </>
                )}

                {/* A ponta: rótulo apontando para DENTRO (fora não há margem). */}
                {ultimo && !soUma && (
                  <>
                    <TextoComHalo
                      x={ultimo.x + (ultimo.x >= ESPIRAL_CENTRO.x ? -12 : 12)}
                      y={ultimo.y - 3}
                      anchor={ultimo.x >= ESPIRAL_CENTRO.x ? 'end' : 'start'}
                      size={8.5}
                      className="fill-soft font-mono"
                    >
                      mais recente
                    </TextoComHalo>
                    <TextoComHalo
                      x={ultimo.x + (ultimo.x >= ESPIRAL_CENTRO.x ? -12 : 12)}
                      y={ultimo.y + 8}
                      anchor={ultimo.x >= ESPIRAL_CENTRO.x ? 'end' : 'start'}
                      size={9.5}
                      className="tabular fill-mid font-mono"
                    >
                      {dataCurta(ultimo.ponto.submetidaEm)}
                    </TextoComHalo>
                  </>
                )}
              </g>
            )}

            {/* ── Carregando: pontos-fantasma (sem cor — ausência de medida não é medida) ── */}
            {carregando && (
              <g aria-hidden className="pointer-events-none">
                {FANTASMAS.map((autonomia, i) => {
                  const p = posicaoEspiral(i, FANTASMAS.length)
                  return (
                    <circle
                      key={`fantasma-${i}`}
                      cx={p.x}
                      cy={p.y}
                      r={raioPorAutonomia_TAMANHO(autonomia)}
                      className="ci-pulse fill-line"
                      style={{ animationDelay: `${i * 0.12}s` }}
                    />
                  )
                })}
              </g>
            )}

            {/* ── Decoração da seleção (por baixo dos alvos, para não roubar o clique) ── */}
            {selecionado && !carregando && (
              <g aria-hidden className="pointer-events-none">
                {/* O anel de tempo daquele ponto: reforça que a DISTÂNCIA AO CENTRO é o quando. */}
                <circle
                  cx={ESPIRAL_CENTRO.x}
                  cy={ESPIRAL_CENTRO.y}
                  r={raioDoTempo(selecionado.i, n)}
                  fill="none"
                  stroke={corDaClasse(selecionado.ponto.k, tema)}
                  strokeWidth={1}
                  strokeDasharray="2 4"
                  opacity={escuro ? 0.45 : 0.5}
                />
                <circle
                  cx={selecionado.x}
                  cy={selecionado.y}
                  r={selecionado.halo}
                  fill={corDaClasse(selecionado.ponto.k, tema)}
                  opacity={escuro ? 0.28 : 0.24}
                />
                <circle
                  cx={selecionado.x}
                  cy={selecionado.y}
                  r={selecionado.nucleo + SEL_ANEL_FOLGA}
                  fill="none"
                  stroke={corDaClasse(selecionado.ponto.k, tema)}
                  strokeWidth={1}
                  opacity={escuro ? 0.55 : 0.6}
                />
              </g>
            )}

            {/* ── Os pontos: cor = classe · tamanho = autonomia ─────────────── */}
            {!carregando && (
              <g>
                {marcadores.map((m) => {
                  const cor = corDaClasse(m.ponto.k, tema)
                  const selecionada = m.ponto.resolucaoId === selecionadoId
                  const ativa = m.ponto.resolucaoId === destacadoId
                  const focada = m.ponto.resolucaoId === focoId
                  const alvo = limitar(m.nucleo + 5.5, ALVO_MIN, ALVO_MAX)

                  return (
                    <g key={m.ponto.resolucaoId} style={{ color: cor }}>
                      {/* O halo da selecionada já foi desenhado (maior) na decoração. */}
                      {!selecionada && (
                        <circle
                          cx={m.x}
                          cy={m.y}
                          r={m.halo}
                          fill={cor}
                          opacity={ativa ? 0.34 : escuro ? 0.2 : 0.18}
                          className="transition-opacity duration-150 motion-reduce:transition-none"
                        />
                      )}

                      {/* REGRA 3: MEDIDO = disco cheio · ≈ ESTIMADO = anel VAZADO. */}
                      {NUCLEO_MEDIDO ? (
                        <circle
                          cx={m.x}
                          cy={m.y}
                          r={m.nucleo}
                          fill={selecionada ? 'var(--estrela-nucleo)' : cor}
                        />
                      ) : (
                        <circle
                          cx={m.x}
                          cy={m.y}
                          r={m.nucleo}
                          fill="none"
                          stroke={selecionada ? 'var(--estrela-nucleo)' : cor}
                          strokeWidth={selecionada ? SEL_NUCLEO_TRACO : NUCLEO_TRACO}
                        />
                      )}

                      {/* Foco de teclado: o SVG não tem box-shadow — o anel é geometria. */}
                      {focada && (
                        <circle
                          cx={m.x}
                          cy={m.y}
                          r={m.nucleo + FOCO_FOLGA}
                          fill="none"
                          stroke="var(--ink)"
                          strokeWidth={1}
                          opacity={0.9}
                        />
                      )}

                      {/* Alvo invisível (fill transparent RECEBE evento; fill none não). */}
                      <circle
                        cx={m.x}
                        cy={m.y}
                        r={alvo}
                        fill="transparent"
                        role={clicavel ? 'button' : 'img'}
                        tabIndex={0}
                        aria-label={rotuloAcessivel(m.ponto, m.i, n)}
                        aria-pressed={clicavel ? selecionada : undefined}
                        className={cn('outline-none', clicavel && 'cursor-pointer')}
                        onPointerEnter={() => setHoverId(m.ponto.resolucaoId)}
                        onPointerLeave={() =>
                          setHoverId((id) => (id === m.ponto.resolucaoId ? null : id))
                        }
                        onFocus={() => setFocoId(m.ponto.resolucaoId)}
                        onBlur={() => setFocoId((id) => (id === m.ponto.resolucaoId ? null : id))}
                        onClick={() => onSelecionar?.(m.ponto.resolucaoId)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault()
                            onSelecionar?.(m.ponto.resolucaoId)
                          }
                        }}
                      />
                    </g>
                  )
                })}
              </g>
            )}
          </svg>

          {/* ── Callout (HTML sobre o SVG — nunca <title>: o nativo rouba o clique) ── */}
          {destacado && !carregando && (
            <Callout ponto={destacado.ponto} x={destacado.x} y={destacado.y} i={destacado.i} n={n} tema={tema} />
          )}

          {/* ── Vazio: a forma do gráfico fica; as resoluções é que não existem ── */}
          {vazio && (
            <div className="absolute inset-0 grid place-items-center p-3">
              <EmptyState
                size="sm"
                icon={Folder}
                title="Nenhuma resolução analisada ainda"
                description={
                  dataset.total > 0
                    ? `${pluralPt(dataset.semMetrica.total, 'resolução sem métrica', 'resoluções sem métrica')}. ${NOTA_METRICAS_SO_JAVA}`
                    : 'Submeta uma resolução em Java para ver o primeiro ponto da sua espiral.'
                }
                className="bg-recess/85 backdrop-blur-[1px]"
              />
            </div>
          )}
        </div>

        <Legenda tema={tema} />
      </div>

      {/* Uma resolução só não tem trajetória: ela é o começo E o fim. Dizer isso é mais honesto
          do que desenhar uma espiral de um ponto. */}
      {soUma && primeiro && (
        <p className="tabular font-mono text-[10px] leading-none text-soft">
          uma única resolução ({dataCurta(primeiro.ponto.submetidaEm)}) — a espiral aparece a partir
          da segunda
        </p>
      )}
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// LEGENDA — as TRÊS codificações, ditas com todas as letras.
// Tamanho sem escala não se lê: por isso a régua de autonomia (3 bolinhas) existe.
// ════════════════════════════════════════════════════════════════════════════

function Legenda({ tema }: { tema: PropsGrafico['tema'] }) {
  return (
    <div className="flex flex-col gap-3.5 md:w-[212px]">
      {/* ── Mini "como ler" (2 linhas) ────────────────────────────────────── */}
      <div className="flex flex-col gap-1">
        <span className="font-mono text-[9px] uppercase tracking-[.06em] text-mid">como ler</span>
        <p className="text-[11px] leading-[1.5] text-soft">
          Siga a trilha do centro para a borda — é a ordem em que você enviou.
          <br />
          Pontos <b className="font-semibold text-body">maiores</b> = mais autônomo. Pontos mais{' '}
          <b className="font-semibold text-body">verdes</b> = menos custosos.
        </p>
      </div>

      <div className="h-px w-full bg-line-soft" />

      {/* ── 1. RAIO = TEMPO ───────────────────────────────────────────────── */}
      <div className="flex flex-col gap-1.5">
        <span className="font-mono text-[9.5px] uppercase tracking-[.05em] text-ink">
          raio = tempo
        </span>
        <div className="flex items-center gap-2">
          <svg width="52" height="12" viewBox="0 0 52 12" aria-hidden className="shrink-0">
            <circle cx="3" cy="6" r="1.6" className="fill-soft" />
            <line x1="6" y1="6" x2="43" y2="6" strokeWidth={1} className="stroke-graf-eixo" />
            <path d="M0,0 L-5,-2.6 L-5,2.6 Z" transform="translate(48 6)" className="fill-graf-eixo" />
          </svg>
          <span className="font-mono text-[9.5px] leading-tight text-soft">
            centro: mais antiga
            <br />
            borda: mais recente
          </span>
        </div>
      </div>

      {/* ── 2. TAMANHO = AUTONOMIA (neutra — regra 4) ─────────────────────── */}
      <div className="flex flex-col gap-1.5">
        <span className="font-mono text-[9.5px] uppercase tracking-[.05em] text-ink">
          tamanho = autonomia
        </span>
        <div className="flex items-center gap-2">
          {/* Cinza de propósito: autonomia NUNCA veste o colormap. */}
          <svg width="52" height="14" viewBox="0 0 52 14" aria-hidden className="shrink-0">
            {[1, 3, 5].map((a, i) => (
              <circle
                key={a}
                cx={[6, 21, 40][i]}
                cy={7}
                r={raioPorAutonomia_TAMANHO(a)}
                fill="none"
                strokeWidth={1.2}
                className="stroke-mid"
              />
            ))}
          </svg>
          <span className="tabular font-mono text-[9.5px] leading-tight text-soft">
            aut 1 · 3 · 5
            <br />
            (1 = mais apoio de IA)
          </span>
        </div>
      </div>

      {/* ── 3. COR = CLASSE DE COMPLEXIDADE (o colormap) ──────────────────── */}
      <div className="flex flex-col gap-1.5">
        <span className="font-mono text-[9.5px] uppercase tracking-[.05em] text-ink">
          cor = complexidade
        </span>
        <div className="flex gap-[1.5px]">
          {CLASSES.map((c) => (
            <div
              key={c.k}
              className="h-[6px] flex-1 rounded-[2px]"
              style={{ backgroundColor: corDaClasse(c.k, tema) }}
            />
          ))}
        </div>
        <div className="flex justify-between font-mono text-[9px] text-soft">
          <span>O(1)</span>
          <span>O(n!)</span>
        </div>
      </div>

      {/* ── A incerteza da métrica, na própria legenda (regra 3) ──────────── */}
      <div className="flex items-start gap-2">
        <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden className="mt-[1px] shrink-0">
          <circle cx="6" cy="6" r="3.4" fill="none" strokeWidth={1.3} className="stroke-mid" />
        </svg>
        <span className="font-mono text-[9px] leading-[1.4] text-soft">
          marcador vazado + ≈ : classe estimada por análise estática (AST), não medida
        </span>
      </div>
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// CALLOUT — o mesmo vocabulário da Carta, mais a POSIÇÃO NO TEMPO (aqui o raio é
// tempo: esconder o "quando" seria esconder a coordenada).
// ════════════════════════════════════════════════════════════════════════════

function Callout({
  ponto,
  x,
  y,
  i,
  n,
  tema,
}: {
  ponto: PontoPlotavel
  x: number
  y: number
  i: number
  n: number
  tema: PropsGrafico['tema']
}) {
  const cor = corDaClasse(ponto.k, tema)
  const tinta = tintaDaClasse(ponto.k, tema)
  const pos = posicaoCallout(x, y, ESPIRAL_VIEWBOX)
  const motor = rotuloConfiancaMotor(ponto.confiancaTempo)
  const lingua = LINGUAGEM_META[ponto.linguagem]?.label ?? ponto.linguagem

  return (
    // aria-hidden: tudo isto já está no aria-label do alvo — não repetir para o leitor de tela.
    <div
      aria-hidden
      className="pointer-events-none absolute z-10 flex flex-col gap-[3px] whitespace-nowrap rounded-ci bg-elevated px-[10px] py-2 shadow-callout"
      style={{ ...pos, border: `1px solid ${cor}` }}
    >
      <span className="font-mono text-[11px] font-semibold text-ink">
        {tema === 'dark' ? '✦ ' : ''}
        {ponto.desafioTitulo} · {lingua}
      </span>

      {/* UMA COR POR SEMÂNTICA: só a CLASSE veste o colormap. Ciclomática é contagem (mid) e
          autonomia é NEUTRA (ink) — regras 1 e 4. */}
      <span className="tabular flex items-center gap-1.5 font-mono text-[10px]">
        <span style={{ color: tinta }}>
          {comPrefixoEstimado(rotuloCanonico(ponto.k), CONFIANCA_BIG_O)}
        </span>
        {ponto.ciclomatica != null && <span className="text-mid">· M={ponto.ciclomatica}</span>}
        <span className="text-ink">· aut {ponto.autonomia}/5</span>
      </span>

      <span className="tabular font-mono text-[9.5px] text-soft">
        {i + 1}ª de {n} · enviada {dataCompleta(ponto.submetidaEm)}
        {motor ? ` · ${motor}` : ''}
      </span>
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// TEXTO SOBRE A TRILHA — o rótulo "recorta" o próprio fundo (paint-order) e continua
// legível por cima da espiral e dos anéis. Sem isto, data e trilha viram um borrão.
// ════════════════════════════════════════════════════════════════════════════

function TextoComHalo({
  x,
  y,
  anchor,
  size,
  className,
  children,
}: {
  x: number
  y: number
  anchor: 'start' | 'middle' | 'end'
  size: number
  className?: string
  children: React.ReactNode
}) {
  return (
    <text
      x={x}
      y={y}
      textAnchor={anchor}
      fontSize={size}
      className={className}
      stroke="var(--panel-chart)"
      strokeWidth={HALO_TEXTO}
      strokeLinejoin="round"
      paintOrder="stroke"
    >
      {children}
    </text>
  )
}

// ── Acessibilidade: o que o leitor de tela diz ──────────────────────────────

/** A frase do SVG inteiro: a leitura do gráfico, não a lista dos pontos. */
function rotuloDoGrafico(pontos: PontoPlotavel[]): string {
  const n = pontos.length
  if (n === 0) return 'Espiral do tempo: nenhuma resolução analisada.'
  const de = dataCompleta(pontos[0].submetidaEm)
  const ate = dataCompleta(pontos[n - 1].submetidaEm)
  return (
    `Espiral do tempo: ${n} ${n === 1 ? 'resolução' : 'resoluções'}, do centro (a mais antiga, ${de}) ` +
    `à borda (a mais recente, ${ate}). O tamanho do ponto é a autonomia (1 a 5) e a cor é a classe ` +
    'de complexidade de tempo, estimada por análise estática.'
  )
}

/** Um ponto em uma frase — inclui a posição no tempo, que aqui é a própria coordenada. */
function rotuloAcessivel(ponto: PontoPlotavel, i: number, n: number): string {
  const classe = comPrefixoEstimado(rotuloCanonico(ponto.k), CONFIANCA_BIG_O).trim()
  const natureza = CONFIANCA_BIG_O === 'MEDIDO' ? 'medida' : 'estimada por análise estática'
  const ciclo = ponto.ciclomatica != null ? `, ciclomática ${ponto.ciclomatica} (medida)` : ''
  const motor = rotuloConfiancaMotor(ponto.confiancaTempo)
  const lingua = LINGUAGEM_META[ponto.linguagem]?.label ?? ponto.linguagem
  return (
    `${i + 1}ª de ${n} no tempo: ${ponto.desafioTitulo}, ${lingua}, ` +
    `enviada em ${dataCompleta(ponto.submetidaEm)}, autonomia ${ponto.autonomia} de 5, ` +
    `tempo ${classe} (${natureza}${motor ? `, ${motor}` : ''})${ciclo}`
  )
}
