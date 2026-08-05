package com.projeto.codeinsights.infrastructure.metrica;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.MedidorDoCorpus.Medicao;

/**
 * Exige que o <b>mesmo algoritmo</b> receba a <b>mesma classe</b> de complexidade em Java e em C.
 * <p>
 * Esta e a promessa que sustenta o eixo de complexidade da pesquisa. O piloto e majoritariamente
 * em C, mas a plataforma tambem recebe Java: se uma regra existir de um lado so, o mesmo algoritmo
 * muda de classe ao trocar de linguagem, e comparar a evolucao de um participante que escreve em C
 * com a de outro que escreve em Java passa a medir a linguagem, nao o aprendizado. Nenhum outro
 * teste cobre isso — o corpus valida cada motor contra a literatura, isoladamente.
 * <p>
 * <b>Por que pares proprios, e nao os nomes que ja se repetem nos dois corpora.</b> Quinze casos
 * existem com o mesmo nome em {@code gabarito.json} e {@code gabarito-c.json}, mas os arquivos
 * foram escritos de forma independente: {@code exponenciacao rapida} e <i>recursiva</i> em Java
 * (pilha O(log n)) e <i>iterativa</i> em C (O(1)). Comparar por nome acusaria uma assimetria que
 * nao existe — mediria quem escreveu os arquivos, nao o motor. Os pares aqui sao transliterados de
 * proposito: mesma estrutura de controle, mesmos limites de laco, mesma forma de recursao, com a
 * unica diferenca sendo o idioma que a linguagem obriga (por exemplo {@code new int[n][n]} contra
 * {@code malloc(n * n * sizeof(int))} numa grade linearizada).
 * <p>
 * A classe esperada tambem e declarada no manifesto. Sem ela, os dois motores poderiam derivar
 * juntos para a mesma resposta errada e o teste continuaria verde.
 */
class EquivalenciaEntreLinguagensTest {

    private static final String RAIZ = "/equivalencia/";

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * @param base    nome dos dois arquivos do par, sem extensao
     * @param regra   o que este par exercita — aparece na mensagem de falha
     * @param tempo   classe de tempo que ambas as linguagens devem produzir
     * @param espaco  classe de espaco que ambas as linguagens devem produzir
     */
    record Par(String base, String nome, String regra, String tempo, String espaco) {

        @Override
        public String toString() {
            return nome;
        }
    }

    /**
     * Par em que as duas linguagens <b>legitimamente</b> respondem coisas diferentes hoje. Fica
     * registrado com as duas respostas em vez de escondido: assim a assimetria e uma decisao
     * visivel e datada, e o dia em que alguem mudar qualquer um dos lados o teste avisa.
     *
     * @param motivo por que a diferenca existe — se e limite do motor ou da linguagem
     */
    record Divergencia(String base, String nome, String motivo,
            String tempoJava, String tempoC, String espacoJava, String espacoC) {

        @Override
        public String toString() {
            return nome;
        }
    }

    private record Manifesto(List<Par> pares, List<Divergencia> divergencias) {
    }

    private static Manifesto manifesto() {
        try (InputStream entrada = abrir(RAIZ + "pares.json")) {
            return JSON.readValue(entrada, Manifesto.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o manifesto dos pares.", e);
        }
    }

    static List<Par> pares() {
        return manifesto().pares();
    }

    static List<Divergencia> divergencias() {
        return manifesto().divergencias();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pares")
    void oMesmoAlgoritmoRecebeAMesmaClasseNasDuasLinguagens(Par par) {
        Medicao emJava = MedidorDoCorpus.medir(ler(par.base() + ".java"), LinguagemProgramacao.JAVA);
        Medicao emC = MedidorDoCorpus.medir(ler(par.base() + ".c"), LinguagemProgramacao.C);

        assertThat(emC.tempo())
                .as("tempo divergiu entre as linguagens (%s): Java diz %s, C diz %s",
                        par.regra(), emJava.tempo(), emC.tempo())
                .isEqualTo(emJava.tempo());

        assertThat(emC.espaco())
                .as("espaco divergiu entre as linguagens (%s): Java diz %s, C diz %s",
                        par.regra(), emJava.espaco(), emC.espaco())
                .isEqualTo(emJava.espaco());
    }

    /**
     * Concordar nao basta: dois motores podem concordar e estar ambos errados. Este teste prende a
     * resposta ao valor correto, para que uma deriva simultanea das duas linguagens apareca.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("pares")
    void aClasseAcordadaEhACorreta(Par par) {
        Medicao emJava = MedidorDoCorpus.medir(ler(par.base() + ".java"), LinguagemProgramacao.JAVA);

        assertThat(emJava.tempo()).as("tempo de %s (%s)", par.nome(), par.regra()).isEqualTo(par.tempo());

        // espaco nulo = resposta genuinamente ambigua (mesma convencao do corpus): o espaco
        // auxiliar de uma ordenacao de biblioteca depende da implementacao, entao nao se declara
        // qual e o certo. A igualdade ENTRE as linguagens continua exigida no teste acima.
        if (par.espaco() != null) {
            assertThat(emJava.espaco()).as("espaco de %s (%s)", par.nome(), par.regra()).isEqualTo(par.espaco());
        }
    }

    /**
     * Prende as duas respostas de cada assimetria conhecida. Nao proibe conserta-la: se alguem
     * portar a deteccao de memoizacao para C, este teste fica vermelho pedindo que o registro
     * seja atualizado — que e exatamente quando a decisao precisa ser reexaminada.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("divergencias")
    void aAssimetriaConhecidaContinuaOndeFoiDocumentada(Divergencia divergencia) {
        Medicao emJava = MedidorDoCorpus.medir(ler(divergencia.base() + ".java"), LinguagemProgramacao.JAVA);
        Medicao emC = MedidorDoCorpus.medir(ler(divergencia.base() + ".c"), LinguagemProgramacao.C);

        assertThat(emJava.tempo()).as("tempo em Java (%s)", divergencia.motivo()).isEqualTo(divergencia.tempoJava());
        assertThat(emC.tempo()).as("tempo em C (%s)", divergencia.motivo()).isEqualTo(divergencia.tempoC());
        assertThat(emJava.espaco()).as("espaco em Java (%s)", divergencia.motivo()).isEqualTo(divergencia.espacoJava());
        assertThat(emC.espaco()).as("espaco em C (%s)", divergencia.motivo()).isEqualTo(divergencia.espacoC());
    }

    /**
     * Guarda contra registro inutil: uma entrada em {@code divergencias} que responde igual nas
     * duas linguagens pertence a {@code pares}, onde a igualdade e <b>exigida</b> em vez de apenas
     * anotada — deixa-la aqui esconderia um par que ja concorda atras de uma excecao documentada.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("divergencias")
    void todaDivergenciaRegistradaRealmenteDiverge(Divergencia divergencia) {
        assertThat(divergencia.tempoJava().equals(divergencia.tempoC())
                && divergencia.espacoJava().equals(divergencia.espacoC()))
                .as("%s nao diverge em nada: deveria estar em 'pares'", divergencia.nome())
                .isFalse();
    }

    /** Um par listado no manifesto sem os dois arquivos no disco nao mede equivalencia nenhuma. */
    @Test
    void todoParTemOsDoisArquivos() {
        assertThat(pares()).isNotEmpty();
        pares().forEach(par -> {
            assertThat(ler(par.base() + ".java")).as("%s.java", par.base()).isNotBlank();
            assertThat(ler(par.base() + ".c")).as("%s.c", par.base()).isNotBlank();
        });
    }

    private static String ler(String arquivo) {
        try (InputStream entrada = abrir(RAIZ + arquivo)) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler " + arquivo + ".", e);
        }
    }

    private static InputStream abrir(String caminho) {
        InputStream entrada = EquivalenciaEntreLinguagensTest.class.getResourceAsStream(caminho);
        if (entrada == null) {
            throw new IllegalStateException("Recurso de equivalencia nao encontrado: " + caminho);
        }
        return entrada;
    }
}
