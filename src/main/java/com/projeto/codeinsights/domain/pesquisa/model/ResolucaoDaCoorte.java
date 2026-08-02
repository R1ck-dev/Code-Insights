package com.projeto.codeinsights.domain.pesquisa.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;

/**
 * Value object: uma resolucao vista de fora do portfolio de um aluno — a unidade de analise da
 * pesquisa. E a mesma leitura de {@code PontoCarta}, sem o filtro de autor e <b>com</b> o autor
 * como dimensao, porque o corte que interessa aqui e a coorte inteira.
 * <p>
 * Guarda o {@code autorId} real, e nao o pseudonimo: a pseudonimizacao acontece na fronteira de
 * saida (o caso de uso), nao na consulta. Assim a mesma projecao serve tanto a tela pseudonimizada
 * quanto a revelacao sob demanda, sem duas consultas.
 * <p>
 * Os campos de metrica sao nulaveis: so existem apos a analise estatica. <b>Nulo, e nao zero</b> —
 * zero e uma complexidade legitima ({@code O(1)}, ordem 0), e {@code -1} e o motor dizendo que
 * rodou e nao conseguiu classificar. Os tres estados sao distintos e a analise depende disso.
 */
public record ResolucaoDaCoorte(
        UUID resolucaoId,
        UUID autorId,
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
        NivelConfianca confiancaEspaco,
        Integer ciclomatica,
        OffsetDateTime submetidaEm) {

    /** A analise rodou e produziu numero — o unico estado em que a linha entra numa estatistica. */
    public boolean temMetrica() {
        return tempoOrdem != null;
    }
}
