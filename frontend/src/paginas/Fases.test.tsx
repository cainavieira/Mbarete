import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import type { FaseListada } from '@/hooks/useFases'
import { apiFalsa } from '@/testes/apiFalsa'
import { Fases } from './Fases'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function fase(parcial: Partial<FaseListada> & Pick<FaseListada, 'id' | 'dataInicio'>): FaseListada {
  return {
    tipo: 'CUTTING',
    dataFim: null,
    nivel: 'MODERADO',
    taxaAlvoValor: 0.5,
    taxaAlvoUnidade: 'PERCENTUAL_SEMANA',
    pesoAlvo: 75,
    situacao: 'ENCERRADA',
    registros: 10,
    aoApagar: null,
    historico: null,
    ...parcial,
  }
}

function abrir(fases: FaseListada[]) {
  apiFalsa({ 'GET /fases': [[200, fases]] })
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <Fases />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

// Pega as encerradas na ordem em que o backend as manda, que é a do
// identificador: o histórico classificado depois do fato ficaria fora do
// lugar (UC-09)
test('as encerradas vêm da mais recente para a mais antiga', async () => {
  abrir([
    fase({ id: 1, dataInicio: '2026-03-01', dataFim: '2026-03-31' }),
    fase({ id: 2, dataInicio: '2025-01-01', dataFim: '2025-01-31' }),
    fase({ id: 3, dataInicio: '2026-06-01', dataFim: '2026-06-30', tipo: 'BULKING' }),
  ])

  const encerradas = await screen.findByRole('region', { name: 'Encerradas' })
  expect(
    within(encerradas)
      .getAllByRole('link')
      .map((link) => link.getAttribute('aria-label')),
  ).toEqual([
    'Bulking encerrado, de 01/06/2026 a 30/06/2026, abrir a fase',
    'Cutting encerrado, de 01/03/2026 a 31/03/2026, abrir a fase',
    'Cutting encerrado, de 01/01/2025 a 31/01/2025, abrir a fase',
  ])
})

// Pega o fim no futuro da corrente sem a explicação, que parece um
// encerramento marcado à mão, ou procurada na agendada errada (RN-67)
test('o fim da corrente que acompanha a agendada diz isso', async () => {
  abrir([
    fase({ id: 1, dataInicio: '2026-08-03', dataFim: '2026-11-02', situacao: 'CORRENTE' }),
    fase({ id: 2, dataInicio: '2026-11-03', tipo: 'MANUTENCAO', situacao: 'FUTURA' }),
  ])

  expect(await screen.findByText('O fim acompanha o início da manutenção agendada.')).toBeTruthy()
})
