import { useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Cpu, FileCode2, Info, Lock } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { Breadcrumb } from '@/components/page/Breadcrumb'
import { QueryBoundary } from '@/components/page/states'
import { CodeEditor } from '@/components/CodeEditor'
import { AutonomyInput, type NivelAutonomia } from '@/components/AutonomyInput'
import { SnippetsDoDesafioSection } from '@/components/snippets/SnippetsDoDesafioSection'
import { LanguageDot } from '@/components/domain/badges'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { FormField } from '@/components/ui/form-field'
import { EmptyState } from '@/components/ui/empty-state'
import { toast } from '@/components/ui/toaster'
import { useDesafioDetalhe } from '@/features/desafios/hooks'
import { useSubmeterResolucao } from '@/features/resolucoes/hooks'
import {
  LINGUAGEM_COM_METRICAS,
  LINGUAGEM_META,
  LINGUAGENS,
  ROTULO_SEM_METRICA,
} from '@/domain/enums'
import type { LinguagemProgramacao } from '@/types/api'
import { apiErrorMessage } from '@/lib/api'
import { cn } from '@/lib/utils'

/**
 * Tela N · Submeter resolução.
 *
 * Regra 4: o Índice de Autonomia é AUTODECLARADO, neutro (osso/tinta) e sem julgamento —
 * 1 = muito apoio de IA · 5 = autônomo (maior é mais autônomo, nunca inverter).
 * Regra 7 (§4.4 do contrato): métricas de complexidade hoje só existem para Java —
 * a nota sob o seletor é PERMANENTE e a linguagem sem métrica é dita em voz alta.
 */
export function SubmeterResolucaoPage() {
  const { desafioId } = useParams<{ desafioId: string }>()
  const navigate = useNavigate()
  const desafioQuery = useDesafioDetalhe(desafioId)
  const submeter = useSubmeterResolucao(desafioId ?? '')

  const [codigo, setCodigo] = useState('')
  const [linguagem, setLinguagem] = useState<LinguagemProgramacao>('JAVA')
  const [autonomia, setAutonomia] = useState<NivelAutonomia | null>(null)
  const [apoio, setApoio] = useState('')
  const [tentouEnviar, setTentouEnviar] = useState(false)

  const enviando = submeter.isPending
  const meta = LINGUAGEM_META[linguagem]
  const semMetricas = linguagem !== LINGUAGEM_COM_METRICAS

  // Roving tabindex do radiogroup de linguagem (WAI-ARIA): um tab stop, setas movem.
  const refsLinguagem = useRef<(HTMLButtonElement | null)[]>([])
  const indiceLinguagem = LINGUAGENS.findIndex((l) => l.value === linguagem)

  function moverLinguagem(de: number, passo: number) {
    const alvo = (de + passo + LINGUAGENS.length) % LINGUAGENS.length
    setLinguagem(LINGUAGENS[alvo].value)
    refsLinguagem.current[alvo]?.focus()
  }

  function aoTeclarLinguagem(e: React.KeyboardEvent<HTMLButtonElement>, i: number) {
    switch (e.key) {
      case 'ArrowRight':
      case 'ArrowDown':
        e.preventDefault()
        moverLinguagem(i, 1)
        break
      case 'ArrowLeft':
      case 'ArrowUp':
        e.preventDefault()
        moverLinguagem(i, -1)
        break
      case 'Home':
        e.preventDefault()
        moverLinguagem(0, 0)
        break
      case 'End':
        e.preventDefault()
        moverLinguagem(LINGUAGENS.length - 1, 0)
        break
      default:
        break
    }
  }

  // Erros inline (spec §6.3) — a validação continua acontecendo no submit.
  const erroCodigo =
    tentouEnviar && !codigo.trim() ? 'Cole o código-fonte da sua resolução antes de enviar.' : null
  const erroAutonomia =
    tentouEnviar && autonomia == null ? 'Selecione o Índice de Autonomia IA (1 a 5).' : null

  async function handleSubmit() {
    setTentouEnviar(true)
    if (!codigo.trim()) {
      toast.error('Cole o código-fonte da sua resolução antes de enviar.')
      return
    }
    if (autonomia == null) {
      toast.error('Selecione o Índice de Autonomia IA (1 a 5).')
      return
    }
    try {
      const res = await submeter.mutateAsync({
        codigoFonte: codigo,
        linguagem,
        indiceAutonomiaIA: autonomia,
        descricaoApoioIA: apoio.trim() || null,
      })
      toast.success('Resolução enviada! Calculando métricas…')
      navigate('/app/resolucoes/' + res.id)
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Não foi possível enviar a resolução.'))
    }
  }

  if (!desafioId) {
    return (
      <PageContainer>
        <EmptyState
          icon={FileCode2}
          title="Desafio não informado"
          description="Abra um desafio e use o botão de nova resolução para submeter."
          action={<Button onClick={() => navigate('/app/desafios')}>Ver desafios</Button>}
        />
      </PageContainer>
    )
  }

  return (
    <PageContainer>
      <QueryBoundary query={desafioQuery}>
        {(desafio) => (
          <>
            <Breadcrumb
              items={[
                { label: 'Desafios', to: '/app/desafios' },
                { label: desafio.titulo, to: `/app/desafios/${desafio.id}` },
                { label: 'Nova resolução' },
              ]}
            />

            <PageHeader
              title="Submeter resolução"
              subtitle={
                <>
                  para <span className="font-semibold text-ink">{desafio.titulo}</span> · nasce
                  privada e passa por análise automática.
                </>
              }
            />

            <div className="grid grid-cols-1 items-start gap-[18px] lg:grid-cols-[1.62fr_1fr]">
              {/* Coluna esquerda — editor */}
              <fieldset
                disabled={enviando}
                className="min-w-0 transition-opacity disabled:opacity-60"
              >
                <FormField
                  label="Código-fonte"
                  required
                  htmlFor="codigo"
                  error={erroCodigo}
                  className="gap-[8px]"
                >
                  {/* Anel de erro por fora: a moldura do CodeEditor é pintada por style inline. */}
                  <div className={cn('rounded-ci', erroCodigo && 'ring-1 ring-erro-estrutura')}>
                    <CodeEditor
                      id="codigo"
                      value={codigo}
                      onChange={setCodigo}
                      lang={meta.codeLang}
                      label={`Editor · ${meta.label}`}
                      placeholder="Cole aqui o código da sua resolução…"
                      minHeight={340}
                      // Sem isto o leitor de tela nunca ouve "Cole o código-fonte…": o
                      // `FormField` gera `#codigo-erro`, mas ninguém apontava para ele.
                      aria-invalid={!!erroCodigo}
                      aria-describedby={erroCodigo ? 'codigo-erro' : undefined}
                    />
                  </div>
                </FormField>
              </fieldset>

              {/* Coluna direita — metadados da submissão */}
              <div className="flex min-w-0 flex-col gap-[18px]">
                <fieldset
                  disabled={enviando}
                  className="flex min-w-0 flex-col gap-[18px] transition-opacity disabled:opacity-60"
                >
                  {/* 1 · Linguagem — a nota do Java é permanente (regra 7) */}
                  <FormField
                    label="Linguagem"
                    required
                    className="gap-[9px]"
                    hint={
                      <span className="flex flex-col gap-[5px]">
                        <span className="flex items-start gap-[7px]">
                          <Cpu
                            size={13}
                            strokeWidth={2}
                            aria-hidden
                            className="mt-[1px] shrink-0 text-atencao"
                          />
                          <span>
                            Métricas de complexidade hoje só para{' '}
                            <span className="font-semibold text-ink">Java</span>.
                          </span>
                        </span>
                        {semMetricas && (
                          <span className="flex items-start gap-[7px]">
                            <Info
                              size={13}
                              strokeWidth={2}
                              aria-hidden
                              className="mt-[1px] shrink-0 text-soft"
                            />
                            <span>
                              Em {meta.label}, tempo, espaço e ciclomática ficam como{' '}
                              <span className="font-mono text-[10.5px] text-mid">
                                {ROTULO_SEM_METRICA}
                              </span>
                              . O Índice de Autonomia continua sendo registrado.
                            </span>
                          </span>
                        )}
                      </span>
                    }
                  >
                    {/* Radiogroup DE VERDADE: um só tab stop + setas (WAI-ARIA). Antes os 5
                        botões eram todos tabbable e as setas não faziam nada. */}
                    <div role="radiogroup" aria-label="Linguagem" className="grid grid-cols-2 gap-2">
                      {LINGUAGENS.map((l, i) => {
                        const sel = l.value === linguagem
                        return (
                          <button
                            key={l.value}
                            ref={(el) => {
                              refsLinguagem.current[i] = el
                            }}
                            type="button"
                            role="radio"
                            aria-checked={sel}
                            tabIndex={i === indiceLinguagem ? 0 : -1}
                            onClick={() => setLinguagem(l.value)}
                            onKeyDown={(e) => aoTeclarLinguagem(e, i)}
                            className={cn(
                              'ci-foco-botao inline-flex h-[38px] cursor-pointer items-center gap-2',
                              'rounded-ci border px-3 font-mono text-[12.5px] transition-colors',
                              'disabled:cursor-not-allowed',
                              sel
                                ? 'border-ink bg-ink font-semibold text-ink-on'
                                : 'border-line-strong bg-panel font-medium text-mid hover:text-ink',
                            )}
                          >
                            <LanguageDot lang={l.value} size={8} />
                            {l.label}
                          </button>
                        )
                      })}
                    </div>
                  </FormField>

                  {/* 2 · Índice de Autonomia IA — autodeclarado, neutro, sem julgamento */}
                  {/* `htmlFor` obrigatório: sem ele o `FormField` não gera `id` na mensagem, e o
                      erro/hint do grupo fica órfão (o leitor de tela nunca o associa). */}
                  <FormField
                    label="Índice de Autonomia IA"
                    htmlFor="autonomia"
                    required
                    className="gap-[9px]"
                    error={erroAutonomia}
                    hint="Autodeclarado e sem julgamento: registra como você trabalhou nesta tentativa, não a qualidade da solução."
                  >
                    <div className="flex flex-col gap-[6px]">
                      <AutonomyInput
                        id="autonomia"
                        value={autonomia}
                        onChange={setAutonomia}
                        aria-invalid={!!erroAutonomia}
                        aria-describedby={erroAutonomia ? 'autonomia-erro' : 'autonomia-hint'}
                      />
                      <div className="flex items-center justify-between gap-2 font-mono text-[10.5px] tracking-[.06em] text-soft uppercase">
                        <span>
                          <span className="tabular">1</span> · muito apoio de IA
                        </span>
                        <span>
                          <span className="tabular">5</span> · autônomo
                        </span>
                      </div>
                    </div>
                  </FormField>

                  {/* 3 · Como a IA ajudou (opcional) */}
                  <div className="flex flex-col gap-[7px]">
                    <Label htmlFor="apoio">
                      Como a IA ajudou <span className="text-soft">(opcional)</span>
                    </Label>
                    <Textarea
                      id="apoio"
                      value={apoio}
                      onChange={(e) => setApoio(e.target.value)}
                      minHeight={64}
                      placeholder="Usei IA só para lembrar a sintaxe de HashMap; a lógica foi minha."
                      hint="Fica no seu registro para dar contexto ao índice — não é avaliado."
                    />
                  </div>
                </fieldset>

                {/* Aviso permanente: privada + análise automática */}
                <div className="flex items-start gap-[9px] rounded-ci border border-line bg-recess px-3 py-[10px]">
                  <Lock
                    size={13}
                    strokeWidth={2}
                    aria-hidden
                    className="mt-[2px] shrink-0 text-mid"
                  />
                  <p className="text-[11.5px] leading-[1.5] text-soft">
                    A resolução nasce <span className="font-medium text-mid">privada</span> e entra
                    em <span className="font-medium text-mid">análise automática</span> assim que
                    você enviar. Você decide depois se quer torná-la pública.
                  </p>
                </div>

                {/* 4 · Ações */}
                <div className="flex justify-end gap-2.5">
                  <Button
                    variant="secondary"
                    size="lg"
                    disabled={enviando}
                    onClick={() => navigate(-1)}
                  >
                    Cancelar
                  </Button>
                  <Button size="lg" loading={enviando} onClick={handleSubmit}>
                    {enviando ? 'Enviando…' : 'Submeter'}
                  </Button>
                </div>
              </div>
            </div>

            {/* Snippets deste desafio: o aluno pode guardar trechos já vinculados. */}
            <SnippetsDoDesafioSection desafioId={desafio.id} />
          </>
        )}
      </QueryBoundary>
    </PageContainer>
  )
}
