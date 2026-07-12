/*
 * PAINEL DE GRÁFICOS — a moldura que unifica as 3 visualizações (spec 02 §1 e §8).
 *
 * ⚠ Eram 5. Duas saíram, por decisão do usuário:
 * · ESPECTRO: plotava CLASSES, não resoluções — nada a selecionar, nada que conversasse com o
 *   painel da estrela ao lado —, e o dashboard já exibe a MESMA distribuição no card
 *   "Distribuição · Espectro". O histograma continua vivo lá; saiu a duplicata do seletor.
 * · ESPIRAL (ex-Órbitas): REDUNDANTE com a Linha temporal — respondia à mesma pergunta
 *   (autonomia × tempo, complexidade × tempo) por canais perceptuais mais fracos (tamanho e
 *   ângulo, em vez de posição num eixo). Menos gráficos, cada um dizendo uma coisa.
 *
 * Cabeçalho (título + subtítulo que MUDA com o gráfico + `?` de "como ler") · seletor ·
 * o gráfico escolhido · RODAPÉ HONESTO.
 *
 * ── QUEM BUSCA O DADO ───────────────────────────────────────────────────────────────
 * O painel NÃO busca nada: recebe `dataset` (e `carregando`/`erro`) por prop. Quem chama
 * é dono da query — o Dashboard faz `const q = useCartaCeleste()` +
 * `montarDataset(q.data ?? [])`. Motivos: (1) o mesmo painel serve o portfólio público, que
 * lê outro endpoint; (2) testar um gráfico não exige subir um QueryClient; (3) a seleção
 * (`selecionadoId`) é da página — ela também alimenta o `PainelEstrelaSelecionada` ao lado.
 *
 * ── O RODAPÉ NÃO É OPCIONAL ─────────────────────────────────────────────────────────
 * "18 de 23 resoluções plotadas · 5 sem métrica". Um gráfico que descarta 5 de 23 pontos em
 * silêncio mente sobre o portfólio — e a honestidade do descarte é requisito da pesquisa,
 * não enfeite (00-INDICE §4.4). Quando `semMetrica > 0`, o painel diz também o PORQUÊ.
 *
 * ── ESTADOS ─────────────────────────────────────────────────────────────────────────
 * erro       → `ErrorState` no lugar do gráfico (sem rodapé: não há contagem em que confiar).
 * carregando → repassado ao gráfico: cada um desenha o seu esqueleto (grade + eixos +
 *              estrelas-fantasma pulsando), o que é mais informativo que uma barra cinza.
 * vazio      → cada gráfico já sobrepõe o seu `EmptyState` mantendo grade/eixos (spec 02
 *              §2.11). O rodapé continua — "0 de 5 resoluções plotadas · 5 sem métrica" é
 *              exatamente o que explica um céu vazio.
 */
import { useId } from 'react'
import { InfoButton } from '@/components/ui/info-button'
import { ErrorState } from '@/components/page/states'
import { NOTA_METRICAS_SO_JAVA } from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'
import { cn } from '@/lib/utils'
import type { InfoSecao } from '@/domain/metricas-explicacao'
import { Carta } from './Carta'
import { LINHA_JANELA_MESES, Linha } from './Linha'
import { Matriz } from './Matriz'
import { rotuloRodape } from './dataset'
import { SeletorDeGrafico, type TipoGrafico } from './SeletorDeGrafico'
import type { DatasetCarta, Granularidade } from './tipos'

// ════════════════════════════════════════════════════════════════════════════
// COMO LER CADA GRÁFICO (conteúdo do `?`)
// ════════════════════════════════════════════════════════════════════════════

/**
 * Duas seções entram em TODOS os `?`: a distinção MEDIDO × ≈ ESTIMADO (regra 3) e o
 * descarte por falta de métrica (§4.4). São as duas coisas que um gráfico bonito esconderia.
 */
const SECOES_COMUNS: readonly InfoSecao[] = [
  {
    rotulo: 'Medido × ≈ estimado',
    texto:
      'Marcador cheio = medido diretamente na árvore sintática (AST) do código — é o caso da complexidade ciclomática. Marcador vazado, com o prefixo ≈, = inferido por análise estática: é o caso de toda classe Big O, porque determinar a complexidade exata de um código qualquer é indecidível no caso geral. Por isso as estrelas da complexidade são sempre vazadas: a incerteza nunca é escondida.',
  },
  {
    rotulo: 'Confiança do motor',
    texto:
      'É um segundo eixo, e não se confunde com o primeiro: diz quanto o motor confia no valor que ele mesmo estimou (alta = reconheceu todos os construtos; média = assumiu algum default conservador; baixa = não classificou). Confiança alta não transforma uma estimativa em medição.',
  },
  {
    rotulo: 'O que não está no gráfico',
    texto:
      'Resoluções sem classe de complexidade não viram ponto: a análise ainda pode estar rodando, a linguagem pode não ter analisador (hoje só Java tem) ou o motor pode não ter conseguido classificar o código. O rodapé diz quantas ficaram de fora e por quê; nenhuma some em silêncio. A autonomia, essa, é autodeclarada e continua valendo em qualquer linguagem.',
  },
]

interface MetaGrafico {
  titulo: string
  subtitulo: string
  secoes: readonly InfoSecao[]
  ariaLabel: string
}

const META: Record<TipoGrafico, MetaGrafico> = {
  carta: {
    titulo: 'Carta de resoluções',
    subtitulo: 'autonomia × complexidade · cor = complexidade',
    ariaLabel: 'Como ler a carta de resoluções',
    secoes: [
      {
        rotulo: 'Como ler',
        texto:
          'Cada estrela é uma resolução. O eixo horizontal é o Índice de Autonomia IA autodeclarado (1 = mais apoio da IA · 5 = mais autônomo); o eixo vertical é a classe de complexidade de tempo, de O(1) na base a O(n!) no topo. A cor repete a classe do eixo.',
      },
      {
        rotulo: 'Constelações',
        texto:
          'As linhas ligam resoluções do mesmo desafio, em ordem cronológica. É a trajetória que a pesquisa mede: força bruta com muito apoio de IA → solução refinada e autônoma. Desafio com uma só resolução fica como estrela solitária.',
      },
      ...SECOES_COMUNS,
    ],
  },
  linha: {
    titulo: 'Linha temporal',
    /*
     * A janela DEIXOU de ser fixa em meses (a Linha tem seletor de granularidade: diário,
     * semanal, mensal). Um subtítulo que dissesse "últimos 8 meses" mentiria assim que o
     * usuário escolhesse "diário" — o número dos meses fica no `?`, onde é qualificado.
     */
    subtitulo: 'evolução no tempo · autonomia × complexidade',
    ariaLabel: 'Como ler a linha temporal',
    secoes: [
      {
        rotulo: 'Como ler',
        texto:
          'Dois sinais por período (mês, semana ou dia, no seletor do gráfico): a autonomia média (linha clara e neutra, com marcadores cheios — autonomia nunca usa o colormap) e a classe de complexidade média arredondada (linha mais discreta, com marcadores vazados pintados pela classe). As duas são lidas juntas: a hipótese da pesquisa é que autonomia crescente e complexidade decrescente andam juntas ao longo do tempo. O gráfico mostra o que aconteceu no seu caso — ele não avalia a sua solução.',
      },
      {
        rotulo: 'Complexidade típica do mês',
        texto:
          'É a média das classes Big O de tempo das resoluções analisadas naquele mês, arredondada para uma classe real. Como toda classe Big O aqui, vem de análise estática da AST — é uma estimativa (marcador vazado, prefixo ≈), não uma medição, e só existe para Java. Difere da "complexidade típica" do card ao lado, que é a MEDIANA de todo o histórico: são estatísticas diferentes do mesmo dado, e podem apontar classes diferentes.',
      },
      {
        rotulo: 'Traço sólido × traço tracejado',
        texto:
          'O traço é SÓLIDO entre dois períodos vizinhos que têm medição — aí a linha é dado. Quando dois períodos com dado estão separados por períodos SEM medição, eles são ligados assim mesmo, mas com traço TRACEJADO: ali não houve medição, e o tracejado é continuidade visual, não dado. Nenhum ponto é desenhado no vão e nenhum valor intermediário é inventado; o período vazio continua ocupando o seu lugar no eixo do tempo (o tempo não anda mais devagar porque você parou de enviar).',
      },
      {
        rotulo: 'Períodos vazios e períodos sem métrica',
        texto:
          'São coisas diferentes. Período SEM RESOLUÇÃO deixa as duas séries sem ponto (elas o atravessam tracejadas). Já o período com resoluções que não têm métrica (ex.: Python) NÃO é um período vazio: a autonomia tem ponto ali e a sua linha segue sólida (a autonomia é autodeclarada e independe da linguagem); só a complexidade fica sem ponto e atravessa tracejada.',
      },
      {
        rotulo: 'A janela',
        texto:
          `O eixo do tempo mostra sempre os períodos mais recentes, e quantos cabem depende da granularidade escolhida — na visão mensal (a padrão), os últimos ${LINHA_JANELA_MESES} meses. O limite é de espaço: mais pontos do que isso e os rótulos das datas se sobreporiam.`,
      },
      ...SECOES_COMUNS,
    ],
  },
  matriz: {
    titulo: 'Matriz',
    subtitulo: 'densidade autonomia × classe',
    ariaLabel: 'Como ler a matriz',
    secoes: [
      {
        rotulo: 'Como ler',
        texto:
          'Cada célula cruza um nível de autonomia (colunas, 1 a 5) com uma classe de complexidade (linhas, O(n!) no topo e O(1) na base — a mesma orientação da carta). A intensidade da célula é o número de resoluções ali; o ponto `·` significa nenhuma.',
      },
      {
        rotulo: 'Para que serve',
        texto:
          'A matriz mostra onde o portfólio se concentra. Concentração à esquerda indica resoluções feitas com mais apoio de IA; concentração no topo, classes de complexidade mais custosas. O gráfico descreve a distribuição — a leitura do que isso significa é sua.',
      },
      ...SECOES_COMUNS,
    ],
  },
}

// ════════════════════════════════════════════════════════════════════════════
// PAINEL
// ════════════════════════════════════════════════════════════════════════════

export interface PainelDeGraficosProps {
  /** Já montado com `montarDataset(pontos)` — o painel não busca nem transforma dado. */
  dataset: DatasetCarta
  carregando?: boolean
  /** Mensagem de erro da query (use `apiErrorMessage(query.error)`). */
  erro?: string | null
  onTentarNovamente?: () => void
  /** Seleção — vive na página, porque o `PainelEstrelaSelecionada` ao lado também a lê. */
  selecionadoId?: string | null
  onSelecionar?: (resolucaoId: string) => void
  /**
   * As duas LENTES do painel — controladas pela página (`useGraficoNaUrl` + um `useState`).
   *
   * ⚠ Elas subiram de nível porque o painel deixou de ser o único a depender delas: o
   * `PainelEstrelaSelecionada`, que fica ao lado, navega entre as resoluções do MESMO alvo que o
   * gráfico agrupou — e "o mesmo alvo" é a célula na Carta/Matriz e o BUCKET DO TEMPO na Linha.
   * Sem saber a visualização e a granularidade, ele navegava pelo agrupamento errado (o bug das
   * setas aparecendo na Linha com as irmãs da Carta).
   */
  view: TipoGrafico
  onViewChange: (proxima: TipoGrafico) => void
  granularidade: Granularidade
  onGranularidadeChange: (proxima: Granularidade) => void
  className?: string
}

export function PainelDeGraficos({
  dataset,
  carregando = false,
  erro,
  onTentarNovamente,
  selecionadoId,
  onSelecionar,
  view,
  onViewChange,
  granularidade,
  onGranularidadeChange,
  className,
}: PainelDeGraficosProps) {
  const { theme } = useTheme()
  const baseId = useId()
  const meta = META[view]

  const props = { dataset, selecionadoId, onSelecionar, tema: theme, carregando }

  return (
    <section
      className={cn(
        'relative flex flex-col overflow-hidden rounded-ci border border-line bg-panel-chart',
        className,
      )}
    >
      {/* ── Cabeçalho: título + como ler + seletor ──────────────────────────── */}
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-line-soft px-4 py-3.5">
        <div className="flex min-w-0 flex-col gap-0.5">
          <div className="flex items-center gap-1.5">
            <h3 className="text-[13.5px] font-semibold text-ink">{meta.titulo}</h3>
            <InfoButton
              titulo={meta.titulo}
              subtitulo={meta.subtitulo}
              secoes={[...meta.secoes]}
              ariaLabel={meta.ariaLabel}
            />
          </div>
          <span className="font-mono text-[10.5px] tracking-[.04em] text-soft">
            {meta.subtitulo}
          </span>
        </div>

        <SeletorDeGrafico value={view} onChange={onViewChange} baseId={baseId} />
      </header>

      {/* ── O gráfico (ou o erro) ─────────────────────────────────────────────
           ⚠ SEM `justify-center` (correção — o usuário viu "um espaçamento que não parece muito
           natural em cima e embaixo do gráfico"). Este painel ESTICA até a altura da coluna de
           cards ao lado, e centralizar repartia toda a sobra em duas faixas de ar, uma acima e
           outra abaixo do desenho — o gráfico ficava boiando no meio de um cartão vazio. Agora o
           conteúdo ancora no topo (`justify-start`, o padrão) e a sobra vira uma única folga no
           rodapé, onde ela lê como respiro do cartão e não como buraco.

           A outra metade da correção está DENTRO dos gráficos: a Linha reservava y ∈ [0, 60] do
           seu viewBox para nada (26% da altura, ~130px de ar em cima da série). Ver `VB` em
           Linha.tsx. */}
      <div
        id={`${baseId}-painel`}
        role="tabpanel"
        aria-labelledby={`${baseId}-tab-${view}`}
        tabIndex={-1}
        className="flex flex-1 flex-col p-4"
      >
        {erro ? (
          <ErrorState message={erro} onRetry={onTentarNovamente} />
        ) : (
          <>
            {view === 'carta' && <Carta {...props} />}
            {view === 'linha' && (
              <Linha
                {...props}
                granularidade={granularidade}
                onGranularidadeChange={onGranularidadeChange}
              />
            )}
            {view === 'matriz' && <Matriz {...props} />}
          </>
        )}
      </div>

      {/* ── Rodapé honesto: o que entrou, o que ficou de fora e por quê ─────── */}
      {!erro && (
        <footer className="flex flex-wrap items-center gap-x-2 gap-y-1 border-t border-line-soft px-4 py-2.5">
          <span className="tabular font-mono text-[10.5px] text-mid">
            {carregando ? 'carregando resoluções…' : rotuloRodape(dataset)}
          </span>
          {/*
           * A nota só-Java explica UM dos três motivos de descarte. Colá-la em qualquer
           * descarte faria o aluno que acabou de submeter 3 resoluções EM JAVA ler
           * "3 sem métrica · métricas só para Java" — explicação falsa para número certo.
           */}
          {!carregando && dataset.semMetrica.semAnalisador > 0 && (
            <span className="font-mono text-[10.5px] text-soft">· {NOTA_METRICAS_SO_JAVA}</span>
          )}
        </footer>
      )}
    </section>
  )
}
