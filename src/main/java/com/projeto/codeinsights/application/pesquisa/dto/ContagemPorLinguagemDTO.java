package com.projeto.codeinsights.application.pesquisa.dto;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Quantas resolucoes ha em cada linguagem, e se existe analisador para ela.
 * <p>
 * O {@code comAnalisador} vem do backend, e nao de uma constante no front, porque e a porta
 * {@code AnalisadorMetricas} que sabe a resposta. Quando o motor Python entrar, a tela passa a
 * dizer a verdade sem que ninguem se lembre de editar o front.
 */
public record ContagemPorLinguagemDTO(LinguagemProgramacao linguagem, int total, boolean comAnalisador) {
}
