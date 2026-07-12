/*
 * Fachada do módulo de gráficos.
 *
 *   import { PainelDeGraficos, PainelEstrelaSelecionada, montarDataset, pontoPorId }
 *     from '@/components/charts'
 *
 * Uso canônico (Dashboard): a PÁGINA busca o dado e é dona da seleção; o painel só desenha.
 *
 *   const carta = useCartaCeleste()
 *   const dataset = useMemo(() => montarDataset(carta.data ?? []), [carta.data])
 *   const [selecionadoId, setSelecionadoId] = useState<string | null>(null)
 *
 *   <div className="grid gap-3.5 lg:grid-cols-[1.62fr_1fr]">
 *     <PainelDeGraficos
 *       dataset={dataset}
 *       carregando={carta.isPending}
 *       erro={carta.isError ? apiErrorMessage(carta.error) : null}
 *       onTentarNovamente={() => void carta.refetch()}
 *       selecionadoId={selecionadoId}
 *       onSelecionar={setSelecionadoId}
 *     />
 *     <PainelEstrelaSelecionada ponto={pontoPorId(dataset, selecionadoId)} />
 *   </div>
 *
 * ⚠ `./escalas` NÃO é reexportado aqui: ele exporta a interface `Matriz` (a grade de dados),
 * que colidiria com o componente `Matriz`. Quem precisa de geometria importa
 * `@/components/charts/escalas` direto.
 */

// Painel e seletor
export { PainelDeGraficos, type PainelDeGraficosProps } from './PainelDeGraficos'
export {
  PainelEstrelaSelecionada,
  type PainelEstrelaSelecionadaProps,
} from './PainelEstrelaSelecionada'
export {
  GRAFICOS,
  GRAFICO_PADRAO,
  PARAM_GRAFICO,
  SeletorDeGrafico,
  ehTipoGrafico,
  useGraficoNaUrl,
  type ItemGrafico,
  type SeletorDeGraficoProps,
  type TipoGrafico,
} from './SeletorDeGrafico'

// As 5 visualizações
export { Carta, type CartaProps } from './Carta'
export { Orbitas, type PropsOrbitas } from './Orbitas'
export { Espectro, type EspectroProps } from './Espectro'
export { LINHA_JANELA_MESES, Linha, type PropsLinha } from './Linha'
export { Matriz, type MatrizProps } from './Matriz'

// Dado plotável
export {
  AUTONOMIA_MAX,
  AUTONOMIA_MIN,
  TOTAL_AUTONOMIA,
  type Constelacao,
  type DatasetCarta,
  type NivelAutonomia,
  type PontoPlotavel,
  type PropsGrafico,
} from './tipos'
export {
  DATASET_VAZIO,
  MIN_PONTOS_CONSTELACAO,
  confiancaDeNivel,
  constelacaoDoPonto,
  ehPontoPlotavel,
  montarConstelacoes,
  montarDataset,
  pontoPorId,
  rotuloRodape,
} from './dataset'
