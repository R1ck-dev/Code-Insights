package com.projeto.codeinsights.infrastructure.web.pesquisa.dto;

import jakarta.validation.constraints.NotNull;

/**
 * O campo e {@code Boolean} e nao {@code boolean}: com o primitivo, um corpo sem o campo chegaria
 * como {@code false} — uma recusa que ninguem manifestou. Aqui a ausencia e rejeitada pelo
 * {@code @NotNull}, que e o unico tratamento correto para "nao sei o que a pessoa quis dizer".
 */
public record DecisaoDeConsentimentoRequest(
        @NotNull(message = "Informe se voce autoriza o uso das suas submissoes na pesquisa.")
        Boolean autoriza) {
}
