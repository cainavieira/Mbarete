import { vi } from 'vitest'

/** O que uma rota responde: o status e o corpo, como o backend os manda. */
export type Resposta = [status: number, corpo?: unknown]

/**
 * O fetch no lugar da api, por método e caminho ("POST /fases"). Cada rota
 * responde a lista na ordem, e a última se repete. Rota sem resposta falha o
 * teste, em vez de virar um 404 que esconde a chamada a mais.
 */
export function apiFalsa(rotas: Record<string, Resposta[]>) {
  const pendentes = new Map(Object.entries(rotas).map(([chave, lista]) => [chave, [...lista]]))
  const fetch = vi.fn(async (url: string, init?: RequestInit) => {
    const chave = `${init?.method ?? 'GET'} ${url.replace(/^\/api\/v1/, '')}`
    const lista = pendentes.get(chave)
    if (lista === undefined || lista.length === 0) {
      throw new Error(`A api falsa não tem resposta para ${chave}`)
    }
    const [status, corpo] = lista.length > 1 ? (lista.shift() ?? lista[0]) : lista[0]
    return new Response(corpo === undefined ? null : JSON.stringify(corpo), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', fetch)
  return fetch
}

/** Os corpos mandados a uma rota, na ordem. */
export function corpos(fetch: ReturnType<typeof apiFalsa>, chave: string): unknown[] {
  return fetch.mock.calls
    .filter(([url, init]) => `${init?.method ?? 'GET'} ${url.replace(/^\/api\/v1/, '')}` === chave)
    .map(([, init]) => JSON.parse(String(init?.body)))
}

export function recusa(status: number, codigo: string, campo: string | null, mensagem: string): Resposta {
  return [status, { erro: { codigo, campo, mensagem, regra: null } }]
}

/** O que o leitor de tela anuncia junto do campo, pelo aria-describedby. */
export function descricao(campo: HTMLElement): string {
  return (campo.getAttribute('aria-describedby') ?? '')
    .split(' ')
    .map((id) => document.getElementById(id)?.textContent ?? '')
    .join(' ')
    .trim()
}
