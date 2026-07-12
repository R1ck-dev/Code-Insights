/*
 * PLATAFORMAS DE ORIGEM — os juízes online de onde o desafio veio.
 *
 * É uma SUGESTÃO, não um enum. O campo `plataformaOrigem` é `String` livre no backend, e continua
 * sendo: um aluno pode registrar um desafio da lista da disciplina, de um livro, de um processo
 * seletivo. Fechar a lista num enum obrigaria uma migration a cada juiz novo — e um aluno com um
 * desafio de plataforma nem-listada ficaria sem casa.
 *
 * O que a lista faz é EVITAR AS VARIANTES: sem ela, o mesmo juiz entra como "beecrowd", "Beecrowd",
 * "BeeCrowd" e "URI", e o filtro por plataforma (que agrupa por texto exato) trata os quatro como
 * lugares diferentes. Uma grafia canônica sugerida resolve isso sem proibir nada.
 *
 * Ordem: os mais prováveis no contexto do piloto (clube de programação competitiva no Brasil)
 * primeiro — a lista é filtrada por digitação, mas quem só abre o menu vê o que provavelmente quer.
 */
export const PLATAFORMAS: readonly string[] = [
  'beecrowd',
  'Neps Academy',
  'Codeforces',
  'LeetCode',
  'AtCoder',
  'SPOJ',
  'HackerRank',
  'CSES',
  'CodeChef',
  'UVa Online Judge',
  'Kattis',
  'AceptaElReto',
  'Advent of Code',
  'Project Euler',
]
