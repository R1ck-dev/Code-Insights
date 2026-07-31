package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.javaparser.ast.CompilationUnit;

/**
 * Rede de regressao do motor: cada caso do corpus assere contra o que o motor
 * <b>responde hoje</b>, nao contra o gabarito da literatura.
 * <p>
 * A diferenca e proposital. Se a assercao fosse contra o gabarito, os casos que o motor
 * sabidamente erra (recursao mutua, O(raiz de n), complexidades multivariaveis) deixariam
 * a suite vermelha para sempre — e a saida seria remover esses casos do corpus, que e
 * exatamente o vies que a validacao existe para eliminar. Aqui eles ficam, presos ao
 * comportamento atual: se o motor mudar de resposta, este teste falha e a mudanca precisa
 * ser deliberada.
 * <p>
 * Quanto o motor <b>acerta</b> e medido por {@link RelatorioDeAcuraciaTest}, que compara
 * contra o gabarito e trata divergencia como dado, nao como falha.
 */
class MotorDeMetricasCorpusTest {

    private static final Path RAIZ_DO_CORPUS = Path.of("src", "test", "resources", "corpus");

    private final BigOTempoAnalisador tempo = new BigOTempoAnalisador();
    private final EspacoAnalisador espaco = new EspacoAnalisador();

    static List<CasoDeCorpus> casos() {
        return CorpusDeAlgoritmos.casos();
    }

    @ParameterizedTest(name = "tempo: {0}")
    @MethodSource("casos")
    void tempoBateComOComportamentoAtualDoMotor(CasoDeCorpus caso) {
        CompilationUnit unidade = AnalisadorTestSupport.parse(caso.codigo());
        MetricaCalculada calculada = tempo.analisar(unidade);

        assertThat(calculada.rotulo())
                .as("%s (gabarito %s) -> %s", caso.nome(), caso.tempoGabarito(), calculada.detalhe())
                .isEqualTo(caso.tempoEsperadoDoMotor());
    }

    @ParameterizedTest(name = "espaco: {0}")
    @MethodSource("casos")
    void espacoBateComOComportamentoAtualDoMotor(CasoDeCorpus caso) {
        assumeTrue(caso.espacoEsperadoDoMotor() != null, "caso sem gabarito de espaco");

        CompilationUnit unidade = AnalisadorTestSupport.parse(caso.codigo());
        MetricaCalculada calculada = espaco.analisar(unidade);

        assertThat(calculada.rotulo())
                .as("%s (gabarito %s) -> %s", caso.nome(), caso.espacoGabarito(), calculada.detalhe())
                .isEqualTo(caso.espacoEsperadoDoMotor());
    }

    @Test
    void todoCasoDoCorpusParseia() {
        assertThatCode(() -> casos().forEach(caso -> AnalisadorTestSupport.parse(caso.codigo())))
                .doesNotThrowAnyException();
    }

    /**
     * Guarda contra dessincronizacao: um arquivo adicionado a {@code resources/corpus/} sem entrada
     * no manifesto seria carregado por ninguem e passaria despercebido — um caso de validacao que
     * existe no disco mas nao e medido.
     */
    @Test
    void todoArquivoDoCorpusTemEntradaNoManifesto() throws IOException {
        Set<String> noManifesto = casos().stream().map(CasoDeCorpus::arquivo).collect(Collectors.toSet());

        try (Stream<Path> arquivos = Files.walk(RAIZ_DO_CORPUS)) {
            Set<String> noDisco = arquivos
                    .filter(caminho -> caminho.toString().endsWith(".java"))
                    .map(caminho -> RAIZ_DO_CORPUS.relativize(caminho).toString().replace('\\', '/'))
                    .collect(Collectors.toSet());

            assertThat(noDisco).isEqualTo(noManifesto);
        }
    }
}
