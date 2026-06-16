---
name: relatorio-semanal-ic
description: >-
  Gera o relatório parcial semanal da Iniciação Científica (PIBIFSP/IFSP) do projeto
  CodeInsights, no mesmo formato e linguagem dos relatórios anteriores em
  docs/ifsp-documentacao/. Use sempre que o usuário pedir o "relatório da semana", a
  "documentação semanal", a "próxima semana da IC", o "relatório parcial", ou disser algo
  como "documenta o que fiz essa semana" / "fecha a semana N" — mesmo sem usar a palavra
  "relatório". A skill descobre qual é a próxima semana, levanta o que foi feito no período
  a partir do git e de uma entrevista, e redige o documento seguindo o Modelo Base do IFSP.
---

# Relatório Semanal da Iniciação Científica — CodeInsights

Esta skill produz o **relatório parcial semanal** da IC e o salva como Markdown em
[docs/ifsp-documentacao/](../../../docs/ifsp-documentacao/). Cada relatório segue a estrutura
fixa do `Modelo Base.pdf` e reproduz o tom acadêmico-formal da `Semana 1`.

O objetivo do documento é registrar, semana a semana, o avanço da pesquisa — tanto o trabalho
técnico (código) quanto o intelectual (leitura de referencial, decisões de arquitetura,
definição de métricas). O relatório é um artefato institucional assinado pela orientadora e
pelo bolsista, então a precisão factual importa: **não invente atividades, resultados ou
referências.** Tudo o que entra no relatório vem do git ou do que o usuário confirmar.

## Fluxo de trabalho

### 1. Descobrir qual é a próxima semana

Liste os arquivos de `docs/ifsp-documentacao/` no padrão `Semana N (dd-mm a dd-mm)`. O maior
`N` é a última semana fechada; a nova semana é `N+1`. Guarde a **data final** da semana
anterior — ela sugere o início da nova (as semanas costumam ser contíguas, mas não têm
exatamente 7 dias: a Semana 1 foi 03/06 a 11/06).

### 2. Confirmar o período

Pergunte ao usuário o **intervalo de datas** da nova semana, propondo um padrão a partir da
data final anterior (ex.: anterior terminou em 11/06 → sugira "12/06 a …"). Use
`AskUserQuestion` ou uma pergunta direta. O período define tanto o nome do arquivo quanto a
frase de abertura das Atividades.

### 3. Levantar o que foi feito (git + entrevista)

Trabalho técnico vem do **git**:

```bash
git log --since=<inicio> --until=<fim> --pretty=format:"%h %ad %s" --date=short
git log --since=<inicio> --until=<fim> -p --stat   # diffs e arquivos tocados, se precisar de detalhe
```

Leia os commits e, quando necessário, os diffs/arquivos alterados para entender o **trabalho
concreto** (entidades criadas, infraestrutura configurada, endpoints, migrations, etc.).
Converta isso em linguagem de pesquisa — não liste commits crus; descreva a atividade e o
porquê dela dentro dos objetivos do projeto.

Muito do trabalho de uma IC **não vira commit** (leitura de artigos, decisões de escopo,
reuniões de orientação, estudos de viabilidade). Por isso, **entreviste o usuário** para
preencher lacunas. Faça perguntas como:

- Que leituras / referencial bibliográfico novo entrou nesta semana? (vira REFERÊNCIAS)
- Houve alguma decisão de arquitetura, escopo ou metodologia? (vira RESULTADOS OBTIDOS)
- Algo relevante que você fez e não está no git?
- Qual o foco/objetivo específico desta fase da pesquisa? (ajusta o OBJETIVO)

Prefira `AskUserQuestion` para as escolhas fechadas e perguntas abertas em texto para o resto.
Monte um rascunho mental antes de perguntar, para entrevistar de forma cirúrgica em vez de
fazer o usuário escrever tudo do zero.

### 4. Redigir o relatório

Use o scaffold de [assets/modelo-relatorio.md](assets/modelo-relatorio.md) como ponto de
partida: ele contém as partes **constantes** (cabeçalho, INTRODUÇÃO, objetivo geral, rodapé)
que devem ser reproduzidas **verbatim**, e marcadores `«…»` para as partes que variam por
semana. Preencha:

- **ATIVIDADES REALIZADAS** — frase de abertura com o período, depois `Atividade 1..N`. Cada
  atividade tem um **nome em negrito** e uma descrição de **4 a 10 linhas**. Agrupe o trabalho
  da semana em poucas atividades coerentes (não uma por commit).
- **RESULTADOS OBTIDOS** — narrativa do que foi alcançado e, principalmente, das **decisões**
  tomadas e seus motivos. É aqui que o avanço vira conhecimento.
- **CRONOGRAMA FINAL** — lista numerada das próximas etapas, em conformidade com o Plano de
  Trabalho.
- **REFERÊNCIAS** — só inclua se houve referencial novo na semana; formato **ABNT**. Se não
  houve, omita a seção (não repita as referências de semanas anteriores).
- **OBJETIVO ou PROPOSIÇÃO** — mantenha o objetivo geral constante e ajuste apenas a frase do
  **objetivo específico** à fase atual da pesquisa (na Semana 1 era levantamento bibliográfico;
  conforme o projeto avança, passa a modelagem, implementação, coleta de dados, etc.).

Antes de escrever, leia o guia de tom em [references/estilo.md](references/estilo.md) e
inspire-se na `Semana 1` já existente para manter a voz consistente.

### 5. Salvar

Salve em `docs/ifsp-documentacao/Semana N (dd-mm a dd-mm).md`. Depois, **mostre o relatório
completo ao usuário** no chat (o pedido é "me passar a documentação da nova semana") e avise o
caminho do arquivo. O usuário converterá para PDF depois (Google Docs/Word), mantendo o padrão
dos relatórios anteriores.

## Princípios

- **Fidelidade acima de tudo.** Um relatório de IC é avaliado por uma orientadora que sabe o
  que aconteceu. Atividade ou resultado inventado é pior que uma semana modesta bem descrita.
  Em dúvida, pergunte.
- **Continuidade.** Cada semana retoma de onde a anterior parou. Leia o relatório anterior para
  que o CRONOGRAMA FINAL da semana passada vire as ATIVIDADES da atual quando fizer sentido.
- **Linguagem de pesquisa, não de changelog.** Traduza commits e código em contribuições para
  os objetivos científicos do projeto (autonomia frente à IA, complexidade algorítmica, gestão
  de conhecimento).
