import {
  AlertTriangle,
  Clock,
  Database,
  Grid3x3,
  TableProperties,
  TriangleAlert,
  UserRound,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Numero, Painel, Titulo, Trilho, percentual } from '@/components/pesquisa/primitivos'
import { buttonClasses } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { EmptyState } from '@/components/ui/empty-state'
import { LanguageDot } from '@/components/domain/badges'
import { LINGUAGEM_META } from '@/domain/enums'
import { useQualidadeDaCoorte } from '@/features/pesquisa/hooks'
import { formatDate, pluralPt } from '@/lib/utils'
import type { ContagemDTO, ContagemPorLinguagemDTO, QualidadeDaCoorteDTO } from '@/types/api'

/*
 * P · Qualidade dos dados (área de pesquisa) — ÓRBITA.
 *
 * Esta tela mede o INSTRUMENTO, não o resultado. Ela responde "o dado que estou coletando serve?"
 * e nada além disso: não há correlação, coeficiente nem conclusão aqui. Cruzamento de variáveis é
 * da tela de coorte; concluir é do pesquisador.
 *
 * O que ela deliberadamente NÃO faz:
 *  - não calcula r, p-valor ou significância — inferência sobre Big O estimado, impressa com a
 *    mesma autoridade de uma medição, é como viés entra sem ninguém perceber;
 *  - não suaviza distribuição: com n pequeno, curva suave esconde que são 14 pontos;
 *  - não esconde o buraco — nível de autonomia sem nenhuma resolução aparece como zero, porque
 *    ausência também é dado.
 */

export function QualidadeDosDadosPage() {
  const query = useQualidadeDaCoorte()

  return (
    <PageContainer className="gap-[18px]">
      <PageHeader
        title="Qualidade dos dados"
        subtitle="A saúde da amostra durante a coleta — cobertura da análise, distribuições e buracos."
        actions={
          <div className="flex flex-wrap gap-[7px]">
            <Link
              to="/app/pesquisa/cruzamento"
              className={buttonClasses({ variant: 'secondary', size: 'sm' })}
            >
              <Grid3x3 size={14} strokeWidth={2} aria-hidden />
              Autonomia × complexidade
            </Link>
            <Link
              to="/app/pesquisa/coorte"
              className={buttonClasses({ variant: 'secondary', size: 'sm' })}
            >
              <TableProperties size={14} strokeWidth={2} aria-hidden />
              Ver dado bruto
            </Link>
          </div>
        }
      />

      <QueryBoundary query={query}>
        {(dados) =>
          dados.resolucoes === 0 ? (
            <EmptyState
              icon={Database}
              title="Nenhuma resolução na plataforma ainda."
              description="Assim que os participantes começarem a submeter, a cobertura da análise aparece aqui."
            />
          ) : (
            <div className="flex flex-col gap-[18px]">
              {!dados.termoAprovadoPeloComite && <AvisoDeTermoNaoAprovado versao={dados.versaoDoTermo} />}
              <Resumo dados={dados} />
              <Consentimento dados={dados} />
              <Cobertura dados={dados} />
              <Alertas dados={dados} />

              <div className="grid grid-cols-1 gap-[18px] lg:grid-cols-3">
                <Painel titulo="Linguagens">
                  <ListaDeLinguagens dados={dados} />
                </Painel>
                <Painel
                  titulo="Confiança do motor"
                  nota="Sobre as resoluções que têm métrica de tempo. Sem métrica não é confiança baixa — é confiança nenhuma."
                >
                  <Distribuicao itens={dados.porConfiancaDoTempo} />
                </Painel>
                <Painel
                  titulo="Autonomia declarada"
                  nota="1 = mais apoio de IA · 5 = autônomo. Escala inteira, inclusive os níveis sem ninguém."
                >
                  <Distribuicao itens={dados.porAutonomia} />
                </Painel>
              </div>
            </div>
          )
        }
      </QueryBoundary>
    </PageContainer>
  )
}

/* ---------------------------------------------------------------- resumo --- */

function Resumo({ dados }: { dados: QualidadeDaCoorteDTO }) {
  return (
    <div className="grid grid-cols-2 gap-[14px] lg:grid-cols-4">
      <Numero rotulo="Participantes" valor={dados.participantes} icone={UserRound} />
      <Numero rotulo="Resoluções" valor={dados.resolucoes} icone={Database} />
      <Numero
        rotulo="Com métrica"
        valor={dados.comMetrica}
        nota={`${percentual(dados.comMetrica, dados.resolucoes)} da amostra`}
      />
      <Numero
        rotulo="Período"
        valor={formatDate(dados.primeiraSubmissao)}
        nota={`até ${formatDate(dados.ultimaSubmissao)}`}
        icone={Clock}
        pequeno
      />
    </div>
  )
}

/* ---------------------------------------------------- consentimento (TCLE) --- */

function AvisoDeTermoNaoAprovado({ versao }: { versao: string }) {
  return (
    <div
      role="status"
      className="flex items-start gap-[11px] rounded-ci border border-atencao-line bg-atencao-bg p-[15px]"
    >
      <TriangleAlert size={16} strokeWidth={2} aria-hidden className="mt-[2px] shrink-0 text-atencao-ink" />
      <div className="flex flex-col gap-[5px]">
        <p className="text-[13px] font-semibold text-atencao-ink">
          Termo <span className="font-mono">{versao}</span> não aprovado pelo Comitê de Ética.
        </p>
        <p className="text-[12.5px] leading-[1.6] text-atencao-ink">
          Isto aqui é ensaio, não coleta: nada nesta tela pode ir para o artigo. O arquivo exportado
          carrega a versão do termo no nome, justamente para não se misturar com dado válido depois.
        </p>
      </div>
    </div>
  )
}

/**
 * A cobertura de consentimento é o denominador de tudo o mais nesta tela. Sem ela, uma coorte de 3
 * resoluções numa plataforma de 40 pareceria uma plataforma de 3 resoluções — e a conclusão sairia
 * sobre uma amostra que ninguém sabia que era pequena.
 *
 * "Sem resposta" aparece separado de "recusou" porque são coisas acionáveis diferentes: a um se
 * convida de novo, ao outro não se insiste.
 */
function Consentimento({ dados }: { dados: QualidadeDaCoorteDTO }) {
  const responderam = dados.participantesQueConsentiram + dados.participantesQueRecusaram
  const total = responderam + dados.participantesSemResposta

  return (
    <Card className="flex flex-col gap-[13px] p-[18px]">
      <div className="flex flex-wrap items-baseline justify-between gap-[9px]">
        <Titulo texto="Consentimento" />
        <span className="font-mono text-[11px] text-soft">
          termo {dados.versaoDoTermo} · {percentual(dados.participantesQueConsentiram, total)} de
          quem submeteu
        </span>
      </div>

      <ul className="flex flex-wrap gap-x-[22px] gap-y-[9px]">
        <Contagem rotulo="Autorizaram" valor={dados.participantesQueConsentiram} />
        <Contagem rotulo="Recusaram" valor={dados.participantesQueRecusaram} />
        <Contagem rotulo="Sem resposta" valor={dados.participantesSemResposta} />
        <Contagem rotulo="Resoluções fora da coorte" valor={dados.resolucoesForaDaCoorte} />
      </ul>

      <p className="text-[11.5px] leading-[1.5] text-soft">
        Tudo o mais nesta tela e na tabela descreve <strong className="font-semibold">apenas</strong>{' '}
        quem autorizou. Quem recusou ou não respondeu não aparece em nenhuma contagem, distribuição
        ou export.
      </p>
    </Card>
  )
}

function Contagem({ rotulo, valor }: { rotulo: string; valor: number }) {
  return (
    <li className="flex items-baseline gap-[7px]">
      <span className="text-[12.5px] text-mid">{rotulo}</span>
      <span className="tabular font-mono text-[15px] font-semibold text-ink">{valor}</span>
    </li>
  )
}

/* ------------------------------------------------------------- cobertura --- */

/**
 * Os quatro estados somam o total — o backend garante a partição. A separação entre "falha" e
 * "sem analisador" é a que importa: as duas produzem o mesmo estado no banco (analisada, sem
 * métrica) e significam coisas opostas. Uma é defeito a investigar, a outra é escopo conhecido.
 */
function Cobertura({ dados }: { dados: QualidadeDaCoorteDTO }) {
  const faixas = [
    { rotulo: 'Com métrica', total: dados.comMetrica, cor: 'bg-ink' },
    { rotulo: 'Aguardando análise', total: dados.aguardandoAnalise, cor: 'bg-mid' },
    { rotulo: 'Falha de análise', total: dados.falhaDeAnalise, cor: 'bg-erro-estrutura' },
    { rotulo: 'Sem analisador', total: dados.semAnalisadorDeLinguagem, cor: 'bg-soft' },
  ]

  return (
    <Card className="flex flex-col gap-[13px] p-[18px]">
      <Titulo texto="Cobertura da análise" />

      <div className="flex h-[10px] w-full overflow-hidden rounded-ci-sm bg-recess" aria-hidden>
        {faixas
          .filter((faixa) => faixa.total > 0)
          .map((faixa) => (
            <div
              key={faixa.rotulo}
              className={faixa.cor}
              style={{ width: `${(faixa.total / dados.resolucoes) * 100}%` }}
            />
          ))}
      </div>

      <ul className="flex flex-wrap gap-x-[22px] gap-y-[7px]">
        {faixas.map((faixa) => (
          <li key={faixa.rotulo} className="flex items-center gap-[7px]">
            <span aria-hidden className={`size-[7px] shrink-0 ${faixa.cor}`} />
            <span className="text-[12.5px] text-mid">{faixa.rotulo}</span>
            <span className="tabular font-mono text-[12.5px] font-semibold text-ink">
              {faixa.total}
            </span>
          </li>
        ))}
      </ul>
    </Card>
  )
}

/* --------------------------------------------------------------- alertas --- */

function Alertas({ dados }: { dados: QualidadeDaCoorteDTO }) {
  const alertas = []

  if (dados.falhaDeAnalise > 0) {
    alertas.push({
      tom: 'erro' as const,
      icone: TriangleAlert,
      texto: `${pluralPt(dados.falhaDeAnalise, 'resolução não pôde ser analisada', 'resoluções não puderam ser analisadas')} — o motor sabia a linguagem e o código não parseou. Vale investigar antes do fim da coleta.`,
    })
  }
  if (dados.participantesComUmaResolucao > 0) {
    alertas.push({
      tom: 'atencao' as const,
      icone: AlertTriangle,
      texto: `${pluralPt(dados.participantesComUmaResolucao, 'participante submeteu', 'participantes submeteram')} uma vez só. Com uma submissão não há evolução a medir.`,
    })
  }

  if (alertas.length === 0) return null

  return (
    <div className="flex flex-col gap-[9px]">
      {alertas.map(({ tom, icone: Icone, texto }) => (
        <div
          key={tom}
          role="status"
          className={
            tom === 'erro'
              ? 'flex items-start gap-[9px] rounded-ci border border-erro-card-line bg-erro-card-bg p-[13px]'
              : 'flex items-start gap-[9px] rounded-ci border border-atencao-line bg-atencao-bg p-[13px]'
          }
        >
          <Icone
            size={14}
            strokeWidth={2}
            aria-hidden
            className={`mt-[2px] shrink-0 ${tom === 'erro' ? 'text-erro-texto' : 'text-atencao-ink'}`}
          />
          <p
            className={`text-[12.5px] leading-[1.5] ${tom === 'erro' ? 'text-erro-texto' : 'text-atencao-ink'}`}
          >
            {texto}
          </p>
        </div>
      ))}
    </div>
  )
}

/* --------------------------------------------------------- distribuições --- */

function ListaDeLinguagens({ dados }: { dados: QualidadeDaCoorteDTO }) {
  const maior = Math.max(...dados.porLinguagem.map((item) => item.total), 1)

  return (
    <ul className="flex flex-col gap-[11px]">
      {dados.porLinguagem.map((item) => (
        <li key={item.linguagem} className="flex flex-col gap-[5px]">
          <div className="flex items-center gap-[7px]">
            <LanguageDot lang={item.linguagem} />
            <span className="text-[12.5px] text-body">
              {LINGUAGEM_META[item.linguagem]?.label ?? item.linguagem}
            </span>
            <CoberturaDaLinguagem item={item} />
            <span className="flex-1" />
            <span className="tabular font-mono text-[12.5px] font-semibold text-ink">
              {item.total}
            </span>
          </div>
          <Trilho fracao={item.total / maior} apagado={!item.comAnalisador} />
        </li>
      ))}
    </ul>
  )
}

/**
 * Três estados, e não dois. "Com analisador" sozinho seria contraditório para C: a linha diria que
 * a linguagem é analisada enquanto a cobertura acima conta as mesmas resoluções como sem classe de
 * tempo. Nomear o suporte parcial é o que reconcilia os dois números.
 */
function CoberturaDaLinguagem({ item }: { item: ContagemPorLinguagemDTO }) {
  if (!item.comAnalisador) {
    return (
      <span className="font-mono text-[10px] tracking-[.05em] text-soft uppercase">
        sem analisador
      </span>
    )
  }

  if (!item.metricasSuportadas.includes('BIG_O_TEMPO')) {
    return (
      <span className="font-mono text-[10px] tracking-[.05em] text-soft uppercase">
        só ciclomática
      </span>
    )
  }

  return null
}

function Distribuicao({ itens }: { itens: ContagemDTO[] }) {
  const maior = Math.max(...itens.map((item) => item.total), 1)

  if (itens.length === 0) {
    return <p className="text-[12.5px] text-soft">Sem dado ainda.</p>
  }

  return (
    <ul className="flex flex-col gap-[11px]">
      {itens.map((item) => (
        <li key={item.chave} className="flex flex-col gap-[5px]">
          <div className="flex items-center gap-[9px]">
            <span className="font-mono text-[12px] text-body">{item.chave}</span>
            <span className="flex-1" />
            <span className="tabular font-mono text-[12.5px] font-semibold text-ink">
              {item.total}
            </span>
          </div>
          <Trilho fracao={item.total / maior} apagado={item.total === 0} />
        </li>
      ))}
    </ul>
  )
}

/* Painel, Titulo, Numero, Trilho e percentual vivem em components/pesquisa/primitivos — a tela de
 * cruzamento usa os mesmos, e duas cópias divergiriam. */
