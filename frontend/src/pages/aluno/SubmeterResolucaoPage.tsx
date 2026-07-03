import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Cpu } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { Breadcrumb } from '@/components/page/Breadcrumb'
import { QueryBoundary } from '@/components/page/states'
import { CodeEditor } from '@/components/CodeEditor'
import { LanguageDot } from '@/components/domain/badges'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { FormField } from '@/components/ui/form-field'
import { EmptyState } from '@/components/ui/empty-state'
import { toast } from '@/components/ui/toaster'
import { useDesafioDetalhe } from '@/features/desafios/hooks'
import { useSubmeterResolucao } from '@/features/resolucoes/hooks'
import { LINGUAGEM_META, LINGUAGENS } from '@/domain/enums'
import type { LinguagemProgramacao } from '@/types/api'
import { apiErrorMessage } from '@/lib/api'
import { cn } from '@/lib/utils'

const AUTONOMIA_OPCOES = [1, 2, 3, 4, 5]

export function SubmeterResolucaoPage() {
  const { desafioId } = useParams<{ desafioId: string }>()
  const navigate = useNavigate()
  const desafioQuery = useDesafioDetalhe(desafioId)
  const submeter = useSubmeterResolucao(desafioId ?? '')

  const [codigo, setCodigo] = useState('')
  const [linguagem, setLinguagem] = useState<LinguagemProgramacao>('JAVA')
  const [autonomia, setAutonomia] = useState<number | null>(null)
  const [apoio, setApoio] = useState('')

  async function handleSubmit() {
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
          icon={Cpu}
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

            <div className="flex flex-col gap-1">
              <h1 className="text-[23px] font-bold leading-tight tracking-[-0.02em] text-heading">
                Submeter resolução
              </h1>
              <p className="text-[13.5px] text-muted">
                para <span className="font-semibold text-fg">{desafio.titulo}</span> · nasce privada e
                passa por análise automática.
              </p>
            </div>

            <div className="grid grid-cols-1 items-start gap-[18px] lg:grid-cols-[1.62fr_1fr]">
              {/* Coluna esquerda: editor */}
              <FormField label="Código-fonte" required htmlFor="codigo">
                <CodeEditor
                  id="codigo"
                  value={codigo}
                  onChange={setCodigo}
                  label={`Editor · ${LINGUAGEM_META[linguagem].label}`}
                  placeholder="Cole aqui o código da sua resolução…"
                  minHeight={340}
                />
              </FormField>

              {/* Coluna direita: metadados */}
              <div className="flex flex-col gap-[18px]">
                <FormField
                  label="Linguagem"
                  required
                  hint={
                    <span className="flex items-center gap-1.5">
                      <Cpu size={13} className="shrink-0 text-warning" />
                      Métricas de complexidade hoje só para{' '}
                      <span className="font-semibold text-fg">Java</span>.
                    </span>
                  }
                >
                  <div className="grid grid-cols-2 gap-2">
                    {LINGUAGENS.map((l) => {
                      const sel = l.value === linguagem
                      return (
                        <button
                          key={l.value}
                          type="button"
                          aria-pressed={sel}
                          onClick={() => setLinguagem(l.value)}
                          className={cn(
                            'inline-flex h-[38px] items-center gap-2 rounded-[9px] border px-3 text-[13px] transition',
                            sel
                              ? 'border-brand bg-brand/[.13] font-semibold text-heading ring-[3px] ring-brand/25'
                              : 'border-border bg-input font-medium text-muted hover:border-border-strong hover:text-fg',
                          )}
                        >
                          <LanguageDot linguagem={l.value} size={8} />
                          {l.label}
                        </button>
                      )
                    })}
                  </div>
                </FormField>

                <FormField
                  label="Índice de Autonomia IA"
                  required
                  hint="1 = fiz com bastante apoio de IA · 5 = fiz de forma autônoma."
                >
                  <div className="flex gap-[7px]">
                    {AUTONOMIA_OPCOES.map((n) => {
                      const sel = autonomia === n
                      return (
                        <button
                          key={n}
                          type="button"
                          aria-pressed={sel}
                          onClick={() => setAutonomia(n)}
                          className={cn(
                            'flex h-[38px] flex-1 items-center justify-center rounded-[9px] border font-mono text-[14px] transition',
                            sel
                              ? 'border-brand bg-brand font-semibold text-brand-on ring-[3px] ring-brand/25'
                              : 'border-border-strong bg-input text-muted hover:border-brand/50 hover:text-fg',
                          )}
                        >
                          {n}
                        </button>
                      )
                    })}
                  </div>
                </FormField>

                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="apoio">
                    Como a IA ajudou <span className="font-normal text-subtle">(opcional)</span>
                  </Label>
                  <Textarea
                    id="apoio"
                    value={apoio}
                    onChange={(e) => setApoio(e.target.value)}
                    rows={3}
                    placeholder="Ex.: usei IA só para lembrar a sintaxe de HashMap; a lógica foi minha."
                  />
                </div>

                <div className="flex justify-end gap-2.5">
                  <Button variant="secondary" onClick={() => navigate(-1)}>
                    Cancelar
                  </Button>
                  <Button onClick={handleSubmit} loading={submeter.isPending}>
                    Submeter
                  </Button>
                </div>
              </div>
            </div>
          </>
        )}
      </QueryBoundary>
    </PageContainer>
  )
}
