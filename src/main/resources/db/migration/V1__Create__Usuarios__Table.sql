-- V1: Contexto identity (usuarios + tokens de verificacao)

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,                  -- ALUNO, PESQUISADOR, ADMIN
    visibilidade_perfil VARCHAR(20) NOT NULL,   -- PUBLICO, PRIVADO
    status VARCHAR(50) NOT NULL,                -- PENDENTE_VERIFICACAO, ATIVO, INATIVO, SUSPENSO
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_username ON usuarios(username);

CREATE TABLE tokens_verificacao (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    data_expiracao TIMESTAMP NOT NULL,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE,
    tipo VARCHAR(50) NOT NULL           -- ATIVACAO
);

CREATE INDEX idx_tokens_verificacao_usuario ON tokens_verificacao(usuario_id);
