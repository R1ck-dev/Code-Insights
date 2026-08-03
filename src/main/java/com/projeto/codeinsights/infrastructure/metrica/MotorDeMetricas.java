package com.projeto.codeinsights.infrastructure.metrica;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.AnalisadorMetricas;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.extern.slf4j.Slf4j;

/**
 * A porta de analise vista pela aplicacao: escolhe o analisador da linguagem e delega.
 * <p>
 * Ate aqui a porta era implementada direto pelo analisador de Java, e o javadoc dele prometia
 * crescer "por adicao" — mas um segundo bean da porta teria quebrado a injecao por tipo no
 * {@code AnalisarResolucaoUseCase}. Este composite e o que torna a promessa verdadeira: acrescentar
 * uma linguagem agora e escrever um {@link AnalisadorDeLinguagem} e nada mais.
 */
@Component
@Slf4j
public class MotorDeMetricas implements AnalisadorMetricas {

    private final Map<LinguagemProgramacao, AnalisadorDeLinguagem> porLinguagem =
            new EnumMap<>(LinguagemProgramacao.class);

    /**
     * Duas implementacoes para a mesma linguagem fariam uma vencer em silencio, conforme a ordem em
     * que o Spring listasse os beans — e o corpus passaria a ser medido por um motor diferente do
     * que se pensa. Falhar no startup e a unica forma de isso nao passar despercebido.
     */
    public MotorDeMetricas(List<AnalisadorDeLinguagem> analisadores) {
        for (AnalisadorDeLinguagem analisador : analisadores) {
            AnalisadorDeLinguagem anterior = porLinguagem.put(analisador.linguagem(), analisador);
            if (anterior != null) {
                throw new NegocioException(
                        "Ha dois analisadores registrados para %s: %s e %s.".formatted(
                                analisador.linguagem(),
                                anterior.getClass().getSimpleName(),
                                analisador.getClass().getSimpleName()));
            }
        }
        log.info("Motor de metricas com {} linguagem(ns): {}", porLinguagem.size(), porLinguagem.keySet());
    }

    @Override
    public Set<TipoMetrica> metricasSuportadas(LinguagemProgramacao linguagem) {
        AnalisadorDeLinguagem analisador = porLinguagem.get(linguagem);
        return analisador == null ? Set.of() : analisador.metricasSuportadas();
    }

    @Override
    public List<ResultadoMetrica> analisar(Resolucao resolucao) {
        AnalisadorDeLinguagem analisador = porLinguagem.get(resolucao.getLinguagem());
        if (analisador == null) {
            log.info("Sem analisador para {} (resolucao {}).", resolucao.getLinguagem(), resolucao.getId());
            return List.of();
        }
        return analisador.analisar(resolucao);
    }
}
