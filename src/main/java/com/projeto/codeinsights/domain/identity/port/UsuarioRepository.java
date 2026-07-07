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

    Optional<Usuario> buscarPorEmail(String email);

    boolean existePorEmail(String email);

    boolean existePorUsername(String username);

    List<Usuario> buscarPorIds(Collection<UUID> ids);

    /** Perfis publicos e ativos (exceto o solicitante), paginados e filtrados por username. */
    Pagina<Usuario> listarPublicos(UUID excluidoId, String filtroUsername, int pagina, int tamanho);
}
