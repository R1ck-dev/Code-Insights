package com.projeto.codeinsights.domain.knowledge.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Value object: uma entrada da atividade recente do autor no dashboard — uma
 * {@code Resolucao} com o titulo do desafio e a classe de Big O de tempo (se ja
 * analisada e calculavel). {@code complexidadeRotulo}/{@code complexidadeOrdem}
 * sao null quando a resolucao ainda nao foi analisada ou nao gerou metrica de
 * tempo (ex.: linguagem sem analisador).
 */
public record AtividadeRecente(
        UUID resolucaoId,
        UUID desafioId,
        String desafioTitulo,
        LinguagemProgramacao linguagem,
        int indiceAutonomiaIA,
        boolean analisada,
        String complexidadeRotulo,
        Integer complexidadeOrdem,
        OffsetDateTime submetidaEm) {
}
