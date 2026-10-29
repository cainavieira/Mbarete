import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { dataDoDispositivo } from '@/api/cliente'
import type { FaseListada } from '@/hooks/useFases'
import { apiFalsa, corpos, descricao, recusa, type Resposta } from '@/testes/apiFalsa'
import { EditarFase } from './EditarFase'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const NIVEIS: Resposta = [
  200,
  [
    { tipo: 'CUTTING', nivel: 'MODERADO', valor: 0.5, unidade: 'PERCENTUAL_SEMANA' },
    { tipo: 'BULKING', nivel: 'MODERADO', valor: 0.3, unidade: 'KG_SEMANA' },
  ],
]

/** O cutting corrente, com o fim no futuro que a manutenção agendada pôs (RN-67). */
const CUTTING: FaseListada = {
  id: 7,
  tipo: 'CUTTING',
  dataInicio: '2026-08-03',
  dataFim: '2026-11-02',
  nivel: 'MODERADO',
  taxaAlvoValor: 0.5,
  taxaAlvoUnidade: 'PERCENTUAL_SEMANA',
  pesoAlvo: 75,
  situacao: 'CORRENTE',
  registros: 70,
  aoApagar: null,
  historico: null,
}

const AGENDADA: FaseListada = {
  ...CUTTING,
  id: 8,
  tipo: 'MANUTENCAO',
  dataInicio: '2026-11-03',
  dataFim: null,
  nivel: null,
  taxaAlvoValor: null,
  taxaAlvoUnidade: null,
  pesoAlvo: null,
  situacao: 'FUTURA',
  registros: 0,
  aoApagar: { fase: 7, dataFim: null },
}

function abrir(id: number, rotas: Record<string, Resposta[]> = {}) {
  const fetch = apiFalsa({
    'GET /niveis': [NIVEIS],
    'GET /fases': [[200, [CUTTING, AGENDADA]]],
    ...rotas,
  })
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[`/fases/${id}`]}>
        <Routes>
          <Route path="/fases/:id" element={<EditarFase />} />
          <Route path="/fases" element={<p>Lista de fases</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { fetch, pessoa: userEvent.setup() }
}

// Pega o formulário aberto sem o fim no futuro, que salvaria a corrente em
// aberto por cima da agendada, ou sem o plano gravado (RN-67, UC-06)
test('salvar sem mexer manda a fase como ela está', async () => {
  const { fetch, pessoa } = abrir(7, { 'PATCH /fases/7': [[200, {}]] })

  await pessoa.click(await screen.findByRole('button', { name: 'Salvar' }))

  expect(corpos(fetch, 'PATCH /fases/7')).toEqual([
    {
      tipo: 'CUTTING',
      dataInicio: '2026-08-03',
      dataFim: '2026-11-02',
      nivel: 'MODERADO',
      taxaAlvoValor: 0.5,
      taxaAlvoUnidade: 'PERCENTUAL_SEMANA',
      pesoAlvo: 75,
    },
  ])
})

// Pega o fim da anterior movido sem o sim, ou o segundo envio sem a
// confirmação, que voltaria 409 de novo (RN-67, UC-06 2i)
test('mudar o início que move o fim da anterior só salva com o sim', async () => {
  const { fetch, pessoa } = abrir(8, {
    'PATCH /fases/8': [
      recusa(409, 'FIM_DA_ANTERIOR_SERA_MOVIDO', 'dataInicio', 'O cutting iniciado em 03/08/2026 passa a terminar em 09/11/2026.'),
      [200, {}],
    ],
  })

  await pessoa.click(await screen.findByRole('button', { name: 'Salvar' }))
  expect(await screen.findByRole('alertdialog', { name: 'Mudar também o fim do cutting?' })).toBeTruthy()
  await pessoa.click(screen.getByRole('button', { name: 'Salvar e mover' }))

  expect(await screen.findByText('Lista de fases')).toBeTruthy()
  const [primeiro, segundo] = corpos(fetch, 'PATCH /fases/8')
  expect(primeiro).not.toHaveProperty('moverFimDaAnterior')
  expect(segundo).toMatchObject({ moverFimDaAnterior: true })
})

// Pega a confirmação de apagar sem o efeito no fim da anterior, que vem no
// aoApagar, e o encerrar oferecido a quem não é corrente (UC-07 3b, RN-27)
test('apagar a agendada diz que a corrente volta a ficar em aberto', async () => {
  const { fetch, pessoa } = abrir(8, { 'DELETE /fases/8': [[204]] })

  // Antes do diálogo, que esconde o resto da tela do leitor
  expect(await screen.findByRole('button', { name: 'Apagar fase' })).toBeTruthy()
  expect(screen.queryByRole('button', { name: 'Encerrar fase' })).toBeNull()
  await pessoa.click(screen.getByRole('button', { name: 'Apagar fase' }))

  expect(
    screen.getByText('O cutting corrente perde o fim em 02/11/2026 e volta a ficar em aberto.'),
  ).toBeTruthy()
  await pessoa.click(screen.getByRole('button', { name: 'Apagar fase' }))
  expect(await screen.findByText('Lista de fases')).toBeTruthy()
  expect(fetch.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(true)
})

// Pega a data de encerramento que não é a escolhida, e a recusa do backend
// fora do diálogo, onde não se vê (UC-14 2a)
test('encerrar manda a data, e a recusa fica embaixo dela', async () => {
  const { fetch, pessoa } = abrir(7, {
    'POST /fases/7/encerramento': [
      recusa(400, 'DATA_FIM_FUTURA', 'dataFim', 'A data de fim não pode estar no futuro.'),
    ],
  })

  await pessoa.click(await screen.findByRole('button', { name: 'Encerrar fase' }))
  await pessoa.click(screen.getByRole('button', { name: 'Encerrar' }))

  expect(corpos(fetch, 'POST /fases/7/encerramento')).toEqual([{ dataFim: dataDoDispositivo() }])
  expect(
    descricao(await screen.findByRole('button', { name: /Data de encerramento/ })),
  ).toContain('A data de fim não pode estar no futuro.')
})

// Pega a troca para manutenção sem o aviso de que o plano vai embora, que o
// UC-06 2c pede antes de salvar
test('virar manutenção avisa que o plano será descartado', async () => {
  const { pessoa } = abrir(7)

  await pessoa.click(await screen.findByRole('radio', { name: 'manutenção' }))

  expect(screen.getByText('Nível, taxa alvo e peso alvo desta fase serão descartados.')).toBeTruthy()
})
