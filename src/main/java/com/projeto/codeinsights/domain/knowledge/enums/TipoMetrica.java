package com.projeto.codeinsights.domain.knowledge.enums;

/**
 * Tipo de metrica estatica calculada a partir da AST de uma {@code Resolucao}.
 * A camada de metricas cresce por adicao: uma nova metrica entra como um novo
 * valor aqui + um novo analisador, sem alterar o schema nem o modelo existente.
 */
public enum TipoMetrica {
    COMPLEXIDADE_CICLOMATICA,
    BIG_O_TEMPO,
    COMPLEXIDADE_ESPACO
}
