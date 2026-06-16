#!/usr/bin/env node
/*
 * Hook: skill-reminder  (evento: PreToolUse -> Edit|Write|MultiEdit|NotebookEdit)
 *
 * Objetivo: garantir que, ANTES de começar a alterar o projeto, as skills
 * disponíveis sejam analisadas para escolher a que melhor se aplica ao cenário.
 *
 * Comportamento: injeta um lembrete (additionalContext) UMA vez por sessão,
 * logo antes da primeira modificação de arquivo. Nas edições seguintes da mesma
 * sessão fica em silêncio (controle por arquivo-marcador em %TEMP%/$TMPDIR,
 * indexado pelo session_id). Nunca bloqueia a edição.
 */
'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (chunk) => { raw += chunk; });
process.stdin.on('end', () => {
  let sessionId = 'sem-sessao';
  try {
    const payload = JSON.parse(raw || '{}');
    if (payload && typeof payload.session_id === 'string' && payload.session_id) {
      sessionId = payload.session_id;
    }
  } catch (_) { /* entrada inválida: segue com o default */ }

  // Marcador por sessão: só lembramos uma vez por sessão.
  const safeId = sessionId.replace(/[^a-zA-Z0-9_-]/g, '_');
  const marker = path.join(os.tmpdir(), `claude-skill-reminder-${safeId}.flag`);

  if (fs.existsSync(marker)) {
    process.exit(0); // já lembrado nesta sessão
  }
  try { fs.writeFileSync(marker, new Date().toISOString()); } catch (_) { /* ok */ }

  const reminder = [
    'LEMBRETE (uma vez por sessão, antes da primeira alteração de arquivo):',
    'antes de editar, analise as skills disponíveis nesta sessão (a lista "available skills")',
    'e avalie qual melhor se aplica a esta tarefa — por exemplo clean-code, software-architecture,',
    'senior-backend, senior-fullstack, react-best-practices, database-design, supabase-postgres-best-practices,',
    'code-reviewer, systematic-debugging, docker-expert, mermaid-diagrams, etc.',
    'Se houver uma skill adequada, invoque-a com a ferramenta Skill ANTES de prosseguir com a edição.',
    'Se nenhuma se aplicar, siga normalmente.'
  ].join(' ');

  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: 'PreToolUse',
      additionalContext: reminder
    },
    suppressOutput: true
  }));
  process.exit(0);
});
