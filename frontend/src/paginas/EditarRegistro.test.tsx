import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { apiFalsa, corpos, recusa, type Resposta } from '@/testes/apiFalsa'
import { EditarRegistro } from './EditarRegistro'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const GRAVADO: Resposta = [200, [{ data: '2026-10-26', peso: 78, cintura: 88.1 }]]

function abrir(rotas: Record<string, Resposta[]>) {
  const fetch = apiFalsa({ 'GET /registros?de=2026-10-26&ate=2026-10-26': [GRAVADO], ...rotas })
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={['/historico/2026-10-26']}>
        <Routes>
          <Route path="/historico/:data" element={<EditarRegistro />} />
          <Route path="/historico" element={<p>Lista de registros</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { fetch, pessoa: userEvent.setup() }
}

// Pega a cintura apagada no campo e mandada mesmo assim: o PATCH troca o
// registro inteiro, e a cintura vazia é a que some (UC-03)
test('abre com o que está gravado, e a cintura apagada vai sem cintura', async () => {
  const { fetch, pessoa } = abrir({ 'PATCH /registros/2026-10-26': [[200, {}]] })

  expect((await screen.findByLabelText<HTMLInputElement>('Peso')).value).toBe('78,0')
  await pessoa.clear(screen.getByLabelText(/Cintura/))
  await pessoa.click(screen.getByRole('button', { name: 'Salvar' }))

  expect(await screen.findByText('Lista de registros')).toBeTruthy()
  expect(corpos(fetch, 'PATCH /registros/2026-10-26')).toEqual([{ data: '2026-10-26', peso: 78 }])
})

// Pega a data ocupada substituída sem o sim, ou o segundo envio sem a
// substituição, que voltaria 409 de novo (UC-03 2a)
test('mover para data ocupada só substitui com o sim', async () => {
  const { fetch, pessoa } = abrir({
    'PATCH /registros/2026-10-26': [
      recusa(409, 'REGISTRO_JA_EXISTE', 'data', 'Já existe registro em 25/10/2026, com 78,0 kg.'),
      [200, {}],
    ],
  })

  await pessoa.click(await screen.findByRole('button', { name: 'Salvar' }))
  await pessoa.click(await screen.findByRole('button', { name: 'Substituir' }))

  expect(await screen.findByText('Lista de registros')).toBeTruthy()
  const [primeiro, segundo] = corpos(fetch, 'PATCH /registros/2026-10-26')
  expect(primeiro).not.toHaveProperty('substituir')
  expect(segundo).toMatchObject({ substituir: true })
})

// Pega o registro apagado sem o sim, e o diálogo sem o que se perde (UC-04)
test('apagar mostra o que se perde e só apaga com o sim', async () => {
  const { fetch, pessoa } = abrir({ 'DELETE /registros/2026-10-26': [[204]] })

  await pessoa.click(await screen.findByRole('button', { name: 'Apagar registro' }))
  expect(screen.getByText(/78,0 kg e 88,1 cm de cintura\. Apagar não se desfaz\./)).toBeTruthy()
  expect(fetch.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(false)
  await pessoa.click(screen.getByRole('button', { name: 'Apagar' }))

  expect(await screen.findByText('Lista de registros')).toBeTruthy()
  expect(fetch.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(true)
})
