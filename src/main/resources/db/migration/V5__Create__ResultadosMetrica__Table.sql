-- V5: Contexto knowledge - resultados_metrica (metricas estaticas calculadas por analise da AST).
-- Modelo uniforme (uma linha por resolucao x tipo de metrica): novas metricas entram por adicao
-- (novo valor de 'tipo' + novo analisador), sem alterar este schema. 'valor' guarda a magnitude
-- numerica (contagem, no caso da ciclomatica, ou a ordem da classe de complexidade em Big O/espaco).

CREATE TABLE resultados_metrica (
    id UUID PRIMARY KEY,
    resolucao_id UUID NOT NULL REFERENCES resolucoes(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,          -- COMPLEXIDADE_CICLOMATICA, BIG_O_TEMPO, COMPLEXIDADE_ESPACO
    valor INT NOT NULL,                 -- contagem (ciclomatica) ou ordem da ClasseComplexidade
    rotulo VARCHAR(50) NOT NULL,        -- exibicao: "7", "O(n^2)"
    detalhe TEXT,                       -- raciocinio da heuristica
    analisado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_resultado_metrica_resolucao_tipo UNIQUE (resolucao_id, tipo)
);

CREATE INDEX idx_resultados_metrica_resolucao ON resultados_metrica(resolucao_id);
