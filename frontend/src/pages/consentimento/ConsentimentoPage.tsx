import { CircleCheck, CircleSlash, FlaskConical, TriangleAlert } from 'lucide-react'
import { PageContainer } from '@/components/page/PageContainer'
import { PageHeader } from '@/components/page/PageHeader'
import { QueryBoundary } from '@/components/page/states'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Markdown } from '@/components/ui/markdown'
import { toast } from '@/components/ui/toaster'
import { useDecidirConsentimento, useMeuConsentimento } from '@/features/pesquisa/hooks'
import { apiErrorMessage } from '@/lib/api'
import { formatDateTime } from '@/lib/utils'
import type { MeuConsentimentoDTO } from '@/types/api'

/*
 * Termo de Consentimento Livre e Esclarecido (TCLE) — ÓRBITA.
 *
 * Três coisas nesta tela são exigência ética, não escolha de UX:
 *
 *  1. NADA AQUI BLOQUEIA A PLATAFORMA. Não há modal, não há redirecionamento forçado, e recusar não
 *     tira nenhuma funcionalidade. Consentimento condicionado a acesso não é livre — é preço.
 *  2. RETIRAR CUSTA O MESMO QUE DAR. Os dois botões têm o mesmo peso visual e ficam lado a lado;
 *     esconder a revogação atrás de um link cinza seria desenhar contra a pessoa.
 *  3. O TEXTO É MOSTRADO INTEIRO, antes dos botões. Um resumo com "leia o termo completo" transforma
 *     consentimento informado em consentimento presumido.
 *
 * O aviso de rascunho fica no topo porque, enquanto o CEP não aprovar, o aceite não autoriza nada —
 * e quem clica em "autorizar" precisa saber disso antes de clicar, não depois.
 */

export function ConsentimentoPage() {
  const query = useMeuConsentimento()

  return (
    <PageContainer className="max-w-[760px] gap-[18px]">
      <PageHeader
        title="Termo de consentimento"
        subtitle="Sua participação na pesquisa é voluntária, e a decisão pode mudar a qualquer momento."
      />

      <QueryBoundary query={query}>
        {(consentimento) => (
          <div className="flex flex-col gap-[18px]">
            {!consentimento.podeSerUsadoEmPesquisa && <AvisoDeRascunho />}
            <EstadoAtual consentimento={consentimento} />

            <Card className="p-[22px]">
              <Markdown texto={consentimento.texto} />
            </Card>

            <Decisao consentimento={consentimento} />
          </div>
        )}
      </QueryBoundary>
    </PageContainer>
  )
}

/* ---------------------------------------------------------------- avisos --- */

function AvisoDeRascunho() {
  return (
    <div
      role="status"
      className="flex items-start gap-[11px] rounded-ci border border-atencao-line bg-atencao-bg p-[15px]"
    >
      <TriangleAlert
        size={16}
        strokeWidth={2}
        aria-hidden
        className="mt-[2px] shrink-0 text-atencao-ink"
      />
      <div className="flex flex-col gap-[5px]">
        <p className="text-[13px] font-semibold text-atencao-ink">
          Este termo ainda não foi aprovado pelo Comitê de Ética.
        </p>
        <p className="text-[12.5px] leading-[1.6] text-atencao-ink">
          Ele está no ar para que a plataforma possa ser testada antes do piloto. Responder agora{' '}
          <strong className="font-semibold">não autoriza</strong> uso dos seus dados em pesquisa, e
          você será consultado de novo quando o texto aprovado entrar no lugar deste.
        </p>
      </div>
    </div>
  )
}

/* ---------------------------------------------------------------- estado --- */

/**
 * Três estados, não dois: sem resposta é diferente de recusa. Tratar silêncio como "não" seria
 * decidir pela pessoa; tratar como "sim" seria pior ainda.
 */
function EstadoAtual({ consentimento }: { consentimento: MeuConsentimentoDTO }) {
  if (consentimento.decisao === null) {
    return (
      <Faixa
        icone={FlaskConical}
        titulo="Você ainda não respondeu."
        detalhe="Suas submissões não estão sendo usadas na pesquisa até que você autorize."
      />
    )
  }

  const autorizou = consentimento.decisao === 'ACEITE'
  return (
    <Faixa
      icone={autorizou ? CircleCheck : CircleSlash}
      titulo={autorizou ? 'Você autorizou o uso das suas submissões.' : 'Você não autorizou.'}
      detalhe={`Registrado em ${formatDateTime(consentimento.decididoEm)} · termo ${consentimento.versao}`}
      destacado={autorizou}
    />
  )
}

function Faixa({
  icone: Icone,
  titulo,
  detalhe,
  destacado,
}: {
  icone: typeof CircleCheck
  titulo: string
  detalhe: string
  destacado?: boolean
}) {
  return (
    <Card className="flex items-start gap-[11px] p-[15px]">
      <Icone
        size={16}
        strokeWidth={2}
        aria-hidden
        className={`mt-[2px] shrink-0 ${destacado ? 'text-ink' : 'text-soft'}`}
      />
      <div className="flex flex-col gap-[3px]">
        <p className="text-[13px] font-medium text-ink">{titulo}</p>
        <p className="font-mono text-[11.5px] text-soft">{detalhe}</p>
      </div>
    </Card>
  )
}

/* --------------------------------------------------------------- decisão --- */

function Decisao({ consentimento }: { consentimento: MeuConsentimentoDTO }) {
  const decidir = useDecidirConsentimento()
  const autorizou = consentimento.decisao === 'ACEITE'

  const responder = (autoriza: boolean) => {
    decidir.mutate(autoriza, {
      onSuccess: () =>
        toast.success(
          autoriza
            ? 'Consentimento registrado. Obrigado por participar.'
            : 'Registrado. Suas submissões ficam fora da pesquisa.',
        ),
      onError: (err) =>
        toast.error(apiErrorMessage(err, 'Não foi possível registrar sua decisão.')),
    })
  }

  return (
    <Card className="flex flex-col gap-[15px] p-[18px]">
      <p className="text-[13px] leading-[1.6] text-body">
        Você pode mudar esta resposta quando quiser, nesta mesma tela. Retirar o consentimento não
        afeta seu acesso à plataforma nem nada além da pesquisa.
      </p>

      {/* Mesmo peso visual nos dois: a variante forte marca a decisão ATUAL, e não a que a
          plataforma prefere que você tome. */}
      <div className="flex flex-wrap gap-[11px]">
        <Button
          variant={autorizou ? 'primary' : 'secondary'}
          icon={CircleCheck}
          loading={decidir.isPending}
          onClick={() => responder(true)}
        >
          Autorizar o uso
        </Button>
        <Button
          variant={consentimento.decisao === 'RECUSA' ? 'destructive-solid' : 'secondary'}
          icon={CircleSlash}
          loading={decidir.isPending}
          onClick={() => responder(false)}
        >
          {autorizou ? 'Retirar meu consentimento' : 'Não autorizar'}
        </Button>
      </div>
    </Card>
  )
}
