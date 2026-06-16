-- V3: Contexto knowledge - resolucoes (solucao submetida para um desafio).
-- As colunas de metrica sao NULLABLE e ficam vazias neste ciclo: a extracao/calculo
-- (autonomia IA 1-5, Big O de tempo/espaco, complexidade ciclomatica) sera adicionada
-- depois, por adicao, sem alterar este schema.

CREATE TABLE resolucoes (
    id UUID PRIMARY KEY,
    desafio_id UUID NOT NULL REFERENCES desafios(id) ON DELETE CASCADE,
    autor_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    linguagem VARCHAR(50),
    codigo_fonte TEXT NOT NULL,
    indice_autonomia_ia INT CHECK (indice_autonomia_ia >= 1 AND indice_autonomia_ia <= 5),
    complexidade_tempo VARCHAR(50),     -- ex: O(n log n)
    complexidade_espaco VARCHAR(50),    -- ex: O(n)
    complexidade_ciclomatica INT,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resolucoes_desafio ON resolucoes(desafio_id);
CREATE INDEX idx_resolucoes_autor ON resolucoes(autor_id);
