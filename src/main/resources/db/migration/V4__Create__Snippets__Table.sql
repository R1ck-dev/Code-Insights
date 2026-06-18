-- V4: Contexto knowledge - snippets (trechos de codigo categorizados;
-- biblioteca pessoal de conhecimento do usuario, referenciando o autor por id).

CREATE TABLE snippets (
    id UUID PRIMARY KEY,
    autor_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    codigo TEXT NOT NULL,
    descricao TEXT,
    categoria VARCHAR(50) NOT NULL,     -- ESTRUTURA_DADOS, RECURSAO, ORDENACAO, GRAFOS, ...
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_snippets_autor ON snippets(autor_id);
