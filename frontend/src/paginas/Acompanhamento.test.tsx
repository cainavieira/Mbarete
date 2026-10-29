import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import type { Esquema } from '@/api/tipos'
import type { Acompanhamento as Dados } from '@/hooks/useAcompanhamento'
import { Acompanhamento } from './Acompanhamento'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

/** O dia que fica tantos dias antes de 28/10/2026, o hoje da série. */
function dia(antes: number): string {
  return new Date(Date.UTC(2026, 9, 28 - antes)).toISOString().slice(0, 10)
}

function serie(dias: number): Esquema<'PontoDaSerie'>[] {
  return Array.from({ length: dias }, (_, indice) => ({
    data: dia(dias - 1 - indice),
    peso: 80,
    pesoSemanal: 80,
    faseId: null,
  }))
}

const CUTTING: Esquema<'FaseDto'> = {
  id: 1,
  tipo: 'CUTTING',
  dataInicio: dia(9),
  dataFim: null,
  nivel: 'MODERADO',
  taxaAlvoValor: 1,
  taxaAlvoUnidade: 'PERCENTUAL_SEMANA',
  pesoAlvo: 72,
}

function acompanhamento(parcial: Partial<Dados>): Dados {
  return {
    pesoSemanal: 80,
    registrosNaJanela: 7,
    variacoes: { semana: null, mes: null, desdeOInicio: null },
    faseCorrente: null,
    veredito: null,
    taxaAlvoKgSemana: null,
    faixa: null,
    progresso: null,
    cintura: null,
    serie: serie(120),
    ...parcial,
  }
}

function abrir(dados: Dados) {
  const resposta = () =>
    new Response(JSON.stringify(dados), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  vi.stubGlobal('fetch', vi.fn(async () => resposta()))
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <Acompanhamento />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

// Pega a barra de dados insuficientes e o veredito mostrados sem peso semanal
// hoje: o backend manda os dois com esse estado, e a tela é que os cala (UC-08 1a, 5c)
test('sem peso semanal hoje, não há veredito, barra nem variações', async () => {
  abrir(
    acompanhamento({
      pesoSemanal: null,
      registrosNaJanela: 2,
      faseCorrente: CUTTING,
      veredito: { estado: 'DADOS_INSUFICIENTES', taxaReal: null, aPartirDe: null },
      taxaAlvoKgSemana: null,
      progresso: { estado: 'DADOS_INSUFICIENTES', percentual: null, atingido: false },
    }),
  )

  expect(await screen.findByText('Só 2 registros nos últimos 7 dias.')).toBeTruthy()
  expect(screen.queryByText(/Progresso até/)).toBeNull()
  expect(screen.queryByText('Variação do peso semanal')).toBeNull()
  expect(screen.getAllByText('dados insuficientes')).toHaveLength(1)
})

// Pega a palavra do ritmo na manutenção e a taxa alvo de zero que o backend
// manda nela, no lugar que é da faixa (RN-37, UC-08 4b)
test('na manutenção, o veredito fala da faixa e a taxa alvo não aparece', async () => {
  abrir(
    acompanhamento({
      faseCorrente: { ...CUTTING, tipo: 'MANUTENCAO', taxaAlvoValor: null, taxaAlvoUnidade: null, pesoAlvo: null },
      veredito: { estado: 'NO_RITMO', taxaReal: null, aPartirDe: null },
      taxaAlvoKgSemana: 0,
      faixa: { minimo: 77, maximo: 78.6 },
    }),
  )

  expect(await screen.findByText('na faixa')).toBeTruthy()
  expect(screen.getByText('de 77,0 a 78,6 kg')).toBeTruthy()
  expect(screen.queryByText('taxa alvo')).toBeNull()
})

// Pega a data do veredito ignorada, e o percentual sem o sinal do cutting ao
// lado da taxa em quilos, que tem (UC-08 4a, RN-29)
test('antes dos 14 dias, diz quando o veredito aparece', async () => {
  abrir(
    acompanhamento({
      faseCorrente: CUTTING,
      veredito: { estado: 'DADOS_INSUFICIENTES', taxaReal: null, aPartirDe: '2026-11-02' },
      taxaAlvoKgSemana: -0.8,
      progresso: { estado: 'BARRA', percentual: 7, atingido: false },
    }),
  )

  expect(await screen.findByText('O veredito aparece a partir de 02/11/2026.')).toBeTruthy()
  expect(screen.getByText('(-1,00%)')).toBeTruthy()
})

// Pega o período contado com um dia a mais ou a menos, e o botão que não
// recorta (RF-49)
test('o botão de período recorta a série até hoje', async () => {
  const pessoa = abrir(acompanhamento({}))

  await pessoa.click(await screen.findByRole('radio', { name: '30 dias' }))

  expect(screen.getByRole('heading', { name: 'Peso nos últimos 30 dias' })).toBeTruthy()
  expect(screen.getByRole('img').getAttribute('aria-label')).toMatch(/^Gráfico de 29\/09\/2026 a 28\/10\/2026,/)
})

// Pega o título que promete 120 dias quando a série começa depois, no
// primeiro registro (RF-49, UC-08 2b)
test('a série mais curta que o período diz desde quando começa', async () => {
  abrir(acompanhamento({ serie: serie(11) }))

  expect(await screen.findByRole('heading', { name: 'Peso desde 18/10/2026' })).toBeTruthy()
})

// Pega a estrutura vazia, com dados insuficientes em tudo, mostrada a quem
// ainda não registrou nada (UC-08 1b)
test('a conta sem registro recebe o convite ao primeiro', async () => {
  abrir(acompanhamento({ pesoSemanal: null, registrosNaJanela: 0, serie: [] }))

  expect(await screen.findByRole('heading', { name: 'Registre o primeiro peso' })).toBeTruthy()
  expect(screen.queryByText('dados insuficientes')).toBeNull()
})
