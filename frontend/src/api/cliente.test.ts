import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { chamar, dataDoDispositivo, definirAoPerderSessao, ErroDaApi } from './cliente'

const perderSessao = vi.fn()

beforeEach(() => {
  definirAoPerderSessao(perderSessao)
})

afterEach(() => {
  vi.unstubAllGlobals()
  perderSessao.mockReset()
})

function resposta(status: number, corpo?: unknown): Response {
  return new Response(corpo === undefined ? null : JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function caminhos(fetch: ReturnType<typeof vi.fn>): string[] {
  return fetch.mock.calls.map(([url]) => String(url))
}

// Pega o dia tirado do UTC: às 23h30 de 28/10 em São Paulo, o UTC já está em
// 29/10 (RN-04). O fuso dos testes é fixado no vite.config.ts
test('o Data-Local é o dia do calendário do dispositivo', () => {
  expect(dataDoDispositivo(new Date('2026-10-29T02:30:00Z'))).toBe('2026-10-28')
})

// Pega a sessão perdida em vez de renovada, e a chamada que não se repete
// depois da renovação (endpoints.md)
test('com 401, renova uma vez e repete a chamada', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(resposta(401))
    .mockResolvedValueOnce(resposta(200))
    .mockResolvedValueOnce(resposta(200, { nome: 'Fulana' }))
  vi.stubGlobal('fetch', fetch)

  await expect(chamar('/conta')).resolves.toEqual({ nome: 'Fulana' })
  expect(caminhos(fetch)).toEqual(['/api/v1/conta', '/api/v1/auth/renovacao', '/api/v1/conta'])
  expect(perderSessao).not.toHaveBeenCalled()
})

// Pega a renovação em laço e a pessoa deixada numa tela que não carrega
test('sem renovação, vai para a entrada sem repetir a chamada', async () => {
  const fetch = vi.fn().mockResolvedValue(resposta(401))
  vi.stubGlobal('fetch', fetch)

  await expect(chamar('/conta')).rejects.toBeInstanceOf(ErroDaApi)
  expect(caminhos(fetch)).toEqual(['/api/v1/conta', '/api/v1/auth/renovacao'])
  expect(perderSessao).toHaveBeenCalledOnce()
})

// Pega duas renovações com o mesmo token, que disputariam a sessão: a tela
// principal e a lista de fases pedem ao mesmo tempo quando o acesso vence
test('chamadas com 401 ao mesmo tempo esperam a mesma renovação', async () => {
  const vistas = new Set<string>()
  const fetch = vi.fn((url: string) => {
    if (url.endsWith('/auth/renovacao')) {
      return Promise.resolve(resposta(200))
    }
    const primeira = !vistas.has(url)
    vistas.add(url)
    return Promise.resolve(primeira ? resposta(401) : resposta(200, []))
  })
  vi.stubGlobal('fetch', fetch)

  await Promise.all([chamar('/acompanhamento'), chamar('/fases')])
  expect(caminhos(fetch).filter((url) => url.endsWith('/auth/renovacao'))).toHaveLength(1)
})

// Pega a mensagem do backend trocada por uma do front (convencoes-frontend.md,
// antipadrão 8) e o código perdido, que é por onde a tela decide
test('a recusa chega com o código, o campo e a mensagem do backend', async () => {
  const erro = {
    codigo: 'REGISTRO_JA_EXISTE',
    campo: 'data',
    mensagem: 'Já existe registro em 07/09/2026, com 82,4 kg.',
    regra: 'RN-01',
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resposta(409, { erro })))

  await expect(chamar('/registros', { metodo: 'POST', corpo: {} })).rejects.toMatchObject({
    status: 409,
    codigo: 'REGISTRO_JA_EXISTE',
    campo: 'data',
    message: 'Já existe registro em 07/09/2026, com 82,4 kg.',
  })
})
