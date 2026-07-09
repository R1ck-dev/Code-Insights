package com.projeto.codeinsights.infrastructure.metrica.custo;

import com.projeto.codeinsights.domain.knowledge.enums.ClasseComplexidade;

/**
 * Custo assintotico simbolico: o reticulado sobre o qual o motor de metricas faz
 * aritmetica. Em vez de casar padroes e cuspir um rotulo, o motor <b>calcula</b>
 * um custo, compondo custos de sub-expressoes.
 * <p>
 * A forma normal e {@code n^grauPoli * log^grauLog(n)}, mais tres formas fora da
 * escala polinomial: {@link Forma#EXPONENCIAL}, {@link Forma#FATORIAL} e
 * {@link Forma#DESCONHECIDO} (absorvente: se um pedaco e desconhecido, o todo e).
 * <p>
 * Duas operacoes bastam para percorrer a AST:
 * <ul>
 *   <li>{@link #mais(Custo)} - soma assintotica, {@code O(f) + O(g) = O(max(f,g))};
 *       usada em sequencias de comandos e ramos de {@code if};</li>
 *   <li>{@link #vezes(Custo)} - produto, usada em laco (iteracoes x corpo).</li>
 * </ul>
 * O {@code n} aqui e "o tamanho da entrada" de forma deliberadamente unica: o motor
 * nao distingue O(n*m) de O(n^2) (isso exigiria analise de fluxo de dados).
 */
public record Custo(Forma forma, int grauPoli, int grauLog) {

    public enum Forma {
        POLILOG,
        EXPONENCIAL,
        FATORIAL,
        DESCONHECIDO
    }

    public static final Custo CONSTANTE = poliLog(0, 0);
    public static final Custo LOGARITMICO = poliLog(0, 1);
    public static final Custo LINEAR = poliLog(1, 0);
    public static final Custo N_LOG_N = poliLog(1, 1);
    public static final Custo QUADRATICO = poliLog(2, 0);
    public static final Custo EXPONENCIAL = new Custo(Forma.EXPONENCIAL, 0, 0);
    public static final Custo FATORIAL = new Custo(Forma.FATORIAL, 0, 0);
    public static final Custo DESCONHECIDO = new Custo(Forma.DESCONHECIDO, 0, 0);

    public static Custo poliLog(int grauPoli, int grauLog) {
        return new Custo(Forma.POLILOG, Math.max(grauPoli, 0), Math.max(grauLog, 0));
    }

    public boolean ehDesconhecido() {
        return forma == Forma.DESCONHECIDO;
    }

    /** Soma assintotica: {@code O(f) + O(g) = O(max(f, g))}. */
    public Custo mais(Custo outro) {
        if (ehDesconhecido() || outro.ehDesconhecido()) {
            return DESCONHECIDO;
        }
        return comparar(outro) >= 0 ? this : outro;
    }

    /** Produto assintotico: um laco de {@code this} iteracoes com corpo {@code outro}. */
    public Custo vezes(Custo outro) {
        if (ehDesconhecido() || outro.ehDesconhecido()) {
            return DESCONHECIDO;
        }
        if (forma == Forma.FATORIAL || outro.forma == Forma.FATORIAL) {
            return FATORIAL;
        }
        if (forma == Forma.EXPONENCIAL || outro.forma == Forma.EXPONENCIAL) {
            return EXPONENCIAL;
        }
        return poliLog(grauPoli + outro.grauPoli, grauLog + outro.grauLog);
    }

    /** Ordem total: negativo se este e mais barato que {@code outro}. */
    public int comparar(Custo outro) {
        if (forma != outro.forma) {
            return Integer.compare(patamar(), outro.patamar());
        }
        if (forma != Forma.POLILOG) {
            return 0;
        }
        if (grauPoli != outro.grauPoli) {
            return Integer.compare(grauPoli, outro.grauPoli);
        }
        return Integer.compare(grauLog, outro.grauLog);
    }

    private int patamar() {
        return switch (forma) {
            case POLILOG -> 0;
            case EXPONENCIAL -> 1;
            case FATORIAL -> 2;
            case DESCONHECIDO -> 3;
        };
    }

    /**
     * Projeta o custo simbolico na escala fixa de {@link ClasseComplexidade}. A escala
     * e mais grossa que o reticulado, entao a projecao <b>arredonda para cima</b> (nunca
     * subestima): {@code log^2 n -> O(n)}, {@code n^2 log n -> O(n^3)}. Graus polinomiais
     * acima de 3 saturam em {@code O(n^3)} - a forma exata continua legivel em
     * {@link #descricao()}, que o motor grava no campo {@code detalhe}.
     */
    public ClasseComplexidade classe() {
        return switch (forma) {
            case DESCONHECIDO -> ClasseComplexidade.DESCONHECIDO;
            case FATORIAL -> ClasseComplexidade.O_FATORIAL;
            case EXPONENCIAL -> ClasseComplexidade.O_EXPONENCIAL;
            case POLILOG -> classePoliLog();
        };
    }

    private ClasseComplexidade classePoliLog() {
        if (grauPoli == 0) {
            if (grauLog == 0) {
                return ClasseComplexidade.O_1;
            }
            return grauLog == 1 ? ClasseComplexidade.O_LOG_N : ClasseComplexidade.O_N;
        }
        if (grauPoli == 1) {
            if (grauLog == 0) {
                return ClasseComplexidade.O_N;
            }
            return grauLog == 1 ? ClasseComplexidade.O_N_LOG_N : ClasseComplexidade.O_N2;
        }
        if (grauPoli == 2) {
            return grauLog == 0 ? ClasseComplexidade.O_N2 : ClasseComplexidade.O_N3;
        }
        return ClasseComplexidade.O_N3;
    }

    /** {@code true} quando {@link #classe()} perde informacao (a escala nao representa este custo). */
    public boolean forcadoNaEscala() {
        if (forma != Forma.POLILOG) {
            return false;
        }
        if (grauPoli >= 3) {
            return grauPoli > 3 || grauLog > 0;
        }
        return grauLog >= 2 || (grauPoli == 2 && grauLog >= 1);
    }

    /** Forma simbolica legivel: {@code "1"}, {@code "n log n"}, {@code "n^2 log^2 n"}, {@code "2^n"}. */
    public String descricao() {
        return switch (forma) {
            case DESCONHECIDO -> "?";
            case FATORIAL -> "n!";
            case EXPONENCIAL -> "2^n";
            case POLILOG -> descricaoPoliLog();
        };
    }

    private String descricaoPoliLog() {
        if (grauPoli == 0 && grauLog == 0) {
            return "1";
        }
        StringBuilder texto = new StringBuilder();
        if (grauPoli == 1) {
            texto.append("n");
        } else if (grauPoli > 1) {
            texto.append("n^").append(grauPoli);
        }
        if (grauLog > 0) {
            if (!texto.isEmpty()) {
                texto.append(" ");
            }
            texto.append(grauLog == 1 ? "log n" : "log^" + grauLog + " n");
        }
        return texto.toString();
    }
}
