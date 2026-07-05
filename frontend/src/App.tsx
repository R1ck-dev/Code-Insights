import { Navigate, Route, Routes } from 'react-router-dom'
import { PublicLayout } from '@/layouts/PublicLayout'
import { AppLayout } from '@/layouts/AppLayout'
import { RedirectIfAuthenticated, RequireAuth } from '@/auth/guards'

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
import { DesafiosPage } from '@/pages/aluno/DesafiosPage'
import { DesafioDetalhePage } from '@/pages/aluno/DesafioDetalhePage'
import { SubmeterResolucaoPage } from '@/pages/aluno/SubmeterResolucaoPage'
import { ResolucaoDetalhePage } from '@/pages/aluno/ResolucaoDetalhePage'
import { SnippetsPage } from '@/pages/aluno/SnippetsPage'
import { MeuPerfilPage } from '@/pages/aluno/MeuPerfilPage'

export function App() {
  return (
    <Routes>
      {/* Público (topnav) */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
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

      {/* App autenticado (sidebar + topbar) */}
      <Route element={<RequireAuth />}>
        <Route path="/app" element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="desafios" element={<DesafiosPage />} />
          <Route path="desafios/:desafioId" element={<DesafioDetalhePage />} />
          <Route path="desafios/:desafioId/submeter" element={<SubmeterResolucaoPage />} />
          <Route path="resolucoes/:resolucaoId" element={<ResolucaoDetalhePage />} />
          <Route path="snippets" element={<SnippetsPage />} />
          <Route path="perfil" element={<MeuPerfilPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
