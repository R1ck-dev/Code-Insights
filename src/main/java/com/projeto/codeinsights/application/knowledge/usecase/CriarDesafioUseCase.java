package com.projeto.codeinsights.application.knowledge.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.knowledge.dto.CriarDesafioInput;
import com.projeto.codeinsights.application.knowledge.dto.DesafioResumoDTO;
import com.projeto.codeinsights.domain.identity.model.Usuario;
import com.projeto.codeinsights.domain.identity.port.UsuarioRepository;
import com.projeto.codeinsights.domain.knowledge.model.Desafio;
import com.projeto.codeinsights.domain.knowledge.port.DesafioRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CriarDesafioUseCase {

    private final DesafioRepository desafioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public DesafioResumoDTO execute(CriarDesafioInput input) {
        Usuario autor = usuarioRepository.buscarPorId(input.autorId())
                .orElseThrow(() -> new NegocioException("Usuario nao encontrado."));

        Desafio desafio = new Desafio(
                null,
                input.autorId(),
                input.titulo(),
                input.descricao(),
                input.origemPlataforma(),
                input.dificuldade());

        Desafio salvo = desafioRepository.salvar(desafio);

        return new DesafioResumoDTO(
                salvo.getId(),
                salvo.getAutorId(),
                autor.getUsername(),
                salvo.getTitulo(),
                salvo.getOrigemPlataforma(),
                salvo.getDificuldade(),
                salvo.isPublico(),
                salvo.getDataCriacao());
    }
}
