import { useMemo, useState } from 'react'
import { Database, Grid3x3, TableProperties } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Matriz } from '@/components/charts/Matriz'
import { montarDataset, rotuloRodape } from '@/components/charts/dataset'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Numero, Painel, percentual } from '@/components/pesquisa/primitivos'
import { buttonClasses } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { Switch } from '@/components/ui/switch'
import { corDaClasse, rotuloCanonico } from '@/domain/complexidade'
import { NOTA_COMPLEXIDADE_SO_JAVA } from '@/domain/enums'
import {
  type ColunaDeAutonomia,
  type FiltrosDeSensibilidade,
  MIN_PARTICIPANTES_PARA_DETALHE,
  PISO_PARA_PROPORCAO,
  SEM_FILTRO,
  aplicarFiltros,
  medirRecorte,
  montarColunas,
  paraPontoCarta,
  ultimaAnalise,
} from '@/features/pesquisa/cruzamento'
import { useCoorte } from '@/features/pesquisa/hooks'
import { useTheme } from '@/theme/ThemeProvider'
import { formatDateTime, pluralPt } from '@/lib/utils'
import type { ResolucaoDaCoorteDTO } from '@/types/api'

/*
 * P · Cruzamento autonomia × complexidade (área de pesquisa) — ÓRBITA.
 *
 * É a única pergunta do projeto que usa a palavra "correlação" (§2.3), e até aqui nenhuma tela a
 * respondia: a de qualidade mede o instrumento, a de coorte lista linhas.
 *
 * O que esta tela NÃO faz, e por quê:
 *  - não calcula r nem p-valor. Inferência sobre Big O ESTIMADO, impressa com a autoridade de uma
 *    medição, é como viés entra sem ninguém perceber. Ela mostra a distribuição condicional com o
 *    denominador sempre à vista, e deixa o teste para o R/pandas com o CSV;
 *  - não normaliza coluna com poucas resoluções. Percentual sobre n=3 dá segmentos de 33 pontos, e
 *    "33% contra 8%" lê-se como "4× mais" ainda que o n esteja impresso ao lado;
 *  - não deixa clicar numa célula. A Matriz é montada SEM `onSelecionar` de propósito: abrir a
 *    resolução de outra pessoa a partir de um agregado inverteria a lógica da pseudonimização, em
 *    que revelar é ato deliberado e separado (é o que a tela de coorte faz, com diálogo).
 *
 * A assimetria dos dois eixos é o fato mais importante daqui: autonomia é autodeclarada e existe em
 * TODA resolução; classe de complexidade só existe onde há analisador — hoje, só Java. Por isso as
 * colunas trazem dois denominadores, e uma coluna sem classe nenhuma diz "sem analisador" em vez de
 * aparecer vazia, que se leria como "ninguém submeteu".
 */

export function CruzamentoPage() {
  const query = useCoorte()
  const [filtros, setFiltros] = useState<FiltrosDeSensibilidade>(SEM_FILTRO)

  return (
    <PageContainer className="gap-[18px]">
      <PageHeader
        title="Autonomia × complexidade"
        subtitle="Como as classes de complexidade se distribuem dentro de cada nível de autonomia declarada."
        actions={
          <Link
            to="/app/pesquisa/coorte"
            className={buttonClasses({ variant: 'secondary', size: 'sm' })}
          >
            <TableProperties size={14} strokeWidth={2} aria-hidden />
            Ver dado bruto
          </Link>
        }
      />

      <QueryBoundary query={query}>
        {(coorte) =>
          coorte.length === 0 ? (
            <EmptyState
              icon={Database}
              title="Nenhuma resolução na coorte."
              description="A coorte reúne apenas quem consentiu com a participação na pesquisa. Enquanto ninguém consentir — ou submeter — não há o que cruzar."
            />
          ) : (
            <Cruzamento coorte={coorte} filtros={filtros} onFiltrar={setFiltros} />
          )
        }
      </QueryBoundary>
    </PageContainer>
  )
}

function Cruzamento({
  coorte,
  filtros,
  onFiltrar,
}: {
  coorte: ResolucaoDaCoorteDTO[]
  filtros: FiltrosDeSensibilidade
  onFiltrar: (filtros: FiltrosDeSensibilidade) => void
}) {
  const { theme } = useTheme()

  const recortada = useMemo(() => aplicarFiltros(coorte, filtros), [coorte, filtros])
  const colunas = useMemo(() => montarColunas(recortada), [recortada])
  const dataset = useMemo(() => montarDataset(recortada.map(paraPontoCarta)), [recortada])

  const bruto = medirRecorte(coorte)
  const recorte = medirRecorte(recortada)
  const analisadoEm = ultimaAnalise(recortada)

  return (
    <div className="flex flex-col gap-[18px]">
      <Sensibilidade filtros={filtros} onFiltrar={onFiltrar} bruto={bruto} recorte={recorte} />

      <div className="grid grid-cols-1 gap-[18px] lg:grid-cols-2">
        <Painel
          titulo="Densidade"
          nota="Cada célula conta as resoluções com aquela classe de tempo naquele nível de autonomia. A intensidade é contagem; o matiz é a classe."
        >
          {/* Sem onSelecionar: célula é leitura, não porta de navegação. */}
          <Matriz dataset={dataset} tema={theme} />
        </Painel>

        <Painel
          titulo="Distribuição dentro de cada nível"
          nota={`Abaixo de ${PISO_PARA_PROPORCAO} resoluções classificadas, a coluna desenha cada resolução em vez de uma proporção — ${PISO_PARA_PROPORCAO - 2} unidades não viram porcentagem.`}
        >
          <Colunas colunas={colunas} tema={theme} />
        </Painel>
      </div>

      <Denominadores dataset={dataset} colunas={colunas} analisadoEm={analisadoEm} />

      <LimitesDoDado />
    </div>
  )
}

/* -------------------------------------------------------- sensibilidade --- */

/**
 * Os dois cortes e o preço de cada um, em números, na mesma linha. É a única parte da tela que
 * ensina sobre a fragilidade da amostra em vez de escondê-la: apertar o filtro e ver
 * "23 → 11 resoluções · 5 → 3 participantes" diz mais sobre o n do que qualquer aviso.
 */
function Sensibilidade({
  filtros,
  onFiltrar,
  bruto,
  recorte,
}: {
  filtros: FiltrosDeSensibilidade
  onFiltrar: (filtros: FiltrosDeSensibilidade) => void
  bruto: { resolucoes: number; participantes: number }
  recorte: { resolucoes: number; participantes: number }
}) {
  const mudou = recorte.resolucoes !== bruto.resolucoes

  return (
    <Card className="flex flex-col gap-[15px] p-[18px]">
      <div className="flex flex-col gap-[13px] md:flex-row md:items-start md:gap-[28px]">
        <Interruptor
          id="confianca-alta"
          ligado={filtros.somenteConfiancaAlta}
          onMudar={(v) => onFiltrar({ ...filtros, somenteConfiancaAlta: v })}
          rotulo="Só confiança ALTA do motor"
          nota="O motor tem confiança alta onde entende bem, e entende melhor o código simples — este filtro seleciona na direção do próprio desfecho."
        />
        <Interruptor
          id="minimo-por-participante"
          ligado={filtros.minimoDeResolucoesPorParticipante > 1}
          onMudar={(v) =>
            onFiltrar({ ...filtros, minimoDeResolucoesPorParticipante: v ? 3 : 1 })
          }
          rotulo="Só quem tem 3 ou mais resoluções"
          nota="Tira de cena quem submeteu uma vez só. Também tira a chance de o resultado descrever justamente quem persistiu."
        />
      </div>

      <p className="font-mono text-[11.5px] text-mid">
        {mudou ? (
          <>
            {bruto.resolucoes} → <strong className="font-semibold text-ink">{recorte.resolucoes}</strong>{' '}
            {pluralPt(recorte.resolucoes, 'resolução', 'resoluções').replace(/^\d+\s/, '')} ·{' '}
            {bruto.participantes} →{' '}
            <strong className="font-semibold text-ink">{recorte.participantes}</strong> participantes
          </>
        ) : (
          <>
            {pluralPt(bruto.resolucoes, 'resolução', 'resoluções')} ·{' '}
            {pluralPt(bruto.participantes, 'participante', 'participantes')} · amostra sem recorte
          </>
        )}
      </p>
    </Card>
  )
}

function Interruptor({
  id,
  ligado,
  onMudar,
  rotulo,
  nota,
}: {
  id: string
  ligado: boolean
  onMudar: (ligado: boolean) => void
  rotulo: string
  nota: string
}) {
  return (
    <div className="flex flex-1 items-start gap-[11px]">
      <Switch id={id} checked={ligado} onCheckedChange={onMudar} />
      <label htmlFor={id} className="flex cursor-pointer flex-col gap-[3px]">
        <span className="text-[12.5px] font-medium text-ink">{rotulo}</span>
        <span className="text-[11.5px] leading-[1.5] text-soft">{nota}</span>
      </label>
    </div>
  )
}

/* --------------------------------------------------------------- colunas --- */

function Colunas({ colunas, tema }: { colunas: ColunaDeAutonomia[]; tema: 'dark' | 'light' }) {
  return (
    <div className="flex items-end gap-[9px]">
      {colunas.map((coluna) => (
        <Coluna key={coluna.autonomia} coluna={coluna} tema={tema} />
      ))}
    </div>
  )
}

const ALTURA_DA_COLUNA = 132

function Coluna({ coluna, tema }: { coluna: ColunaDeAutonomia; tema: 'dark' | 'light' }) {
  return (
    <div className="flex flex-1 flex-col items-center gap-[7px]">
      <div
        className="flex w-full flex-col justify-end overflow-hidden rounded-ci-sm bg-recess"
        style={{ height: ALTURA_DA_COLUNA }}
      >
        {coluna.comClasse === 0 ? null : coluna.proporcaoConfiavel ? (
          <Empilhada coluna={coluna} tema={tema} />
        ) : (
          <Unidades coluna={coluna} tema={tema} />
        )}
      </div>

      <span className="font-mono text-[12px] font-semibold text-ink">{coluna.autonomia}</span>

      <span className="text-center font-mono text-[10px] leading-[1.35] text-soft">
        {coluna.total === 0 ? (
          'sem resolução'
        ) : (
          <>
            n={coluna.total}
            <br />
            {coluna.comClasse} com classe
            <br />
            {coluna.detalhavel
              ? `${coluna.participantes} particip.`
              : `<${MIN_PARTICIPANTES_PARA_DETALHE} particip.`}
          </>
        )}
      </span>
    </div>
  )
}

/** Barra 100% empilhada — só acima do piso, onde a proporção significa alguma coisa. */
function Empilhada({ coluna, tema }: { coluna: ColunaDeAutonomia; tema: 'dark' | 'light' }) {
  const classes = [...coluna.porClasse.entries()].sort((a, b) => b[0] - a[0])

  return (
    <>
      {classes.map(([k, contagem]) => (
        <div
          key={k}
          title={`${rotuloCanonico(k)} · ${contagem} de ${coluna.comClasse} (${percentual(contagem, coluna.comClasse)})`}
          style={{
            height: `${(contagem / coluna.comClasse) * 100}%`,
            backgroundColor: corDaClasse(k, tema),
          }}
        />
      ))}
    </>
  )
}

/**
 * Abaixo do piso, uma faixa por resolução. Não há porcentagem para mentir: três faixas leem-se
 * como três resoluções, que é exatamente o que são.
 */
function Unidades({ coluna, tema }: { coluna: ColunaDeAutonomia; tema: 'dark' | 'light' }) {
  const altura = Math.min(18, ALTURA_DA_COLUNA / Math.max(coluna.classes.length, 1))

  return (
    <>
      {[...coluna.classes].reverse().map((k, i) => (
        <div
          key={i}
          title={rotuloCanonico(k)}
          className="w-full shrink-0 border-b border-panel last:border-b-0"
          style={{ height: altura, backgroundColor: corDaClasse(k, tema) }}
        />
      ))}
    </>
  )
}

/* --------------------------------------------------------- denominadores --- */

/**
 * O denominador desta tela é DIFERENTE do da tela de qualidade, e dizê-lo é obrigatório: lá,
 * "autonomia 3" conta a coorte inteira, inclusive linguagem sem analisador; aqui, a Matriz conta só
 * o que tem classe. Os dois números estão certos, e um pesquisador que os visse divergir sem
 * explicação não teria como saber qual usar.
 */
function Denominadores({
  dataset,
  colunas,
  analisadoEm,
}: {
  dataset: ReturnType<typeof montarDataset>
  colunas: ColunaDeAutonomia[]
  analisadoEm: string | null
}) {
  const declaradas = colunas.reduce((s, c) => s + c.total, 0)
  const classificadas = colunas.reduce((s, c) => s + c.comClasse, 0)

  return (
    <div className="grid grid-cols-1 gap-[13px] sm:grid-cols-3">
      <Numero
        rotulo="Autonomia declarada"
        valor={declaradas}
        pequeno
        nota="Toda resolução tem nível — é autodeclarado e independe da linguagem."
      />
      <Numero
        rotulo="Com classe medida"
        valor={classificadas}
        pequeno
        nota={
          declaradas === 0
            ? 'Sem resoluções no recorte.'
            : `${percentual(classificadas, declaradas)} do recorte. ${rotuloRodape(dataset)}.`
        }
      />
      <Numero
        rotulo="Última análise"
        valor={analisadoEm ? formatDateTime(analisadoEm) : '—'}
        pequeno
        icone={Grid3x3}
        nota={
          analisadoEm
            ? 'Safra do dado na tela. Uma reanálise reescreve as métricas e move esta data.'
            : 'Nenhuma métrica gravada neste recorte.'
        }
      />
    </div>
  )
}

/**
 * Os limites são do SCHEMA, não desta tela — e é por isso que ficam fixos, e não como aviso que se
 * fecha. Sem eles, o gráfico é lido como relação de causa, que é o erro que ele mais convida.
 */
function LimitesDoDado() {
  return (
    <Card className="flex flex-col gap-[7px] p-[18px]">
      <p className="text-[12.5px] font-medium text-ink">O que este cruzamento não pode afirmar</p>
      <ul className="flex list-disc flex-col gap-[5px] pl-[18px] text-[11.5px] leading-[1.55] text-body">
        <li>
          <strong className="font-medium text-ink">Não há registro de corretude.</strong> Nenhuma
          tabela guarda se a solução funcionava — um <code>O(1)</code> errado ocupa a mesma célula
          que um <code>O(1)</code> certo.
        </li>
        <li>
          <strong className="font-medium text-ink">Não há dificuldade do desafio.</strong> A tabela
          de desafios não tem nível, tópico nem pontuação, e é plausível que se recorra mais à IA
          justamente nos problemas mais difíceis — o que explicaria as duas variáveis de uma vez.
        </li>
        <li>
          <strong className="font-medium text-ink">A autonomia é autodeclarada</strong> na submissão,
          sem segunda fonte que a valide e sem edição posterior.
        </li>
        <li>
          <strong className="font-medium text-ink">{NOTA_COMPLEXIDADE_SO_JAVA}</strong> Resolução em
          outra linguagem entra na coorte e no nível de autonomia, mas nunca ganha classe — coluna
          alta sem classe nenhuma é falta de instrumento, não ausência de trabalho. Em C o motor
          mede a complexidade ciclomática, que não aparece neste cruzamento.
        </li>
      </ul>
    </Card>
  )
}
