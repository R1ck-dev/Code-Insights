package com.projeto.codeinsights.application.knowledge.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.DesafioDetalheDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.knowledge.port.ResolucaoRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuscarDesafioDetalheUseCase {

    private final DesafioRepository desafioRepository;
    private final ResolucaoRepository resolucaoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public DesafioDetalheDTO execute(UUID desafioId, UUID solicitanteId) {
        Desafio desafio = desafioRepository.buscarPorId(desafioId)
                .orElseThrow(() -> new NegocioException("Desafio nao encontrado."));

        if (!desafio.pertenceA(solicitanteId) && !desafio.ehPublico()) {
            throw new NegocioException("Voce nao tem acesso a este desafio.");
        }

        // Nao-dono/anonimo veem apenas a contagem de resolucoes PUBLICAS
        // (o total incluiria as privadas e vazaria sua existencia/quantidade).
        boolean dono = desafio.pertenceA(solicitanteId);
        long qtdResolucoes = dono
                ? resolucaoRepository.contarPorDesafio(desafioId)
                : resolucaoRepository.contarPublicasPorDesafio(desafioId);

        Usuario autor = usuarioRepository.buscarPorId(desafio.getAutorId())
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        return new DesafioDetalheDTO(
                desafio.getId(),
                desafio.getAutorId(),
                autor.getUsername(),
                desafio.getTitulo(),
                desafio.getEnunciado(),
                desafio.getPlataformaOrigem(),
                desafio.getIdentificadorExterno(),
                desafio.getUrlExterna(),
                desafio.getVisibilidade(),
                qtdResolucoes,
                desafio.getCriadoEm(),
                desafio.getAtualizadoEm());
    }
}
