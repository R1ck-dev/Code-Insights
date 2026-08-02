package com.projeto.codeinsights.infrastructure.web.pesquisa.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.codeinsights.application.pesquisa.dto.MeuConsentimentoDTO;
import com.projeto.codeinsights.application.pesquisa.usecase.ObterMeuConsentimentoUseCase;
import com.projeto.codeinsights.application.pesquisa.usecase.RegistrarDecisaoDeConsentimentoUseCase;
import com.projeto.codeinsights.infrastructure.config.security.CurrentUserId;
import com.projeto.codeinsights.infrastructure.web.pesquisa.dto.DecisaoDeConsentimentoRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * O consentimento do proprio participante. Vive no contexto {@code pesquisa} porque e do que ele
 * trata, mas a rota e <b>{@code /api/consentimento}</b>, deliberadamente fora de
 * {@code /api/pesquisa/**}.
 * <p>
 * O motivo e concreto: {@code SecurityConfig} restringe {@code /api/pesquisa/**} a PESQUISADOR e
 * ADMIN. Quem responde ao termo e o aluno. Pendurar esta rota naquele prefixo daria 403 a todo
 * participante, e "consertar" isso exigiria uma excecao antes da regra geral — uma ordenacao fragil
 * que a proxima pessoa a mexer no SecurityConfig quebraria sem perceber. Prefixo diferente nao tem
 * como quebrar.
 * <p>
 * Toda operacao e sobre quem esta autenticado, via {@code @CurrentUserId}: nao ha como responder
 * pelo termo de outra pessoa, nem por engano.
 */
@RestController
@RequestMapping("/api/consentimento")
@RequiredArgsConstructor
public class ConsentimentoController {

    private final ObterMeuConsentimentoUseCase obterMeuConsentimentoUseCase;
    private final RegistrarDecisaoDeConsentimentoUseCase registrarDecisaoDeConsentimentoUseCase;

    /** O termo em vigor e a minha resposta a ele — ou {@code null} se eu ainda nao respondi. */
    @GetMapping
    public ResponseEntity<MeuConsentimentoDTO> meuConsentimento(@CurrentUserId UUID participanteId) {
        return ResponseEntity.ok(obterMeuConsentimentoUseCase.execute(participanteId));
    }

    /**
     * Registra aceite ou recusa. E a mesma rota para os dois porque retirar o consentimento nao pode
     * custar mais do que da-lo — nem em cliques, nem em codigo mal mantido de um caminho que quase
     * ninguem exercita.
     */
    @PostMapping
    public ResponseEntity<MeuConsentimentoDTO> decidir(@CurrentUserId UUID participanteId,
            @Valid @RequestBody DecisaoDeConsentimentoRequest request) {
        return ResponseEntity.ok(
                registrarDecisaoDeConsentimentoUseCase.execute(participanteId, request.autoriza()));
    }
}
