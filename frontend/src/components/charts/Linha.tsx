/*
 * LINHA — evolução temporal (o 4º dos 5 gráficos do painel).
 *
 * Duas séries, DUAS ESCALAS, um mesmo eixo de tempo:
 *   • AUTONOMIA média do mês (1–5) → eixo Y ESQUERDO, neutro (regra 4: autonomia nunca é colormap).
 *   • COMPLEXIDADE típica do mês (k médio, 0–7) → eixo Y DIREITO, que É a barra de colormap.
 *
 * Fonte: docs/design/specs/02-graficos.md §5 (geometria conferida contra o protótipo) e
 * 00-INDICE.md §2.7 (regras invioláveis). As lacunas K/L/M/N da spec estão resolvidas aqui —
 * cada decisão está marcada com DECISÃO e diz de onde veio.
 *
 * O componente é PURO: recebe o dataset, desenha. Não busca dados, não desenha o cartão do
 * painel, não conhece o seletor.
 */
import { useMemo, useState } from 'react'
import { Folder } from 'lucide-react'
import { EmptyState } from '@/components/ui/empty-state'
import { cn, pluralPt } from '@/lib/utils'
import {
  type ClasseK,
  COMPLEXIDADE_ORDEM_MAX,
  CONFIANCA_BIG_O,
  comPrefixoEstimado,
  corDaClasse,
  ordemArredondada,
  rotuloCanonico,
  rotuloCurto,
  tintaDaClasse,
} from '@/domain/enums'
import {
  type BucketMes,
  type PontoSerie,
  bucketsMensais,
  LINHA_GRADE_Y,
  LINHA_LARGURA,
  LINHA_TICKS_CLASSE,
  LINHA_Y_BASE,
  LINHA_Y_TOPO,
  numeroPt,
  pontosParaPolyline,
  segmentosDaSerie,
  temSerieTemporal,
  tendenciaDaLinha,
  xDaLinha,
  yAutonomiaLinha,
  yClasseLinha,
} from './escalas'
import type { PropsGrafico } from './tipos'

// ════════════════════════════════════════════════════════════════════════════
// GEOMETRIA
// ════════════════════════════════════════════════════════════════════════════

/**
 * DECISÃO (janela) — pedido do produto, MANDA sobre o `LINHA_MAX_BUCKETS` (8) de `escalas.ts`,
 * que é o número de buckets que o protótipo por acaso desenhou: a janela é de **12 meses**.
 * `bucketsMensais` devolve o intervalo CONTÍNUO do 1º ao último mês com atividade (mês vazio
 * ocupa seu x — o tempo não anda mais devagar porque o aluno parou de enviar) e, se o intervalo
 * for maior que a janela, fica com os 12 meses MAIS RECENTES. Menos de 12 meses de história →
 * todos os meses.
 */
export const LINHA_JANELA_MESES = 12

/**
 * DECISÃO (viewBox) — o protótipo é `0 0 400 220` (`LINHA_VIEWBOX`), mas ele **não tem eixo Y**
 * (a própria spec registra isso na Lacuna K). Dois eixos rotulados + a linha do ano não cabem
 * ali. A geometria dos DADOS é preservada byte a byte (x ∈ [40, 384], y ∈ [60, 200] — tudo vem
 * de `escalas.ts`); o que cresce é só a margem:
 *   • direita  +30 → barra de colormap (x 394–401) + ticks de classe (x 405);
 *   • inferior +12 → segunda linha de rótulo do eixo X para o ANO (a janela de 12 meses
 *     atravessa o Ano-Novo; sem isso "jan" seria ambíguo).
 */
const VB = { largura: 430, altura: 232 } as const

const GRADE_X0 = 32
const GRADE_X1 = 390

/** Eixo Y DIREITO = a barra de colormap (o mesmo idioma do eixo Y da Carta, spec 02 §2.4). */
const BARRA_X = 394
const BARRA_LARGURA = 7
const TICK_CLASSE_X = 405
const TICK_AUT_X = 26

const ROTULO_MES_Y = 214
const ROTULO_ANO_Y = 226

/** Altura da faixa de uma classe: (200 − 60) / 7 = 20px exatos. */
const FAIXA_CLASSE = (LINHA_Y_BASE - LINHA_Y_TOPO) / COMPLEXIDADE_ORDEM_MAX

/**
 * As 7 células da barra de colormap (k = 1..7). Cada célula é a faixa ABAIXO da linha da sua
 * classe — idêntico ao eixo Y da Carta.
 */
const CELULAS_CLASSE = Array.from({ length: COMPLEXIDADE_ORDEM_MAX }, (_, i) => {
  const k = (i + 1) as ClasseK
  return { k, y: yClasseLinha(k), altura: FAIXA_CLASSE }
})

/**
 * DECISÃO (Lacuna C da Carta, aplicada aqui por coerência): a barra tem 7 células para 8 classes
 * — a faixa de O(1) cairia abaixo da linha de base. Quadrado 7×7 centrado na base (y = 200) = o
 * "piso" da escala. Sem ele o verde nunca aparece no eixo.
 */
const CELULA_O1 = { y: LINHA_Y_BASE - BARRA_LARGURA / 2, k: 0 as ClasseK }

// ── Marcas ──────────────────────────────────────────────────────────────────

const R_AUT = 2.4
const R_AUT_ATIVO = 3.2
const R_CLS = 3
const R_CLS_ATIVO = 3.8

// ════════════════════════════════════════════════════════════════════════════
// PROPS
// ════════════════════════════════════════════════════════════════════════════

export interface PropsLinha extends PropsGrafico {
  /** Esqueleto pulsante no lugar das séries (a grade e os dois eixos continuam). */
  carregando?: boolean
  /** Janela em meses. Padrão: `LINHA_JANELA_MESES` (12). */
  janelaMeses?: number
  className?: string
}

interface DadosBucket {
  bucket: BucketMes
  i: number
  x: number
  /** `null` só no mês SEM RESOLUÇÃO (autonomia independe da linguagem — §4.4). */
  yAut: number | null
  /** `null` quando nenhuma resolução do mês tem classe de complexidade. */
  yCls: number | null
  /** classe média ARREDONDADA — a cor do ponto e do rótulo. `null` sem classe no mês. */
  k: ClasseK | null
}

// ════════════════════════════════════════════════════════════════════════════
// COMPONENTE
// ════════════════════════════════════════════════════════════════════════════

export function Linha({
  dataset,
  selecionadoId,
  onSelecionar,
  tema,
  carregando = false,
  janelaMeses = LINHA_JANELA_MESES,
  className,
}: PropsLinha) {
  const [hoverIdx, setHoverIdx] = useState<number | null>(null)

  /*
   * ⚠ `dataset.todas`, NÃO `dataset.pontos`. A série de AUTONOMIA é autodeclarada e INDEPENDE
   * da linguagem (§4.4): um mês com 3 resoluções em Python é um mês com 3 resoluções — não um
   * mês "sem resolução". Só a série de COMPLEXIDADE se interrompe ali.
   */
  const buckets = useMemo(
    () => bucketsMensais(dataset.todas, janelaMeses),
    [dataset.todas, janelaMeses],
  )

  const dados = useMemo<DadosBucket[]>(
    () =>
      buckets.map((bucket, i) => ({
        bucket,
        i,
        x: xDaLinha(i, buckets.length),
        yAut: bucket.mediaAutonomia == null ? null : yAutonomiaLinha(bucket.mediaAutonomia),
        yCls: bucket.mediaClasse == null ? null : yClasseLinha(bucket.mediaClasse),
        k: ordemArredondada(bucket.mediaClasse),
      })),
    [buckets],
  )

  const segAut = useMemo(() => segmentosDaSerie(buckets, 'autonomia'), [buckets])
  const segCls = useMemo(() => segmentosDaSerie(buckets, 'classe'), [buckets])
  const tendencia = useMemo(() => tendenciaDaLinha(buckets), [buckets])

  /** ≥ 2 meses COM dado. Abaixo disso não se desenha linha — só os pontos (pedido do produto). */
  const temSerie = temSerieTemporal(buckets)
  /** Vazio = nenhuma RESOLUÇÃO (não "nenhuma plotável"): a autonomia existe sem métrica. */
  const vazio = !carregando && dataset.todas.length === 0

  const idxSelecionado = useMemo(() => {
    if (!selecionadoId) return -1
    return buckets.findIndex((b) => b.resolucoes.some((p) => p.resolucaoId === selecionadoId))
  }, [buckets, selecionadoId])

  /** O bucket "ativo" — o hover manda; sem hover, o mês da resolução selecionada. */
  const idxAtivo = hoverIdx ?? (idxSelecionado >= 0 ? idxSelecionado : null)
  const ativo = idxAtivo == null ? null : dados[idxAtivo] ?? null

  /** Largura da coluna de hover (também é o passo entre meses). */
  const passo = buckets.length > 1 ? LINHA_LARGURA / (buckets.length - 1) : LINHA_LARGURA

  function selecionar(b: BucketMes) {
    // Só as COM métrica viram estrela nos outros gráficos — selecionar uma resolução que não
    // existe na Carta deixaria a seleção pendurada. Mês sem nenhuma plotável não é clicável.
    if (!onSelecionar || b.comMetrica.length === 0) return
    // `bucket.comMetrica` está em ordem cronológica ASCENDENTE.
    // DECISÃO: o mês é um AGREGADO — não dá para "selecionar um mês". Clicar seleciona a
    // resolução MAIS RECENTE do mês, e o tooltip diz isso em voz alta quando há mais de uma.
    // Nunca escolher em silêncio: o clique é explicado antes de acontecer.
    onSelecionar(b.comMetrica[b.comMetrica.length - 1].resolucaoId)
  }

  return (
    <div className={cn('flex flex-col gap-2.5', className)}>
      <Legenda tema={tema} />

      <div className="relative" onMouseLeave={() => setHoverIdx(null)}>
        {/*
         * `role="group"`, não `role="img"`: por ARIA, os descendentes de um `img` são
         * PRESENTATIONAL — os 12 alvos de mês (`role="button"` + `<title>`) simplesmente
         * não existiriam para o leitor de tela. Um gráfico interativo é um grupo.
         */}
        <svg
          viewBox={`0 0 ${VB.largura} ${VB.altura}`}
          width="100%"
          className="block"
          role="group"
          aria-label={rotuloAcessivel(buckets, tendencia.texto, carregando)}
        >
          {/* ── grade: regrada nos INTEIROS de autonomia (DECISÃO — Lacuna K) ────────── */}
          {LINHA_GRADE_Y.map(({ autonomia, y }) => (
            <line
              key={`g-${autonomia}`}
              x1={GRADE_X0}
              x2={GRADE_X1}
              y1={y}
              y2={y}
              className={autonomia === 1 ? 'stroke-graf-eixo' : 'stroke-graf-grade'}
            />
          ))}
          <line
            x1={GRADE_X0}
            x2={GRADE_X0}
            y1={LINHA_Y_TOPO}
            y2={LINHA_Y_BASE}
            className="stroke-graf-eixo"
          />

          {/* ── eixo Y esquerdo: autonomia 1..5 (NEUTRO — regra 4) ───────────────────── */}
          {LINHA_GRADE_Y.map(({ autonomia, y }) => (
            <text
              key={`ta-${autonomia}`}
              x={TICK_AUT_X}
              y={y}
              textAnchor="end"
              dominantBaseline="middle"
              className="fill-soft font-mono tabular"
              fontSize={9}
            >
              {autonomia}
            </text>
          ))}

          {/* ── eixo Y direito: a barra de COLORMAP (a única cor do sistema) ─────────── */}
          <g aria-hidden="true">
            {CELULAS_CLASSE.map(({ k, y, altura }) => (
              <rect
                key={`cx-${k}`}
                x={BARRA_X}
                y={y}
                width={BARRA_LARGURA}
                height={altura}
                fill={corDaClasse(k, tema)}
              />
            ))}
            <rect
              x={BARRA_X}
              y={CELULA_O1.y}
              width={BARRA_LARGURA}
              height={BARRA_LARGURA}
              fill={corDaClasse(CELULA_O1.k, tema)}
            />
          </g>
          {LINHA_TICKS_CLASSE.map((k) => (
            <text
              key={`tk-${k}`}
              x={TICK_CLASSE_X}
              y={yClasseLinha(k)}
              textAnchor="start"
              dominantBaseline="middle"
              className="fill-soft font-mono"
              fontSize={9}
            >
              {rotuloCurto(k)}
            </text>
          ))}

          {/* ── eixo X: mês (+ ano quando vira o calendário) ─────────────────────────── */}
          {dados.map(({ bucket, i, x }) => (
            <g key={bucket.chave}>
              <text
                x={x}
                y={ROTULO_MES_Y}
                textAnchor="middle"
                fontSize={9}
                className={cn(
                  'font-mono',
                  idxAtivo === i ? 'fill-mid' : 'fill-soft',
                  bucket.total === 0 && 'opacity-55',
                )}
              >
                {bucket.rotulo}
              </text>
              {/* o ano aparece no 1º bucket e a cada janeiro — a janela de 12 meses cruza anos */}
              {(i === 0 || bucket.mes === 1) && (
                <text
                  x={x}
                  y={ROTULO_ANO_Y}
                  textAnchor="middle"
                  fontSize={8.5}
                  className="fill-soft font-mono tabular opacity-70"
                >
                  {bucket.ano}
                </text>
              )}
            </g>
          ))}

          {carregando ? (
            <Fantasma />
          ) : (
            <>
              {/* ── guia vertical do bucket ativo ──────────────────────────────────── */}
              {ativo && (
                <line
                  x1={ativo.x}
                  x2={ativo.x}
                  y1={LINHA_Y_TOPO - 4}
                  y2={LINHA_Y_BASE}
                  className="stroke-line-strong"
                  strokeDasharray="3 3"
                />
              )}

              {/* ── série COMPLEXIDADE: linha NEUTRA tracejada, pontos no COLORMAP ──── */}
              {temSerie &&
                segCls.map((seg) => (
                  <polyline
                    key={`cls-${seg[0].bucket.chave}`}
                    points={pontosParaPolyline(seg)}
                    fill="none"
                    className="stroke-mid"
                    strokeWidth={2}
                    strokeDasharray="2 5"
                    strokeLinecap="round"
                    opacity={0.9}
                  />
                ))}

              {/* ── série AUTONOMIA: sólida, NEUTRA (regra 4) ───────────────────────── */}
              {temSerie &&
                segAut.map((seg) => (
                  <polyline
                    key={`aut-${seg[0].bucket.chave}`}
                    points={pontosParaPolyline(seg)}
                    fill="none"
                    className="stroke-ink"
                    strokeWidth={2.2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                ))}

              {/* ── marcas ─────────────────────────────────────────────────────────── */}
              {dados.map((d) => {
                if (d.yCls == null || d.k == null) return null
                const cor = corDaClasse(d.k, tema)
                const r = idxAtivo === d.i ? R_CLS_ATIVO : R_CLS
                // MEDIDO = disco CHEIO · ≈ ESTIMADO = anel VAZADO (regra 3). A classe de tempo
                // é inferência estática → sempre ESTIMADA → sempre anel. Anel em vez de disco
                // com fill do fundo: assim a marca não depende da cor do cartão atrás.
                return CONFIANCA_BIG_O === 'MEDIDO' ? (
                  <circle key={`mc-${d.bucket.chave}`} cx={d.x} cy={d.yCls} r={r} fill={cor} />
                ) : (
                  <circle
                    key={`mc-${d.bucket.chave}`}
                    cx={d.x}
                    cy={d.yCls}
                    r={r + 0.2}
                    fill="none"
                    stroke={cor}
                    strokeWidth={1.6}
                  />
                )
              })}

              {dados.map((d) =>
                d.yAut == null ? null : (
                  <circle
                    key={`ma-${d.bucket.chave}`}
                    cx={d.x}
                    cy={d.yAut}
                    r={idxAtivo === d.i ? R_AUT_ATIVO : R_AUT}
                    className="fill-ink"
                  />
                ),
              )}

              {/* "onde você está agora": halo no último mês COM dado da série de autonomia */}
              <Agora segmentos={segAut} />

              {/* ── alvos de hover/clique: uma coluna por mês ───────────────────────── */}
              {dados.map((d) => {
                const x0 = Math.max(GRADE_X0, d.x - passo / 2)
                const x1 = Math.min(GRADE_X1, d.x + passo / 2)
                const clicavel = !!onSelecionar && d.bucket.comMetrica.length > 0
                // Mês sem resolução não vira parada de tabulação muda: ou é botão de verdade,
                // ou é um `img` com rótulo (o leitor ainda ouve "mar 2026: sem resolução").
                const temDado = d.bucket.total > 0
                return (
                  <rect
                    key={`hit-${d.bucket.chave}`}
                    x={x0}
                    y={LINHA_Y_TOPO - 12}
                    width={Math.max(1, x1 - x0)}
                    height={LINHA_Y_BASE - LINHA_Y_TOPO + 20}
                    fill="transparent"
                    style={{ cursor: clicavel ? 'pointer' : 'default' }}
                    tabIndex={clicavel || temDado ? 0 : -1}
                    role={clicavel ? 'button' : 'img'}
                    aria-label={resumoDoBucket(d)}
                    onMouseEnter={() => setHoverIdx(d.i)}
                    onFocus={() => setHoverIdx(d.i)}
                    onBlur={() => setHoverIdx(null)}
                    onClick={() => selecionar(d.bucket)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault()
                        selecionar(d.bucket)
                      }
                    }}
                  >
                    <title>{resumoDoBucket(d)}</title>
                  </rect>
                )
              })}
            </>
          )}
        </svg>

        {/* ── tooltip (HTML sobre o SVG — spec 02 §2.9) ─────────────────────────────── */}
        {ativo && !carregando && (
          <Tooltip dados={ativo} tema={tema} clicavel={!!onSelecionar && ativo.bucket.total > 0} />
        )}

        {/* ── poucos dados: pontos sim, linha não ───────────────────────────────────── */}
        {!carregando && !vazio && !temSerie && (
          <div
            className="pointer-events-none absolute left-1/2 top-[13%] -translate-x-1/2 rounded-ci border border-line bg-recess px-2.5 py-1 font-mono text-[10px] text-soft"
            role="note"
          >
            poucos dados para uma tendência
          </div>
        )}

        {/* ── vazio ────────────────────────────────────────────────────────────────── */}
        {vazio && (
          <div className="absolute inset-0 flex items-center justify-center p-3">
            <EmptyState
              size="sm"
              icon={Folder}
              title="Nenhuma resolução ainda."
              description="Submeta sua primeira resolução: a autonomia entra na linha desde já; a complexidade, quando o código for Java."
              className="max-w-[300px] bg-recess/90"
            />
          </div>
        )}
      </div>

      {/* ── rodapé: texto DERIVADO DOS DADOS, nunca fixo (Lacuna L) ─────────────────── */}
      <p className="font-mono text-[10px] text-soft tabular">
        {carregando
          ? 'calculando a evolução…'
          : temSerie
            ? `${tendencia.texto} · ${meses(buckets.length)}`
            : 'mês a mês · esquerda = autonomia (1–5) · direita = complexidade típica'}
      </p>
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// PEÇAS
// ════════════════════════════════════════════════════════════════════════════

/**
 * Legenda (spec 02 §5.1). Três itens, porque são três coisas a distinguir:
 * a série neutra (autonomia), a série cromática (complexidade) e — regra 3 — a CONFIANÇA.
 */
function Legenda({ tema }: { tema: Tema }) {
  const corExemplo = corDaClasse(3, tema)
  return (
    <div className="flex flex-wrap items-center gap-x-3.5 gap-y-1.5 py-0.5 font-mono text-[10px] text-mid">
      <span className="flex items-center gap-1.5">
        <span className="h-[2.4px] w-3.5 rounded-ci-sm bg-ink" aria-hidden="true" />
        autonomia
      </span>
      <span className="flex items-center gap-1.5">
        <svg width={14} height={8} aria-hidden="true" className="overflow-visible">
          <line
            x1={0}
            x2={14}
            y1={4}
            y2={4}
            className="stroke-mid"
            strokeWidth={2}
            strokeDasharray="2 5"
            strokeLinecap="round"
          />
        </svg>
        complexidade típica
      </span>
      {/* Só o marcador VAZADO aparece nesta série: a classe de tempo é sempre ≈ estimada
          (o disco cheio é reservado a métrica MEDIDA — regra 3). */}
      <span className="flex items-center gap-1.5 text-soft">
        <svg width={10} height={8} aria-hidden="true">
          <circle cx={5} cy={4} r={3.2} fill="none" stroke={corExemplo} strokeWidth={1.6} />
        </svg>
        ≈ estimado (análise estática)
      </span>
    </div>
  )
}

/** Halo no último mês com dado da autonomia — "onde você está agora" (protótipo, §5.3). */
function Agora({ segmentos }: { segmentos: PontoSerie[][] }) {
  const ultimoSeg = segmentos[segmentos.length - 1]
  const ultimo = ultimoSeg?.[ultimoSeg.length - 1]
  if (!ultimo) return null
  return (
    <circle
      cx={ultimo.x}
      cy={ultimo.y}
      r={6}
      fill="none"
      className="stroke-ink"
      strokeWidth={1}
      opacity={0.35}
    />
  )
}

/** Esqueleto: a grade e os eixos ficam; as séries viram fantasmas pulsando (spec 02 §2.11). */
function Fantasma() {
  const xs = [0, 1, 2, 3, 4, 5, 6, 7].map((i) => xDaLinha(i, 8))
  const ys = [168, 158, 150, 138, 132, 120, 112, 100]
  const pontos = xs.map((x, i) => ({ x, y: ys[i] }))
  return (
    <g className="ci-pulse" aria-hidden="true">
      <polyline
        points={pontosParaPolyline(pontos)}
        fill="none"
        className="stroke-line"
        strokeWidth={2.2}
        strokeLinecap="round"
      />
      {pontos.map((p) => (
        <circle key={p.x} cx={p.x} cy={p.y} r={2.6} className="fill-line" />
      ))}
    </g>
  )
}

/**
 * Tooltip do bucket ativo. Mono em tudo (regra 5), `≈` quando a classe do mês é estimada
 * (regra 3), e a contagem do mês — o mês é um agregado e isso não pode ficar implícito.
 */
function Tooltip({
  dados,
  tema,
  clicavel,
}: {
  dados: DadosBucket
  tema: Tema
  clicavel: boolean
}) {
  const { bucket, x, k } = dados
  const yAncora = Math.min(dados.yAut ?? LINHA_Y_BASE, dados.yCls ?? LINHA_Y_BASE)
  const pos = posicaoTooltip(x, yAncora)
  /** SEM RESOLUÇÃO ≠ sem métrica. O mês só é vazio quando o aluno não enviou nada. */
  const semResolucao = bucket.total === 0

  return (
    <div
      role="tooltip"
      className="pointer-events-none absolute z-10 flex flex-col gap-[3px] whitespace-nowrap rounded-ci border bg-elevated px-2.5 py-2 shadow-callout"
      style={{
        ...pos,
        borderColor: k == null ? 'var(--line)' : corDaClasse(k, tema),
      }}
    >
      <div className="flex items-center gap-2">
        <span className="font-mono text-[11px] font-semibold text-ink tabular">
          {bucket.rotulo} {bucket.ano}
        </span>
        <span className="font-mono text-[10px] text-soft tabular">
          {semResolucao
            ? 'sem resolução'
            : pluralPt(bucket.total, 'resolução', 'resoluções')}
          {bucket.semMetrica > 0 && ` · ${bucket.semMetrica} sem métrica`}
        </span>
      </div>

      {/* Autonomia: existe em TODO mês com resolução — independe da linguagem (§4.4). */}
      {!semResolucao && (
        <span className="flex items-center gap-1.5 font-mono text-[10px] text-mid tabular">
          <span className="h-[2px] w-2.5 rounded-ci-sm bg-ink" aria-hidden="true" />
          aut. média {numeroPt(bucket.mediaAutonomia as number, 1)}/5
        </span>
      )}

      {/* Complexidade: só quando ao menos uma resolução do mês foi classificada. */}
      {k != null && bucket.mediaClasse != null ? (
        <span
          className="flex items-center gap-1.5 font-mono text-[10px] tabular"
          style={{ color: tintaDaClasse(k, tema) }}
        >
          <span
            aria-hidden="true"
            className="inline-block size-[7px] shrink-0"
            style={
              CONFIANCA_BIG_O === 'MEDIDO'
                ? { background: corDaClasse(k, tema) }
                : { boxShadow: `inset 0 0 0 1.5px ${corDaClasse(k, tema)}` }
            }
          />
          {comPrefixoEstimado(rotuloCanonico(k), CONFIANCA_BIG_O)}
          <span className="text-soft">· classe média {numeroPt(bucket.mediaClasse, 1)}</span>
        </span>
      ) : (
        !semResolucao && (
          <span className="font-mono text-[10px] text-soft">sem métrica de complexidade</span>
        )
      )}

      {clicavel && bucket.comMetrica.length > 1 && (
        <span className="font-mono text-[9px] text-soft">clique → a mais recente do mês</span>
      )}
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// APOIO
// ════════════════════════════════════════════════════════════════════════════

type Tema = PropsGrafico['tema']

/**
 * Variante da `posicaoCallout` (spec 02 §2.9 + Lacuna F) calibrada para a Linha: aqui os pontos
 * vivem numa banda alta (y ∈ [60, 200]), então o limiar de "estourar por cima" da spec (y < 18.6%
 * do viewBox) nunca dispararia e o tooltip sairia do cartão na autonomia 5. Limiares locais:
 * perto da direita, o tooltip inverte para a esquerda; no terço superior, cresce para BAIXO.
 */
function posicaoTooltip(x: number, y: number) {
  const inverterX = x > VB.largura * 0.62
  const abaixo = y < VB.altura * 0.42 // ≈ y < 97 → o topo da banda de dados
  return {
    left: `${((x / VB.largura) * 100).toFixed(2)}%`,
    top: `${((y / VB.altura) * 100).toFixed(2)}%`,
    transform: `translate(${inverterX ? 'calc(-100% - 10px)' : '10px'}, ${abaixo ? '10px' : 'calc(-100% - 10px)'})`,
  }
}

/**
 * Texto nativo do `<title>`/`aria-label` da coluna — é o que o leitor de tela lê ao focar o mês.
 * "sem resolução" é reservado ao mês em que NADA foi enviado. Mês com resoluções sem métrica
 * diz quantas ficaram sem classe: o trabalho existiu, só não pôde ser medido.
 */
function resumoDoBucket(d: DadosBucket): string {
  const { bucket, k } = d
  if (bucket.total === 0) return `${bucket.rotulo} ${bucket.ano}: sem resolução`

  const partes = [
    `${bucket.rotulo} ${bucket.ano}: ${pluralPt(bucket.total, 'resolução', 'resoluções')}`,
    `autonomia média ${numeroPt(bucket.mediaAutonomia as number, 1)} de 5`,
  ]
  if (k != null) {
    partes.push(`complexidade típica ${comPrefixoEstimado(rotuloCanonico(k), CONFIANCA_BIG_O)}`)
  }
  if (bucket.semMetrica > 0) {
    partes.push(`${bucket.semMetrica} sem métrica de complexidade`)
  }
  return partes.join(' · ')
}

function meses(n: number): string {
  return `${n} ${pluralPt(n, 'mês', 'meses')}`
}

function rotuloAcessivel(buckets: BucketMes[], tendencia: string, carregando: boolean): string {
  if (carregando) return 'Carregando a evolução mensal.'
  const comResolucao = buckets.filter((b) => b.total > 0).length
  if (comResolucao === 0) return 'Evolução mensal: nenhuma resolução enviada.'
  const semMetrica = buckets.reduce((s, b) => s + b.semMetrica, 0)
  const nota = semMetrica > 0 ? ` ${semMetrica} resolução(ões) sem métrica de complexidade.` : ''
  return (
    `Evolução mensal em ${meses(buckets.length)}, ` +
    `${comResolucao} com resolução. Autonomia média (1 a 5) e complexidade típica ` +
    `(O(1) a O(n!), estimada). ${tendencia}.${nota}`
  )
}

export default Linha
