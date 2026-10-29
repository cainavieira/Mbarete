import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { apiFalsa, corpos, descricao, recusa, type Resposta } from '@/testes/apiFalsa'
import { Conta } from './Conta'
import { Entrada } from './Entrada'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const CONTA: Resposta = [200, { nome: 'Marina Costa', email: 'marina@exemplo.com', foto: null }]

function abrir(rotas: Record<string, Resposta[]>) {
  const fetch = apiFalsa({ 'GET /conta': [CONTA], ...rotas })
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={['/conta']}>
        <Routes>
          <Route path="/conta" element={<Conta />} />
          <Route path="/entrar" element={<Entrada />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { fetch, pessoa: userEvent.setup() }
}

// Pega o e-mail digitado que não é o mandado, e a volta à entrada sem o
// aviso de que a conta foi apagada, que o UC-12 3 pede
test('apagar manda o e-mail digitado e a entrada confirma a exclusão', async () => {
  const { fetch, pessoa } = abrir({ 'POST /conta/exclusao': [[204]] })

  await pessoa.click(await screen.findByRole('button', { name: 'Apagar a conta' }))
  await pessoa.type(screen.getByLabelText(/Para confirmar, digite seu e-mail/), 'marina@exemplo.com')
  await pessoa.click(screen.getByRole('button', { name: 'Apagar a conta' }))

  expect(await screen.findByText('A conta foi apagada.')).toBeTruthy()
  expect(corpos(fetch, 'POST /conta/exclusao')).toEqual([{ email: 'marina@exemplo.com' }])
})

// Pega o e-mail da conta mandado no lugar do digitado, que tornaria a
// confirmação inútil, e a recusa fora do campo, ou o diálogo fechado por ela,
// que faria a pessoa achar que a conta saiu (UC-12 2a)
test('o e-mail que não confere fica no campo, e a conta continua', async () => {
  const { fetch, pessoa } = abrir({
    'POST /conta/exclusao': [
      recusa(400, 'EMAIL_NAO_CONFERE', 'email', 'O e-mail digitado não é o da conta.'),
    ],
  })

  await pessoa.click(await screen.findByRole('button', { name: 'Apagar a conta' }))
  const campo = screen.getByLabelText(/Para confirmar, digite seu e-mail/)
  await pessoa.type(campo, 'marina@exemplo.co')
  await pessoa.click(screen.getByRole('button', { name: 'Apagar a conta' }))

  expect(await screen.findByText('O e-mail digitado não é o da conta.')).toBeTruthy()
  expect(descricao(campo)).toContain('O e-mail digitado não é o da conta.')
  expect(corpos(fetch, 'POST /conta/exclusao')).toEqual([{ email: 'marina@exemplo.co' }])
  expect(screen.queryByText('A conta foi apagada.')).toBeNull()
})

// Pega o sair que fica na tela, com a sessão ainda viva no cookie (RF-04)
test('sair encerra a sessão e volta à entrada', async () => {
  const { fetch, pessoa } = abrir({ 'POST /auth/logout': [[204]] })

  await pessoa.click(await screen.findByRole('button', { name: 'Sair' }))

  expect(await screen.findByRole('link', { name: /Entrar com o Google/ })).toBeTruthy()
  expect(fetch.mock.calls.some(([url]) => url === '/api/v1/auth/logout')).toBe(true)
  expect(screen.queryByText('A conta foi apagada.')).toBeNull()
})
