# Hooks do projeto CodeInsights

Dois hooks que reforçam comportamentos do Claude Code automaticamente. Ficam
registrados em [`../settings.json`](../settings.json) e rodam via Node.js a
partir da raiz do projeto.

## 1. `skill-reminder.js` — analisar skills antes de alterar o projeto

- **Evento:** `PreToolUse` com `matcher` `Edit|Write|MultiEdit|NotebookEdit`.
- **O que faz:** **uma vez por sessão**, logo antes da primeira edição de arquivo,
  injeta um lembrete para revisar as skills disponíveis e invocar (via ferramenta
  `Skill`) a que melhor se aplica ao cenário antes de mexer no código.
- **Controle de "uma vez":** cria um arquivo-marcador em `%TEMP%`
  (`claude-skill-reminder-<session_id>.flag`). Apague-o para forçar o lembrete de
  novo na mesma sessão. Os marcadores são inofensivos e o SO limpa o `TEMP`.
- **Não bloqueia** a edição — apenas adiciona contexto.

## 2. `interview-reminder.js` — entrevistar o usuário em escolhas

- **Evento:** `UserPromptSubmit` (a cada pedido).
- **O que faz:** injeta um lembrete para, quando o pedido envolver uma escolha real
  entre caminhos/abordagens (sem default claramente melhor), pausar e usar a
  ferramenta `AskUserQuestion` em vez de presumir.
- **Não bloqueia** o prompt — apenas adiciona contexto.

## Mecanismo

Ambos leem o JSON do evento via STDIN e respondem (exit 0) com:

```json
{ "hookSpecificOutput": { "hookEventName": "...", "additionalContext": "..." }, "suppressOutput": true }
```

`additionalContext` entra no contexto do Claude; `suppressOutput: true` evita
poluir a UI.

## Testar manualmente

```powershell
# Hook 1 (simula a 1ª edição de uma sessão de teste):
'{"session_id":"teste-1","hook_event_name":"PreToolUse","tool_name":"Edit"}' | node .claude/hooks/skill-reminder.js
# Rodar de novo com o mesmo session_id deve sair em silêncio (marcador já existe).

# Hook 2:
'{"hook_event_name":"UserPromptSubmit","prompt":"qualquer coisa"}' | node .claude/hooks/interview-reminder.js
```

Cada um deve imprimir o JSON com `additionalContext`. Depois de editar os hooks
ou o `settings.json`, rode `/hooks` no Claude Code (ou reinicie a sessão) para
recarregar a configuração.

## Desativar

Remova o bloco correspondente em [`../settings.json`](../settings.json) (ou todo o
objeto `hooks`). Os arquivos `.js` podem ficar; sem registro, não são executados.
