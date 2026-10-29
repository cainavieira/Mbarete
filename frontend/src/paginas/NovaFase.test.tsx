import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { dataDoDispositivo } from '@/api/cliente'
import { apiFalsa, corpos, descricao, recusa, type Resposta } from '@/testes/apiFalsa'
import { NovaFase } from './NovaFase'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const NIVEIS: Resposta = [
  200,
  [
    { tipo: 'CUTTING', nivel: 'CONSERVADOR', valor: 0.25, unidade: 'PERCENTUAL_SEMANA' },
    { tipo: 'CUTTING', nivel: 'MODERADO', valor: 0.5, unidade: 'PERCENTUAL_SEMANA' },
    { tipo: 'CUTTING', nivel: 'AGRESSIVO', valor: 1, unidade: 'PERCENTUAL_SEMANA' },
    { tipo: 'BULKING', nivel: 'MODERADO', valor: 0.3, unidade: 'KG_SEMANA' },
  ],
]

const EM_ABERTO = {
  id: 3,
  tipo: 'MANUTENCAO',
  dataInicio: '2026-09-14',
  dataFim: null,
  nivel: null,
  taxaAlvoValor: null,
  taxaAlvoUnidade: null,
  pesoAlvo: null,
  situacao: 'CORRENTE',
  registros: 38,
  aoApagar: null,
  historico: null,
}

function abrir(criar: Resposta[], endereco = '/fases/nova') {
  const fetch = apiFalsa({
    'GET /niveis': [NIVEIS],
    'GET /fases': [[200, [EM_ABERTO]]],
    'POST /fases': criar,
  })
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[endereco]}>
        <Routes>
          <Route path="/fases/nova" element={<NovaFase />} />
          <Route path="/fases" element={<p>Lista de fases</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { fetch, pessoa: userEvent.setup() }
}

const CRIADA: Resposta = [201, { id: 4 }]

// Pega o nível escolhido sem a taxa dele, que o backend gravaria como
// personalizado sem avisar, e a taxa mexida com o nível de antes marcado
// (RN-30, RN-31, UC-05 3a)
test('o nível preenche a taxa, e mexer na taxa passa a personalizado', async () => {
  const { fetch, pessoa } = abrir([CRIADA])

  await pessoa.click(await screen.findByRole('radio', { name: /agressivo/ }))
  expect(screen.getByLabelText<HTMLInputElement>('Taxa alvo por semana').value).toBe('1,00')
  await pessoa.type(screen.getByLabelText('Peso alvo'), '75,0')
  await pessoa.click(screen.getByRole('button', { name: 'Criar fase' }))

  expect(corpos(fetch, 'POST /fases')).toEqual([
    {
      tipo: 'CUTTING',
      dataInicio: dataDoDispositivo(),
      nivel: 'AGRESSIVO',
      taxaAlvoValor: 1,
      taxaAlvoUnidade: 'PERCENTUAL_SEMANA',
      pesoAlvo: 75,
    },
  ])
})

// Pega a taxa editada com o nível de antes ainda marcado: a tela diria
// moderado, e o backend gravaria personalizado (RN-30, UC-05 3a)
test('mexer na taxa marca o personalizado', async () => {
  const { pessoa } = abrir([CRIADA])

  await pessoa.type(await screen.findByLabelText('Taxa alvo por semana'), '5')

  expect(screen.getByRole('radio', { name: /personalizado/ }).getAttribute('aria-checked')).toBe(
    'true',
  )
})

// Pega a taxa do cutting, em porcentagem, mandada num bulking (RN-28, RN-31)
test('trocar o tipo leva a taxa ao padrão do nível no tipo novo', async () => {
  const { pessoa } = abrir([CRIADA])

  await pessoa.click(await screen.findByRole('radio', { name: 'bulking' }))

  expect(screen.getByLabelText<HTMLInputElement>('Taxa alvo por semana').value).toBe('0,30')
  expect(screen.getByRole('radio', { name: 'quilos' }).getAttribute('aria-checked')).toBe('true')
})

// Pega a taxa escondida da manutenção conferida mesmo assim: a recusa ficaria
// num campo que não aparece, e a fase não sairia (RN-33, UC-05 1a)
test('a manutenção não leva plano, nem o que ficou nos campos escondidos', async () => {
  const { fetch, pessoa } = abrir([CRIADA])

  await pessoa.clear(await screen.findByLabelText('Taxa alvo por semana'))
  await pessoa.type(screen.getByLabelText('Taxa alvo por semana'), '-0,5')
  await pessoa.click(screen.getByRole('radio', { name: 'manutenção' }))
  await pessoa.click(screen.getByRole('button', { name: 'Criar fase' }))

  expect(corpos(fetch, 'POST /fases')).toEqual([
    { tipo: 'MANUTENCAO', dataInicio: dataDoDispositivo() },
  ])
})

// Pega a taxa com sinal mandada ao backend, ou recusada como "não é número",
// quando o problema é o sinal, que vem do tipo (RN-29, ADR-0025)
test('a taxa com sinal é recusada no front, dizendo por quê', async () => {
  const { fetch, pessoa } = abrir([CRIADA])

  await pessoa.clear(await screen.findByLabelText('Taxa alvo por semana'))
  await pessoa.type(screen.getByLabelText('Taxa alvo por semana'), '-0,5')
  await pessoa.click(screen.getByRole('button', { name: 'Criar fase' }))

  expect(descricao(screen.getByLabelText('Taxa alvo por semana'))).toContain(
    'Digite sem sinal: o tipo da fase dá o sinal.',
  )
  expect(corpos(fetch, 'POST /fases')).toEqual([])
})

// Pega a em aberto encerrada sem o sim, ou o segundo envio sem a confirmação,
// que voltaria 409 de novo (RN-22, UC-05 2a)
test('a fase que encerra a em aberto só sai com o sim, no segundo envio', async () => {
  const { fetch, pessoa } = abrir([
    recusa(409, 'FASE_EM_ABERTO_SERA_ENCERRADA', 'dataInicio', 'A manutenção iniciada em 14/09/2026 termina em 27/10/2026.'),
    CRIADA,
  ])

  await pessoa.type(await screen.findByLabelText('Peso alvo'), '75,0')
  await pessoa.click(screen.getByRole('button', { name: 'Criar fase' }))
  expect(await screen.findByRole('alertdialog', { name: 'Encerrar a manutenção em aberto?' })).toBeTruthy()
  await pessoa.click(screen.getByRole('button', { name: 'Criar e encerrar' }))

  expect(await screen.findByText('Lista de fases')).toBeTruthy()
  const [primeiro, segundo] = corpos(fetch, 'POST /fases')
  expect(primeiro).not.toHaveProperty('encerrarEmAberto')
  expect(segundo).toMatchObject({ encerrarEmAberto: true })
})

// Pega a recusa no fim perdida quando a fase não tem fim: o campo do fim não
// aparece, e a recusa iria junto com ele (RN-19, UC-05 2h)
test('sem fim, a recusa que cai no fim aparece no interruptor', async () => {
  const { pessoa } = abrir([
    recusa(409, 'FASE_SOBREPOSTA', 'dataFim', 'As datas invadem a manutenção de 03/11/2026 em diante.'),
  ])

  await pessoa.type(await screen.findByLabelText('Peso alvo'), '75,0')
  await pessoa.click(screen.getByRole('button', { name: 'Criar fase' }))

  expect(await screen.findByText('As datas invadem a manutenção de 03/11/2026 em diante.')).toBeTruthy()
  expect(descricao(screen.getByRole('switch'))).toContain('As datas invadem a manutenção')
})

// Pega o período da importação perdido no caminho para a fase nova, que a
// pessoa teria de digitar de novo (UC-10 4b)
test('vinda da importação, a fase nova já tem as datas do período', async () => {
  const { fetch, pessoa } = abrir([CRIADA], '/fases/nova?inicio=2025-11-10&fim=2026-03-09')

  await pessoa.type(await screen.findByLabelText('Peso alvo'), '75,0')
  await pessoa.click(screen.getByRole('button', { name: 'Criar fase' }))

  expect(corpos(fetch, 'POST /fases')[0]).toMatchObject({
    dataInicio: '2025-11-10',
    dataFim: '2026-03-09',
  })
})
