-- V7: confianca do motor de analise estatica no valor que ele proprio calculou.
-- Inferir complexidade de codigo arbitrario e indecidivel: em varios pontos o motor assume um
-- default ("este laco roda n vezes", "esta chamada externa custa O(1)"). Registrar isso separa
-- medicao de chute e permite filtrar a amostra na analise empirica da pesquisa.
-- ALTA = nenhuma suposicao; MEDIA = ao menos um default conservador; BAIXA = nao classificado.
-- Linhas ja existentes foram produzidas pelo motor antigo, que nunca registrava suposicoes:
-- marca-las como MEDIA seria mentir para baixo e ALTA, para cima. Ficam BAIXA e serao
-- recalculadas na proxima submissao/reanalise.

ALTER TABLE resultados_metrica
    ADD COLUMN confianca VARCHAR(10) NOT NULL DEFAULT 'BAIXA';

ALTER TABLE resultados_metrica
    ALTER COLUMN confianca DROP DEFAULT;
