package com.projeto.codeinsights.application.identity.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.identity.dto.UsuarioPublicoDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.shared.Pagina;

import lombok.RequiredArgsConstructor;

/**
 * Diretorio de portfolios: lista os usuarios com perfil publico (e ativos),
 * exceto o proprio solicitante, paginados e com filtro opcional por username.
 */
@Service
@RequiredArgsConstructor
public class ListarUsuariosPublicosUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Pagina<UsuarioPublicoDTO> execute(UUID solicitanteId, String filtroUsername, int pagina, int tamanho) {
        Pagina<Usuario> resultado = usuarioRepository.listarPublicos(solicitanteId, filtroUsername, pagina, tamanho);

        return new Pagina<>(
                resultado.itens().stream()
                        .map(u -> new UsuarioPublicoDTO(u.getId(), u.getUsername(), u.getVisibilidadePerfil()))
                        .toList(),
                resultado.paginaAtual(),
                resultado.totalPaginas(),
                resultado.totalItens());
    }
}
