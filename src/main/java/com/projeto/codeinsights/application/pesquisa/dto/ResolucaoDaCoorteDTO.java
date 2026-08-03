package com.projeto.codeinsights.application.pesquisa.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * Uma resolucao da coorte como ela sai da plataforma: com {@code pseudonimo} no lugar do autor.
 * <p>
 * O {@code autorId} <b>nao</b> aparece aqui, e a ausencia e o ponto. A projecao de dominio carrega
 * o id real; e nesta fronteira que ele para. Quem precisa saber de quem e a linha pede a revelacao
 * explicitamente, num endpoint proprio — assim identificar e um ato, e nao o padrao.
 */
public record ResolucaoDaCoorteDTO(
        UUID resolucaoId,
        String pseudonimo,
        UUID desafioId,
        String desafioTitulo,
        LinguagemProgramacao linguagem,
        int indiceAutonomiaIA,
        boolean analisada,
        /** Ex.: {@code "O(n log n)"}, ou {@code "?"} quando {@code tempoOrdem == -1}. */
        String tempoRotulo,
        /** Ordem da classe: 0..7 na escala; {@code -1} = desconhecido; {@code null} = sem dado. */
        Integer tempoOrdem,
        NivelConfianca confiancaTempo,
        String espacoRotulo,
        Integer espacoOrdem,
        NivelConfianca confiancaEspaco,
        Integer ciclomatica,
        OffsetDateTime submetidaEm,
        /**
         * Quando o motor gravou esta metrica. Nulo quando nao ha metrica — mesmo conjunto de linhas
         * em que {@code tempoOrdem} e nulo. E o carimbo que torna uma extracao reproduzivel: duas
         * leituras da mesma resolucao com instantes diferentes provam que houve reanalise no meio.
         */
        OffsetDateTime analisadoEm) {
}
