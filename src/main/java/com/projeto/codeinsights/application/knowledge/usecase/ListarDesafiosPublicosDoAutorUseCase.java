package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.DesafioResumoDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListarDesafiosPublicosDoAutorUseCase {

    private final DesafioRepository desafioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Pagina<DesafioResumoDTO> execute(UUID autorId, int pagina, int tamanho) {
        Usuario autor = usuarioRepository.buscarPorId(autorId)
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        Pagina<Desafio> resultado = desafioRepository.listarPublicosPorAutor(autorId, pagina, tamanho);

        return mapear(resultado, autor.getUsername());
    }

    private Pagina<DesafioResumoDTO> mapear(Pagina<Desafio> resultado, String autorUsername) {
        return new Pagina<>(
                resultado.itens().stream()
                        .map(desafio -> new DesafioResumoDTO(
                                desafio.getId(),
                                desafio.getAutorId(),
                                autorUsername,
                                desafio.getTitulo(),
                                desafio.getOrigemPlataforma(),
                                desafio.getDificuldade(),
                                desafio.isPublico(),
                                desafio.getDataCriacao()))
                        .toList(),
                resultado.paginaAtual(),
                resultado.totalPaginas(),
                resultado.totalItens());
    }
}
