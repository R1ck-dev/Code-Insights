-- V4: Contexto knowledge - snippets (trechos de codigo categorizados;
-- biblioteca pessoal do usuario, com vinculo opcional a uma resolucao).

CREATE TABLE snippets (
    id UUID PRIMARY KEY,
    autor_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    resolucao_id UUID REFERENCES resolucoes(id) ON DELETE SET NULL,
    titulo VARCHAR(255) NOT NULL,
    codigo TEXT NOT NULL,
    descricao TEXT,
    categoria_conceito VARCHAR(100),    -- ex: algoritmo, estrutura_dados, padrao
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_snippets_autor ON snippets(autor_id);
CREATE INDEX idx_snippets_resolucao ON snippets(resolucao_id);
