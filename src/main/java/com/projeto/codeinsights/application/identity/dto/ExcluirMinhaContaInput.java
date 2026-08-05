package com.projeto.codeinsights.application.identity.dto;

import java.util.UUID;

/**
 * @param senhaAtual confirmacao de identidade. A exclusao e irreversivel e o token JWT sozinho nao
 *                   basta: um navegador esquecido aberto nao pode ser suficiente para apagar meses
 *                   de trabalho de alguem.
 */
public record ExcluirMinhaContaInput(UUID usuarioId, String senhaAtual) {
}
