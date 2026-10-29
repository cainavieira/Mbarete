import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { dataDoDispositivo } from '@/api/cliente'
import { RegistrarPeso } from './RegistrarPeso'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function resposta(status: number, corpo?: unknown): Response {
  return new Response(corpo === undefined ? null : JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function recusa(status: number, codigo: string, campo: string | null, mensagem: string): Response {
  return resposta(status, { erro: { codigo, campo, mensagem, regra: null } })
}

function corpos(fetch: ReturnType<typeof vi.fn>): unknown[] {
  return fetch.mock.calls.map(([, init]) => JSON.parse(String((init as RequestInit).body)))
}

/** O que o leitor de tela anuncia junto do campo, pelo aria-describedby. */
function descricao(campo: HTMLElement): string {
  const ids = campo.getAttribute('aria-describedby') ?? ''
  return ids
    .split(' ')
    .map((id) => document.getElementById(id)?.textContent ?? '')
    .join(' ')
    .trim()
}

function abrir(fetch: ReturnType<typeof vi.fn>) {
  vi.stubGlobal('fetch', fetch)
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={['/registrar']}>
        <Routes>
          <Route path="/registrar" element={<RegistrarPeso />} />
          <Route path="/" element={<p>Acompanhamento</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

// Pega a segunda casa decimal mandada ao backend, que a recusaria como corpo
// malformado, sem dizer o que está errado (ADR-0025)
test('o peso com duas casas é recusado no front, sem chamar a api', async () => {
  const fetch = vi.fn()
  const pessoa = abrir(fetch)

  await pessoa.type(screen.getByLabelText('Peso'), '77,95')
  await pessoa.click(screen.getByRole('button', { name: 'Registrar' }))

  expect(descricao(screen.getByLabelText('Peso'))).toBe('Use uma casa decimal, como 78,4.')
  expect(fetch).not.toHaveBeenCalled()
})

// Pega o número enviado como o texto digitado, a cintura vazia enviada como
// número, e a data que não é a de hoje do dispositivo (RN-04, RN-62)
test('registra hoje com o peso em ponto e volta ao acompanhamento', async () => {
  const fetch = vi.fn().mockResolvedValue(resposta(201, {}))
  const pessoa = abrir(fetch)

  await pessoa.type(screen.getByLabelText('Peso'), '77,9')
  await pessoa.click(screen.getByRole('button', { name: 'Registrar' }))

  expect(await screen.findByText('Acompanhamento')).toBeDefined()
  expect(corpos(fetch)).toEqual([{ data: dataDoDispositivo(), peso: 77.9 }])
})

// Pega a recusa de regra mostrada longe do campo, ou sem o aria-describedby
// que faz o leitor de tela anunciá-la junto dele (convencoes-frontend.md)
test('a recusa do backend aparece no campo que ela destaca', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(
      recusa(400, 'CINTURA_FORA_DE_FAIXA', 'cintura', 'A cintura deve estar entre 40,0 e 200,0 cm.'),
    )
  const pessoa = abrir(fetch)

  await pessoa.type(screen.getByLabelText('Peso'), '77,9')
  await pessoa.type(screen.getByLabelText(/Cintura/), '8,5')
  await pessoa.click(screen.getByRole('button', { name: 'Registrar' }))

  await screen.findByText('A cintura deve estar entre 40,0 e 200,0 cm.')
  expect(descricao(screen.getByLabelText(/Cintura/))).toBe(
    'A cintura deve estar entre 40,0 e 200,0 cm.',
  )
  expect(descricao(screen.getByLabelText('Peso'))).toBe('')
})

// Uma resposta nova a cada uso: o corpo de uma Response só se lê uma vez
function conflito(): Response {
  return recusa(
    409,
    'REGISTRO_JA_EXISTE',
    'data',
    'Já existe registro em 28/10/2026, com 78,2 kg e 88,5 cm de cintura.',
  )
}

// Pega a substituição sem o sim da pessoa e o segundo envio sem a
// confirmação, que voltaria 409 para sempre (RN-01, UC-02 4a)
test('a data ocupada só é substituída depois do sim, no segundo envio', async () => {
  const fetch = vi.fn().mockResolvedValueOnce(conflito()).mockResolvedValueOnce(resposta(201, {}))
  const pessoa = abrir(fetch)

  await pessoa.type(screen.getByLabelText('Peso'), '77,9')
  await pessoa.click(screen.getByRole('button', { name: 'Registrar' }))
  const dialogo = await screen.findByRole('alertdialog')
  expect(dialogo.textContent).toContain(
    'Já existe registro em 28/10/2026, com 78,2 kg e 88,5 cm de cintura.',
  )
  expect(dialogo.textContent).toContain('O novo fica no lugar dele: 77,9 kg, sem cintura.')
  await pessoa.click(screen.getByRole('button', { name: 'Substituir' }))

  expect(await screen.findByText('Acompanhamento')).toBeDefined()
  expect(corpos(fetch)).toEqual([
    { data: dataDoDispositivo(), peso: 77.9 },
    { data: dataDoDispositivo(), peso: 77.9, substituir: true },
  ])
})

// Pega o "não" que grava assim mesmo, ou que tira a pessoa do formulário (UC-02 4b)
test('manter o de antes não grava e deixa o formulário como estava', async () => {
  const fetch = vi.fn().mockResolvedValueOnce(conflito())
  const pessoa = abrir(fetch)

  await pessoa.type(screen.getByLabelText('Peso'), '77,9')
  await pessoa.click(screen.getByRole('button', { name: 'Registrar' }))
  await pessoa.click(await screen.findByRole('button', { name: 'Manter o de antes' }))

  expect(screen.queryByRole('alertdialog')).toBeNull()
  expect(screen.getByLabelText<HTMLInputElement>('Peso').value).toBe('77,9')
  expect(fetch).toHaveBeenCalledOnce()
})
