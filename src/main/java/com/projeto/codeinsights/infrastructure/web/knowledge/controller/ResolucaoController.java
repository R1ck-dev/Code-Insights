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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.knowledge.dto.AtualizarResolucaoInput;
import com.projeto.codeinsights.application.knowledge.dto.ResolucaoDetalheDTO;
import com.projeto.codeinsights.application.knowledge.dto.ResolucaoResumoDTO;
import com.projeto.codeinsights.application.knowledge.dto.SubmeterResolucaoInput;
import com.projeto.codeinsights.application.knowledge.usecase.AtualizarResolucaoUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.BuscarResolucaoDetalheUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.ListarResolucoesDoDesafioUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.RemoverResolucaoUseCase;
import com.projeto.codeinsights.application.knowledge.usecase.SubmeterResolucaoUseCase;
import com.projeto.codeinsights.domain.shared.Pagina;
import com.projeto.codeinsights.infrastructure.config.security.CurrentUserId;
import com.projeto.codeinsights.infrastructure.web.knowledge.dto.AtualizarResolucaoRequest;
import com.projeto.codeinsights.infrastructure.web.knowledge.dto.SubmeterResolucaoRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ResolucaoController {

    private final SubmeterResolucaoUseCase submeterResolucaoUseCase;
    private final ListarResolucoesDoDesafioUseCase listarResolucoesDoDesafioUseCase;
    private final BuscarResolucaoDetalheUseCase buscarResolucaoDetalheUseCase;
    private final AtualizarResolucaoUseCase atualizarResolucaoUseCase;
    private final RemoverResolucaoUseCase removerResolucaoUseCase;

    @PostMapping("/api/desafios/{desafioId}/resolucoes")
    public ResponseEntity<ResolucaoResumoDTO> submeter(
            @PathVariable UUID desafioId,
            @CurrentUserId UUID usuarioId,
            @RequestBody @Valid SubmeterResolucaoRequest request) {
        ResolucaoResumoDTO resumo = submeterResolucaoUseCase.execute(new SubmeterResolucaoInput(
                desafioId,
                usuarioId,
                request.linguagem(),
                request.codigoFonte()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resumo);
    }

    @GetMapping("/api/desafios/{desafioId}/resolucoes")
    public ResponseEntity<Pagina<ResolucaoResumoDTO>> listar(
            @PathVariable UUID desafioId,
            @CurrentUserId UUID usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(
                listarResolucoesDoDesafioUseCase.execute(desafioId, usuarioId, pagina, tamanho));
    }

    @GetMapping("/api/resolucoes/{resolucaoId}")
    public ResponseEntity<ResolucaoDetalheDTO> buscarDetalhe(
            @PathVariable UUID resolucaoId,
            @CurrentUserId UUID usuarioId) {
        return ResponseEntity.ok(buscarResolucaoDetalheUseCase.execute(resolucaoId, usuarioId));
    }

    @PatchMapping("/api/resolucoes/{resolucaoId}")
    public ResponseEntity<Void> atualizar(
            @PathVariable UUID resolucaoId,
            @CurrentUserId UUID usuarioId,
            @RequestBody @Valid AtualizarResolucaoRequest request) {
        atualizarResolucaoUseCase.execute(new AtualizarResolucaoInput(
                resolucaoId,
                usuarioId,
                request.linguagem(),
                request.codigoFonte()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/resolucoes/{resolucaoId}")
    public ResponseEntity<Void> remover(
            @PathVariable UUID resolucaoId,
            @CurrentUserId UUID usuarioId) {
        removerResolucaoUseCase.execute(resolucaoId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
