/*
 * PAINEL DE GRÁFICOS — a moldura que unifica as 5 visualizações (spec 02 §1 e §8).
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
import { Espectro } from './Espectro'
import { LINHA_JANELA_MESES, Linha } from './Linha'
import { Matriz } from './Matriz'
import { Orbitas } from './Orbitas'
import { rotuloRodape } from './dataset'
import {
  SeletorDeGrafico,
  type TipoGrafico,
  useGraficoNaUrl,
} from './SeletorDeGrafico'
import type { DatasetCarta } from './tipos'

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
      'Marcador cheio = medido diretamente na árvore sintática do código. Marcador vazado, com o prefixo ≈, = estimado por análise estática: é uma heurística e pode divergir do pior caso real. A incerteza nunca é escondida.',
  },
  {
    rotulo: 'O que não está no gráfico',
    texto:
      'Resoluções sem classe de complexidade não viram ponto — hoje as métricas de complexidade só existem para Java, e o motor pode não classificar um código. O rodapé diz quantas ficaram de fora; nenhuma some em silêncio.',
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
  orbitas: {
    titulo: 'Órbitas',
    subtitulo: 'polar · autonomia = raio · ângulo = tempo',
    ariaLabel: 'Como ler as órbitas',
    secoes: [
      {
        rotulo: 'Como ler',
        texto:
          'O mesmo dado da carta, em coordenadas polares. O raio é a autonomia: quanto mais longe do centro, mais autônoma foi a resolução (o centro é o maior apoio da IA). O ângulo é o relógio do portfólio — a resolução mais antiga fica às 12h e as seguintes seguem no sentido horário.',
      },
      {
        rotulo: 'Por que polar',
        texto:
          'A leitura é de forma, não de valor exato: um portfólio que vai empurrando as estrelas para fora dos anéis é um portfólio ganhando autonomia ao longo do tempo.',
      },
      ...SECOES_COMUNS,
    ],
  },
  espectro: {
    titulo: 'Espectro',
    subtitulo: 'distribuição por classe de complexidade',
    ariaLabel: 'Como ler o espectro',
    secoes: [
      {
        rotulo: 'Como ler',
        texto:
          'Um histograma: quantas resoluções caem em cada classe de complexidade. As 8 classes aparecem sempre, mesmo com contagem zero — o que não existe também é informação (uma faixa vazia em O(log n) diz algo).',
      },
      {
        rotulo: 'Sem interação',
        texto:
          'Uma barra é uma classe (um agregado), não uma resolução: não há o que selecionar. A barra da classe da resolução selecionada em outro gráfico ganha uma hairline clara — é só um eco da seleção.',
      },
      ...SECOES_COMUNS,
    ],
  },
  linha: {
    titulo: 'Linha temporal',
    subtitulo: `evolução · últimos ${LINHA_JANELA_MESES} meses`,
    ariaLabel: 'Como ler a linha temporal',
    secoes: [
      {
        rotulo: 'Como ler',
        texto:
          'Dois sinais por mês: a autonomia média (linha clara e neutra — autonomia nunca usa o colormap) e a classe de complexidade média arredondada (linha colorida pela classe). Ler as duas juntas é o ponto: subir a autonomia enquanto a complexidade desce é o amadurecimento.',
      },
      {
        rotulo: 'Meses vazios',
        texto:
          'Mês sem resolução fica vago e a linha se interrompe. Interpolar por cima de um mês sem dado inventaria uma tendência que não aconteceu.',
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
          'A matriz mostra onde o portfólio se concentra. Densidade no canto de baixa autonomia e alta complexidade é o retrato de quem ainda depende da IA para força bruta.',
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
  /** Nome do query param da visualização. Só mude se houver dois painéis na mesma tela. */
  paramUrl?: string
  className?: string
}

export function PainelDeGraficos({
  dataset,
  carregando = false,
  erro,
  onTentarNovamente,
  selecionadoId,
  onSelecionar,
  paramUrl,
  className,
}: PainelDeGraficosProps) {
  const [view, setView] = useGraficoNaUrl(paramUrl)
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

        <SeletorDeGrafico value={view} onChange={setView} baseId={baseId} />
      </header>

      {/* ── O gráfico (ou o erro) ───────────────────────────────────────────── */}
      <div
        id={`${baseId}-painel`}
        role="tabpanel"
        aria-labelledby={`${baseId}-tab-${view}`}
        tabIndex={-1}
        className="flex flex-1 flex-col justify-center p-4"
      >
        {erro ? (
          <ErrorState message={erro} onRetry={onTentarNovamente} />
        ) : (
          <>
            {view === 'carta' && <Carta {...props} />}
            {view === 'orbitas' && <Orbitas {...props} />}
            {view === 'espectro' && <Espectro {...props} />}
            {view === 'linha' && <Linha {...props} />}
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
          {!carregando && dataset.semMetrica > 0 && (
            <span className="font-mono text-[10.5px] text-soft">· {NOTA_METRICAS_SO_JAVA}</span>
          )}
        </footer>
      )}
    </section>
  )
}
