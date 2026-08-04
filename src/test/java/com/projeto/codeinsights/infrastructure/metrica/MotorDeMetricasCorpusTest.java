package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.infrastructure.metrica.MedidorDoCorpus.Medicao;
import com.projeto.codeinsights.infrastructure.metrica.c.CodigoDeC;

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

    private static final Set<String> EXTENSOES = Set.of(".java", ".c");

    static List<CasoDeCorpus> casos() {
        return CorpusDeAlgoritmos.casos();
    }

    @ParameterizedTest(name = "tempo: {0}")
    @MethodSource("casos")
    void tempoBateComOComportamentoAtualDoMotor(CasoDeCorpus caso) {
        Medicao medicao = MedidorDoCorpus.medir(caso);

        assertThat(medicao.tempo())
                .as("%s (gabarito %s)", caso, caso.tempoGabarito())
                .isEqualTo(caso.tempoEsperadoDoMotor());
    }

    @ParameterizedTest(name = "espaco: {0}")
    @MethodSource("casos")
    void espacoBateComOComportamentoAtualDoMotor(CasoDeCorpus caso) {
        assumeTrue(caso.espacoEsperadoDoMotor() != null, "caso sem gabarito de espaco");

        Medicao medicao = MedidorDoCorpus.medir(caso);

        assertThat(medicao.espaco())
                .as("%s (gabarito %s)", caso, caso.espacoGabarito())
                .isEqualTo(caso.espacoEsperadoDoMotor());
    }

    /**
     * Todo caso precisa ser <b>lido</b> pelo motor da sua linguagem. Um caso que o parser recusa
     * ainda produz um rotulo ({@code ?}) e passaria pelos testes acima sem que ninguem notasse que
     * o corpus deixou de exercitar o avaliador de custo.
     */
    @ParameterizedTest(name = "estrutura lida: {0}")
    @MethodSource("casos")
    void todoCasoDoCorpusEhLidoPeloMotor(CasoDeCorpus caso) {
        if (caso.linguagem() == LinguagemProgramacao.C) {
            CodigoDeC codigo = CodigoDeC.de(caso.codigo());
            assertThat(codigo.programa().integro())
                    .as("o parser estrutural de C desistiu de %s: %s", caso.arquivo(), codigo.programa().motivo())
                    .isTrue();
            return;
        }
        assertThat(AnalisadorTestSupport.parse(caso.codigo())).isNotNull();
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
                    .map(caminho -> RAIZ_DO_CORPUS.relativize(caminho).toString().replace('\\', '/'))
                    .filter(caminho -> EXTENSOES.stream().anyMatch(caminho::endsWith))
                    .collect(Collectors.toSet());

            assertThat(noDisco).isEqualTo(noManifesto);
        }
    }

    /**
     * O corpus so mede o que o analisador entrega. Se uma metrica for acrescentada a C — ou
     * retirada —, {@link MedidorDoCorpus} precisa ser revisto junto, e este teste e o que avisa.
     */
    @Test
    void oAnalisadorDeCEntregaAsTresMetricasDoProjeto() {
        assertThat(MedidorDoCorpus.analisadorDe(LinguagemProgramacao.C).metricasSuportadas())
                .containsExactlyInAnyOrder(TipoMetrica.BIG_O_TEMPO, TipoMetrica.COMPLEXIDADE_ESPACO,
                        TipoMetrica.COMPLEXIDADE_CICLOMATICA);
    }
}
