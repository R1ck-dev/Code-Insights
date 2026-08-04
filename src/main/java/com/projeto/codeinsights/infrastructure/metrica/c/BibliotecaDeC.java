package com.projeto.codeinsights.infrastructure.metrica.c;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.projeto.codeinsights.infrastructure.metrica.custo.Custo;
import com.projeto.codeinsights.infrastructure.metrica.custo.CustoAvaliado;

/**
 * Custo assintotico das funcoes da biblioteca padrao de C — o equivalente da
 * {@code TabelaDeCustoApi} do lado Java, e pelos mesmos motivos: sem ela,
 * {@code qsort(v, n, ...)} custaria O(1) e a armadilha classica de C passaria batida.
 * <p>
 * <b>A entrada que mais importa e {@code strlen}.</b> Escrever
 * {@code for (i = 0; i < strlen(s); i++)} e o erro mais comum de quem esta aprendendo C: a chamada
 * fica na condicao e e reavaliada a cada volta, transformando uma varredura O(n) em O(n^2). O
 * avaliador ja multiplica o custo da condicao pelas iteracoes; e esta tabela que da a esse custo o
 * valor certo. Sem ela o motor concordaria com o aluno que o laco e linear.
 * <p>
 * <b>Coerencia com o lado Java, e nao com a verdade literal.</b> Duas entradas sao arredondadas de
 * proposito para que o mesmo algoritmo receba a mesma classe nas duas linguagens:
 * <ul>
 *   <li>{@code printf} e {@code puts} percorrem a cadeia que imprimem, mas entram como O(1) porque
 *       {@code System.out.println} entra como O(1) no lado Java. Cobrar de C o que nao se cobra de
 *       Java faria a mesma solucao mudar de classe so por trocar de linguagem.</li>
 *   <li>{@code strstr} e O(n*m); a escala do motor tem um simbolo de tamanho so, e o lado Java ja
 *       trata {@code String.indexOf} como O(n). Segue a mesma convencao.</li>
 * </ul>
 * <b>{@code malloc} nao e {@code calloc}.</b> {@code malloc} devolve memoria sem tocar nela: O(1)
 * de tempo. {@code calloc} zera tudo que reservou, e por isso e O(n). A distincao e real e o motor
 * a faz — o espaco de ambos e contado a parte, em {@link CustoDeEspacoEmC}.
 */
final class BibliotecaDeC {

    /** Percorre uma cadeia ou um bloco de memoria inteiro. */
    private static final Map<String, Custo> LINEARES = Map.ofEntries(
            Map.entry("strlen", Custo.LINEAR),
            Map.entry("strcpy", Custo.LINEAR),
            Map.entry("strncpy", Custo.LINEAR),
            Map.entry("strcat", Custo.LINEAR),
            Map.entry("strncat", Custo.LINEAR),
            Map.entry("strcmp", Custo.LINEAR),
            Map.entry("strncmp", Custo.LINEAR),
            Map.entry("strcasecmp", Custo.LINEAR),
            Map.entry("strchr", Custo.LINEAR),
            Map.entry("strrchr", Custo.LINEAR),
            Map.entry("strstr", Custo.LINEAR),
            Map.entry("strdup", Custo.LINEAR),
            Map.entry("strtok", Custo.LINEAR),
            Map.entry("strspn", Custo.LINEAR),
            Map.entry("strcspn", Custo.LINEAR),
            Map.entry("memset", Custo.LINEAR),
            Map.entry("memcpy", Custo.LINEAR),
            Map.entry("memmove", Custo.LINEAR),
            Map.entry("memcmp", Custo.LINEAR),
            Map.entry("memchr", Custo.LINEAR),
            Map.entry("bzero", Custo.LINEAR),
            Map.entry("calloc", Custo.LINEAR),
            Map.entry("realloc", Custo.LINEAR),
            Map.entry("fgets", Custo.LINEAR),
            Map.entry("gets", Custo.LINEAR));

    private static final Map<String, Custo> ORDENACAO = Map.of(
            "qsort", Custo.N_LOG_N,
            "bsearch", Custo.LOGARITMICO);

    /**
     * Chamadas de custo fixo. Estao aqui para que o avaliador nao as trate como "funcao
     * desconhecida" — o que rebaixaria a confianca de praticamente toda solucao em C, ja que
     * quase todo programa de aluno chama {@code printf} e {@code scanf}.
     */
    private static final Set<String> CONSTANTES = Set.of(
            "printf", "scanf", "sprintf", "sscanf", "fprintf", "fscanf", "snprintf",
            "putchar", "getchar", "puts", "putc", "getc", "fputc", "fgetc", "fputs",
            "fopen", "fclose", "fflush", "feof", "ferror", "rewind", "fseek", "ftell",
            "malloc", "free", "exit", "abort", "atexit",
            "abs", "labs", "fabs", "sqrt", "pow", "exp", "log", "log2", "log10",
            "sin", "cos", "tan", "atan", "atan2", "floor", "ceil", "round", "fmod",
            "rand", "srand", "time", "clock",
            "atoi", "atof", "atol", "strtol", "strtod",
            "isdigit", "isalpha", "isalnum", "isspace", "isupper", "islower",
            "toupper", "tolower", "assert", "va_start", "va_end", "va_arg");

    private BibliotecaDeC() {
    }

    /** Custo da chamada, ou vazio quando o nome nao e da biblioteca padrao. */
    static Optional<CustoAvaliado> custo(String nome) {
        Custo linear = LINEARES.get(nome);
        if (linear != null) {
            return Optional.of(CustoAvaliado.exato(linear));
        }
        Custo ordenacao = ORDENACAO.get(nome);
        if (ordenacao != null) {
            return Optional.of(CustoAvaliado.exato(ordenacao));
        }
        if (CONSTANTES.contains(nome)) {
            return Optional.of(CustoAvaliado.exato(Custo.CONSTANTE));
        }
        return Optional.empty();
    }

    static boolean conhece(String nome) {
        return custo(nome).isPresent();
    }

    /** {@code calloc} zera o que reserva; {@code malloc} e {@code realloc} entregam sem tocar. */
    static boolean alocaMemoria(String nome) {
        return nome.equals("malloc") || nome.equals("calloc") || nome.equals("realloc");
    }
}
