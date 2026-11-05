package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.projeto.codeinsights.infrastructure.metrica.ReducaoAoEscala.Metrica;
import com.projeto.codeinsights.infrastructure.metrica.ReducaoAoEscala.Reducao;

/** A tabela fechada do Adendo 6, item 2: cinco reducoes, com trava no texto do gabarito. */
class ReducaoAoEscalaTest {

    @ParameterizedTest(name = "{0} {1}: {2} vira {3}")
    @CsvSource(delimiter = '|', value = {
            "021-leetcode-3022.java | TEMPO  | O(n * B)    | O(n)",
            "049-leetcode-1334.c    | TEMPO  | O(n^3 + e)  | O(n^3)",
            "315-leetcode-2225.c    | TEMPO  | O(m log m)  | O(n log n)",
            "315-leetcode-2225.c    | ESPACO | O(m)        | O(n)",
            "373-leetcode-392.c     | TEMPO  | O(m)        | O(n)"
    })
    void aplicaAsCincoReducoesDoAdendo(String arquivo, Metrica metrica, String gabarito, String nota) {
        assertThat(ReducaoAoEscala.nota(arquivo, metrica, gabarito)).isEqualTo(nota);
    }

    @Test
    void tabelaTemExatamenteCincoEntradas() {
        assertThat(ReducaoAoEscala.tabela()).hasSize(5);
    }

    @Test
    void todaNotaDaTabelaEstaNaEscalaDoMotor() {
        assertThat(ReducaoAoEscala.tabela())
                .extracting(Reducao::nota)
                .allMatch(CasoDeCorpus::estaNaEscala);
    }

    @Test
    void todoGabaritoDaTabelaEstaForaDaEscala() {
        assertThat(ReducaoAoEscala.tabela())
                .extracting(Reducao::gabarito)
                .noneMatch(CasoDeCorpus::estaNaEscala);
    }

    @Test
    void foraDaTabelaANotaEOGabarito() {
        assertThat(ReducaoAoEscala.nota("005-leetcode-3479.java", Metrica.TEMPO, "O(n log n)"))
                .isEqualTo("O(n log n)");
        assertThat(ReducaoAoEscala.nota("037-leetcode-1482.java", Metrica.TEMPO, "O(n log D)"))
                .isEqualTo("O(n log D)");
    }

    @Test
    void aReducaoValeSoParaAMetricaDaTabela() {
        assertThat(ReducaoAoEscala.nota("021-leetcode-3022.java", Metrica.ESPACO, "O(1)")).isEqualTo("O(1)");
        assertThat(ReducaoAoEscala.nota("373-leetcode-392.c", Metrica.ESPACO, "O(1)")).isEqualTo("O(1)");
    }

    @Test
    void textoDeGabaritoDiferenteDaTabelaFalhaEmVezDeReduzir() {
        assertThatThrownBy(() -> ReducaoAoEscala.nota("021-leetcode-3022.java", Metrica.TEMPO, "O(n)"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("021-leetcode-3022.java")
                .hasMessageContaining("Adendo 6");
        assertThatThrownBy(() -> ReducaoAoEscala.nota("373-leetcode-392.c", Metrica.TEMPO, "O(m + n)"))
                .isInstanceOf(IllegalStateException.class);
    }
}
