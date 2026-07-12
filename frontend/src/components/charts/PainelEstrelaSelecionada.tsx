/*
 * PAINEL "ESTRELA SELECIONADA" (spec 02 §2.12) — o cartão lateral da resolução clicada.
 *
 * Traduz um ponto do gráfico de volta para o que ele é: uma resolução, com data, métricas e
 * autonomia. Fora do SVG, à direita (a página compõe a grade `1.62fr 1fr`, gap 14px).
 *
 * Regras que este cartão carrega inteiras:
 *   3 — MEDIDO (quadrado CHEIO) vs. ≈ ESTIMADO (quadrado VAZADO + prefixo `≈`). O espaço é
 *       estimado por natureza (análise estática); a ciclomática é MEDIDA (contagem exata no
 *       AST) e por isso não tem quadrado nem cor: é `ink` puro.
 *   4 — autonomia é NEUTRA: `AutonomyMeter`, nunca colormap.
 *   6 — "sem métrica" é um estado de primeira classe: `—` em `soft`, jamais um valor inventado.
 *
 * ── NAVEGADOR DE GRUPO (rodada de correção) ─────────────────────────────────────────────────
 * Os três gráficos AGRUPAM resoluções num único alvo, e o clique abre UMA delas. Sem um caminho
 * de volta, as outras ficariam inalcançáveis pelo gráfico — o dado existiria e não teria porta.
 * Este painel é a porta: quando a resolução selecionada divide o alvo com outras, ele mostra
 * "‹ 2 de 3 ›" e diz POR QUE elas dividem o ponto.
 *
 * ⚠ O CRITÉRIO DO GRUPO NÃO É DAQUI (bug que o usuário viu). Ele era fixo — "mesma célula da
 * carta" — e as setas apareciam mesmo com a LINHA na tela, oferecendo navegar por autonomia ×
 * classe enquanto o gráfico agrupava por PERÍODO: a seta levava a uma resolução que não estava
 * no ponto clicado. Agora o grupo chega PRONTO, calculado por `grupoDeIrmaos(...)` a partir da
 * visualização ativa (célula, na Carta e na Matriz; bucket do tempo, na Linha). Este componente
 * não sabe — nem deve saber — qual gráfico está na tela: ele só renderiza o grupo que recebeu.
 */
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { StatusChip } from '@/components/domain/badges'
import {
  CONFIANCA_BIG_O,
  ORDEM_DESCONHECIDA,
  PREFIXO_ESTIMADO,
  ROTULO_DESCONHECIDO,
  TIPO_METRICA_META,
  corDaClasse,
  rotuloCanonico,
  rotuloConfiancaMotor,
  tintaDaClasse,
} from '@/domain/enums'
import { useTheme } from '@/theme/ThemeProvider'
import { cn } from '@/lib/utils'
import { dataCompleta } from './escalas'
import type { GrupoDeIrmaos } from './irmaos'
import type { PontoPlotavel } from './tipos'

export interface PainelEstrelaSelecionadaProps {
  /** `null` → estado de repouso ("clique numa estrela"), com a mesma altura: nada pula. */
  ponto: PontoPlotavel | null | undefined
  /**
   * O grupo em que a selecionada está DENTRO do gráfico que está na tela — monte-o com
   * `grupoDeIrmaos({ view, granularidade, dataset, selecionadoId })` (`./irmaos`). Omitir é
   * legítimo: o painel funciona como sempre, só não oferece a navegação entre as irmãs.
   *
   * ⚠ O critério de agrupamento MUDA com o gráfico (célula na Carta/Matriz, período na Linha).
   * Por isso ele não pode ser recalculado aqui: quem sabe o que está desenhado é a página.
   */
  grupo?: GrupoDeIrmaos | null
  /**
   * Trocar de irmã é trocar a SELEÇÃO da página (o mesmo callback dos gráficos): o gráfico realça
   * o mesmo alvo — ele não se move, é o agrupamento — e este painel troca de conteúdo.
   */
  onSelecionar?: (resolucaoId: string) => void
  /**
   * Rota da resolução. Padrão: a do aluno dono (`/app/resolucoes/:id`). O portfólio público
   * passa a sua (`/u/:usuarioId/desafios/:desafioId/resolucoes/:id`).
   */
  hrefResolucao?: (ponto: PontoPlotavel) => string
  className?: string
}

const CARTAO =
  'flex flex-col gap-[13px] rounded-ci border border-line bg-recess px-4 py-[15px]'

const CABECALHO = 'font-mono text-[10.5px] uppercase tracking-[.1em] text-mid'

function hrefPadrao(ponto: PontoPlotavel): string {
  return `/app/resolucoes/${ponto.resolucaoId}`
}

export function PainelEstrelaSelecionada({
  ponto,
  grupo,
  onSelecionar,
  hrefResolucao = hrefPadrao,
  className,
}: PainelEstrelaSelecionadaProps) {
  const { theme } = useTheme()

  /*
   * As resoluções que dividem o alvo com a selecionada — inclusive ela —, em ordem cronológica.
   * Resolução sozinha no alvo devolve `[ela]`: o caso comum (`length < 2`) não ganha navegador
   * nenhum e o painel fica exatamente como era.
   */
  const irmaos = grupo?.irmaos ?? []

  if (!ponto) {
    return (
      <aside className={cn(CARTAO, 'justify-center', className)}>
        <span className={CABECALHO}>Estrela selecionada</span>
        <p className="text-[12.5px] leading-[1.5] text-soft">
          Clique numa estrela para ver a resolução por trás dela: complexidade, autonomia e código.
        </p>
      </aside>
    )
  }

  const escuro = theme === 'dark'
  /*
   * Big-O de tempo/espaço é ESTIMADO por natureza (`TIPO_METRICA_META`) — não se deriva isso
   * do `NivelConfianca`. A confiança do motor (alta/média/baixa) é o eixo secundário e sai
   * como TEXTO abaixo, exatamente como a tela D já faz.
   */
  const estimadoTempo = CONFIANCA_BIG_O === 'ESTIMADO'
  const confiancaMotor = rotuloConfiancaMotor(ponto.confiancaTempo)

  // Posição da selecionada entre as irmãs (base 0). `-1` é impossível aqui (ela está na lista
  // por construção), mas o array vazio — quando `pontos` não foi passado — cai no mesmo lugar.
  const indice = irmaos.findIndex((p) => p.resolucaoId === ponto.resolucaoId)
  const podeNavegar = irmaos.length > 1 && indice >= 0 && typeof onSelecionar === 'function'

  /** Trocar de irmã = trocar a seleção da página. Nas pontas, não faz nada. */
  const irPara = (delta: -1 | 1) => {
    const alvo = irmaos[indice + delta]
    if (!alvo || !onSelecionar) return
    onSelecionar(alvo.resolucaoId)
  }

  return (
    <aside
      className={cn(CARTAO, className)}
      aria-live="polite"
      /*
       * ← → navegam o cluster com o foco em QUALQUER lugar do painel (setas, "Ver resolução").
       * O handler mora aqui — e só aqui: se as setas também escutassem, um ← disparado com o
       * foco na seta seria tratado duas vezes e pularia duas resoluções.
       */
      onKeyDown={(evento) => {
        if (!podeNavegar) return
        if (evento.key === 'ArrowLeft') {
          evento.preventDefault()
          irPara(-1)
        } else if (evento.key === 'ArrowRight') {
          evento.preventDefault()
          irPara(1)
        }
      }}
    >
      {/* ── Identificação ───────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-2">
        <span className={CABECALHO}>Estrela selecionada</span>
        <StatusChip status={ponto.visibilidade === 'PUBLICO' ? 'publico' : 'privado'} />
      </div>

      <div className="flex flex-col gap-1">
        <h3 className="text-[17px] font-semibold leading-[1.3] text-ink">
          {escuro && (
            <span aria-hidden className="mr-1.5 text-mid">
              ✦
            </span>
          )}
          {ponto.desafioTitulo}
        </h3>
        <span className="tabular font-mono text-[11px] text-soft">
          enviada {dataCompleta(ponto.submetidaEm)}
        </span>
      </div>

      {/* ── Navegador do grupo (só quando o alvo do gráfico é compartilhado) ─── */}
      {grupo && irmaos.length > 1 && indice >= 0 ? (
        <NavegadorDoGrupo
          grupo={grupo}
          indice={indice}
          onIr={podeNavegar ? irPara : undefined}
        />
      ) : null}

      {/* ── Métricas ────────────────────────────────────────────────────────── */}
      <dl className="flex flex-col gap-[9px] border-t border-line pt-3">
        <LinhaMetrica rotulo="tempo">
          <ValorClasse
            k={ponto.k}
            texto={rotuloCanonico(ponto.k)}
            estimado={estimadoTempo}
            tema={theme}
          />
        </LinhaMetrica>

        <LinhaMetrica rotulo="espaço">
          {/*
           * TRÊS estados, não dois (types/api.ts): `-1` = o motor rodou e NÃO classificou (`?`)
           * — é resultado. `null` = não há dado (`—`) — é ausência. Colapsar os dois apagaria a
           * diferença entre "tentei e não consegui" e "não tenho analisador".
           */}
          {ponto.kEspaco !== null ? (
            /* O espaço sai da mesma análise estática do tempo: é ESTIMADO por natureza. */
            <ValorClasse
              k={ponto.kEspaco}
              texto={rotuloCanonico(ponto.kEspaco)}
              estimado={TIPO_METRICA_META.COMPLEXIDADE_ESPACO.confianca === 'ESTIMADO'}
              tema={theme}
            />
          ) : ponto.espacoOrdem === ORDEM_DESCONHECIDA ? (
            <NaoClassificado />
          ) : (
            <SemMetrica />
          )}
        </LinhaMetrica>

        <LinhaMetrica rotulo="ciclomática">
          {ponto.ciclomatica === null ? (
            <SemMetrica />
          ) : (
            /* Contagem exata (McCabe) — MEDIDA: sem `≈`, sem cor, sem quadrado de classe. */
            <span className="tabular font-mono text-[15px] font-semibold text-ink">
              M = {ponto.ciclomatica}
            </span>
          )}
        </LinhaMetrica>

        {/* Eixo secundário: quanto o motor confia na ESTIMATIVA que ele mesmo fez. */}
        {confiancaMotor && (
          <span className="font-mono text-[10.5px] text-soft">{confiancaMotor}</span>
        )}
      </dl>

      {/* ── Autonomia (neutra) + ação ───────────────────────────────────────── */}
      <div className="flex flex-col gap-2 border-t border-line pt-3">
        <span className={CABECALHO}>Autonomia IA</span>
        <AutonomyMeter value={ponto.autonomia} size="md" />
      </div>

      <Link
        to={hrefResolucao(ponto)}
        className="ci-foco-botao -mx-1 inline-flex items-center gap-1 self-start rounded-ci px-1 py-0.5 font-mono text-[12px] text-steel transition-colors hover:text-steel-hover"
      >
        Ver resolução
        <ChevronRight size={14} strokeWidth={2} aria-hidden />
      </Link>
    </aside>
  )
}

// ════════════════════════════════════════════════════════════════════════════
// NAVEGADOR DO GRUPO — "‹ 2 de 3 ›"
// ════════════════════════════════════════════════════════════════════════════

/**
 * As setas só existem quando a resolução DIVIDE o alvo do gráfico com outras. Aparecer do nada,
 * sem explicação, seria pior do que não existir: por isso a faixa DIZ o motivo antes de oferecer
 * a navegação — "3 resoluções com autonomia 2 e ≈ O(n²)" na Carta/Matriz, "2 resoluções em
 * mai/2026" na Linha. É a leitura literal do que o gráfico juntou, e é a razão pela qual ele
 * desenhou UM alvo onde há três resoluções. O texto vem de `grupoDeIrmaos` (`./irmaos`), que é
 * quem sabe qual gráfico está na tela.
 *
 * ── PONTAS: parar, não circular ─────────────────────────────────────────────────────────────
 * A lista de irmãs é CRONOLÓGICA (a mais antiga primeiro) — e tempo não dá a volta. Circular
 * levaria a última de volta à primeira sugerindo um ciclo que não existe no dado, e apagaria a
 * informação "você está na mais recente". Parar nas pontas é também o que o `Pagination` do
 * sistema já faz ("Página 1 de N", setas inertes nos extremos): mesma gramática, nada novo a
 * aprender.
 *
 * ── POR QUE `aria-disabled` E NÃO `disabled` ────────────────────────────────────────────────
 * Um `<button disabled>` sai da ordem de foco. Ao chegar na ponta pelo teclado, o navegador
 * jogaria o foco no `<body>` — e as setas ← → parariam de funcionar no meio da navegação, que
 * é exatamente quando o usuário está usando o teclado. Com `aria-disabled` a seta continua
 * focável, é anunciada como indisponível, e o clique/tecla simplesmente não faz nada
 * (o `onIr` já valida os limites). Visualmente é idêntica à seta desabilitada do sistema.
 */
function NavegadorDoGrupo({
  grupo,
  indice,
  onIr,
}: {
  grupo: GrupoDeIrmaos
  indice: number
  onIr?: (delta: -1 | 1) => void
}) {
  const total = grupo.irmaos.length
  const noComeco = indice <= 0
  const noFim = indice >= total - 1

  return (
    <div className="flex flex-col gap-2 rounded-ci border border-line bg-panel px-2.5 py-2">
      <span className={CABECALHO}>{grupo.titulo}</span>

      {/*
       * O PORQUÊ, em uma linha. Neutro de propósito: a classe já aparece em cor na tinta da sua
       * própria linha de métrica, logo abaixo — repetir o colormap aqui só ruído acrescentaria.
       */}
      <p className="tabular font-mono text-[10.5px] leading-[1.45] text-soft">{grupo.motivo}</p>

      <div
        role="group"
        aria-label={`Resoluções agrupadas: ${grupo.motivo}`}
        className="flex items-center gap-1.5"
      >
        <SetaIrma
          direcao="anterior"
          inerte={noComeco || !onIr}
          onClick={() => onIr?.(-1)}
        />
        <span className="tabular min-w-[52px] text-center font-mono text-[11px] text-mid">
          <span className="font-semibold text-ink">{indice + 1}</span> de {total}
        </span>
        <SetaIrma direcao="proxima" inerte={noFim || !onIr} onClick={() => onIr?.(1)} />
        {/* Ordem cronológica: a 1ª é a mais antiga. Sem isto, "2 de 3" não diz de que fila. */}
        <span className="ml-auto font-mono text-[9.5px] text-soft">mais antiga → recente</span>
      </div>
    </div>
  )
}

/** Alvo de verdade: `<button>`, foco visível (`ci-foco-botao`) e rótulo que diz o que faz. */
function SetaIrma({
  direcao,
  inerte,
  onClick,
}: {
  direcao: 'anterior' | 'proxima'
  inerte: boolean
  onClick: () => void
}) {
  const anterior = direcao === 'anterior'
  const Icone = anterior ? ChevronLeft : ChevronRight

  return (
    <button
      type="button"
      onClick={inerte ? undefined : onClick}
      aria-disabled={inerte}
      aria-label={
        anterior
          ? 'Resolução anterior deste agrupamento'
          : 'Próxima resolução deste agrupamento'
      }
      className={cn(
        'ci-foco-botao flex h-[22px] w-[22px] items-center justify-center rounded-ci border transition-colors',
        inerte
          ? 'cursor-not-allowed border-line text-mid opacity-40'
          : 'cursor-pointer border-line-strong text-ink hover:bg-elevated',
      )}
    >
      <Icone size={14} strokeWidth={2} aria-hidden />
    </button>
  )
}

// ── Peças ─────────────────────────────────────────────────────────────────────

function LinhaMetrica({ rotulo, children }: { rotulo: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="font-mono text-[11px] text-mid">{rotulo}</dt>
      <dd className="flex items-center gap-2">{children}</dd>
    </div>
  )
}

/** `—` em `soft`: a ausência de métrica é dita, nunca disfarçada de zero. */
function SemMetrica() {
  return (
    <span className="font-mono text-[15px] font-semibold text-soft" title="Sem dado de métrica">
      —
    </span>
  )
}

/** `?` em `soft`: o motor RODOU e não classificou. É um resultado, não uma ausência. */
function NaoClassificado() {
  return (
    <span
      className="font-mono text-[15px] font-semibold text-soft"
      title="O motor analisou o código e não conseguiu classificar esta complexidade"
    >
      {ROTULO_DESCONHECIDO}
    </span>
  )
}

/**
 * Valor de classe do colormap: quadrado 8×8 + rótulo na tinta da classe.
 * CHEIO = MEDIDO · VAZADO + `≈` = ESTIMADO (regra 3, a mesma gramática do `ConfidenceChip`).
 */
function ValorClasse({
  k,
  texto,
  estimado,
  tema,
}: {
  k: number
  texto: string
  estimado: boolean
  tema: 'dark' | 'light'
}) {
  const cor = corDaClasse(k, tema)

  return (
    <span
      className="flex items-center gap-2 font-mono text-[15px] font-semibold"
      style={{ color: tintaDaClasse(k, tema) }}
      title={estimado ? 'Estimado por análise estática' : 'Medido no AST'}
    >
      <span
        aria-hidden
        style={{
          width: 8,
          height: 8,
          flex: '0 0 auto',
          background: estimado ? 'transparent' : cor,
          border: estimado ? `1.5px solid ${cor}` : undefined,
        }}
      />
      {estimado ? `${PREFIXO_ESTIMADO}${texto}` : texto}
    </span>
  )
}
