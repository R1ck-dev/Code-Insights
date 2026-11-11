package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.infrastructure.metrica.MedidorDoCorpus.Medicao;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * Rede de seguranca dos totais do corpus proprio: eles sao a base do veredito de H3 (Adendo 6, item 6)
 * e nao podem mudar sem que alguem perceba. As linhas vem do relatorio renderizado, nao de uma conta
 * paralela, para que o teste pegue tambem uma mudanca no jeito de contar.
 */
class TotaisDoProprioTest {

    private static final String AVISO = " Se o motor foi corrigido de proposito numa issue futura, a falha e esperada: "
            + "atualize a base do Adendo 6 (54/55 e 37/40 no tempo) e o veredito junto, com o motivo registrado.";

    private static final LocalDate DATA_FIXA = LocalDate.of(2026, 11, 5);

    @Test
    void totaisPorLinguagemSaoABaseCongelada() {
        List<String> linhas = linhasDoRelatorio();

        assertThat(linhas)
                .as("resumo por linguagem." + AVISO)
                .contains("| **JAVA** | 59 | 54/55 (98.2%) | 57/57 (100.0%) |",
                        "| **C** | 40 | 37/40 (92.5%) | 40/40 (100.0%) |");
    }

    @Test
    void totaisPorCategoriaSaoABaseCongelada() {
        List<String> linhas = linhasDoRelatorio();

        assertThat(linhas)
                .as("acuracia por categoria." + AVISO)
                .contains("| JAVA | `CANONICO` | 30 | 29/29 (100.0%) | 29/29 (100.0%) |",
                        "| JAVA | `ADVERSARIAL` | 15 | 11/12 (91.7%) | 14/14 (100.0%) |",
                        "| JAVA | `ALUNO` | 14 | 14/14 (100.0%) | 14/14 (100.0%) |",
                        "| C | `CANONICO` | 11 | 11/11 (100.0%) | 11/11 (100.0%) |",
                        "| C | `ADVERSARIAL` | 13 | 10/13 (76.9%) | 13/13 (100.0%) |",
                        "| C | `ALUNO` | 16 | 16/16 (100.0%) | 16/16 (100.0%) |");
    }

    @Test
    void resumoDeUmaLinhaConcordaComATabela() {
        String resumo = RelatorioDeAcuracia.resumoDeUmaLinha(medirOProprio());

        assertThat(resumo)
                .as("resumo de uma linha." + AVISO)
                .contains("JAVA 59 casos: tempo 54/55 (98.2%), espaco 57/57 (100.0%)")
                .contains("C 40 casos: tempo 37/40 (92.5%), espaco 40/40 (100.0%)");
    }

    private List<String> linhasDoRelatorio() {
        return RelatorioDeAcuracia.renderizar(medirOProprio(), DATA_FIXA).lines().toList();
    }

    private List<Medida> medirOProprio() {
        return CorpusDeAlgoritmos.casos().stream().map(TotaisDoProprioTest::medir).toList();
    }

    private static Medida medir(CasoDeCorpus caso) {
        Medicao medicao = MedidorDoCorpus.medir(caso);
        return new Medida(caso, medicao.tempo(), medicao.confiancaTempo(),
                medicao.espaco(), medicao.confiancaEspaco());
    }
}
