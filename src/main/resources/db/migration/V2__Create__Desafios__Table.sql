-- V2: Contexto knowledge - desafios (enunciado do problema)

CREATE TABLE desafios (
    id UUID PRIMARY KEY,
    autor_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    origem_plataforma VARCHAR(100),     -- ex: LeetCode, HackerRank
    dificuldade VARCHAR(50),            -- FACIL, MEDIO, DIFICIL
    publico BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_desafios_autor ON desafios(autor_id);
CREATE INDEX idx_desafios_publico ON desafios(publico) WHERE publico = TRUE;
