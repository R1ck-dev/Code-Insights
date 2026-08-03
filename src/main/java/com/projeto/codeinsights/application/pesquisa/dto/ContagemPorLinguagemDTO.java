package com.projeto.codeinsights.application.pesquisa.dto;

import java.util.Set;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;

/**
 * Quantas resolucoes ha em cada linguagem, e o que o motor sabe medir nela.
 * <p>
 * Vem do backend, e nao de uma constante no front, porque e a porta {@code AnalisadorMetricas} que
 * sabe a resposta. Quando uma linguagem nova entrar, a tela passa a dizer a verdade sem que ninguem
 * se lembre de editar o front.
 * <p>
 * {@code metricasSuportadas} existe porque o suporte e <b>parcial</b>: C tem ciclomatica e nao tem
 * Big-O. Um booleano sozinho obrigaria a escolher entre duas mentiras — dizer "sem analisador" para
 * uma linguagem que e analisada, ou dizer "com analisador" ao lado de uma cobertura de classe de
 * tempo igual a zero, sem nada explicando a contradicao.
 *
 * @param comAnalisador ha algum analisador — derivado, e mantido para a tela nao precisar saber a
 *        regra
 */
public record ContagemPorLinguagemDTO(LinguagemProgramacao linguagem, int total,
        boolean comAnalisador, Set<TipoMetrica> metricasSuportadas) {

    public ContagemPorLinguagemDTO(LinguagemProgramacao linguagem, int total,
            Set<TipoMetrica> metricasSuportadas) {
        this(linguagem, total, !metricasSuportadas.isEmpty(), metricasSuportadas);
    }
}
