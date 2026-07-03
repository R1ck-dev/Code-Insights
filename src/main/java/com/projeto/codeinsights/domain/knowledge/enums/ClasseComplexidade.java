package com.projeto.codeinsights.domain.knowledge.enums;

/**
 * Classe de complexidade assintotica usada pelas metricas de Big O (tempo) e de
 * espaco. Cada classe carrega uma {@code ordem} inteira que a torna comparavel:
 * quanto maior a ordem, mais complexa a solucao. A ordem e persistida como o
 * {@code valor} numerico do resultado, servindo direto a analise estatistica da
 * evolucao algoritmica do aluno. {@link #DESCONHECIDO} (ordem -1) marca os casos
 * fora da escala (nao classificaveis pela heuristica).
 */
public enum ClasseComplexidade {
    O_1(0, "O(1)"),
    O_LOG_N(1, "O(log n)"),
    O_N(2, "O(n)"),
    O_N_LOG_N(3, "O(n log n)"),
    O_N2(4, "O(n^2)"),
    O_N3(5, "O(n^3)"),
    O_EXPONENCIAL(6, "O(2^n)"),
    O_FATORIAL(7, "O(n!)"),
    DESCONHECIDO(-1, "?");

    private final int ordem;
    private final String rotulo;

    ClasseComplexidade(int ordem, String rotulo) {
        this.ordem = ordem;
        this.rotulo = rotulo;
    }

    public int getOrdem() {
        return ordem;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Retorna a classe de maior ordem entre esta e {@code outra}. */
    public ClasseComplexidade maisComplexa(ClasseComplexidade outra) {
        return this.ordem >= outra.ordem ? this : outra;
    }
}
