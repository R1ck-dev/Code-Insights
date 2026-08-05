package com.projeto.codeinsights.domain.identity.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.shared.Pagina;

public interface UsuarioRepository {
    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    /**
     * Apaga a conta e, <b>por cascata do schema</b>, tudo que pende dela: tokens de verificacao,
     * desafios, resolucoes, resultados de metrica, snippets e o consentimento de pesquisa. Todas as
     * chaves estrangeiras que apontam para {@code usuarios} sao {@code ON DELETE CASCADE} desde a
     * V1, e o teste {@code ExcluirMinhaContaUseCaseTest} guarda essa expectativa.
     * <p>
     * E irreversivel e nao ha copia: a reprodutibilidade de uma analise depende do CSV exportado na
     * data do corte, nao de manter a conta viva.
     */
    void remover(UUID id);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existePorEmail(String email);

    boolean existePorUsername(String username);

    List<Usuario> buscarPorIds(Collection<UUID> ids);

    /** Perfis publicos e ativos (exceto o solicitante), paginados e filtrados por username. */
    Pagina<Usuario> listarPublicos(UUID excluidoId, String filtroUsername, int pagina, int tamanho);

    /**
     * <b>Todas</b> as contas, sem filtro de visibilidade nem de status, paginadas e filtradas por
     * username ou e-mail. E o oposto de {@link #listarPublicos}: existe para a administracao, onde
     * justamente as contas que nao aparecem no diretorio — pendentes de verificacao, perfis privados
     * — sao as que precisam de acao.
     */
    Pagina<Usuario> listarTodos(String busca, int pagina, int tamanho);
}
