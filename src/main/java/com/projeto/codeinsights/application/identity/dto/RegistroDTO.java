package com.projeto.codeinsights.application.identity.dto;

/**
 * Resultado do registro. O campo existe para o front saber o que de fato aconteceu em vez de
 * presumir: com provedor de e-mail configurado a conta nasce pendente e o aluno precisa abrir o
 * link; sem provedor, ela ja nasce ativa e ele pode entrar direto.
 * <p>
 * Devolver isso — em vez de fixar o comportamento na tela — significa que religar o e-mail um dia
 * e so trocar uma variavel de ambiente: a interface acompanha sozinha.
 */
public record RegistroDTO(boolean precisaAtivarPorEmail) {
}
