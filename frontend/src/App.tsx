import { Navigate, Route, Routes } from 'react-router-dom'
import { PublicLayout } from '@/layouts/PublicLayout'
import { AppLayout } from '@/layouts/AppLayout'
import { RedirectIfAuthenticated, RequireAuth, RequireRole } from '@/auth/guards'

// Acesso
import { LoginPage } from '@/pages/acesso/LoginPage'
import { RegistroPage } from '@/pages/acesso/RegistroPage'
import { VerifiqueEmailPage } from '@/pages/acesso/VerifiqueEmailPage'
import { ContaAtivadaPage } from '@/pages/acesso/ContaAtivadaPage'
import { EsqueciSenhaPage } from '@/pages/acesso/EsqueciSenhaPage'
import { DefinirNovaSenhaPage } from '@/pages/acesso/DefinirNovaSenhaPage'

// Visitante
import { LandingPage } from '@/pages/visitante/LandingPage'
import { PortfolioAutorPage } from '@/pages/visitante/PortfolioAutorPage'
import { DesafioPublicoPage } from '@/pages/visitante/DesafioPublicoPage'
import { ResolucaoPublicaPage } from '@/pages/visitante/ResolucaoPublicaPage'

// Aluno
import { DashboardPage } from '@/pages/aluno/DashboardPage'
import { ExplorarPage } from '@/pages/aluno/ExplorarPage'
import { DesafiosPage } from '@/pages/aluno/DesafiosPage'
import { DesafioDetalhePage } from '@/pages/aluno/DesafioDetalhePage'
import { SubmeterResolucaoPage } from '@/pages/aluno/SubmeterResolucaoPage'
import { ResolucaoDetalhePage } from '@/pages/aluno/ResolucaoDetalhePage'
import { SnippetsPage } from '@/pages/aluno/SnippetsPage'
import { MeuPerfilPage } from '@/pages/aluno/MeuPerfilPage'

// Consentimento (participante — não é área de pesquisa, é do próprio aluno)
import { ConsentimentoPage } from '@/pages/consentimento/ConsentimentoPage'

// Pesquisa
import { QualidadeDosDadosPage } from '@/pages/pesquisa/QualidadeDosDadosPage'
import { CoortePage } from '@/pages/pesquisa/CoortePage'

export function App() {
  return (
    <Routes>
      {/* Público (nav pública) */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
        {/*
         * Explorar é servida por DUAS rotas com o MESMO componente (00-INDICE §6-A, Lacuna 1):
         * aqui, pública (destino do CTA "Ver um portfólio" da landing); e em /app/explorar,
         * dentro do shell autenticado. Os portfólios listados já são públicos.
         */}
        <Route path="/explorar" element={<ExplorarPage />} />
        <Route path="/u/:usuarioId" element={<PortfolioAutorPage />} />
        <Route path="/u/:usuarioId/desafios/:desafioId" element={<DesafioPublicoPage />} />
        <Route
          path="/u/:usuarioId/desafios/:desafioId/resolucoes/:resolucaoId"
          element={<ResolucaoPublicaPage />}
        />
      </Route>

      {/* Acesso (AuthLayout é interno a cada página) */}
      <Route element={<RedirectIfAuthenticated />}>
        <Route path="/entrar" element={<LoginPage />} />
        <Route path="/criar-conta" element={<RegistroPage />} />
      </Route>
      <Route path="/verifique-email" element={<VerifiqueEmailPage />} />
      <Route path="/ativar" element={<ContaAtivadaPage />} />
      <Route path="/recuperar-senha" element={<EsqueciSenhaPage />} />
      <Route path="/definir-senha" element={<DefinirNovaSenhaPage />} />

      {/* App autenticado (sidebar 236px + topbar 60px) */}
      <Route element={<RequireAuth />}>
        <Route path="/app" element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="explorar" element={<ExplorarPage />} />
          <Route path="desafios" element={<DesafiosPage />} />
          <Route path="desafios/:desafioId" element={<DesafioDetalhePage />} />
          <Route path="desafios/:desafioId/submeter" element={<SubmeterResolucaoPage />} />
          <Route path="resolucoes/:resolucaoId" element={<ResolucaoDetalhePage />} />
          <Route path="snippets" element={<SnippetsPage />} />
          <Route path="perfil" element={<MeuPerfilPage />} />

          {/* Sem RequireRole: o termo é do participante, não do pesquisador. Todo mundo que tem
              conta responde ao seu, inclusive quem já é PESQUISADOR ou ADMIN e também submete. */}
          <Route path="consentimento" element={<ConsentimentoPage />} />

          {/* Pesquisa: dentro do mesmo shell, mas atrás do papel. Esconder a rota é conveniência —
              quem autoriza de verdade é o SecurityConfig em /api/pesquisa/**. */}
          <Route element={<RequireRole roles={['PESQUISADOR', 'ADMIN']} />}>
            <Route path="pesquisa" element={<QualidadeDosDadosPage />} />
            <Route path="pesquisa/coorte" element={<CoortePage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
