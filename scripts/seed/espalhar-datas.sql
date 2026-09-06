-- Espalha as datas do seed ao longo dos ultimos 6 meses.
--
-- A API sempre carimba "agora". Sem este passe, as 100+ resolucoes teriam o mesmo timestamp e o
-- grafico de evolucao — que e o argumento central da IC — mostraria uma coluna unica.
--
-- Nao ha data aleatoria: a ordem de insercao ja e a ordem cronologica da trajetoria de cada aluno
-- (o seed submete na ordem em que a pessoa evoluiu), entao basta redistribuir essa ordem no
-- intervalo. O deslocamento em minutos vem do hash do proprio id, para as submissoes nao cairem
-- todas no mesmo horario redondo sem introduzir aleatoriedade de verdade: rodar duas vezes da o
-- mesmo resultado.
--
-- Rodar SOMENTE depois que todas as resolucoes estiverem analisadas.

BEGIN;

-- 1. Resolucoes: a i-esima submissao do aluno cai na fracao i/(n-1) da janela de 175 dias.
WITH ordenadas AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY autor_id ORDER BY submetida_em, id) - 1 AS posicao,
           COUNT(*) OVER (PARTITION BY autor_id) AS total
    FROM resolucoes
)
UPDATE resolucoes r
SET submetida_em = NOW()
        - INTERVAL '178 days'
        + (o.posicao::numeric / GREATEST(o.total - 1, 1)) * INTERVAL '175 days'
        + (abs(hashtext(r.id::text)) % 540) * INTERVAL '1 minute'
FROM ordenadas o
WHERE o.id = r.id;

-- 2. Metricas: analisadas logo apos a submissao que as originou.
UPDATE resultados_metrica m
SET analisado_em = r.submetida_em + INTERVAL '4 seconds'
FROM resolucoes r
WHERE r.id = m.resolucao_id;

-- 3. Desafios: registrados um pouco antes da primeira resolucao que receberam.
UPDATE desafios d
SET criado_em = primeira.momento - INTERVAL '2 hours',
    atualizado_em = primeira.momento - INTERVAL '2 hours'
FROM (
    SELECT desafio_id, MIN(submetida_em) AS momento
    FROM resolucoes
    GROUP BY desafio_id
) primeira
WHERE primeira.desafio_id = d.id;

-- Desafio sem nenhuma resolucao nao entra no passe acima e ficaria com a data de hoje, o que o
-- faria aparecer no topo de "recentes" sem ter historia nenhuma.
UPDATE desafios d
SET criado_em = autor.entrada + INTERVAL '1 day',
    atualizado_em = autor.entrada + INTERVAL '1 day'
FROM (
    SELECT autor_id, MIN(criado_em) AS entrada
    FROM desafios
    GROUP BY autor_id
) autor
WHERE autor.autor_id = d.autor_id
  AND NOT EXISTS (SELECT 1 FROM resolucoes r WHERE r.desafio_id = d.id);

-- 4. Snippets: espalhados na janela de cada autor, entre o primeiro e o ultimo desafio dele.
WITH janela AS (
    SELECT autor_id, MIN(criado_em) AS inicio, MAX(criado_em) AS fim
    FROM desafios
    GROUP BY autor_id
),
ordenados AS (
    SELECT id, autor_id,
           ROW_NUMBER() OVER (PARTITION BY autor_id ORDER BY criado_em, id) - 1 AS posicao,
           COUNT(*) OVER (PARTITION BY autor_id) AS total
    FROM snippets
)
UPDATE snippets s
SET criado_em = j.inicio
        + (o.posicao::numeric / GREATEST(o.total - 1, 1)) * (j.fim - j.inicio)
        + (abs(hashtext(s.id::text)) % 300) * INTERVAL '1 minute'
FROM ordenados o
JOIN janela j ON j.autor_id = o.autor_id
WHERE o.id = s.id;

-- 5. Contas: criadas antes da primeira atividade. Sem isso, uma coorte de 6 meses apareceria como
-- 13 pessoas que se cadastraram hoje.
UPDATE usuarios u
SET criado_em = atividade.inicio - INTERVAL '3 days',
    atualizado_em = atividade.inicio - INTERVAL '3 days'
FROM (
    SELECT autor_id, MIN(criado_em) AS inicio
    FROM desafios
    GROUP BY autor_id
) atividade
WHERE atividade.autor_id = u.id;

-- 6. Consentimento: respondido no comeco da participacao, nao hoje.
UPDATE consentimentos_pesquisa c
SET registrado_em = u.criado_em + INTERVAL '2 hours'
FROM usuarios u
WHERE u.id = c.participante_id;

COMMIT;
