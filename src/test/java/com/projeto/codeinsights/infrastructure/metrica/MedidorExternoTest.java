package com.projeto.codeinsights.infrastructure.metrica;

import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.C_VALIDO;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.JAVA_INVALIDO;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.JAVA_VALIDO;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.PADRAO;
import static com.projeto.codeinsights.infrastructure.metrica.CorpusExternoSintetico.TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.MedidaExterna.MotivoRecusa;
import com.projeto.codeinsights.infrastructure.metrica.MedidorDoCorpus.Medicao;

/**
 * O medidor do corpus externo, sobre fontes sinteticas: resposta, recusa por metrica ausente, por
 * excecao, por estouro de pilha e por tempo limite, e a prova de que nada do fonte vaza.
 */
class MedidorExternoTest {

    private static final Duration LIMITE_CURTO = Duration.ofMillis(200);

    private static FonteVerificada fonte(CorpusExternoSintetico.Arquivo arquivo, String codigo) {
        CasoExterno caso = GabaritoExterno.de(CorpusExternoSintetico.gabarito(PADRAO))
                .casos(arquivo.linguagem()).stream()
                .filter(c -> c.arquivo().equals(arquivo.nome())).findFirst().orElseThrow();
        return new FonteVerificada(caso, codigo);
    }

    private static MedidorExterno medidorQue(BiFunction<String, LinguagemProgramacao, Medicao> analise) {
        return new MedidorExterno(LIMITE_CURTO, analise);
    }

    @Test
    void javaValidoEResponditoPeloMotorSemRecusa() {
        MedidaExterna medida = new MedidorExterno().medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO));

        assertThat(medida.tempo()).isEqualTo("O(n)");
        assertThat(medida.espaco()).isEqualTo("O(1)");
        assertThat(medida.recusouTempo()).isFalse();
        assertThat(medida.recusouEspaco()).isFalse();
        assertThat(medida.classeDaFalha()).isNull();
    }

    @Test
    void cValidoEResponditoPeloMotorSemRecusa() {
        MedidaExterna medida = new MedidorExterno().medir(fonte(CorpusExternoSintetico.UNICO_C, C_VALIDO));

        assertThat(medida.tempo()).isEqualTo("O(n)");
        assertThat(medida.recusouTempo()).isFalse();
        assertThat(medida.recusouEspaco()).isFalse();
    }

    @Test
    void javaInvalidoViraRecusaSemMetricaEnaoSubestimativa() {
        MedidaExterna medida = new MedidorExterno().medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_INVALIDO));

        assertThat(medida.tempo()).isEqualTo(MedidaExterna.INDETERMINADO);
        assertThat(medida.espaco()).isEqualTo(MedidaExterna.INDETERMINADO);
        assertThat(medida.recusaTempo()).isEqualTo(MotivoRecusa.SEM_METRICA);
        assertThat(medida.recusaEspaco()).isEqualTo(MotivoRecusa.SEM_METRICA);
        assertThat(medida.confiancaTempo()).isEqualTo(NivelConfianca.BAIXA);
        assertThat(medida.toString()).doesNotContain(TOKEN);
    }

    @Test
    void soUmaMetricaAusenteRecusaSoEla() {
        Medicao semEspaco = new Medicao("O(n)", NivelConfianca.ALTA, MedidorDoCorpus.AUSENTE, NivelConfianca.BAIXA);

        MedidaExterna medida = medidorQue((codigo, linguagem) -> semEspaco)
                .medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO));

        assertThat(medida.tempo()).isEqualTo("O(n)");
        assertThat(medida.recusouTempo()).isFalse();
        assertThat(medida.espaco()).isEqualTo(MedidaExterna.INDETERMINADO);
        assertThat(medida.recusaEspaco()).isEqualTo(MotivoRecusa.SEM_METRICA);
    }

    @Test
    void excecaoViraRecusaPorFalhaGuardandoSoONomeDaClasse() {
        MedidaExterna medida = medidorQue((codigo, linguagem) -> {
            throw new IllegalArgumentException("trecho do fonte: " + TOKEN);
        }).medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO));

        assertThat(medida.recusaTempo()).isEqualTo(MotivoRecusa.FALHA);
        assertThat(medida.recusaEspaco()).isEqualTo(MotivoRecusa.FALHA);
        assertThat(medida.classeDaFalha()).isEqualTo("IllegalArgumentException");
        assertThat(medida.toString()).doesNotContain(TOKEN);
    }

    @Test
    void estouroDePilhaViraRecusaPorFalha() {
        MedidaExterna medida = medidorQue((codigo, linguagem) -> {
            throw new StackOverflowError(TOKEN);
        }).medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO));

        assertThat(medida.recusaTempo()).isEqualTo(MotivoRecusa.FALHA);
        assertThat(medida.classeDaFalha()).isEqualTo("StackOverflowError");
        assertThat(medida.toString()).doesNotContain(TOKEN);
    }

    @Test
    void assertionErrorViraRecusaPorFalhaSemGuardarAMensagem() {
        MedidaExterna medida = medidorQue((codigo, linguagem) -> {
            throw new AssertionError("trecho do fonte: " + TOKEN);
        }).medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO));

        assertThat(medida.recusaTempo()).isEqualTo(MotivoRecusa.FALHA);
        assertThat(medida.recusaEspaco()).isEqualTo(MotivoRecusa.FALHA);
        assertThat(medida.classeDaFalha()).isEqualTo("AssertionError");
        assertThat(medida.toString()).doesNotContain(TOKEN);
    }

    @Test
    void linkageErrorAbortaSemCitarAMensagem() {
        MedidorExterno medidor = medidorQue((codigo, linguagem) -> {
            throw new LinkageError(TOKEN);
        });

        assertThatThrownBy(() -> medidor.medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LinkageError")
                .hasMessageNotContaining(TOKEN)
                .hasNoCause();
    }

    @Test
    void outroErroDaJvmAbortaSemCitarAMensagem() {
        MedidorExterno medidor = medidorQue((codigo, linguagem) -> {
            throw new OutOfMemoryError(TOKEN);
        });

        assertThatThrownBy(() -> medidor.medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OutOfMemoryError")
                .hasMessageNotContaining(TOKEN)
                .hasNoCause();
    }

    @Test
    void analiseQueTravaViraRecusaPorTempoEmThreadDaemon() {
        AtomicBoolean parar = new AtomicBoolean();
        AtomicReference<Boolean> daemon = new AtomicReference<>();
        MedidorExterno medidor = medidorQue((codigo, linguagem) -> {
            daemon.set(Thread.currentThread().isDaemon());
            while (!parar.get()) {
                Thread.onSpinWait();
            }
            return null;
        });
        try {
            long inicio = System.nanoTime();
            MedidaExterna medida = medidor.medir(fonte(CorpusExternoSintetico.PRIMEIRO_JAVA, JAVA_VALIDO));
            Duration gasto = Duration.ofNanos(System.nanoTime() - inicio);

            assertThat(medida.recusaTempo()).isEqualTo(MotivoRecusa.TEMPO_ESGOTADO);
            assertThat(medida.recusaEspaco()).isEqualTo(MotivoRecusa.TEMPO_ESGOTADO);
            assertThat(medida.classeDaFalha()).isNull();
            assertThat(gasto).isLessThan(Duration.ofSeconds(10));
            assertThat(daemon.get()).isTrue();
        } finally {
            parar.set(true);
        }
    }

    @Test
    void oLimitePadraoEDe60Segundos() {
        assertThat(MedidorExterno.LIMITE_PADRAO).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void medidaNaoTemCampoDeCodigoNemDeDetalhe() {
        assertThat(Arrays.stream(MedidaExterna.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactly("caso", "tempo", "confiancaTempo", "recusaTempo",
                        "espaco", "confiancaEspaco", "recusaEspaco", "classeDaFalha");
    }
}
