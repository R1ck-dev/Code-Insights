package com.projeto.codeinsights.infrastructure.metrica;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.projeto.codeinsights.domain.knowledge.enums.ClasseComplexidade;

/**
 * Um caso do corpus de validacao: o codigo-fonte, a complexidade <b>correta</b> segundo a
 * literatura ({@code gabarito}) e a complexidade que o motor <b>responde hoje</b>
 * ({@code esperadoDoMotor}).
 * <p>
 * Os dois campos existem porque respondem a perguntas diferentes, e misturar as duas foi
 * o vies do corpus antigo:
 * <ul>
 *   <li>o <b>esperado do motor</b> e a rede de regressao — o teste assere contra ele, entao
 *       qualquer mudanca de comportamento aparece na hora, mesmo nos casos que o motor erra;</li>
 *   <li>o <b>gabarito</b> e a medida cientifica — o relatorio de acuracia compara contra ele,
 *       e a divergencia e <b>dado</b>, nao falha de build.</li>
 * </ul>
 * Quando os dois coincidem (o motor acerta), o manifesto omite o esperado e ele e preenchido
 * com o gabarito no carregamento.
 *
 * @param espacoGabarito complexidade de espaco correta, ou {@code null} quando a resposta e
 *                       genuinamente ambigua (ex.: o espaco auxiliar de {@code Arrays.sort}
 *                       varia com a implementacao) — nesses casos o espaco nao e nem
 *                       asserido nem medido.
 */
record CasoDeCorpus(
        String nome,
        Categoria categoria,
        String arquivo,
        String codigo,
        String origem,
        String nota,
        String tempoGabarito,
        String tempoEsperadoDoMotor,
        String espacoGabarito,
        String espacoEsperadoDoMotor) {

    /**
     * De onde veio o caso. A separacao importa no relatorio: acerto alto no corpus canonico
     * diz pouco, porque sao os algoritmos para os quais o motor foi desenhado; o numero que
     * vale e o de {@link #ALUNO}, que e o publico real do piloto.
     */
    enum Categoria {
        /** Algoritmo classico com complexidade fixada pela literatura. */
        CANONICO,
        /** Arquetipo de programacao competitiva — estrutura que o motor nao foi desenhado para ver. */
        ADVERSARIAL,
        /** Codigo no estilo de aluno iniciante: tudo no {@code main}, {@code Scanner}, sem metodo auxiliar. */
        ALUNO
    }

    /** Rotulos que o motor consegue emitir. Um gabarito fora deste conjunto e um limite da escala. */
    private static final Set<String> ESCALA = Arrays.stream(ClasseComplexidade.values())
            .map(ClasseComplexidade::getRotulo)
            .collect(Collectors.toUnmodifiableSet());

    static boolean estaNaEscala(String rotulo) {
        return rotulo != null && ESCALA.contains(rotulo);
    }

    /**
     * O gabarito de tempo nao e representavel na escala de 8 classes do motor (ex.: O(raiz de n),
     * O(n*m), O(n log log n)). Nao e erro do motor: e limite do instrumento, e o relatorio conta
     * esses casos numa tabela propria em vez de somar como acerto ou erro.
     */
    boolean tempoForaDaEscala() {
        return !estaNaEscala(tempoGabarito);
    }

    boolean espacoForaDaEscala() {
        return espacoGabarito != null && !estaNaEscala(espacoGabarito);
    }

    /** O motor acerta o gabarito de tempo? */
    boolean motorAcertaTempo() {
        return tempoGabarito.equals(tempoEsperadoDoMotor);
    }

    boolean motorAcertaEspaco() {
        return espacoGabarito != null && espacoGabarito.equals(espacoEsperadoDoMotor);
    }

    /** Nome exibido no relatorio do JUnit ({@code @ParameterizedTest(name = "...")}). */
    @Override
    public String toString() {
        return nome;
    }
}
