/*
 * Tela K · Desafio público (modo leitura) — spec 03 §K.
 *
 * O visitante vê o enunciado, quem escreveu e a lista de resoluções. Nenhuma ação de dono
 * (sem editar, sem nova resolução, sem alterar visibilidade) — só leitura e dois convites.
 *
 * Regras que esta tela carrega:
 *  - 401 é um estado de produto, não um erro genérico: o desafio existe e pede conta ("Entre
 *    para ver este desafio"). 400/404 mostram a mensagem do backend + volta ao portfólio.
 *  - A métrica de cada resolução vive dentro da `ResolucaoLinha` (que já sabe distinguir
 *    MEDIDO × ≈ ESTIMADO, `calculando` e `sem métrica` — métrica só existe para Java).
 *  - Autonomia é neutra; a única cor da tela é o colormap dos chips Big-O.
 */
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Code2, ExternalLink, Eye } from 'lucide-react'
import { Card } from '@/components/ui/card'
import { Chip } from '@/components/ui/badge'
import { buttonClasses } from '@/components/ui/button'
import { EmptyState } from '@/components/ui/empty-state'
import { Pagination } from '@/components/ui/pagination'
import { QueryBoundary, LoadingSection, ErrorState } from '@/components/page/states'
import { Avatar } from '@/components/Avatar'
import { StatusChip } from '@/components/domain/badges'
import { ResolucaoLinha } from '@/components/domain/ResolucaoLinha'
import { useDesafioDetalhe } from '@/features/desafios/hooks'
import { useResolucoesDoDesafio } from '@/features/resolucoes/hooks'
import { formatDate, pluralPt } from '@/lib/utils'
import { apiErrorMessage, apiErrorStatus } from '@/lib/api'

/** Envelope da tela: o corpo público (spec: `padding: 26px 48px 40px; gap: 22px`). */
function Corpo({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-[22px] px-5 pt-[26px] pb-10 sm:px-8 lg:px-12">
      {children}
    </div>
  )
}

/** Nota permanente do visitante: recuo + hairline, tinta terciária, sem alarme. */
function NotaModoLeitura() {
  return (
    <div className="flex items-start gap-[9px] rounded-ci border border-line bg-recess px-3.5 py-[13px]">
      <Eye size={15} strokeWidth={2} aria-hidden className="mt-px shrink-0 text-steel" />
      <p className="text-[12px] leading-[1.5] text-soft">
        Você está vendo este portfólio em modo leitura.{' '}
        <Link to="/criar-conta" className="font-semibold text-steel hover:text-steel-hover">
          Crie uma conta
        </Link>{' '}
        para montar o seu.
      </p>
    </div>
  )
}

export function DesafioPublicoPage() {
  const { usuarioId, desafioId } = useParams<{ usuarioId: string; desafioId: string }>()
  const desafioQuery = useDesafioDetalhe(desafioId)
  const [pagina, setPagina] = useState(0)
  const resolucoesQuery = useResolucoesDoDesafio(desafioId, pagina)

  const portfolioTo = `/u/${usuarioId ?? ''}`

  if (desafioQuery.isPending) {
    return (
      <Corpo>
        <LoadingSection />
      </Corpo>
    )
  }

  // Lacuna da API: abrir o desafio ainda exige autenticação (401). Diferenciamos o motivo:
  // 401 → convida a entrar; 400/404 (não encontrado / sem acesso) → mostra a mensagem do backend.
  if (desafioQuery.isError || !desafioQuery.data) {
    const precisaLogin = apiErrorStatus(desafioQuery.error) === 401

    return (
      <div className="mx-auto flex w-full max-w-[440px] flex-col gap-4 px-5 py-16">
        {precisaLogin ? (
          <>
            <ErrorState
              title="Entre para ver este desafio"
              message="Este desafio só é visível para quem tem conta. Entre com a sua ou crie uma — o portfólio é grátis."
            />
            <div className="flex items-center gap-2.5">
              <Link to="/entrar" className={buttonClasses({ fullWidth: true })}>
                Entrar
              </Link>
              <Link
                to="/criar-conta"
                className={buttonClasses({ variant: 'secondary', fullWidth: true })}
              >
                Criar conta
              </Link>
            </div>
          </>
        ) : (
          <>
            <ErrorState
              message={apiErrorMessage(desafioQuery.error, 'Não foi possível carregar este desafio.')}
              onRetry={() => void desafioQuery.refetch()}
            />
            <Link
              to={portfolioTo}
              className={buttonClasses({ variant: 'secondary', fullWidth: true })}
            >
              Voltar ao portfólio
            </Link>
          </>
        )}
      </div>
    )
  }

  const desafio = desafioQuery.data
  const rotuloLinkExterno = desafio.plataformaOrigem
    ? `Abrir no ${desafio.plataformaOrigem}`
    : 'Abrir link'

  return (
    <Corpo>
      {/* a) Voltar ao portfólio do autor — mono, tinta de link */}
      <Link
        to={portfolioTo}
        className="ci-foco-botao inline-flex w-fit items-center gap-2 rounded-ci font-mono text-[12.5px] text-steel transition-colors hover:text-steel-hover"
      >
        <ArrowLeft size={15} strokeWidth={2} aria-hidden />
        Portfólio de @{desafio.autorUsername}
      </Link>

      {/* b) Cabeçalho do desafio */}
      <div className="flex flex-wrap items-start justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-[11px]">
          <h1 className="text-[26px] leading-tight font-bold tracking-[-.02em] text-ink sm:text-[28px]">
            {desafio.titulo}
          </h1>
          <div className="flex flex-wrap items-center gap-[9px]">
            {desafio.plataformaOrigem && <Chip>{desafio.plataformaOrigem}</Chip>}
            {desafio.identificadorExterno && (
              <Chip className="tabular">#{desafio.identificadorExterno}</Chip>
            )}
            <StatusChip status={desafio.visibilidade === 'PUBLICO' ? 'publico' : 'privado'} />
            <time
              dateTime={desafio.criadoEm}
              className="tabular font-mono text-[11.5px] text-soft"
            >
              criado em {formatDate(desafio.criadoEm)}
            </time>
          </div>
        </div>

        {desafio.urlExterna && (
          <a
            href={desafio.urlExterna}
            target="_blank"
            rel="noreferrer"
            className={buttonClasses({ variant: 'secondary' })}
          >
            {rotuloLinkExterno}
            <ExternalLink size={15} strokeWidth={2} aria-hidden className="text-mid" />
          </a>
        )}
      </div>

      {/* c) Enunciado + coluna lateral (autor / modo leitura) */}
      <div className="grid grid-cols-1 items-start gap-5 lg:grid-cols-[1.5fr_1fr]">
        <Card className="flex flex-col gap-3.5 p-5">
          <h2 className="text-[14px] font-semibold text-ink">Enunciado</h2>
          {desafio.enunciado ? (
            <p className="text-[14px] leading-[1.65] break-words whitespace-pre-wrap text-body">
              {desafio.enunciado}
            </p>
          ) : (
            <p className="text-[13px] text-soft">Sem enunciado.</p>
          )}
        </Card>

        <div className="flex flex-col gap-4">
          <Card className="flex flex-col gap-3.5 p-[18px]">
            <span className="font-mono text-[11px] tracking-[.08em] text-mid uppercase">
              Sobre o autor
            </span>

            <div className="flex items-center gap-3">
              <Avatar name={desafio.autorUsername} size={44} className="font-bold" />
              <div className="flex min-w-0 flex-col gap-0.5">
                <span className="truncate text-[14px] font-semibold text-ink">
                  @{desafio.autorUsername}
                </span>
                <span className="tabular font-mono text-[11.5px] text-soft">
                  {pluralPt(desafio.qtdResolucoes, 'resolução', 'resoluções')} neste desafio
                </span>
              </div>
            </div>

            <Link
              to={portfolioTo}
              className={buttonClasses({ variant: 'secondary', size: 'sm', fullWidth: true })}
            >
              Ver portfólio
            </Link>
          </Card>

          <NotaModoLeitura />
        </div>
      </div>

      {/* d) Resoluções. Rótulo neutro de propósito: o endpoint não filtra por visibilidade da
          resolução — chamar a lista de "públicas" mentiria para o autor logado no próprio desafio. */}
      <section className="flex flex-col gap-3.5">
        <div className="flex items-center gap-2.5">
          <h2 className="text-[16px] font-semibold text-ink">Resoluções</h2>
          <Chip className="tabular">{desafio.qtdResolucoes}</Chip>
        </div>

        <QueryBoundary query={resolucoesQuery}>
          {(dados) =>
            dados.itens.length === 0 ? (
              <EmptyState
                icon={Code2}
                title="Nenhuma resolução ainda."
                description="Este desafio ainda não tem nenhuma resolução visível."
              />
            ) : (
              <div className="flex flex-col gap-3">
                <ul className="flex flex-col gap-2.5">
                  {/*
                   * `tempoOrdem` vem do próprio `ResolucaoResumoDTO` — o visitante vê na lista o
                   * mesmo Big-O que veria ao abrir a resolução. `0..7` → chip ≈ ESTIMADO ·
                   * `-1` → `?` (o motor não classificou) · `null` → sem métrica (não-Java).
                   * ⚠ `0` é O(1), não "vazio".
                   */}
                  {dados.itens.map((r) => (
                    <li key={r.id}>
                      <ResolucaoLinha
                        variant="cartao"
                        dataFormato="longa"
                        to={`/u/${usuarioId ?? ''}/desafios/${desafioId ?? ''}/resolucoes/${r.id}`}
                        linguagem={r.linguagem}
                        autonomia={r.indiceAutonomiaIA}
                        analisada={r.analisada}
                        tempoOrdem={r.tempoOrdem}
                        confiancaTempo={r.confiancaTempo}
                        submetidaEm={r.submetidaEm}
                      />
                    </li>
                  ))}
                </ul>

                <Pagination page={pagina} totalPages={dados.totalPaginas} onChange={setPagina} />
              </div>
            )
          }
        </QueryBoundary>
      </section>
    </Corpo>
  )
}
