-- V3: Contexto knowledge - resolucoes (solucao submetida para um desafio).
-- O Indice de Autonomia IA (1-5) e autodeclarado na submissao. As metricas estaticas
-- (Big O, ciclomatica, espaco) serao materializadas como ResultadoMetrica (por adicao);
-- a flag 'analisada' marca quando a analise ja rodou.

CREATE TABLE resolucoes (
    id UUID PRIMARY KEY,
    desafio_id UUID NOT NULL REFERENCES desafios(id) ON DELETE CASCADE,
    autor_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    codigo_fonte TEXT NOT NULL,
    linguagem VARCHAR(50) NOT NULL,         -- JAVA, PYTHON, CPP, JAVASCRIPT, C
    indice_autonomia_ia INT NOT NULL CHECK (indice_autonomia_ia >= 1 AND indice_autonomia_ia <= 5),
    descricao_apoio_ia TEXT,
    visibilidade VARCHAR(20) NOT NULL,      -- PUBLICO, PRIVADO
    analisada BOOLEAN NOT NULL DEFAULT FALSE,
    submetida_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resolucoes_desafio ON resolucoes(desafio_id);
CREATE INDEX idx_resolucoes_autor ON resolucoes(autor_id);
