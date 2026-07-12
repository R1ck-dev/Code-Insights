package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

/**
 * Resumo de uma {@code Resolucao} para listas (as resolucoes de um desafio).
 * <p>
 * Carrega a complexidade de <b>tempo</b> — e so ela — porque e nessa lista que o
 * aluno compara suas tentativas lado a lado; a trajetoria de amadurecimento
 * algoritmico e o dado central da pesquisa, e uma lista sem Big O a esconde. As
 * demais metricas (espaco, ciclomatica) ficam no {@code ResolucaoDetalheDTO} /
 * {@code ResultadoMetricaDTO}, onde ha espaco para o retrato completo.
 * <p>
 * Os campos de metrica sao nulaveis, e cada estado significa uma coisa distinta:
 * <ul>
 *   <li><b>{@code null}</b> — <i>nao ha dado</i>: a analise ainda nao rodou
 *       ({@code analisada == false}) ou a linguagem nao tem analisador (hoje so
 *       Java produz metricas de complexidade).</li>
 *   <li><b>{@code tempoOrdem == -1}</b> — o motor rodou e <i>nao classificou</i>
 *       ({@code ClasseComplexidade.DESCONHECIDO}, rotulo {@code "?"}). E
 *       semanticamente DIFERENTE de {@code null}.</li>
 *   <li><b>{@code tempoOrdem == 0}</b> — {@code O(1)}: uma complexidade legitima,
 *       a melhor delas. Nunca e sentinela de "vazio".</li>
 * </ul>
 * {@code confiancaTempo} e a confianca do MOTOR na propria estimativa; nao e o
 * eixo medido x estimado — Big O e <i>sempre</i> estimado.
 */
public record ResolucaoResumoDTO(
        UUID id,
        UUID desafioId,
        UUID autorId,
        LinguagemProgramacao linguagem,
        int indiceAutonomiaIA,
        Visibilidade visibilidade,
        boolean analisada,
        /** Ex.: {@code "O(n log n)"}, ou {@code "?"} quando {@code tempoOrdem == -1}. */
        String tempoRotulo,
        /** Ordem da classe: 0..7 na escala; {@code -1} = desconhecido; {@code null} = sem dado. */
        Integer tempoOrdem,
        NivelConfianca confiancaTempo,
        OffsetDateTime submetidaEm) {
}
