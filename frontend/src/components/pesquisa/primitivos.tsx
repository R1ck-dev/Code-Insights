import { CircleHelp } from 'lucide-react'
import { Card } from '@/components/ui/card'

/*
 * O vocabulário visual das telas de pesquisa — painel, título de seção, número grande e barra
 * neutra. Nasceram locais na QualidadeDosDadosPage e subiram para cá quando a segunda tela precisou
 * dos mesmos quatro: duas cópias divergem, e duas telas da mesma área com painéis levemente
 * diferentes fazem o pesquisador desconfiar de qual está certa.
 */

export function Painel({
  titulo,
  nota,
  children,
}: {
  titulo: string
  nota?: string
  children: React.ReactNode
}) {
  return (
    <Card className="flex flex-col gap-[13px] p-[18px]">
      <Titulo texto={titulo} />
      {children}
      {nota && <p className="text-[11.5px] leading-[1.5] text-soft">{nota}</p>}
    </Card>
  )
}

export function Titulo({ texto }: { texto: string }) {
  return (
    <h2 className="font-mono text-[10.5px] font-semibold tracking-[.06em] text-mid uppercase">
      {texto}
    </h2>
  )
}

export function Numero({
  rotulo,
  valor,
  nota,
  icone: Icone = CircleHelp,
  pequeno,
}: {
  rotulo: string
  valor: string | number
  nota?: string
  icone?: typeof CircleHelp
  pequeno?: boolean
}) {
  return (
    <Card className="flex flex-col gap-[9px] px-[15px] py-3.5">
      <div className="flex items-center gap-[7px]">
        <Icone size={12} strokeWidth={2} aria-hidden className="shrink-0 text-soft" />
        <span className="font-mono text-[10.5px] tracking-[.06em] text-mid uppercase">{rotulo}</span>
      </div>
      <span
        className={`tabular font-mono leading-none font-semibold text-ink ${pequeno ? 'text-[17px]' : 'text-[31px]'}`}
      >
        {valor}
      </span>
      {nota && <span className="text-[11.5px] text-soft">{nota}</span>}
    </Card>
  )
}

/** Barra neutra: a única cor do sistema é o colormap, e aqui não há classe de complexidade. */
export function Trilho({ fracao, apagado }: { fracao: number; apagado?: boolean }) {
  return (
    <div aria-hidden className="h-[5px] w-full overflow-hidden rounded-ci-sm bg-recess">
      <div
        className={apagado ? 'h-full bg-line-strong' : 'h-full bg-ink'}
        style={{ width: `${Math.max(fracao * 100, fracao > 0 ? 3 : 0)}%` }}
      />
    </div>
  )
}

export function percentual(parte: number, total: number): string {
  if (total === 0) return '0%'
  return `${((parte / total) * 100).toFixed(1).replace('.', ',')}%`
}
