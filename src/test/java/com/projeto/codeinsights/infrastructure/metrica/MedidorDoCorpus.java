package com.projeto.codeinsights.infrastructure.metrica;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.model.Resolucao;
import com.projeto.codeinsights.domain.knowledge.model.ResultadoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.c.AnalisadorDeC;
import com.projeto.codeinsights.infrastructure.metrica.c.BigOTempoDeCAnalisador;
import com.projeto.codeinsights.infrastructure.metrica.c.CiclomaticaDeCAnalisador;
import com.projeto.codeinsights.infrastructure.metrica.c.EspacoDeCAnalisador;

/**
 * Mede um caso do corpus <b>pelo mesmo caminho que a producao usa</b>: o analisador da linguagem,
 * inteiro, e nao o avaliador de custo isolado.
 * <p>
 * A diferenca importa em C, onde {@code AnalisadorDeC} aplica o teto de confianca MEDIA. Medir o
 * avaliador direto mostraria uma confianca que a plataforma nunca grava, e a tabela "acerto por
 * confianca declarada" do relatorio — que existe para decidir se da para filtrar a amostra da
 * pesquisa por confianca — descreveria um motor que nao e o que roda.
 */
final class MedidorDoCorpus {

    record Medicao(String tempo, NivelConfianca confiancaTempo, String espaco, NivelConfianca confiancaEspaco) {
    }

    private static final AnalisadorDeLinguagem JAVA = new JavaParserAnalisadorMetricas(
            List.of(new BigOTempoAnalisador(), new EspacoAnalisador(), new CiclomaticaAnalisador()));

    private static final AnalisadorDeLinguagem C = new AnalisadorDeC(
            List.of(new BigOTempoDeCAnalisador(), new EspacoDeCAnalisador(), new CiclomaticaDeCAnalisador()));

    private static final UUID AUTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID DESAFIO = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    /** Fora da escala do motor: o que ele responde quando nao gerou a metrica de todo. */
    private static final String AUSENTE = "-";

    private MedidorDoCorpus() {
    }

    static Medicao medir(CasoDeCorpus caso) {
        Map<TipoMetrica, ResultadoMetrica> porTipo = new EnumMap<>(TipoMetrica.class);
        analisadorDe(caso.linguagem()).analisar(resolucaoDe(caso))
                .forEach(resultado -> porTipo.put(resultado.getTipo(), resultado));

        ResultadoMetrica tempo = porTipo.get(TipoMetrica.BIG_O_TEMPO);
        ResultadoMetrica espaco = porTipo.get(TipoMetrica.COMPLEXIDADE_ESPACO);
        return new Medicao(rotuloDe(tempo), confiancaDe(tempo), rotuloDe(espaco), confiancaDe(espaco));
    }

    static AnalisadorDeLinguagem analisadorDe(LinguagemProgramacao linguagem) {
        return linguagem == LinguagemProgramacao.C ? C : JAVA;
    }

    private static Resolucao resolucaoDe(CasoDeCorpus caso) {
        return new Resolucao(UUID.randomUUID(), AUTOR, DESAFIO, caso.codigo(), caso.linguagem(), 1, null);
    }

    private static String rotuloDe(ResultadoMetrica resultado) {
        return resultado == null ? AUSENTE : resultado.getRotulo();
    }

    private static NivelConfianca confiancaDe(ResultadoMetrica resultado) {
        return resultado == null ? NivelConfianca.BAIXA : resultado.getConfianca();
    }
}
