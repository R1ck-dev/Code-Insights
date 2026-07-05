package com.projeto.codeinsights.domain.knowledge.enums;

/**
 * Granularidade temporal da serie de evolucao do dashboard. Cada valor mapeia
 * o campo aceito pelo {@code date_trunc} do Postgres, usado para agrupar as
 * resolucoes por dia, semana ou mes de submissao.
 */
public enum GranularidadeTempo {
    DIARIO("day"),
    SEMANAL("week"),
    MENSAL("month");

    private final String sqlTrunc;

    GranularidadeTempo(String sqlTrunc) {
        this.sqlTrunc = sqlTrunc;
    }

    public String getSqlTrunc() {
        return sqlTrunc;
    }
}
