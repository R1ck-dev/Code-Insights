import { useEffect, useMemo, useState } from 'react'
import { Database, Download, Filter, IdCard, Search } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { EmptyState } from '@/components/ui/empty-state'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Spinner } from '@/components/ui/spinner'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeaderCell,
  TableRow,
} from '@/components/ui/table'
import { toast } from '@/components/ui/toaster'
import { BigOChip, LangChip } from '@/components/domain/badges'
import { CONFIANCA_BIG_O, LINGUAGENS, rotuloConfiancaMotor } from '@/domain/enums'
import { useCoorte, useExportarCsv, useIdentificarParticipante } from '@/features/pesquisa/hooks'
import { apiErrorMessage } from '@/lib/api'
import { formatDate, pluralPt } from '@/lib/utils'
import type {
  LinguagemProgramacao,
  ParticipanteIdentificadoDTO,
  ResolucaoDaCoorteDTO,
} from '@/types/api'

/*
 * Q · Coorte (área de pesquisa) — ÓRBITA.
 *
 * O dado cru de toda a plataforma, uma linha por resolução, pseudonimizado. É a tela de COMPARAR:
 * por isso usa <table>, o único vocabulário novo do sistema — grade de cards não alinha coluna, e
 * sem alinhamento não há comparação.
 *
 * O que ela deliberadamente NÃO faz:
 *  - não mostra o nome de ninguém por padrão. O pseudônimo é o padrão; revelar é um clique
 *    explícito, para que ler o dado sabendo de quem é seja uma escolha e não o estado natural;
 *  - não exibe o código-fonte, e o CSV também não o leva — comentário e nome de variável
 *    identificam o autor, e o CSV é o artefato que circula;
 *  - filtros e ordenação são LOCAIS: `GET /api/pesquisa/coorte` devolve a amostra inteira de
 *    propósito, porque cobertura calculada sobre uma página não é cobertura.
 */

const TODAS = 'TODAS'
type FiltroLinguagem = typeof TODAS | LinguagemProgramacao

export function CoortePage() {
  const query = useCoorte()
  const exportar = useExportarCsv()

  const [linguagem, setLinguagem] = useState<FiltroLinguagem>(TODAS)
  const [termo, setTermo] = useState('')
  const [filtro, setFiltro] = useState('')
  const [pseudonimoAberto, setPseudonimoAberto] = useState<string | null>(null)

  // Debounce de 300 ms: filtrar a cada tecla numa lista inteira em memória pisca a tabela.
  useEffect(() => {
    const id = setTimeout(() => setFiltro(termo.trim().toLowerCase()), 300)
    return () => clearTimeout(id)
  }, [termo])

  const linhas = useMemo(() => query.data ?? [], [query.data])
  const visiveis = useMemo(
    () =>
      linhas.filter((linha) => {
        if (linguagem !== TODAS && linha.linguagem !== linguagem) return false
        if (!filtro) return true
        return (
          linha.pseudonimo.toLowerCase().includes(filtro) ||
          linha.desafioTitulo.toLowerCase().includes(filtro)
        )
      }),
    [linhas, linguagem, filtro],
  )

  const temFiltro = linguagem !== TODAS || filtro !== ''

  async function baixarCsv() {
    try {
      await exportar.mutateAsync()
      toast.success('CSV gerado. Confira a pasta de downloads.')
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível gerar o CSV.'))
    }
  }

  const botaoExportar = (
    <Button icon={Download} onClick={baixarCsv} loading={exportar.isPending}>
      Baixar CSV
    </Button>
  )

  return (
    <PageContainer className="gap-[18px]">
      <PageHeader
        title="Coorte"
        subtitle={legenda(linhas.length, visiveis.length, temFiltro)}
        actions={botaoExportar}
      />

      <QueryBoundary query={query}>
        {(dados) =>
          dados.length === 0 ? (
            <EmptyState
              icon={Database}
              title="Nenhuma resolução na plataforma ainda."
              description="A tabela aparece assim que os participantes começarem a submeter."
            />
          ) : (
            <div className="flex flex-col gap-[18px]">
              <div className="flex flex-wrap items-center gap-[9px]">
                <Input
                  id="busca"
                  icon={Search}
                  placeholder="Pseudônimo ou desafio"
                  aria-label="Filtrar por pseudônimo ou desafio"
                  value={termo}
                  onChange={(e) => setTermo(e.target.value)}
                  className="w-[240px]"
                />
                <Select
                  value={linguagem}
                  onValueChange={(v) => setLinguagem(v as FiltroLinguagem)}
                >
                  <SelectTrigger aria-label="Filtrar por linguagem" ativo={linguagem !== TODAS}>
                    <SelectValue placeholder="Linguagem" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={TODAS}>Todas as linguagens</SelectItem>
                    {LINGUAGENS.map((lang) => (
                      <SelectItem key={lang.value} value={lang.value}>
                        {lang.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {temFiltro && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setLinguagem(TODAS)
                      setTermo('')
                    }}
                  >
                    Limpar filtros
                  </Button>
                )}
              </div>

              {visiveis.length === 0 ? (
                <EmptyState
                  icon={Filter}
                  title="Nenhuma resolução com esses filtros."
                  description="Ajuste a busca ou a linguagem."
                />
              ) : (
                <TabelaDaCoorte linhas={visiveis} onIdentificar={setPseudonimoAberto} />
              )}

              <p className="text-[11.5px] leading-[1.5] text-soft">
                O CSV traz exatamente estas linhas, sem o código-fonte. Separador vírgula, UTF-8 com
                BOM — em pandas use <span className="font-mono">encoding='utf-8-sig'</span>.
              </p>
            </div>
          )
        }
      </QueryBoundary>

      <DialogDeIdentificacao
        pseudonimo={pseudonimoAberto}
        onFechar={() => setPseudonimoAberto(null)}
      />
    </PageContainer>
  )
}

/* ---------------------------------------------------------------- tabela --- */

function TabelaDaCoorte({
  linhas,
  onIdentificar,
}: {
  linhas: ResolucaoDaCoorteDTO[]
  onIdentificar: (pseudonimo: string) => void
}) {
  return (
    <Table aria-label="Resoluções da coorte">
      <TableHead>
        <TableRow className="hover:bg-panel">
          <TableHeaderCell>Pseudônimo</TableHeaderCell>
          <TableHeaderCell>Desafio</TableHeaderCell>
          <TableHeaderCell>Linguagem</TableHeaderCell>
          <TableHeaderCell numerico>Autonomia</TableHeaderCell>
          <TableHeaderCell>Tempo</TableHeaderCell>
          <TableHeaderCell>Espaço</TableHeaderCell>
          <TableHeaderCell numerico>Ciclomática</TableHeaderCell>
          <TableHeaderCell>Submetida</TableHeaderCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {linhas.map((linha) => (
          <TableRow key={linha.resolucaoId}>
            <TableCell>
              <button
                type="button"
                onClick={() => onIdentificar(linha.pseudonimo)}
                title="Revelar quem é este participante"
                className="ci-foco-botao cursor-pointer rounded-ci font-mono text-[12px] font-semibold text-ink underline decoration-line-strong decoration-dotted underline-offset-[3px] hover:decoration-ink"
              >
                {linha.pseudonimo}
              </button>
            </TableCell>
            <TableCell className="max-w-[240px] truncate" title={linha.desafioTitulo}>
              {linha.desafioTitulo}
            </TableCell>
            <TableCell>
              <LangChip lang={linha.linguagem} />
            </TableCell>
            <TableCell numerico>{linha.indiceAutonomiaIA}/5</TableCell>
            <TableCell>
              <CelulaDeClasse
                ordem={linha.tempoOrdem}
                confiancaMotor={rotuloConfiancaMotor(linha.confiancaTempo)}
              />
            </TableCell>
            <TableCell>
              <CelulaDeClasse
                ordem={linha.espacoOrdem}
                confiancaMotor={rotuloConfiancaMotor(linha.confiancaEspaco)}
              />
            </TableCell>
            <TableCell numerico>{linha.ciclomatica ?? '—'}</TableCell>
            <TableCell>
              <time dateTime={linha.submetidaEm} className="tabular font-mono text-[12px] text-mid">
                {formatDate(linha.submetidaEm)}
              </time>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

/**
 * Sem métrica é `—` neutro, não `?`. Os dois estados são diferentes e a distinção é do dado:
 * `null` = a análise não produziu nada · `-1` = o motor rodou e recusou classificar (`?`, que o
 * próprio BigOChip desenha).
 */
function CelulaDeClasse({
  ordem,
  confiancaMotor,
}: {
  ordem: number | null
  confiancaMotor: string | null
}) {
  if (ordem === null) return <span className="font-mono text-[12px] text-soft">—</span>
  return <BigOChip k={ordem} confianca={CONFIANCA_BIG_O} title={confiancaMotor ?? undefined} />
}

/* --------------------------------------------------------- identificação --- */

/**
 * A revelação é uma chamada de rede própria, e não um campo escondido no payload da listagem: o
 * nome só sai do servidor quando alguém pede. Sem isso, "pseudonimizado" seria só uma escolha de
 * renderização, com a identidade viajando junto o tempo todo.
 */
function DialogDeIdentificacao({
  pseudonimo,
  onFechar,
}: {
  pseudonimo: string | null
  onFechar: () => void
}) {
  const identificar = useIdentificarParticipante()
  const [participante, setParticipante] = useState<ParticipanteIdentificadoDTO | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  /*
   * O `atual` descarta resposta obsoleta. Sem ele: abre A, fecha, abre B — B responde primeiro, A
   * responde depois e sobrescreve. O título continuaria dizendo "Participante B" enquanto os campos
   * mostrariam o nome e o e-mail de A. Numa tela cujo único propósito é ligar pseudônimo a pessoa,
   * isso é a identidade errada atribuída ao dado errado, e nada mais corrige depois.
   *
   * A limpeza vem ANTES do early return de propósito: fechar o diálogo também precisa zerar o
   * estado, senão a resposta em voo cai num componente que continua montado (ele é irmão do
   * QueryBoundary e nunca desmonta) e vaza para a próxima abertura.
   */
  useEffect(() => {
    setParticipante(null)
    setErro(null)
    if (!pseudonimo) return

    let atual = true
    identificar
      .mutateAsync(pseudonimo)
      .then((dados) => {
        if (atual) setParticipante(dados)
      })
      .catch((err) => {
        if (atual) setErro(apiErrorMessage(err, 'Não foi possível identificar o participante.'))
      })
    return () => {
      atual = false
    }
    // Depende SÓ do pseudônimo: `identificar` é recriado a cada render e, se entrasse aqui,
    // dispararia a chamada em laço.
  }, [pseudonimo])

  return (
    <Dialog open={pseudonimo !== null} onOpenChange={(aberto) => !aberto && onFechar()}>
      <DialogContent width={420}>
        <DialogHeader icon={IdCard}>
          <DialogTitle>Participante {pseudonimo}</DialogTitle>
        </DialogHeader>
        <DialogBody>
          {erro && <p className="text-[12.5px] text-erro-texto">{erro}</p>}
          {!erro && !participante && (
            <span className="flex items-center gap-[9px] text-[12.5px] text-mid">
              <Spinner size={13} /> Consultando…
            </span>
          )}
          {participante && (
            <dl className="flex flex-col gap-[11px]">
              <Campo rotulo="Usuário" valor={participante.username} />
              <Campo rotulo="E-mail" valor={participante.email} />
            </dl>
          )}
        </DialogBody>
      </DialogContent>
    </Dialog>
  )
}

function Campo({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="flex flex-col gap-[3px]">
      <dt className="font-mono text-[10.5px] tracking-[.06em] text-mid uppercase">{rotulo}</dt>
      <dd className="font-mono text-[13px] text-ink">{valor}</dd>
    </div>
  )
}

function legenda(total: number, visiveis: number, temFiltro: boolean): string {
  if (temFiltro && visiveis !== total) {
    return `${visiveis} de ${pluralPt(total, 'resolução', 'resoluções')}`
  }
  return pluralPt(total, 'resolução', 'resoluções')
}
