/*
 * LINHA — evolução temporal (a 2ª das 3 visualizações do painel).
 *
 * Duas séries, DUAS ESCALAS, um mesmo eixo de tempo:
 *   • AUTONOMIA média do bucket (1–5) → eixo Y ESQUERDO, neutro (regra 4: autonomia nunca é colormap).
 *   • COMPLEXIDADE típica do bucket (k médio, 0–7) → eixo Y DIREITO, que É a barra de colormap.
 *
 * O TRAÇO diz se houve medição: SÓLIDO entre buckets vizinhos com dado; TRACEJADO quando a linha
 * atravessa buckets sem dado (ver `DASH_VAO` abaixo e `trechosDaSerie` em escalas.ts). O tracejado
 * é continuidade visual, não dado — nenhum marcador no vão, nenhum valor interpolado. Sem isso, o
 * semanal e o diário ficavam MUDOS: quase todo bucket entre duas resoluções está vazio, cada ponto
 * virava um segmento de 1, e polyline de um ponto só não desenha nada.
 *
 * ⚠ Esta visualização absorveu a pergunta da antiga ESPIRAL (ex-Órbitas), removida por redundância:
 * "autonomia e complexidade melhoram com o tempo?" se lê aqui por POSIÇÃO num eixo — canal
 * perceptual mais forte do que o tamanho/ângulo que a espiral usava.
 *
 * O eixo do tempo tem TRÊS GRANULARIDADES (mensal · semanal · diário), trocáveis por um segmented
 * control no cabeçalho. Padrão: MENSAL — o portfólio de um aluno se move em meses, não em dias
 * (`GRANULARIDADE_PADRAO`, tipos.ts). A re-agregação é client-side, do mesmo `dataset` dos outros 4
 * gráficos (`buckets(...)` em escalas.ts): não existe `/api/metricas/evolucao` a consultar, porque
 * duas fontes de verdade sobre o mesmo número divergem e não há como explicar a divergência ao aluno.
 *
 * Fonte: docs/design/specs/02-graficos.md §5 (geometria conferida contra o protótipo) e
 * 00-INDICE.md §2.7 (regras invioláveis).
 *
 * O componente é PURO: recebe o dataset, desenha. Não busca dados, não desenha o cartão do
 * painel, não conhece o seletor de VISUALIZAÇÃO (o de GRANULARIDADE é dele, e o estado é local —
 * é uma lente sobre o mesmo dado, não outra tela: não vai para a URL).
 */
import { useMemo, useState } from 'react'
import { Folder } from 'lucide-react'
import { EmptyState } from '@/components/ui/empty-state'
import { RotuloEstavel } from '@/components/ui/rotulo-estavel'
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
  type BucketTempo,
  type PontoSerie,
  type TrechoSerie,
  buckets,
  LINHA_GRADE_Y,
  LINHA_LARGURA,
  LINHA_TICKS_CLASSE,
  LINHA_Y_BASE,
  LINHA_Y_TOPO,
  MAX_BUCKETS_POR_GRANULARIDADE,
  numeroPt,
  passoDeRotulos,
  pontosDaSerie,
  pontosParaPolyline,
  temSerieTemporal,
  tendenciaDaLinha,
  trechosDaSerie,
  xDaLinha,
  yAutonomiaLinha,
  yClasseLinha,
} from './escalas'
import {
  type Granularidade,
  GRANULARIDADES,
  type PropsGrafico,
  ROTULO_GRANULARIDADE,
} from './tipos'

// ════════════════════════════════════════════════════════════════════════════
// GEOMETRIA
// ════════════════════════════════════════════════════════════════════════════

/**
 * ⚠ COMPAT. A janela DEIXOU DE SER fixa em meses: cada granularidade tem a sua
 * (`MAX_BUCKETS_POR_GRANULARIDADE`, escalas.ts — diário 14 · semanal 12 · mensal 8, e o limite é
 * PIXEL, não gosto: 344px de faixa dividida pela largura do rótulo mono 9px). Este símbolo sobrevive
 * só porque o `PainelDeGraficos` monta com ele o subtítulo do cartão; ele vale para a granularidade
 * PADRÃO (mensal) e nada mais.
 */
export const LINHA_JANELA_MESES = MAX_BUCKETS_POR_GRANULARIDADE.MENSAL

/**
 * DECISÃO (viewBox) — o protótipo é `0 0 400 220` (`LINHA_VIEWBOX`), mas ele **não tem eixo Y**
 * (a própria spec registra isso na Lacuna K). Dois eixos rotulados + a linha do ano não cabem
 * ali. A geometria dos DADOS é preservada byte a byte (x ∈ [40, 384], y ∈ [60, 200] — tudo vem
 * de `escalas.ts`); o que cresce é só a margem:
 *   • direita  +30 → barra de colormap (x 394–401) + ticks de classe (x 405);
 *   • inferior +12 → segunda linha de rótulo do eixo X para o ANO (qualquer das três janelas
 *     pode atravessar o Ano-Novo; sem isso "jan" ou "03.01" seriam ambíguos).
 *
 * ⚠ `y0` NÃO É ZERO (correção — o usuário viu). A banda de dados começa em y=60 (`LINHA_Y_TOPO`,
 * herdado do protótipo), e nada é desenhado acima dela: a faixa y ∈ [0, 60] era ar puro — 26% da
 * altura do gráfico —, e como o SVG escala com a largura do cartão, esse ar virava ~130px de vazio
 * acima da linha. Recorto pelo `min-y` do viewBox em vez de mexer nas escalas: as fórmulas de
 * `escalas.ts` (compartilhadas e testadas) ficam intactas, e o que sobra em cima é a folga real
 * de que as marcas precisam (a coluna de hover começa em y=48; a guia vertical, em y=56).
 */
const VB = { y0: 44, largura: 430, altura: 188 } as const

const GRADE_X0 = 32
const GRADE_X1 = 390

/** Eixo Y DIREITO = a barra de colormap (o mesmo idioma do eixo Y da Carta, spec 02 §2.4). */
const BARRA_X = 394
const BARRA_LARGURA = 7
const TICK_CLASSE_X = 405
const TICK_AUT_X = 26

const ROTULO_TEMPO_Y = 214
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

// ── SÓLIDO × TRACEJADO: o traço diz se houve medição ────────────────────────
//
// REGRA (decisão do usuário): buckets VIZINHOS ambos com dado → traço SÓLIDO (medição contígua).
// Dois buckets com dado separados por buckets SEM dado → são ligados assim mesmo, mas com traço
// TRACEJADO. O tracejado NÃO é dado: significa "aqui não houve medição; a linha é continuidade
// visual". Nenhum marcador é desenhado no vão (o marcador só existe onde há bucket com dado) e
// nenhum valor intermediário é inventado; o bucket vazio continua ocupando o seu x no eixo.
//
// POR QUE (bug): a regra antiga — quebrar a série em todo bucket vazio e parar aí — deixava o
// SEMANAL e o DIÁRIO MUDOS. Neles quase todo bucket entre duas resoluções está vazio, então cada
// ponto virava um segmento de 1, e polyline de 1 ponto não desenha nada: pontos soltos, sem linha.
//
// ⚠ CONSEQUÊNCIA DE PROJETO: o tracejado deixou de ser a identidade da série de complexidade (era
// pontilhada de nascença) e passou a ter UM significado só no gráfico inteiro — "sem medição". Um
// mesmo tracejado com dois sentidos ("é a complexidade" / "não houve medição") seria ilegível. As
// duas séries continuam distinguíveis pelo que sempre as distinguiu de fato: a autonomia é ink,
// mais grossa, com marcador CHEIO; a complexidade é `mid`, mais discreta, com marcador VAZADO
// pintado pelo colormap (regra 3: Big O é sempre ≈ estimado; regra 4: autonomia nunca é colormap).
const DASH_VAO = '5 4'
/** O vão é subordinado ao dado: mesma cor e espessura da série, bem mais apagado. */
const OPACIDADE_VAO = 0.42

const AUT_LARGURA = 2.2
const CLS_LARGURA = 2
const CLS_OPACIDADE = 0.85

// ── Vocabulário do tempo ────────────────────────────────────────────────────

/**
 * Como se fala de cada granularidade. O rodapé e o rótulo acessível são DERIVADOS (Lacuna L):
 * nenhum texto do gráfico pode dizer "mês a mês" quando o eixo está em dias.
 */
const ESCALA: Record<Granularidade, { singular: string; plural: string; passo: string }> = {
  DIARIO: { singular: 'dia', plural: 'dias', passo: 'dia a dia' },
  SEMANAL: { singular: 'semana', plural: 'semanas', passo: 'semana a semana' },
  MENSAL: { singular: 'mês', plural: 'meses', passo: 'mês a mês' },
}

/**
 * Ordem do segmented control: do mais GROSSO ao mais fino — o padrão (mensal) vem primeiro e a
 * leitura "afunila" da esquerda para a direita, como um zoom. `GRANULARIDADES` (tipos.ts) guarda
 * a lista canônica na ordem inversa (fino → grosso); inverto aqui em vez de duplicar a lista.
 */
const ORDEM_SELETOR: readonly Granularidade[] = [...GRANULARIDADES].reverse()

// ════════════════════════════════════════════════════════════════════════════
// PROPS
// ════════════════════════════════════════════════════════════════════════════

export interface PropsLinha extends PropsGrafico {
  /** Esqueleto pulsante no lugar das séries (a grade e os dois eixos continuam). */
  carregando?: boolean
  /**
   * Granularidade do eixo do tempo — CONTROLADA (era estado local).
   *
   * ⚠ Ela subiu de nível porque deixou de ser assunto só da Linha: o painel "estrela selecionada"
   * agora navega entre as resoluções DO PERÍODO clicado, e período depende da granularidade. Com
   * o estado preso aqui dentro, o painel ao lado navegaria por um agrupamento que não é o que
   * está desenhado. Continua sendo uma LENTE (não vai para a URL) — só que uma lente que dois
   * componentes precisam enxergar.
   */
  granularidade: Granularidade
  onGranularidadeChange: (proxima: Granularidade) => void
  className?: string
}

interface DadosBucket {
  bucket: BucketTempo
  i: number
  x: number
  /** `null` só no bucket SEM RESOLUÇÃO (autonomia independe da linguagem — §4.4). */
  yAut: number | null
  /** `null` quando nenhuma resolução do bucket tem classe de complexidade. */
  yCls: number | null
  /** classe média ARREDONDADA — a cor do ponto e do rótulo. `null` sem classe no bucket. */
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
  granularidade,
  onGranularidadeChange,
  className,
}: PropsLinha) {
  const [hoverIdx, setHoverIdx] = useState<number | null>(null)

  /*
   * ⚠ `dataset.todas`, NÃO `dataset.pontos`. A série de AUTONOMIA é autodeclarada e INDEPENDE
   * da linguagem (§4.4): um mês com 3 resoluções em Python é um mês com 3 resoluções — não um
   * mês "sem resolução". Só a série de COMPLEXIDADE se interrompe ali. Vale nas três escalas.
   */
  const janela = useMemo(
    () => buckets(dataset.todas, granularidade),
    [dataset.todas, granularidade],
  )

  const dados = useMemo<DadosBucket[]>(
    () =>
      janela.map((bucket, i) => ({
        bucket,
        i,
        x: xDaLinha(i, janela.length),
        yAut: bucket.mediaAutonomia == null ? null : yAutonomiaLinha(bucket.mediaAutonomia),
        yCls: bucket.mediaClasse == null ? null : yClasseLinha(bucket.mediaClasse),
        k: ordemArredondada(bucket.mediaClasse),
      })),
    [janela],
  )

  /*
   * Trechos SÓLIDOS (buckets vizinhos com dado) + PONTES TRACEJADAS (sobre buckets sem dado).
   * As duas séries têm bases diferentes, e é isso que o vão de cada uma quer dizer:
   *   · autonomia   → vão = período SEM RESOLUÇÃO;
   *   · complexidade → vão = período sem resolução CLASSIFICADA (inclui o mês só de Python:
   *     ali a autonomia tem ponto e segue sólida — §4.4).
   * Por isso a legenda diz "sem medição", e não "sem resolução": é o termo verdadeiro nas duas.
   */
  const trAut = useMemo(() => trechosDaSerie(janela, 'autonomia'), [janela])
  const trCls = useMemo(() => trechosDaSerie(janela, 'classe'), [janela])
  /** Existe algum vão na tela? Só então a legenda explica o tracejado (marca que não está lá não se explica). */
  const temVao = useMemo(
    () => [...trAut, ...trCls].some((t) => !t.continuo),
    [trAut, trCls],
  )
  /** Último ponto COM dado da autonomia — a marca "onde você está agora". */
  const ultimaAut = useMemo(() => {
    const pontos = pontosDaSerie(janela, 'autonomia')
    return pontos[pontos.length - 1] ?? null
  }, [janela])
  const tendencia = useMemo(() => tendenciaDaLinha(janela), [janela])

  /*
   * RÓTULOS DO EIXO X (item 4 do pedido). O passo vem de `passoDeRotulos(n, granularidade)`, que
   * compara o espaçamento em pixels com a largura do rótulo mono 9px — em DIÁRIO (14 buckets em
   * 344px = 26,5px cada) um `dd.mm` de ~28px não cabe em todos, e o passo vira 2.
   *
   * ⚠ Rotular RALO não é agregar RALO: os 14 dias continuam ocupando o eixo (inclusive os vazios) —
   * só o texto é rareado. E a contagem vai DE TRÁS PARA A FRENTE (`(n−1−i) % passo === 0`), nunca
   * da frente: assim o ÚLTIMO bucket — a data de referência da leitura, "onde você está agora" — é
   * SEMPRE rotulado, e o espaçamento entre rótulos é uniforme. Ancorar no primeiro deixaria o
   * último de fora, ou o colocaria colado no penúltimo rótulo (exatamente a sobreposição a evitar).
   */
  const passoRotulos = passoDeRotulos(dados.length, granularidade)
  const rotulados = useMemo(() => {
    const marcados = new Set<number>()
    for (let i = dados.length - 1; i >= 0; i -= passoRotulos) marcados.add(i)
    return marcados
  }, [dados.length, passoRotulos])

  /**
   * O ano aparece sob o PRIMEIRO rótulo visível e sempre que o ano VIRA — nas três escalas
   * (`jan` e `03.01` são igualmente ambíguos sem ele). Só sob buckets rotulados: um ano solto
   * embaixo de um bucket sem rótulo seria uma segunda linha de texto sem âncora.
   */
  const anosVisiveis = useMemo(() => {
    const mostrar = new Set<number>()
    let anterior: number | null = null
    for (let i = 0; i < dados.length; i++) {
      if (!rotulados.has(i)) continue
      const ano = dados[i].bucket.inicio.getFullYear()
      if (anterior === null || ano !== anterior) mostrar.add(i)
      anterior = ano
    }
    return mostrar
  }, [dados, rotulados])

  /** ≥ 2 buckets COM dado. Abaixo disso não se desenha linha — só os pontos (pedido do produto). */
  const temSerie = temSerieTemporal(janela)
  /** Vazio = nenhuma RESOLUÇÃO (não "nenhuma plotável"): a autonomia existe sem métrica. */
  const vazio = !carregando && dataset.todas.length === 0

  const idxSelecionado = useMemo(() => {
    if (!selecionadoId) return -1
    return janela.findIndex((b) => b.resolucoes.some((p) => p.resolucaoId === selecionadoId))
  }, [janela, selecionadoId])

  /** O bucket "ativo" — o hover manda; sem hover, o bucket da resolução selecionada. */
  const idxAtivo = hoverIdx ?? (idxSelecionado >= 0 ? idxSelecionado : null)
  const ativo = idxAtivo == null ? null : dados[idxAtivo] ?? null

  /** Largura da coluna de hover (também é o passo entre buckets). */
  const passo = janela.length > 1 ? LINHA_LARGURA / (janela.length - 1) : LINHA_LARGURA

  function trocarGranularidade(proxima: Granularidade) {
    // O índice do bucket muda de significado ao reagregar: um hover pendurado apontaria para
    // outro período. A SELEÇÃO (resolucaoId) sobrevive — ela é de uma resolução, não de um bucket.
    setHoverIdx(null)
    onGranularidadeChange(proxima)
  }

  function selecionar(b: BucketTempo) {
    // Só as COM métrica viram estrela nos outros gráficos — selecionar uma resolução que não
    // existe na Carta deixaria a seleção pendurada. Bucket sem nenhuma plotável não é clicável.
    if (!onSelecionar || b.comMetrica.length === 0) return
    // `bucket.comMetrica` está em ordem cronológica ASCENDENTE.
    // DECISÃO: o bucket é um AGREGADO — não dá para "selecionar um mês". Clicar seleciona a
    // resolução MAIS RECENTE do período, e o tooltip diz isso em voz alta quando há mais de uma.
    // Nunca escolher em silêncio: o clique é explicado antes de acontecer.
    onSelecionar(b.comMetrica[b.comMetrica.length - 1].resolucaoId)
  }

  return (
    <div className={cn('flex flex-col gap-2.5', className)}>
      {/* ── cabeçalho do gráfico: legenda (o que é) + granularidade (em que escala) ───── */}
      <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2">
        <Legenda tema={tema} temVao={temVao && !carregando} />
        <SeletorDeGranularidade value={granularidade} onChange={trocarGranularidade} />
      </div>

      <div className="relative" onMouseLeave={() => setHoverIdx(null)}>
        {/*
         * `role="group"`, não `role="img"`: por ARIA, os descendentes de um `img` são
         * PRESENTATIONAL — os alvos de bucket (`role="button"`) simplesmente não existiriam
         * para o leitor de tela. Um gráfico interativo é um grupo.
         */}
        <svg
          viewBox={`0 ${VB.y0} ${VB.largura} ${VB.altura}`}
          width="100%"
          className="block"
          role="group"
          aria-label={rotuloAcessivel(janela, granularidade, tendencia.texto, carregando)}
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

          {/* ── eixo X: dd.mm (dia/semana) ou mmm/aa (mês) + o ano quando ele vira ───── */}
          {dados.map(({ bucket, i, x }) => {
            if (!rotulados.has(i)) return null
            return (
              <g key={bucket.chave}>
                <text
                  x={x}
                  y={ROTULO_TEMPO_Y}
                  textAnchor="middle"
                  fontSize={9}
                  className={cn(
                    'font-mono tabular',
                    idxAtivo === i ? 'fill-mid' : 'fill-soft',
                    bucket.total === 0 && 'opacity-55',
                  )}
                >
                  {bucket.rotulo}
                </text>
                {anosVisiveis.has(i) && (
                  <text
                    x={x}
                    y={ROTULO_ANO_Y}
                    textAnchor="middle"
                    fontSize={8.5}
                    className="fill-soft font-mono tabular opacity-70"
                  >
                    {bucket.inicio.getFullYear()}
                  </text>
                )}
              </g>
            )
          })}

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

              {/* ── série COMPLEXIDADE: traço NEUTRO e discreto, marcadores no COLORMAP ──
                   Sólido entre buckets vizinhos COM classe; tracejado ao atravessar bucket sem
                   classe (inclusive o que tem resolução, mas sem métrica — ex.: Python). */}
              {temSerie && (
                <TracosDaSerie
                  trechos={trCls}
                  prefixo="cls"
                  cor="stroke-mid"
                  largura={CLS_LARGURA}
                  opacidade={CLS_OPACIDADE}
                />
              )}

              {/* ── série AUTONOMIA: NEUTRA (regra 4) ────────────────────────────────
                   Ela ATRAVESSA sólida os buckets que só têm resolução não-Java: autonomia é
                   autodeclarada e independe da linguagem (§4.4). Só vira tracejado no bucket em
                   que NADA foi enviado — ali, de fato, não houve medição de coisa alguma. */}
              {temSerie && (
                <TracosDaSerie
                  trechos={trAut}
                  prefixo="aut"
                  cor="stroke-ink"
                  largura={AUT_LARGURA}
                  opacidade={1}
                />
              )}

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

              {/* "onde você está agora": halo no último bucket COM dado da série de autonomia */}
              <Agora ponto={ultimaAut} />

              {/* ── alvos de hover/clique: uma coluna por bucket ────────────────────── */}
              {dados.map((d) => {
                const x0 = Math.max(GRADE_X0, d.x - passo / 2)
                const x1 = Math.min(GRADE_X1, d.x + passo / 2)
                const clicavel = !!onSelecionar && d.bucket.comMetrica.length > 0
                // Bucket sem resolução não vira parada de tabulação muda: ou é botão de verdade,
                // ou é um `img` com rótulo (o leitor ainda ouve "mar/2026: sem resolução").
                // ⚠ SEM `<title>` aqui: o navegador desenharia o tooltip NATIVO por cima do
                // callout — dois pop-ups, e o nativo roubando o clique. A acessibilidade vai
                // inteira por `aria-label`; o tooltip visual é só o callout abaixo.
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
                  />
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
      {/*
       * ⚠ NÃO EXISTE MAIS RODAPÉ (decisão do usuário). Ele dizia "autonomia sobe · complexidade
       * típica cai · 6 meses" — uma leitura que o próprio gráfico já entrega pela forma das duas
       * linhas, logo acima, e que o painel repete no rodapé honesto ("17 de 18 resoluções
       * plotadas"). Era uma terceira linha de texto mono num cartão que já tem legenda, seletor e
       * rodapé: ruído, não informação.
       *
       * A tendência NÃO morreu — ela continua sendo calculada (`tendenciaDaLinha`) e vai inteira
       * para o `aria-label` do SVG, que é o único lugar onde ela é insubstituível: quem não vê o
       * gráfico não tem como ler a forma das linhas.
       */}
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// PEÇAS
// ════════════════════════════════════════════════════════════════════════════

/**
 * SEGMENTED CONTROL de granularidade — a mesma pele do `SeletorDeGrafico` (mono 11px, hairline,
 * raio 3px), mas com o ativo em `elevated`/`ink` em vez do `ink` sólido: os dois seletores
 * aparecem no mesmo cartão, e o sólido é do de cima (a VISUALIZAÇÃO manda; a escala do tempo é
 * uma lente dentro dela). A hairline fica no trilho, não em cada botão — assim a altura não muda
 * quando o ativo troca.
 *
 * ⚠ `role="group"` + `aria-pressed`, não `tablist`/`tab`: não há painéis a alternar — são três
 * estados do MESMO gráfico. Três `<button>` de verdade, cada um na sua parada de tabulação, com
 * o anel de foco do sistema (`.ci-foco-botao`).
 *
 * ⚠ BUG CORRIGIDO ("os botões trocam de lugar quando clico em semanal/diário"): o botão ativo
 * ganha `font-semibold`, e negrito é MAIS LARGO que regular. Como os três dividem a mesma fileira,
 * engordar um empurrava os outros — a cada clique a fileira inteira se rearranjava sob o cursor.
 * A correção não é tirar o negrito (ele é o sinal de "este está ativo"): é RESERVAR a largura do
 * negrito o tempo todo, com `RotuloEstavel` — um gêmeo invisível em negrito define a largura, e o
 * rótulo real é pintado por cima. A caixa nunca muda de tamanho; só a tinta muda.
 */
function SeletorDeGranularidade({
  value,
  onChange,
}: {
  value: Granularidade
  onChange: (proxima: Granularidade) => void
}) {
  return (
    <div
      role="group"
      aria-label="Granularidade do eixo do tempo"
      className="flex shrink-0 items-center gap-px rounded-ci border border-line bg-recess p-[2px]"
    >
      {ORDEM_SELETOR.map((g) => {
        const ativo = g === value
        return (
          <button
            key={g}
            type="button"
            aria-pressed={ativo}
            onClick={() => onChange(g)}
            className={cn(
              'ci-foco-botao cursor-pointer rounded-ci px-2 py-[3px] font-mono text-[11px] transition-colors',
              ativo
                ? 'bg-elevated font-semibold text-ink'
                : 'bg-transparent text-soft hover:text-ink',
            )}
          >
            <RotuloEstavel>{ROTULO_GRANULARIDADE[g]}</RotuloEstavel>
          </button>
        )
      })}
    </div>
  )
}

/**
 * Uma série inteira: os trechos SÓLIDOS (medição contígua) e as PONTES TRACEJADAS (vão sem
 * medição). Mesma cor e mesma espessura nos dois — é a MESMA série; o que muda é o traço, que
 * carrega a única distinção que importa: houve, ou não houve, medição entre os dois pontos.
 *
 * A ponte é desenhada com opacidade reduzida porque ela é subordinada ao dado: ninguém pode
 * confundir continuidade visual com medição. E ela nunca ganha marcador — o marcador sai de
 * `dados`, que só conhece bucket COM dado.
 */
function TracosDaSerie({
  trechos,
  prefixo,
  cor,
  largura,
  opacidade,
}: {
  trechos: TrechoSerie[]
  prefixo: string
  /** Classe utilitária de stroke (neutra: `stroke-ink` na autonomia, `stroke-mid` na complexidade). */
  cor: string
  largura: number
  opacidade: number
}) {
  return (
    <>
      {trechos.map((t) => (
        <polyline
          key={`${prefixo}-${t.chave}`}
          points={pontosParaPolyline(t.pontos)}
          fill="none"
          className={cor}
          strokeWidth={largura}
          strokeLinejoin="round"
          // Traço cheio = ponta arredondada. No tracejado a ponta reta preserva o ritmo do dash
          // (a redonda alonga cada risco e o padrão vira quase uma linha cheia).
          strokeLinecap={t.continuo ? 'round' : 'butt'}
          strokeDasharray={t.continuo ? undefined : DASH_VAO}
          opacity={t.continuo ? opacidade : opacidade * OPACIDADE_VAO}
        />
      ))}
    </>
  )
}

/**
 * Legenda (spec 02 §5.1). A série neutra (autonomia), a série da complexidade, a CONFIANÇA
 * (regra 3) e — só quando existe um vão na tela — o significado do TRACEJADO.
 *
 * ⚠ ENXUTA (correção — o usuário viu). Ao trocar para semanal/diário aparecem vãos (quase todo
 * bucket entre duas resoluções está vazio nessas escalas), e o item do tracejado ENTRAVA com uma
 * frase inteira: a legenda quebrava em duas linhas e o cabeçalho do gráfico virava um parágrafo.
 * Cada item agora é um RÓTULO, não uma explicação — a explicação inteira já vive no `?` do painel
 * ("Traço sólido × traço tracejado", "Medido × ≈ estimado"), que é onde ela pode ser lida com
 * calma. A legenda diz o que a marca É; o `?` diz o que ela SIGNIFICA.
 *
 * ⚠ O texto é "sem medição", não "sem resolução": na série de autonomia o vão é mesmo um período
 * sem resolução, mas na de complexidade ele também aparece em período COM resolução e sem métrica
 * (ex.: Python). "Sem resolução" seria falso ali — e o item da legenda é o contrato do traço.
 */
function Legenda({ tema, temVao }: { tema: Tema; temVao: boolean }) {
  const corExemplo = corDaClasse(3, tema)
  return (
    <div className="flex flex-wrap items-center gap-x-3.5 gap-y-1 py-0.5 font-mono text-[10px] text-mid">
      <span className="flex items-center gap-1.5">
        <span className="h-[2.4px] w-3.5 rounded-ci-sm bg-ink" aria-hidden="true" />
        autonomia
      </span>
      {/* A complexidade e a sua natureza (≈ estimada, marcador VAZADO — regra 3) num item só: são
          a mesma série, e o anel vazado é justamente a marca dela no gráfico. */}
      <span className="flex items-center gap-1.5">
        <svg width={22} height={8} aria-hidden="true" className="overflow-visible">
          <line
            x1={0}
            x2={22}
            y1={4}
            y2={4}
            className="stroke-mid"
            strokeWidth={CLS_LARGURA}
            strokeLinecap="round"
            opacity={CLS_OPACIDADE}
          />
          <circle cx={11} cy={4} r={3.2} fill="var(--panel-chart)" stroke={corExemplo} strokeWidth={1.6} />
        </svg>
        complexidade típica (≈ estimada)
      </span>
      {temVao && (
        <span className="flex items-center gap-1.5 text-soft">
          <svg width={16} height={8} aria-hidden="true" className="overflow-visible">
            <line
              x1={0}
              x2={16}
              y1={4}
              y2={4}
              className="stroke-mid"
              strokeWidth={CLS_LARGURA}
              strokeDasharray={DASH_VAO}
              strokeLinecap="butt"
            />
          </svg>
          sem medição
        </span>
      )}
    </div>
  )
}

/** Halo no último bucket com dado da autonomia — "onde você está agora" (protótipo, §5.3). */
function Agora({ ponto }: { ponto: PontoSerie | null }) {
  if (!ponto) return null
  return (
    <circle
      cx={ponto.x}
      cy={ponto.y}
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
 * Tooltip do bucket ativo — o ÚNICO pop-up do gráfico (não há `<title>` no SVG). Mono em tudo
 * (regra 5), `≈` quando a classe do período é estimada (regra 3), o rótulo POR EXTENSO do bucket
 * (`14.03.2026` · `semana de 09.03.2026` · `mar/2026` — só ele desambigua a escala) e a CONTAGEM
 * de resoluções: o bucket é um agregado, e isso não pode ficar implícito.
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
  /** SEM RESOLUÇÃO ≠ sem métrica. O bucket só é vazio quando o aluno não enviou nada. */
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
          {bucket.rotuloLongo}
        </span>
        <span className="font-mono text-[10px] text-soft tabular">
          {semResolucao ? 'sem resolução' : pluralPt(bucket.total, 'resolução', 'resoluções')}
          {bucket.semMetrica > 0 && ` · ${bucket.semMetrica} sem métrica`}
        </span>
      </div>

      {/* Autonomia: existe em TODO bucket com resolução — independe da linguagem (§4.4). */}
      {!semResolucao && (
        <span className="flex items-center gap-1.5 font-mono text-[10px] text-mid tabular">
          <span className="h-[2px] w-2.5 rounded-ci-sm bg-ink" aria-hidden="true" />
          aut. média {numeroPt(bucket.mediaAutonomia as number, 1)}/5
        </span>
      )}

      {/* Complexidade: só quando ao menos uma resolução do bucket foi classificada. */}
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
          {/*
           * ⚠ SEM "classe média 2,0" (correção — o usuário perguntou o que era, e a pergunta é a
           * resposta). Aquele número era a média CRUA das ordens do colormap (0..7) — escala
           * interna do desenho, não uma grandeza do domínio: ninguém escreve código "2,0". O
           * rótulo ao lado (≈ O(n)) JÁ É essa média, arredondada para uma classe que existe de
           * verdade; mostrar os dois era exibir a mesma coisa duas vezes, uma delas em jargão.
           * O `?` do painel explica o arredondamento.
           */}
          {comPrefixoEstimado(rotuloCanonico(k), CONFIANCA_BIG_O)}
          {bucket.comMetrica.length > 1 && (
            <span className="text-soft">
              · média de {pluralPt(bucket.comMetrica.length, 'resolução', 'resoluções')}
            </span>
          )}
        </span>
      ) : (
        !semResolucao && (
          <span className="font-mono text-[10px] text-soft">sem métrica de complexidade</span>
        )
      )}

      {clicavel && bucket.comMetrica.length > 1 && (
        <span className="font-mono text-[9px] text-soft">
          clique → abre o período (navegue pelas {bucket.comMetrica.length} ao lado)
        </span>
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
 * perto da direita, o tooltip inverte para a esquerda; no topo da banda, cresce para BAIXO.
 *
 * ⚠ O `y` do dado é COORDENADA DO viewBox, que começa em `VB.y0` (o topo morto foi recortado) —
 * a porcentagem tem de descontar essa origem, senão o tooltip desce ~24% da altura do gráfico.
 */
function posicaoTooltip(x: number, y: number) {
  const inverterX = x > VB.largura * 0.62
  const relY = (y - VB.y0) / VB.altura
  const abaixo = relY < 0.28 // o topo da banda de dados: aí o tooltip cresce para baixo
  return {
    left: `${((x / VB.largura) * 100).toFixed(2)}%`,
    top: `${(relY * 100).toFixed(2)}%`,
    transform: `translate(${inverterX ? 'calc(-100% - 10px)' : '10px'}, ${abaixo ? '10px' : 'calc(-100% - 10px)'})`,
  }
}

/**
 * Texto do `aria-label` da coluna — é o que o leitor de tela lê ao focar o bucket (e a ÚNICA via
 * de acessibilidade dele: não há `<title>` no SVG). "sem resolução" é reservado ao período em que
 * NADA foi enviado. Bucket com resoluções sem métrica diz quantas ficaram sem classe: o trabalho
 * existiu, só não pôde ser medido.
 */
function resumoDoBucket(d: DadosBucket): string {
  const { bucket, k } = d
  if (bucket.total === 0) return `${bucket.rotuloLongo}: sem resolução`

  const partes = [
    `${bucket.rotuloLongo}: ${pluralPt(bucket.total, 'resolução', 'resoluções')}`,
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

/** "8 meses" · "12 semanas" · "14 dias" — a extensão da janela, na escala corrente. */
function periodos(n: number, g: Granularidade): string {
  return pluralPt(n, ESCALA[g].singular, ESCALA[g].plural)
}

function rotuloAcessivel(
  janela: BucketTempo[],
  granularidade: Granularidade,
  tendencia: string,
  carregando: boolean,
): string {
  const escala = ESCALA[granularidade].passo
  if (carregando) return `Carregando a evolução ${escala}.`
  const comResolucao = janela.filter((b) => b.total > 0).length
  if (comResolucao === 0) return `Evolução ${escala}: nenhuma resolução enviada.`
  const semMetrica = janela.reduce((s, b) => s + b.semMetrica, 0)
  const nota = semMetrica > 0 ? ` ${semMetrica} resolução(ões) sem métrica de complexidade.` : ''
  return (
    `Evolução ${escala} em ${periodos(janela.length, granularidade)}, ` +
    `${comResolucao} com resolução. Autonomia média (1 a 5) e complexidade típica ` +
    `(O(1) a O(n!), estimada). ${tendencia}.${nota}`
  )
}

export default Linha
