#!/usr/bin/env node
/*
 * Hook: interview-reminder  (evento: UserPromptSubmit)
 *
 * Objetivo: indicar que o usuário deve ser entrevistado sempre que necessário —
 * isto é, quando atender ao pedido envolver uma escolha real entre caminhos/
 * abordagens/trade-offs diferentes e não houver um padrão claramente melhor.
 *
 * Comportamento: injeta um lembrete (additionalContext) a cada pedido do usuário.
 * Não bloqueia nem altera o prompt; apenas reforça o uso da ferramenta
 * AskUserQuestion no momento certo.
 */
'use strict';

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (chunk) => { raw += chunk; });
process.stdin.on('end', () => {
  // O conteúdo do prompt não é necessário aqui; consumimos o stdin e seguimos.

  const reminder = [
    'LEMBRETE: se atender a este pedido envolver uma ESCOLHA real entre caminhos viáveis',
    '(arquitetura, biblioteca/dependência, modelagem de dados, design de API/UX, estratégia de migração, etc.)',
    'e não houver um default claramente superior, PAUSE e entreviste o usuário com a ferramenta AskUserQuestion',
    'antes de decidir — apresente as opções com seus trade-offs em vez de presumir.',
    'Se houver um padrão óbvio, ou a escolha for trivial/facilmente reversível, apenas siga e explicite a decisão tomada.'
  ].join(' ');

  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: 'UserPromptSubmit',
      additionalContext: reminder
    },
    suppressOutput: true
  }));
  process.exit(0);
});
