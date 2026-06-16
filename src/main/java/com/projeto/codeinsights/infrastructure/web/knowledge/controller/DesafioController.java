package com.projeto.codeinsights.infrastructure.web.knowledge.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.knowledge.dto.AlterarVisibilidadeDesafioInput;
import com.projeto.codeinsights.application.knowledge.dto.AtualizarDesafioInput;
import com.projeto.codeinsights.application.knowledge.dto.CriarDesafioInput;
import com.projeto.codeinsights.application.knowledge.dto.DesafioDetalheDTO;
import com.projeto.codeinsights.application.knowledge.dto.DesafioResumoDTO;
import com.projeto.codeinsights.application.knowledge.usecase.AlterarVisibilidadeDesafioUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.AtualizarDesafioUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.BuscarDesafioDetalheUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.CriarDesafioUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.ListarDesafiosPublicosDoAutorUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.ListarMeusDesafiosUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.RemoverDesafioUseCase;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.infrastructure.config.security.CurrentUserId;
import com.projeto.codeinsights.infrastructure.web.knowledge.dto.AlterarVisibilidadeDesafioRequest;
import com.projeto.codeinsights.infrastructure.web.knowledge.dto.AtualizarDesafioRequest;
import com.projeto.codeinsights.infrastructure.web.knowledge.dto.CriarDesafioRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/desafios")
@RequiredArgsConstructor
public class DesafioController {

    private final CriarDesafioUseCase criarDesafioUseCase;
    private final ListarMeusDesafiosUseCase listarMeusDesafiosUseCase;
    private final ListarDesafiosPublicosDoAutorUseCase listarDesafiosPublicosDoAutorUseCase;
    private final BuscarDesafioDetalheUseCase buscarDesafioDetalheUseCase;
    private final AtualizarDesafioUseCase atualizarDesafioUseCase;
    private final AlterarVisibilidadeDesafioUseCase alterarVisibilidadeDesafioUseCase;
    private final RemoverDesafioUseCase removerDesafioUseCase;

    @PostMapping
    public ResponseEntity<DesafioResumoDTO> criar(@CurrentUserId UUID autorId,
            @RequestBody @Valid CriarDesafioRequest request) {
        CriarDesafioInput input = new CriarDesafioInput(
                autorId,
                request.titulo(),
                request.descricao(),
                request.origemPlataforma(),
                request.dificuldade());
        DesafioResumoDTO criado = criarDesafioUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public ResponseEntity<Pagina<DesafioResumoDTO>> listarMeus(@CurrentUserId UUID autorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(listarMeusDesafiosUseCase.execute(autorId, pagina, tamanho));
    }

    @GetMapping("/autor/{autorId}")
    public ResponseEntity<Pagina<DesafioResumoDTO>> listarPublicosDoAutor(@PathVariable UUID autorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(listarDesafiosPublicosDoAutorUseCase.execute(autorId, pagina, tamanho));
    }

    @GetMapping("/{desafioId}")
    public ResponseEntity<DesafioDetalheDTO> buscarDetalhe(@CurrentUserId UUID solicitanteId,
            @PathVariable UUID desafioId) {
        return ResponseEntity.ok(buscarDesafioDetalheUseCase.execute(desafioId, solicitanteId));
    }

    @PatchMapping("/{desafioId}")
    public ResponseEntity<Void> atualizar(@CurrentUserId UUID solicitanteId,
            @PathVariable UUID desafioId,
            @RequestBody @Valid AtualizarDesafioRequest request) {
        AtualizarDesafioInput input = new AtualizarDesafioInput(
                desafioId,
                solicitanteId,
                request.titulo(),
                request.descricao(),
                request.origemPlataforma(),
                request.dificuldade());
        atualizarDesafioUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{desafioId}/visibilidade")
    public ResponseEntity<Void> alterarVisibilidade(@CurrentUserId UUID solicitanteId,
            @PathVariable UUID desafioId,
            @RequestBody @Valid AlterarVisibilidadeDesafioRequest request) {
        AlterarVisibilidadeDesafioInput input = new AlterarVisibilidadeDesafioInput(
                desafioId,
                solicitanteId,
                request.publico());
        alterarVisibilidadeDesafioUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{desafioId}")
    public ResponseEntity<Void> remover(@CurrentUserId UUID solicitanteId,
            @PathVariable UUID desafioId) {
        removerDesafioUseCase.execute(desafioId, solicitanteId);
        return ResponseEntity.noContent().build();
    }
}
