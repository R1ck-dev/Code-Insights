-- V6: vincula (opcionalmente) um snippet a um desafio. A coluna e NULLABLE: o
-- snippet continua sendo da biblioteca pessoal do autor e pode nao ter desafio.
-- Ao remover o desafio, o vinculo e desfeito (SET NULL) e o snippet sobrevive
-- como snippet global.

ALTER TABLE snippets
    ADD COLUMN desafio_id UUID REFERENCES desafios(id) ON DELETE SET NULL;

CREATE INDEX idx_snippets_desafio ON snippets(desafio_id);
