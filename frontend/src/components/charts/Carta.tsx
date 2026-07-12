/*
 * CARTA CELESTE — o gráfico-assinatura do Órbita.
 *
 * Um scatter que se lê como um céu: X = Índice de Autonomia IA (1→5), Y = classe de
 * complexidade (O(1) EMBAIXO → O(n!) NO TOPO — quanto mais alto, mais custoso). A cor do
 * ponto é a classe (o colormap é a ÚNICA cor do sistema) e o eixo Y É o próprio colormap.
 *
 * A metáfora central são as CONSTELAÇÕES: uma polyline por desafio, ligando suas resoluções
 * em ordem cronológica. É a trajetória do aluno naquele desafio — força bruta com muito apoio
 * de IA → solução refinada e autônoma. É exatamente o dado que a decisão D2 (nova resolução
 * no lugar de editar) produz, e o que a pesquisa mede.
 *
 * ⚠ RODADA DE CORREÇÃO (o usuário rodou o app e viu):
 *   1. O `<title>` dentro do SVG fazia o NAVEGADOR desenhar o tooltip NATIVO por cima do nosso
 *      callout — dois pop-ups, e o nativo roubava o clique. MORREU. A acessibilidade fica por
 *      `role` + `aria-label`; o único tooltip visual é o callout deste componente.
 *   2. O JITTER morreu. A Carta é DISCRETA (5 autonomias × 8 classes = 40 células): espalhar as
 *      coincidentes num anel de 5px produzia marcadores grudados e impossíveis de acertar. Agora
 *      cada célula ocupada é UM cluster — um marcador, um alvo, um callout — que declara quantas
 *      resoluções tem pelo número no núcleo. Nenhum dado some: o painel lateral navega entre elas.
 *
 * Componente PURO: recebe o dataset já montado (`./dataset`), a geometria vem de `./escalas`
 * (`agruparEmClusters`, `raioDoCluster`, `tracosDasConstelacoes`) e as cores de `@/domain/enums`.
 * Não busca dado, não guarda seleção — só desenha e avisa.
 *
 * Specs: docs/design/specs/02-graficos.md §2 (geometria extraída do protótipo) · 00-INDICE
 * §2.7 (regras invioláveis) · §6-A Lacuna 11 (constelações).
 */
import { useMemo, useState } from 'react'
import { Folder } from 'lucide-react'
import {
  CONFIANCA_BIG_O,
  LINGUAGEM_META,
  NOTA_METRICAS_SO_JAVA,
  comPrefixoEstimado,
  corDaClasse,
  rotuloCanonico,
  rotuloConfiancaMotor,
  rotuloCurto,
  tintaDaClasse,
} from '@/domain/enums'
import { cn, pluralPt } from '@/lib/utils'
import {
  CARTA_CELULA_O1,
  CARTA_EIXO_Y,
  CARTA_GRADE_X_FIM,
  CARTA_PLOT,
  CARTA_TICKS_Y,
  CARTA_TICK_X_Y,
  CARTA_TICK_Y_X,
  CARTA_TITULO_X,
  CARTA_VIEWBOX,
  CARTA_X,
  CLUSTER_ROTULO_MIN,
  agruparEmClusters,
  celulasEixoY,
  clusterDoPonto,
  dataCompleta,
  posicaoCallout,
  raioDoCluster,
  tracosDasConstelacoes,
  xDaAutonomia,
  yDaClasse,
} from './escalas'
import type { ClusterCarta, PontoPlotavel, PropsGrafico } from './tipos'

export interface CartaProps extends PropsGrafico {
  /** Enquanto a query não resolve: grade + eixo + estrelas-fantasma pulsando. */
  carregando?: boolean
  className?: string
}

// ── Marcas (spec 02 §2.8) ───────────────────────────────────────────────────
// Os valores abaixo são os da estrela SOLITÁRIA (total = 1) — `raioDoCluster(1)` devolve
// exatamente `{ halo: 7, nucleo: 2.6 }`. Tudo o mais (seleção, foco, alvo) é DERIVADO do halo,
// e não uma constante solta: assim um cluster grande não fica com o anel de seleção POR DENTRO
// do próprio marcador. Para total = 1 as fórmulas reproduzem os números do protótipo.
/** Ganho de raio no hover: o marcador inteiro respira +2 (era HALO_R 7 → HALO_R_HOVER 9). */
const HOVER_GANHO = 2
/** Halo da seleção: 13 quando total = 1 (protótipo) = 7 + 6. */
const SEL_HALO_GANHO = 6
/** Anel da seleção: 8 quando total = 1 (protótipo) = 7 + 1. */
const SEL_ANEL_GANHO = 1
/**
 * No CLUSTER o anel de seleção precisa se afastar mais: o marcador já tem um contorno traçado
 * no raio do halo, e dois anéis a 1px de distância viram sujeira.
 */
const SEL_ANEL_GANHO_CLUSTER = 3
/** Braços da cruzeta: 129.1 ± 16.1 no protótipo → 7 + 9.1. */
const CRUZETA_GANHO = 9.1
/** Vão central da cruzeta no cluster — a linha NÃO pode riscar o número. */
const CRUZETA_VAO = 2
/** Anel de foco de teclado: 11.5 quando total = 1 (protótipo) = 7 + 4.5. */
const FOCO_GANHO = 4.5
/** Alvo de clique invisível — o núcleo de 2.6px é pequeno demais para mouse e toque. */
const ALVO_R_MIN = 12
const ALVO_GANHO = 4

/**
 * Céu decorativo (spec 02 §2.6): 9 estrelas cenográficas FIXAS. Não são dado — são
 * `aria-hidden` e não recebem eventos. No claro viram pontos de tinta (r +0.4, opacidade +.1).
 */
const CEU: readonly { cx: number; cy: number; r: number; o: number }[] = [
  { cx: 120, cy: 60, r: 0.9, o: 0.7 },
  { cx: 250, cy: 45, r: 0.7, o: 0.5 },
  { cx: 390, cy: 70, r: 1.0, o: 0.8 },
  { cx: 500, cy: 50, r: 0.7, o: 0.5 },
  { cx: 210, cy: 160, r: 0.7, o: 0.45 },
  { cx: 470, cy: 150, r: 0.9, o: 0.6 },
  { cx: 95, cy: 120, r: 0.7, o: 0.5 },
  { cx: 345, cy: 180, r: 0.7, o: 0.45 },
  { cx: 520, cy: 200, r: 0.8, o: 0.55 },
]

/** Estrelas-fantasma do estado *carregando* (autonomia, k) — posições fixas, sem dado. */
const FANTASMAS: readonly [number, number][] = [
  [2, 1],
  [3, 2],
  [1, 2],
  [4, 3],
  [3, 5],
  [5, 4],
  [4, 2],
]

/** Rótulo do eixo X (1..5) — a escala de autonomia é discreta e completa. */
const TICKS_X = [1, 2, 3, 4, 5] as const

/** Quantos títulos diferentes o callout de um cluster lista antes de reticenciar. */
const MAX_TITULOS_CALLOUT = 2

/**
 * REGRA 3 (inviolável). MEDIDO × ≈ ESTIMADO é natureza do TIPO da métrica, não do valor: a
 * classe de tempo SEMPRE sai de inferência estática → SEMPRE ≈ ESTIMADO → marcador VAZADO.
 * (A confiança do motor — ALTA/MEDIA/BAIXA — é outro eixo e vira TEXTO no callout.)
 */
const MEDIDO = CONFIANCA_BIG_O === 'MEDIDO'
const NATUREZA = MEDIDO ? 'medida' : 'estimada por análise estática'

export function Carta({
  dataset,
  selecionadoId,
  onSelecionar,
  tema,
  carregando = false,
  className,
}: CartaProps) {
  // O hover/foco agora endereça o CLUSTER (a célula), não a resolução: é o cluster que é o
  // alvo, o callout e o realce. A SELEÇÃO continua sendo de uma resolução (é ela que o painel
  // lateral abre) — o cluster dela é derivado por `clusterDoPonto`.
  const [hoverChave, setHoverChave] = useState<string | null>(null)
  const [focoChave, setFocoChave] = useState<string | null>(null)

  const escuro = tema === 'dark'
  const { pontos, constelacoes } = dataset
  const clicavel = typeof onSelecionar === 'function'

  /**
   * UMA CÉLULA = UM MARCADOR. O jitter morreu: duas resoluções na mesma coordenada
   * (autonomia × classe) não são mais espalhadas lado a lado — elas viram um cluster, na posição
   * EXATA da célula, que cresce e escreve quantas são. As constelações usam ESTAS posições.
   */
  const clusters = useMemo(() => agruparEmClusters(pontos), [pontos])

  /**
   * Os traços das constelações já vêm sem segmentos degenerados: duas resoluções seguidas do
   * mesmo desafio que caem na MESMA célula não geram um segmento de comprimento zero, e uma
   * constelação inteira dentro de uma célula simplesmente não desenha linha (o cluster já a
   * declara pelo número). Descartar o traço não descarta o dado.
   */
  const tracos = useMemo(() => tracosDasConstelacoes(constelacoes), [constelacoes])

  const clusterSelecionado = clusterDoPonto(clusters, selecionadoId)

  // Hover e foco de teclado revelam a mesma informação (paridade: um scatter que só responde
  // ao mouse exclui quem navega por teclado). A seleção é o estado persistente.
  const destacadaChave = hoverChave ?? focoChave ?? clusterSelecionado?.chave ?? null
  const destacado = destacadaChave
    ? (clusters.find((c) => c.chave === destacadaChave) ?? null)
    : null

  /** Um cluster pode misturar desafios: acende TODAS as constelações que passam por ele. */
  const desafiosAcesos = useMemo(
    () => new Set(destacado?.pontos.map((p) => p.desafioId) ?? []),
    [destacado],
  )

  const vazio = !carregando && pontos.length === 0

  return (
    <div className={cn('relative w-full', className)}>
      <svg
        viewBox={`0 0 ${CARTA_VIEWBOX.largura} ${CARTA_VIEWBOX.altura}`}
        width="100%"
        className="block h-auto w-full"
        role="group"
        aria-label="Carta de resoluções: autonomia de IA (1 a 5) no eixo horizontal, classe de complexidade no eixo vertical."
      >
        {/* ── Céu decorativo ─────────────────────────────────────────────── */}
        <g aria-hidden className="pointer-events-none">
          {CEU.map((e, i) => (
            <circle
              key={i}
              cx={e.cx}
              cy={e.cy}
              r={escuro ? e.r : e.r + 0.4}
              fill="var(--ink)"
              opacity={escuro ? e.o : Math.min(1, e.o + 0.1)}
            />
          ))}
        </g>

        {/* ── Grade, eixos e o eixo Y = colormap ─────────────────────────── */}
        <g aria-hidden className="pointer-events-none">
          {/* 7 linhas horizontais (k = 1..7) */}
          {[1, 2, 3, 4, 5, 6, 7].map((k) => (
            <line
              key={`h${k}`}
              x1={CARTA_PLOT.x0}
              x2={CARTA_GRADE_X_FIM}
              y1={yDaClasse(k)}
              y2={yDaClasse(k)}
              stroke="var(--graf-grade)"
            />
          ))}
          {/* 4 verticais (autonomia 2..5) */}
          {CARTA_X.slice(1).map((x) => (
            <line
              key={`v${x}`}
              x1={x}
              x2={x}
              y1={CARTA_PLOT.y0}
              y2={CARTA_PLOT.y1}
              stroke="var(--graf-grade)"
            />
          ))}
          {/* linha de base (k = 0) e eixo Y */}
          <line
            x1={CARTA_PLOT.x0}
            x2={CARTA_GRADE_X_FIM}
            y1={CARTA_PLOT.y1}
            y2={CARTA_PLOT.y1}
            stroke="var(--graf-eixo)"
          />
          <line
            x1={CARTA_PLOT.x0}
            x2={CARTA_PLOT.x0}
            y1={CARTA_PLOT.y0}
            y2={CARTA_PLOT.y1}
            stroke="var(--graf-eixo)"
          />

          {/* O eixo Y É a barra de colormap: uma célula por classe (k = 1..7)… */}
          {celulasEixoY().map((c) => (
            <rect
              key={`cx${c.k}`}
              x={CARTA_EIXO_Y.x}
              y={c.y}
              width={CARTA_EIXO_Y.largura}
              height={c.altura}
              fill={corDaClasse(c.k, tema)}
            />
          ))}
          {/* …e o "piso" da escala: O(1) não tem faixa (cairia abaixo da base) — sem este
              quadrado o verde nunca apareceria no eixo (spec 02, Lacuna C). */}
          <rect
            x={CARTA_CELULA_O1.x}
            y={CARTA_CELULA_O1.y}
            width={CARTA_CELULA_O1.lado}
            height={CARTA_CELULA_O1.lado}
            fill={corDaClasse(CARTA_CELULA_O1.k, tema)}
          />

          {/* Ticks do eixo Y (k = 0, 2, 4, 7): sem eles ninguém sabe qual cor é qual classe. */}
          {CARTA_TICKS_Y.map((k) => (
            <text
              key={`ty${k}`}
              x={CARTA_TICK_Y_X}
              y={yDaClasse(k) + 3}
              textAnchor="end"
              fontSize={9}
              fill="var(--soft)"
              className="font-mono"
            >
              {rotuloCurto(k)}
            </text>
          ))}

          {/* Eixo X: autonomia 1..5 + título. */}
          {TICKS_X.map((a) => (
            <text
              key={`tx${a}`}
              x={xDaAutonomia(a)}
              y={CARTA_TICK_X_Y}
              textAnchor="middle"
              fontSize={11}
              fill="var(--soft)"
              className="font-mono tabular"
            >
              {a}
            </text>
          ))}
          <text
            x={CARTA_TITULO_X.x}
            y={CARTA_TITULO_X.y}
            textAnchor="middle"
            fontSize={11}
            letterSpacing={1}
            fill="var(--mid)"
            className="font-mono"
          >
            AUTONOMIA IA →
          </text>
        </g>

        {/* ── Constelações: a trajetória do aluno em cada desafio ────────── */}
        <g aria-hidden className="pointer-events-none">
          {tracos.map((t) => {
            const acesa = desafiosAcesos.has(t.desafioId)
            return (
              <polyline
                key={t.desafioId}
                points={t.polyline}
                fill="none"
                stroke="var(--steel)"
                strokeWidth={1}
                strokeLinejoin="round"
                opacity={acesa ? 0.5 : escuro ? 0.22 : 0.3}
              />
            )
          })}
        </g>

        {/* ── Decoração do cluster selecionado (fica ABAIXO dos marcadores para não roubar
               o alvo de clique nem bagunçar a ordem de tabulação) ──────────── */}
        {clusterSelecionado && <DecoracaoSelecionada cluster={clusterSelecionado} tema={tema} />}

        {/* ── Os marcadores (uma CÉLULA ocupada = um marcador) ───────────── */}
        {!carregando && (
          <g>
            {clusters.map((c) => (
              <Marcador
                key={c.chave}
                cluster={c}
                tema={tema}
                selecionado={c.chave === clusterSelecionado?.chave}
                ativo={c.chave === destacadaChave}
                focado={c.chave === focoChave}
                clicavel={clicavel}
                onSelecionar={onSelecionar}
                onHover={setHoverChave}
                onFoco={setFocoChave}
              />
            ))}
          </g>
        )}

        {/* ── Carregando: estrelas-fantasma (sem cor — a ausência de medida não é medida) ── */}
        {carregando && (
          <g aria-hidden className="pointer-events-none">
            {FANTASMAS.map(([a, k], i) => (
              <circle
                key={i}
                cx={xDaAutonomia(a)}
                cy={yDaClasse(k)}
                r={raioDoCluster(1).nucleo}
                fill="var(--line)"
                className="ci-pulse"
                style={{ animationDelay: `${i * 0.12}s` }}
              />
            ))}
          </g>
        )}
      </svg>

      {/* ── Callout do cluster em destaque (HTML sobre o SVG) ─────────────
             ⚠ É o ÚNICO tooltip. Não existe `<title>` no SVG: o navegador desenharia o tooltip
             NATIVO por cima deste e roubaria o clique. */}
      {destacado && !carregando && <Callout cluster={destacado} tema={tema} />}

      {/* ── Vazio: o céu fica, as estrelas é que não existem ─────────────── */}
      {vazio && <CartaVazia total={dataset.total} semMetrica={dataset.semMetrica.total} />}
    </div>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// MARCADOR — uma célula ocupada. Solitária = a estrela de sempre; 2+ = a estrela com o número.
// ════════════════════════════════════════════════════════════════════════════

interface MarcadorProps {
  cluster: ClusterCarta
  tema: PropsGrafico['tema']
  /** A resolução selecionada mora NESTE cluster — o realce é do cluster, não some. */
  selecionado: boolean
  /** Em destaque (hover, foco ou seleção) — mostra o callout e acende a constelação. */
  ativo: boolean
  focado: boolean
  clicavel: boolean
  onSelecionar?: (resolucaoId: string) => void
  onHover: (chave: string | null) => void
  onFoco: (chave: string | null) => void
}

function Marcador({
  cluster,
  tema,
  selecionado,
  ativo,
  focado,
  clicavel,
  onSelecionar,
  onHover,
  onFoco,
}: MarcadorProps) {
  const escuro = tema === 'dark'
  const cor = corDaClasse(cluster.k, tema)
  const { x, y, total } = cluster
  const multiplo = total >= CLUSTER_ROTULO_MIN

  const { halo, nucleo } = raioDoCluster(total)
  // O marcador inteiro respira no hover (halo, contorno e alvo crescem juntos) — não só o halo,
  // senão o número do cluster ficaria solto dentro de um anel que não se mexeu.
  const raioHalo = ativo && !selecionado ? halo + HOVER_GANHO : halo
  const opacidadeHalo = ativo && !selecionado ? 0.35 : 0.2

  /*
   * REGRA 3 (inviolável). Big-O é ≈ ESTIMADO → o marcador é VAZADO, sempre — inclusive o do
   * cluster. Um disco OPACO da cor da classe (que é o jeito óbvio de dar contraste ao número)
   * está PROIBIDO aqui: "cheio" já significa MEDIDO neste sistema, e um cluster cheio ao lado
   * de uma estrela vazada diria "estas 3 foram medidas, aquela foi estimada" — exatamente a
   * confusão que a regra 3 existe para impedir.
   *
   * O contorno traçado é, então, o mesmo em qualquer tamanho — só muda de raio:
   *   total = 1  → anel no núcleo (2.6), com o miolo transparente. Idêntica à estrela de hoje.
   *   total ≥ 2  → anel no halo, com o NÚMERO no miolo (ver `numeroDoCluster`).
   * Nos dois casos o miolo deixa ver o fundo: é isso que "vazado" quer dizer.
   */
  const nucleoSel = 'var(--estrela-nucleo)' // branco-estelar no escuro; a cor da classe no claro
  const traco = selecionado ? nucleoSel : cor
  const larguraTraco = selecionado ? 1.6 : multiplo ? 1.2 : 1.3
  // No solitário, o anel selecionado engorda de 2.6 para 3.4 (protótipo).
  const raioNucleo = selecionado ? nucleo + 0.8 : nucleo

  const raioAlvo = Math.max(ALVO_R_MIN, raioHalo + ALVO_GANHO)
  const primeira = cluster.pontos[0]

  return (
    <g
      role={clicavel ? 'button' : 'img'}
      tabIndex={0}
      aria-label={descreverCluster(cluster, clicavel)}
      aria-current={selecionado ? 'true' : undefined}
      // `color` alimenta o `currentColor` de `--estrela-nucleo` no modo claro.
      style={{ color: cor }}
      className={cn('outline-none', clicavel && 'cursor-pointer')}
      // Um cluster é UM alvo: o clique abre a resolução MAIS ANTIGA (`pontos[0]`, ordem
      // cronológica garantida por `agruparEmClusters`). As irmãs saem pelas setas do painel.
      onClick={() => onSelecionar?.(primeira.resolucaoId)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onSelecionar?.(primeira.resolucaoId)
        }
      }}
      onMouseEnter={() => onHover(cluster.chave)}
      onMouseLeave={() => onHover(null)}
      onFocus={() => onFoco(cluster.chave)}
      onBlur={() => onFoco(null)}
    >
      {/* Halo — quando selecionado, quem desenha (maior) é a camada de decoração. */}
      {!selecionado && (
        <circle
          cx={x}
          cy={y}
          r={raioHalo}
          fill={cor}
          opacity={escuro ? opacidadeHalo : opacidadeHalo - 0.02}
        />
      )}

      {/* CONTORNO VAZADO — a incerteza da métrica é visível na própria marca, não só no callout.
          (Se algum dia a classe passar a ser MEDIDA, `MEDIDO` vira `true` e o miolo enche.) */}
      <circle
        cx={x}
        cy={y}
        r={multiplo ? raioHalo : raioNucleo}
        fill={MEDIDO ? traco : 'none'}
        stroke={traco}
        strokeWidth={larguraTraco}
      />

      {/* O NÚMERO no núcleo do cluster. Ver `numeroDoCluster` para o contraste. */}
      {multiplo && (
        <text
          x={x}
          y={y + fonteDoNumero(total) * 0.35}
          textAnchor="middle"
          fontSize={fonteDoNumero(total)}
          fontWeight={600}
          fill="var(--ink)"
          className="font-mono tabular"
        >
          {total}
        </text>
      )}

      {/* Anel de foco de teclado — o SVG não tem box-shadow: o anel é geometria. */}
      {focado && (
        <circle
          cx={x}
          cy={y}
          r={raioHalo + FOCO_GANHO}
          fill="none"
          stroke="var(--ink)"
          strokeWidth={1}
          opacity={0.9}
        />
      )}

      {/* Alvo de clique/toque invisível (fill transparent recebe evento; fill none não). */}
      <circle cx={x} cy={y} r={raioAlvo} fill="transparent" />
    </g>
  )
}

/**
 * CONTRASTE DO NÚMERO (a decisão desta rodada).
 *
 * O caminho óbvio — número sobre um disco OPACO da cor da classe, escolhendo branco ou
 * quase-preto conforme a classe seja clara (verde) ou escura (vinho) — foi MEDIDO e REPROVADO
 * duas vezes:
 *
 *   1. Não chega a AA. Nas classes do MEIO da escala nenhuma das duas tintas passa de 4,5:1:
 *      no escuro, `O(2ⁿ)` (#CE4C55) dá 4,01:1 com o branco e 4,43:1 com o quase-preto — o
 *      MELHOR dos dois REPROVA. No claro, `O(n³)` (#C85631) dá 3,98 / 4,47. Um dígito de 9–10px
 *      é texto pequeno: precisa de 4,5:1. Não há tinta que salve o meio do colormap.
 *   2. Viola a regra 3. Disco opaco = marcador CHEIO = MEDIDO. Big-O é ≈ ESTIMADO.
 *
 * A saída: o número NÃO se apoia na cor da classe — a cor da classe fica no CONTORNO (onde ela
 * já significa alguma coisa) e o miolo continua vazado, deixando ver o halo (a classe a 20%
 * sobre o `--panel-chart`). Sobre esse fundo o número vai em `--ink`, o par de texto máximo do
 * sistema. Medido nas 8 classes × 2 modos: **pior caso 12,8:1 no escuro e 10,4:1 no claro** —
 * AAA folgado, sem lógica por classe e sem depender do tema.
 *
 * E é também o que a regra 1 pede: a contagem de resoluções NÃO é uma classe de complexidade.
 * Pintá-la com o colormap seria dar cor de complexidade a um número que não é complexidade.
 *
 * A fonte encolhe conforme o número cresce, para o dígito nunca vazar o contorno:
 * o raio do halo satura em 13,46 (`raioDoCluster`), o que comporta "999" a 8px.
 */
function fonteDoNumero(total: number): number {
  if (total < 10) return 10
  if (total < 100) return 9
  return 8
}

/**
 * O que o leitor de tela diz — a célula inteira em uma frase. A complexidade é ESTIMADA (nunca
 * "medida"); a confiança do motor entra como qualificador.
 *
 * ⚠ Este texto NÃO vira `<title>` no SVG (o navegador desenharia o tooltip nativo por cima do
 * callout e roubaria o clique). Ele existe só como `aria-label`.
 */
function descreverCluster(c: ClusterCarta, clicavel: boolean): string {
  if (c.total === 1) return descrever(c.pontos[0], clicavel)

  const tempo = comPrefixoEstimado(rotuloCanonico(c.k), CONFIANCA_BIG_O).trim()
  const titulos = titulosUnicos(c)
  const lista = titulos.slice(0, 3).join(', ') + (titulos.length > 3 ? ' e outros' : '')
  const acao = clicavel ? ' Abrir a mais antiga.' : ''
  return (
    `${pluralPt(c.total, 'resolução', 'resoluções')} em autonomia ${c.autonomia} de 5, ` +
    `complexidade de tempo ${tempo} (${NATUREZA}): ${lista}.${acao}`
  )
}

/** A estrela solitária em uma frase — o texto de sempre. */
function descrever(p: PontoPlotavel, clicavel: boolean): string {
  const lingua = LINGUAGEM_META[p.linguagem]?.label ?? p.linguagem
  const tempo = comPrefixoEstimado(rotuloCanonico(p.k), CONFIANCA_BIG_O).trim()
  const mccabe = p.ciclomatica != null ? ` · ciclomática ${p.ciclomatica} (medida)` : ''
  const motor = rotuloConfiancaMotor(p.confiancaTempo)
  const acao = clicavel ? ' Abrir.' : ''
  return (
    `${p.desafioTitulo} · ${lingua} · complexidade de tempo ${tempo} ` +
    `(${NATUREZA}${motor ? `, ${motor}` : ''})${mccabe} · autonomia ${p.autonomia} de 5 · ` +
    `enviada ${dataCompleta(p.submetidaEm)}.${acao}`
  )
}

/** Os desafios distintos de um cluster, em ordem cronológica de primeira aparição. */
function titulosUnicos(c: ClusterCarta): string[] {
  return [...new Set(c.pontos.map((p) => p.desafioTitulo))]
}

// ════════════════════════════════════════════════════════════════════════════
// DECORAÇÃO DA SELEÇÃO — halo, anel, cruzeta e as guias até os dois eixos
// ════════════════════════════════════════════════════════════════════════════

/**
 * O realce é do CLUSTER: quando a resolução selecionada divide a célula com outras, quem ganha
 * o anel e a cruzeta é o marcador do grupo (ele não some nem se desloca — não há mais jitter).
 */
function DecoracaoSelecionada({
  cluster,
  tema,
}: {
  cluster: ClusterCarta
  tema: PropsGrafico['tema']
}) {
  const escuro = tema === 'dark'
  const cor = corDaClasse(cluster.k, tema)
  const { x, y, total } = cluster
  const multiplo = total >= CLUSTER_ROTULO_MIN
  const { halo } = raioDoCluster(total)

  // Token do sistema: branco-estelar no escuro, `currentColor` (= a classe) no claro.
  const cruzeta = 'var(--estrela-nucleo)'

  const raioHalo = halo + SEL_HALO_GANHO
  const raioAnel = halo + (multiplo ? SEL_ANEL_GANHO_CLUSTER : SEL_ANEL_GANHO)
  const braco = halo + CRUZETA_GANHO
  // No cluster a cruzeta vira 4 BRAÇOS com um vão no meio: uma linha inteira riscaria o número.
  // No solitário o vão é zero — são as duas retas cruzadas do protótipo, sem mudar um pixel.
  const vao = multiplo ? halo + CRUZETA_VAO : 0

  return (
    <g aria-hidden className="pointer-events-none" style={{ color: cor }}>
      {/* Guias tracejadas até o eixo Y (a classe) e até a base (a autonomia) — na cor da
          classe: é a leitura do par (autonomia × complexidade) daquela célula. */}
      <line
        x1={x}
        y1={y}
        x2={CARTA_PLOT.x0}
        y2={y}
        stroke={cor}
        strokeWidth={1}
        strokeDasharray="3 3"
        opacity={escuro ? 0.5 : 0.6}
      />
      <line
        x1={x}
        y1={y}
        x2={x}
        y2={CARTA_PLOT.y1}
        stroke={cor}
        strokeWidth={1}
        strokeDasharray="3 3"
        opacity={escuro ? 0.5 : 0.6}
      />

      <circle cx={x} cy={y} r={raioHalo} fill={cor} opacity={escuro ? 0.22 : 0.2} />
      <circle
        cx={x}
        cy={y}
        r={raioAnel}
        fill="none"
        stroke={cor}
        strokeWidth={1}
        opacity={escuro ? 0.5 : 0.6}
      />

      {/* Cruzeta da estrela (os braços do "astro" apontado). */}
      {[
        { x1: x, y1: y - vao, x2: x, y2: y - braco },
        { x1: x, y1: y + vao, x2: x, y2: y + braco },
        { x1: x - vao, y1: y, x2: x - braco, y2: y },
        { x1: x + vao, y1: y, x2: x + braco, y2: y },
      ].map((l, i) => (
        <line
          key={i}
          {...l}
          stroke={cruzeta}
          strokeWidth={1}
          opacity={escuro ? 0.85 : 0.7}
        />
      ))}
    </g>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// CALLOUT — o rótulo da célula em destaque. O ÚNICO tooltip da Carta.
// ════════════════════════════════════════════════════════════════════════════

function Callout({ cluster, tema }: { cluster: ClusterCarta; tema: PropsGrafico['tema'] }) {
  const escuro = tema === 'dark'
  const cor = corDaClasse(cluster.k, tema)
  const tinta = tintaDaClasse(cluster.k, tema)
  const { left, top, transform } = posicaoCallout(cluster.x, cluster.y)

  const tempo = comPrefixoEstimado(rotuloCanonico(cluster.k), CONFIANCA_BIG_O)
  const multiplo = cluster.total >= CLUSTER_ROTULO_MIN

  return (
    // aria-hidden: tudo isto já está no `aria-label` do marcador — não repetir para o leitor.
    <div
      aria-hidden
      className="pointer-events-none absolute z-10 flex flex-col gap-[3px] whitespace-nowrap rounded-ci bg-elevated px-[10px] py-2 shadow-callout"
      style={{ left, top, transform, border: `1px solid ${cor}` }}
    >
      {multiplo ? (
        <CalloutCluster cluster={cluster} tinta={tinta} tempo={tempo} escuro={escuro} />
      ) : (
        <CalloutSolitaria ponto={cluster.pontos[0]} tinta={tinta} tempo={tempo} escuro={escuro} />
      )}
    </div>
  )
}

/**
 * O callout de um cluster NÃO tenta mostrar as N resoluções (viraria uma lista dentro de um
 * tooltip que ninguém consegue apontar): ele DECLARA quantas são, lista até 2 desafios e diz o
 * que o clique faz. A leitura fina é do painel lateral, que navega entre as irmãs.
 *
 * Ciclomática e confiança do motor ficam de fora: são POR RESOLUÇÃO e variam dentro da célula —
 * mostrar a da primeira como se fosse a do grupo seria mentir com um número certo.
 */
function CalloutCluster({
  cluster,
  tinta,
  tempo,
  escuro,
}: {
  cluster: ClusterCarta
  tinta: string
  tempo: string
  escuro: boolean
}) {
  const titulos = titulosUnicos(cluster)
  const mostrados = titulos.slice(0, MAX_TITULOS_CALLOUT).join(' · ')
  const reticencia = titulos.length > MAX_TITULOS_CALLOUT

  return (
    <>
      <span className="font-mono text-[11px] font-semibold text-ink">
        {escuro && <span className="mr-1">✦</span>}
        {mostrados}
        {reticencia && <span className="text-soft"> …</span>}
      </span>

      {/*
       * UMA COR POR SEMÂNTICA (regras 1 e 4): só a CLASSE veste o colormap. A CONTAGEM é
       * contagem e a autonomia é NEUTRA.
       */}
      <span className="tabular flex items-center gap-1.5 font-mono text-[10px]">
        <span className="text-ink">
          {pluralPt(cluster.total, 'resolução', 'resoluções')}
        </span>
        <span style={{ color: tinta }}>· {tempo.trim()}</span>
        <span className="text-ink">· aut {cluster.autonomia}/5</span>
      </span>

      <span className="font-mono text-[9.5px] text-soft">clique para abrir a mais antiga</span>
    </>
  )
}

/** A célula com uma resolução só: o callout de sempre. */
function CalloutSolitaria({
  ponto,
  tinta,
  tempo,
  escuro,
}: {
  ponto: PontoPlotavel
  tinta: string
  tempo: string
  escuro: boolean
}) {
  const lingua = LINGUAGEM_META[ponto.linguagem]?.label ?? ponto.linguagem
  const motor = rotuloConfiancaMotor(ponto.confiancaTempo)

  return (
    <>
      <span className="font-mono text-[11px] font-semibold text-ink">
        {escuro && <span className="mr-1">✦</span>}
        {ponto.desafioTitulo} · {lingua}
      </span>

      {/*
       * UMA COR POR SEMÂNTICA (regras 1 e 4): só a CLASSE veste o colormap. A ciclomática é
       * contagem (não é classe) e a autonomia é NEUTRA — pintá-las com a tinta da classe diria
       * ao aluno "sua autonomia 4 é laranja porque a solução é O(n²)", que é a associação que
       * este sistema existe para impedir.
       */}
      <span className="tabular flex items-center gap-1.5 font-mono text-[10px]">
        <span style={{ color: tinta }}>{tempo}</span>
        {ponto.ciclomatica != null && <span className="text-mid">· M={ponto.ciclomatica}</span>}
        <span className="text-ink">· aut {ponto.autonomia}/5</span>
      </span>

      {motor && <span className="font-mono text-[9.5px] text-soft">{motor}</span>}
    </>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// VAZIO
// ════════════════════════════════════════════════════════════════════════════

/**
 * O céu continua lá (grade + colormap + estrelas cenográficas): o vazio da Carta é a ausência
 * de estrelas, não a ausência de gráfico. E o motivo do vazio é dito — se há resoluções mas
 * nenhuma virou ponto, o problema é a métrica (só Java), não a falta de trabalho.
 */
function CartaVazia({ total, semMetrica }: { total: number; semMetrica: number }) {
  const descricao =
    total > 0
      ? `${pluralPt(semMetrica, 'resolução sem métrica', 'resoluções sem métrica')}. ${NOTA_METRICAS_SO_JAVA}`
      : 'Submeta uma resolução em Java para ver sua primeira estrela.'

  return (
    <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center gap-[9px] px-5 text-center">
      <div
        className="flex items-center justify-center rounded-ci border border-line-strong bg-elevated"
        style={{ width: 38, height: 38 }}
      >
        <Folder size={18} strokeWidth={2} className="text-soft" />
      </div>
      <div className="flex flex-col gap-1.5">
        <span className="text-[13px] font-medium text-body">Nenhuma resolução analisada ainda.</span>
        <span className="mx-auto max-w-[46ch] text-[12px] leading-[1.5] text-soft">{descricao}</span>
      </div>
    </div>
  )
}
