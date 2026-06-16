-- V1: Contexto identity (usuarios + tokens de verificacao)

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil_publico BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL,        -- PENDENTE_VERIFICACAO, ATIVO, INATIVO, SUSPENSO
    role VARCHAR(50) NOT NULL,          -- USER, ADMIN
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
