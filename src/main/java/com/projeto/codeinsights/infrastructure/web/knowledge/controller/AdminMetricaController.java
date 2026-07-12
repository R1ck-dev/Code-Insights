package com.projeto.codeinsights.infrastructure.web.knowledge.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.knowledge.dto.RelatorioReanaliseDTO;
import com.projeto.codeinsights.application.knowledge.usecase.ReanalisarResolucoesUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Area administrativa das metricas. Fica sob {@code /api/admin/**} — que o SecurityConfig ja
 * restringe a {@code hasRole("ADMIN")} — porque reprocessar o corpus reescreve metricas de
 * resolucoes de TODOS os alunos: e uma operacao de curadoria dos dados da pesquisa, nao uma acao
 * de aluno. Nao ha {@code @CurrentUserId} aqui de proposito: a operacao nao e sobre o portfolio de
 * quem chama.
 */
@RestController
@RequiredArgsConstructor
public class AdminMetricaController {

    private final ReanalisarResolucoesUseCase reanalisarResolucoesUseCase;

    /**
     * Recalcula, com o motor atual, as metricas derivadas do codigo (Big O de tempo, complexidade de
     * espaco, ciclomatica) das resolucoes ja gravadas. Sincrono: a resposta e o proprio relatorio da
     * passada, com o diff do que mudou.
     *
     * @param autorId opcional; ausente = corpus inteiro.
     */
    @PostMapping("/api/admin/metricas/reanalisar")
    public ResponseEntity<RelatorioReanaliseDTO> reanalisar(@RequestParam(required = false) UUID autorId) {
        return ResponseEntity.ok(reanalisarResolucoesUseCase.execute(autorId));
    }
}
