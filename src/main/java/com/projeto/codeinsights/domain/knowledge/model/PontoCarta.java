package com.projeto.codeinsights.domain.knowledge.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.shared.enums.Visibilidade;

/**
 * Value object: uma {@code Resolucao} vista como um ponto da carta celeste — o
 * plano (Indice de Autonomia IA x classe de Big O de tempo) do autor.
 * <p>
 * Os campos de metrica sao nulaveis: so existem apos a analise estatica e, mesmo
 * entao, uma metrica pode faltar (ex.: linguagem sem analisador). Nulo, e nao
 * zero — zero e uma complexidade legitima ({@code O(1)}, ordem 0), e confundi-lo
 * com "sem dado" corromperia a carta. As ordens seguem {@code ClasseComplexidade}:
 * 0..7 na escala e {@code -1} para {@code DESCONHECIDO} (rotulo {@code "?"}).
 */
public record PontoCarta(
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
