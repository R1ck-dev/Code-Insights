import { Link } from 'react-router-dom'
import { ArrowRight, Braces, Cpu, ExternalLink, Gauge, Target, type LucideIcon } from 'lucide-react'
import { buttonClasses } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { CodeBlock } from '@/components/CodeBlock'
import { AutonomyMeter } from '@/components/AutonomyMeter'
import { LanguageBadge, VisibilityBadge } from '@/components/domain/badges'
import { complexityHexByOrdinal, prettyBigO } from '@/domain/enums'
import { cn } from '@/lib/utils'

/** Solução Two Sum (força bruta) usada no card-mock do hero. */
const CODE_TWO_SUM = `class Solution {
    public int[] twoSum(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[] { i, j };
                }
            }
        }
        return new int[] {};
    }
}`

/** Métricas ilustrativas do card-mock (estáticas, sem API). */
const METRICAS_MOCK: {
  nome: string
  rotulo: string
  natureza: 'exata' | 'estimada'
  cor?: string
}[] = [
  { nome: 'Tempo', rotulo: prettyBigO('O(n^2)'), natureza: 'estimada', cor: complexityHexByOrdinal(4) },
  { nome: 'Ciclomática', rotulo: '4', natureza: 'exata' },
  { nome: 'Espaço', rotulo: prettyBigO('O(1)'), natureza: 'estimada', cor: complexityHexByOrdinal(0) },
]

/** Plataformas suportadas — cores de marca (não são neutros do tema). */
const PLATAFORMAS = [
  { nome: 'LeetCode', cor: '#59C36A' },
  { nome: 'Codeforces', cor: '#4C93D6' },
  { nome: 'Neps Academy', cor: '#E0A21E' },
]

const FEATURES: { icon: LucideIcon; titulo: string; descricao: string }[] = [
  {
    icon: Target,
    titulo: 'Desafios & Resoluções',
    descricao: 'Organize exercícios e todas as suas tentativas de solução.',
  },
  {
    icon: Cpu,
    titulo: 'Métricas estáticas',
    descricao: 'Big O de tempo e espaço, e ciclomática de McCabe por resolução.',
  },
  {
    icon: Gauge,
    titulo: 'Autonomia IA',
    descricao: 'Um índice autodeclarado de 1 a 5 para acompanhar seu amadurecimento.',
  },
  {
    icon: Braces,
    titulo: 'Snippets categorizados',
    descricao: 'Guarde trechos por conceito: recursão, grafos, DP e mais.',
  },
]

/** Landing pública (visitante-01) — estática, renderiza dentro do PublicLayout. */
export function LandingPage() {
  return (
    <div className="flex flex-col">
      {/* Hero */}
      <section
        style={{
          backgroundImage:
            'radial-gradient(120% 90% at 12% -10%, rgba(110,95,246,0.16), transparent 55%)',
        }}
      >
        <div className="mx-auto grid w-full max-w-6xl grid-cols-1 items-center gap-11 px-5 pb-14 pt-12 sm:px-8 lg:grid-cols-[1.05fr_1fr]">
          {/* Coluna esquerda */}
          <div className="flex flex-col items-start gap-5">
            <Badge tone="brand" dot>
              Iniciação Científica · portfólio + métricas
            </Badge>
            <h1 className="text-[32px] font-extrabold leading-[1.06] tracking-tight text-heading sm:text-[46px]">
              Seu portfólio de código, com métricas de verdade.
            </h1>
            <p className="max-w-[460px] text-[16.5px] leading-relaxed text-muted">
              Registre desafios de juízes online, submeta suas soluções e veja a{' '}
              <span className="text-fg">complexidade</span> e a{' '}
              <span className="text-fg">evolução</span> do seu raciocínio algorítmico — com
              honestidade sobre o que é exato e o que é estimado.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link to="/criar-conta" className={buttonClasses({ size: 'lg' })}>
                Criar conta
                <ArrowRight size={17} />
              </Link>
              <Link to="/entrar" className={buttonClasses({ variant: 'secondary', size: 'lg' })}>
                Ver um portfólio
                <ExternalLink size={16} />
              </Link>
            </div>
            <div className="mt-1 flex flex-wrap items-center gap-4 font-mono text-[12px] text-subtle">
              {PLATAFORMAS.map((p) => (
                <span key={p.nome} className="inline-flex items-center gap-1.5">
                  <span
                    className="h-1.5 w-1.5 rounded-full"
                    style={{ background: p.cor }}
                  />
                  {p.nome}
                </span>
              ))}
            </div>
          </div>

          {/* Coluna direita: card-mock de resolução */}
          <div className="flex flex-col gap-3 rounded-xl border border-border bg-surface p-4 shadow-[0_30px_70px_-30px_rgba(0,0,0,0.55)]">
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm font-semibold text-heading">Two Sum</span>
              <div className="flex items-center gap-1.5">
                <LanguageBadge linguagem="JAVA" />
                <VisibilityBadge visibilidade="PUBLICO" />
              </div>
            </div>

            <CodeBlock
              code={CODE_TWO_SUM}
              lang="java"
              label="Solution.java"
              lines={false}
              maxHeight={176}
            />

            <div className="flex gap-2.5">
              {METRICAS_MOCK.map((m) => (
                <div
                  key={m.nome}
                  className="flex flex-1 flex-col gap-1 rounded-[10px] border border-border bg-input px-2.5 py-2.5"
                >
                  <span className="text-[10px] font-semibold text-muted">{m.nome}</span>
                  <span
                    className="font-mono text-[21px] font-semibold leading-none tabular-nums"
                    style={{ color: m.cor ?? 'var(--heading)' }}
                  >
                    {m.rotulo}
                  </span>
                  <span
                    className={cn(
                      'font-mono text-[9px] font-semibold uppercase tracking-wider',
                      m.natureza === 'exata' ? 'text-success' : 'text-warning',
                    )}
                  >
                    {m.natureza}
                  </span>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-between rounded-[10px] border border-border bg-input px-3 py-2.5">
              <span className="text-[12px] font-semibold text-label">Autonomia IA</span>
              <AutonomyMeter value={4} size="md" />
            </div>
          </div>
        </div>
      </section>

      {/* Grade de features */}
      <section className="mx-auto w-full max-w-6xl px-5 pb-16 sm:px-8">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map(({ icon: Icon, titulo, descricao }) => (
            <div
              key={titulo}
              className="flex flex-col gap-2.5 rounded-xl border border-border bg-surface p-[18px]"
            >
              <div className="flex h-[38px] w-[38px] items-center justify-center rounded-[10px] bg-brand/[.13]">
                <Icon size={19} className="text-brand-strong" />
              </div>
              <span className="text-sm font-semibold text-heading">{titulo}</span>
              <span className="text-[12.5px] leading-relaxed text-muted">{descricao}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Rodapé sóbrio */}
      <footer className="border-t border-border-subtle">
        <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-5 px-5 py-6 sm:px-8">
          <span className="max-w-[560px] text-[12.5px] text-subtle">
            Projeto de Iniciação Científica — pesquisa sobre autonomia e amadurecimento algorítmico.
          </span>
          <nav className="flex gap-5 text-[12.5px] text-muted">
            <span className="text-subtle">Sobre</span>
            <Link to="/entrar" className="transition-colors hover:text-fg">
              Entrar
            </Link>
            <Link to="/criar-conta" className="transition-colors hover:text-fg">
              Criar conta
            </Link>
          </nav>
        </div>
      </footer>
    </div>
  )
}
