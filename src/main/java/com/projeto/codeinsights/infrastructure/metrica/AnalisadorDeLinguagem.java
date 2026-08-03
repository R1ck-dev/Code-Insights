package com.projeto.codeinsights.infrastructure.metrica;

import java.util.List;
import java.util.Set;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;

/**
 * O motor de UMA linguagem. Cada implementacao e um bean, e {@link MotorDeMetricas} escolhe entre
 * elas — e assim o motor cresce por adicao, como o projeto exige.
 * <p>
 * Deliberadamente <b>nao</b> estende {@code AnalisadorMetricas}, apesar de ter a mesma forma. Se
 * estendesse, cada analisador de linguagem seria tambem um bean da porta, e o
 * {@code AnalisarResolucaoUseCase} — que injeta a porta por tipo — voltaria a ter duas candidatas e
 * quebraria no startup. Os dois papeis sao distintos: a porta e "o motor" (um, para a aplicacao),
 * esta interface e "o motor de uma linguagem" (varios, para o composite).
 */
public interface AnalisadorDeLinguagem {

    LinguagemProgramacao linguagem();

    /** O que este analisador sabe calcular. Ver {@code AnalisadorMetricas.metricasSuportadas}. */
    Set<TipoMetrica> metricasSuportadas();

    List<ResultadoMetrica> analisar(Resolucao resolucao);
}
