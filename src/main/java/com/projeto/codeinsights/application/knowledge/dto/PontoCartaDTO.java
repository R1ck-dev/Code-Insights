package com.projeto.codeinsights.application.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

/**
 * Uma estrela da carta celeste: uma resolucao do autor posicionada por Indice de
 * Autonomia IA (eixo X, 1..5) e classe de Big O de tempo (eixo Y).
 * <p>
 * Os campos de metrica sao nulos enquanto {@code analisada = false}, e continuam
 * nulos se o motor nao gerou aquela metrica. {@code tempoOrdem}/{@code espacoOrdem}
 * vao de 0 a 7 na escala de complexidade, ou {@code -1} (rotulo {@code "?"}) quando
 * o motor nao conseguiu classificar.
 */
public record PontoCartaDTO(
        UUID resolucaoId,
        UUID desafioId,
        String desafioTitulo,
        LinguagemProgramacao linguagem,
        int indiceAutonomiaIA,
        boolean analisada,
        String tempoRotulo,
        Integer tempoOrdem,
        NivelConfianca confiancaTempo,
        String espacoRotulo,
        Integer espacoOrdem,
        Integer ciclomatica,
        Visibilidade visibilidade,
        OffsetDateTime submetidaEm) {
}
