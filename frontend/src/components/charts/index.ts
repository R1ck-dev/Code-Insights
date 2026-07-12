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
 *   // As duas LENTES do painel vivem na página: o painel lateral também precisa delas.
 *   const [view, setView] = useGraficoNaUrl()
 *   const [granularidade, setGranularidade] = useState(GRANULARIDADE_PADRAO)
 *   const grupo = useMemo(
 *     () => grupoDeIrmaos({ view, granularidade, dataset, selecionadoId }),
 *     [view, granularidade, dataset, selecionadoId],
 *   )
 *
 *   <div className="grid gap-3.5 lg:grid-cols-[1.62fr_1fr]">
 *     <PainelDeGraficos
 *       dataset={dataset}
 *       carregando={carta.isPending}
 *       erro={carta.isError ? apiErrorMessage(carta.error) : null}
 *       onTentarNovamente={() => void carta.refetch()}
 *       selecionadoId={selecionadoId}
 *       onSelecionar={setSelecionadoId}
 *       view={view} onViewChange={setView}
 *       granularidade={granularidade} onGranularidadeChange={setGranularidade}
 *     />
 *     <PainelEstrelaSelecionada
 *       ponto={pontoPorId(dataset, selecionadoId)}
 *       grupo={grupo}                    // ← habilita o navegador "‹ 2 de 3 ›"
 *       onSelecionar={setSelecionadoId}  // ← trocar de irmã = trocar a seleção da página
 *     />
 *   </div>
 *
 * ⚠ `grupo` + `onSelecionar` no painel lateral NÃO são decoração: os três gráficos COLAPSAM
 * resoluções num único alvo (a Carta e a Matriz, por célula autonomia × classe; a Linha, por
 * período) e o clique abre só UMA delas. Sem esses dois props, as irmãs ficam sem porta de
 * entrada — e `grupo` tem de vir de `grupoDeIrmaos(...)`, que é quem sabe QUAL gráfico está na
 * tela: um agrupamento fixo faria a seta levar a uma resolução que não está no ponto clicado.
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

// As 3 visualizações. Duas foram REMOVIDAS (componente e arquivo):
// ⚠ `Espectro` — plotava classes, não resoluções, e duplicava o card "Distribuição · Espectro" do
//   dashboard. `linhasEspectro()` (abaixo) continua sendo a fonte daquele card: o histograma não
//   morreu, a duplicata no seletor é que morreu.
// ⚠ `Orbitas`/Espiral — redundante com a `Linha`: mesma pergunta (autonomia × tempo, complexidade
//   × tempo) respondida por canais perceptuais mais fracos (tamanho e ângulo, não posição).
export { Carta, type CartaProps } from './Carta'
export { LINHA_JANELA_MESES, Linha, type PropsLinha } from './Linha'
export { Matriz, type MatrizProps } from './Matriz'

// Dado plotável
export {
  AUTONOMIA_MAX,
  AUTONOMIA_MIN,
  GRANULARIDADE_PADRAO,
  GRANULARIDADES,
  ROTULO_GRANULARIDADE,
  TOTAL_AUTONOMIA,
  type Constelacao,
  type DatasetCarta,
  type DescartesDataset,
  type Granularidade,
  type NivelAutonomia,
  type PontoBase,
  type PontoPlotavel,
  type PropsGrafico,
} from './tipos'
// Quem são as IRMÃS da resolução selecionada — e o critério muda com o gráfico na tela (célula,
// na Carta e na Matriz; período, na Linha). Sem isto, o painel lateral navega pelo agrupamento
// errado. Ver o cabeçalho de `./irmaos`.
export { grupoDeIrmaos, type ContextoDoGrupo, type GrupoDeIrmaos } from './irmaos'
export {
  DATASET_VAZIO,
  MIN_PONTOS_CONSTELACAO,
  constelacaoDoPonto,
  ehPontoPlotavel,
  montarConstelacoes,
  montarDataset,
  pontoPorId,
  rotuloRodape,
} from './dataset'
// ⚠ NÃO existe (nem volte a criar) um `confiancaDeNivel(NivelConfianca) → MEDIDO/ESTIMADO`:
// são dois eixos diferentes. A natureza da métrica vem de `TIPO_METRICA_META` (@/domain/enums).
export { linhasEspectro } from './escalas'
