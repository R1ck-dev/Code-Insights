/**
 * Sinal same-origin de "conta ativada".
 *
 * Permite que a aba parada em /verifique-email reaja quando a conta é ativada
 * em outra aba (o link do e-mail abre /ativar na mesma origem). Usa
 * BroadcastChannel quando disponível, com fallback para o evento `storage` do
 * localStorage. Não envolve backend nem expõe status por e-mail — funciona
 * apenas dentro do mesmo navegador/origem.
 */
const CANAL = 'codeinsights:conta-ativada'

type Payload = { email: string | null; ts: number }

/** Avisa as outras abas da mesma origem que a conta foi ativada. */
export function emitirContaAtivada(email?: string | null): void {
  const payload = JSON.stringify({ email: email ?? null, ts: Date.now() } satisfies Payload)
  try {
    if ('BroadcastChannel' in window) {
      const bc = new BroadcastChannel(CANAL)
      bc.postMessage(payload)
      bc.close()
    }
  } catch {
    /* BroadcastChannel indisponível — segue no fallback */
  }
  try {
    // `ts` garante um valor novo a cada emissão, disparando o evento 'storage'.
    localStorage.setItem(CANAL, payload)
  } catch {
    /* localStorage indisponível — sem fallback */
  }
}

/** Assina o sinal de ativação. Retorna uma função de cancelamento. */
export function ouvirContaAtivada(callback: (email: string | null) => void): () => void {
  const entregar = (raw: string) => {
    try {
      callback((JSON.parse(raw) as Payload).email)
    } catch {
      callback(null)
    }
  }

  let bc: BroadcastChannel | null = null
  try {
    if ('BroadcastChannel' in window) {
      bc = new BroadcastChannel(CANAL)
      bc.onmessage = (e) => entregar(String(e.data))
    }
  } catch {
    bc = null
  }

  const onStorage = (e: StorageEvent) => {
    if (e.key === CANAL && e.newValue) entregar(e.newValue)
  }
  window.addEventListener('storage', onStorage)

  return () => {
    bc?.close()
    window.removeEventListener('storage', onStorage)
  }
}
