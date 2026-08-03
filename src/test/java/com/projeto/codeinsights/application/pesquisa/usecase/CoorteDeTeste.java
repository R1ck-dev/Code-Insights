package com.projeto.codeinsights.application.pesquisa.usecase;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;

/**
 * Fabrica de {@link ResolucaoDaCoorte} para os testes de pesquisa. Sao 16 componentes, e escrever
 * os 16 em cada caso esconderia o unico campo que o teste esta de fato exercitando.
 */
final class CoorteDeTeste {

    static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-08-01T10:00:00Z");
    /** Depois da submissao, e diferente dela: iguais esconderiam a troca entre as duas datas. */
    static final OffsetDateTime ANALISADO_EM = OffsetDateTime.parse("2026-08-02T15:30:00Z");

    private CoorteDeTeste() {
    }

    /** Resolucao Java analisada com as tres metricas — o caso saudavel. */
    static ResolucaoDaCoorte analisada(UUID autorId, int autonomia) {
        return new ResolucaoDaCoorte(UUID.randomUUID(), autorId, UUID.randomUUID(), "Two Sum",
                LinguagemProgramacao.JAVA, autonomia, true,
                "O(n^2)", 4, NivelConfianca.ALTA,
                "O(1)", 0, NivelConfianca.MEDIA,
                7, AGORA, ANALISADO_EM);
    }

    /** Submetida e ainda na fila do motor. */
    static ResolucaoDaCoorte aguardando(UUID autorId) {
        return semMetrica(autorId, LinguagemProgramacao.JAVA, false);
    }

    /** O motor rodou e nao produziu metrica: o codigo nao parseou. */
    static ResolucaoDaCoorte falhaDeAnalise(UUID autorId) {
        return semMetrica(autorId, LinguagemProgramacao.JAVA, true);
    }

    /** Linguagem sem analisador — sai analisada e sem metrica, igual a falha, e nao e falha. */
    static ResolucaoDaCoorte semAnalisador(UUID autorId, LinguagemProgramacao linguagem) {
        return semMetrica(autorId, linguagem, true);
    }

    private static ResolucaoDaCoorte semMetrica(UUID autorId, LinguagemProgramacao linguagem, boolean analisada) {
        return new ResolucaoDaCoorte(UUID.randomUUID(), autorId, UUID.randomUUID(), "Two Sum",
                linguagem, 3, analisada,
                null, null, null,
                null, null, null,
                null, AGORA, null);
    }
}
