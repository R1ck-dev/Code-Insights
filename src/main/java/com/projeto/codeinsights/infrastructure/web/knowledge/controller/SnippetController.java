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

import com.projeto.codeinsights.application.knowledge.dto.AtualizarSnippetInput;
import com.projeto.codeinsights.application.knowledge.dto.CriarSnippetInput;
import com.projeto.codeinsights.application.knowledge.dto.SnippetDTO;
import com.projeto.codeinsights.application.knowledge.usecase.AtualizarSnippetUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.BuscarSnippetDetalheUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.CriarSnippetUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.ListarMeusSnippetsUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.RemoverSnippetUseCase;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.infrastructure.config.security.CurrentUserId;
import com.projeto.codeinsights.infrastructure.web.knowledge.dto.AtualizarSnippetRequest;
import com.projeto.codeinsights.infrastructure.web.knowledge.dto.CriarSnippetRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/snippets")
@RequiredArgsConstructor
public class SnippetController {

    private final CriarSnippetUseCase criarSnippetUseCase;
    private final ListarMeusSnippetsUseCase listarMeusSnippetsUseCase;
    private final BuscarSnippetDetalheUseCase buscarSnippetDetalheUseCase;
    private final AtualizarSnippetUseCase atualizarSnippetUseCase;
    private final RemoverSnippetUseCase removerSnippetUseCase;

    @PostMapping
    public ResponseEntity<SnippetDTO> criar(@CurrentUserId UUID autorId,
            @RequestBody @Valid CriarSnippetRequest request) {
        CriarSnippetInput input = new CriarSnippetInput(
                autorId,
                request.codigo(),
                request.descricao(),
                request.categoria());
        SnippetDTO snippet = criarSnippetUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(snippet);
    }

    @GetMapping
    public ResponseEntity<Pagina<SnippetDTO>> listarMeus(@CurrentUserId UUID autorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(listarMeusSnippetsUseCase.execute(autorId, pagina, tamanho));
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<SnippetDTO> buscarDetalhe(@CurrentUserId UUID solicitanteId,
            @PathVariable UUID snippetId) {
        return ResponseEntity.ok(buscarSnippetDetalheUseCase.execute(snippetId, solicitanteId));
    }

    @PatchMapping("/{snippetId}")
    public ResponseEntity<Void> atualizar(@CurrentUserId UUID solicitanteId,
            @PathVariable UUID snippetId,
            @RequestBody @Valid AtualizarSnippetRequest request) {
        AtualizarSnippetInput input = new AtualizarSnippetInput(
                snippetId,
                solicitanteId,
                request.codigo(),
                request.descricao(),
                request.categoria());
        atualizarSnippetUseCase.execute(input);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{snippetId}")
    public ResponseEntity<Void> remover(@CurrentUserId UUID solicitanteId,
            @PathVariable UUID snippetId) {
        removerSnippetUseCase.execute(snippetId, solicitanteId);
        return ResponseEntity.noContent().build();
    }
}
