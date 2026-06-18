-- V2: Contexto knowledge - desafios (exercicio de Online Judge externo, no portfolio do usuario)

CREATE TABLE desafios (
    id UUID PRIMARY KEY,
    autor_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    enunciado TEXT,
    plataforma_origem VARCHAR(100),         -- ex: NepsAcademy, LeetCode, Codeforces
    identificador_externo VARCHAR(100),     -- id do problema na plataforma de origem
    url_externa VARCHAR(500),
    visibilidade VARCHAR(20) NOT NULL,      -- PUBLICO, PRIVADO
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_desafios_autor ON desafios(autor_id);
CREATE INDEX idx_desafios_visibilidade ON desafios(autor_id, visibilidade);
