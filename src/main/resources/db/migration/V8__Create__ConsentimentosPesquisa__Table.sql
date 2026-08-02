-- V8: Contexto pesquisa - consentimentos_pesquisa (TCLE).
--
-- Livro-razao APPEND-ONLY: uma linha por decisao tomada, nunca uma linha por participante.
-- Mudar de ideia insere; nada atualiza e nada apaga. Por isso NAO existe UNIQUE em
-- (participante_id, versao_termo): a mesma pessoa pode aceitar, revogar e aceitar de novo, e as tres
-- linhas precisam sobreviver. A decisao que vale e a mais recente, e quem aplica essa regra e
-- HistoricoDeConsentimento, no dominio.
--
-- Revogar nao apaga: apagar destruiria a prova de que houve aceite, que e justamente a base para ter
-- usado o dado ate ali.
--
-- ON DELETE CASCADE acompanha o resto do schema: se a conta some, as submissoes somem junto, e um
-- consentimento orfao sobre dado inexistente nao teria a que se referir.

CREATE TABLE consentimentos_pesquisa (
    id UUID PRIMARY KEY,
    participante_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    versao_termo VARCHAR(20) NOT NULL,  -- versao do TCLE a que a decisao se refere (ex.: "v1")
    decisao VARCHAR(10) NOT NULL,       -- ACEITE | RECUSA (RECUSA apos ACEITE = revogacao)
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Le-se por versao (montar a coorte) e por participante (mostrar o proprio historico).
CREATE INDEX idx_consentimentos_versao ON consentimentos_pesquisa(versao_termo);
CREATE INDEX idx_consentimentos_participante ON consentimentos_pesquisa(participante_id);
