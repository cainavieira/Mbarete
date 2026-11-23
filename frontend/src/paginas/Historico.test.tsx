import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { dataDoDispositivo } from '@/api/cliente'
import { somarDias } from '@/formato/data'
import { apiFalsa, type Resposta } from '@/testes/apiFalsa'
import { Historico } from './Historico'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function acompanhamento(registros: number): Resposta {
  return [
    200,
    {
      serie: Array.from({ length: registros }, () => ({
        data: dataDoDispositivo(),
        peso: 80,
        pesoSemanal: null,
        faseId: null,
        faseTipo: null,
      })),
    },
  ]
}

function abrir(endereco: string, rotas: Record<string, Resposta[]>) {
  const fetch = apiFalsa(rotas)
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[endereco]}>
        <Historico />
        <Endereco />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return fetch
}

/** O endereço que a tela deixou, que é o que a volta da edição recebe. */
function Endereco() {
  return <p>Endereço {useLocation().search}</p>
}

const DOIS_MESES: Record<string, Resposta[]> = {
  'GET /registros?de=2026-09-01&ate=2026-10-31': [
    [
      200,
      [
        { data: '2026-10-01', peso: 78.2, cintura: null },
        { data: '2026-09-30', peso: 78.4, cintura: null },
      ],
    ],
  ],
}

/** O registro do dia está na tela, o que só acontece com o mês dele aberto. */
function aparece(dia: string): boolean {
  return screen.queryByRole('link', { name: new RegExp(`registro de ${dia}`) }) !== null
}

// Pega o período padrão com um dia a mais, e os meses fora da ordem em que
// a lista chega, do mais recente para o mais antigo (RF-14)
test('sem período, mostra os últimos 90 dias agrupados por mês', async () => {
  const hoje = dataDoDispositivo()
  abrir('/historico', {
    [`GET /registros?de=${somarDias(hoje, -89)}&ate=${hoje}`]: [
      [
        200,
        [
          { data: '2026-10-02', peso: 78, cintura: null },
          { data: '2026-10-01', peso: 78.2, cintura: 88.5 },
          { data: '2026-09-30', peso: 78.4, cintura: null },
        ],
      ],
    ],
  })

  const meses = await screen.findAllByRole('heading', { level: 2 })
  expect(meses.map((mes) => mes.textContent)).toEqual([
    'outubro de 20262 registros',
    'setembro de 20261 registro',
  ])
  expect(
    screen.getByRole('link', { name: 'Editar o registro de 01/10/2026, 78,2 kg, cintura 88,5 cm' }),
  ).toBeTruthy()
})

// Pega todos os meses abertos ao chegar, a rolagem longa que motivou a
// mudança, ou todos fechados, a tela só com cabeçalhos
test('sem escolha no endereço, só o mês mais recente vem aberto', async () => {
  abrir('/historico?de=2026-09-01&ate=2026-10-31', DOIS_MESES)

  await screen.findByRole('link', { name: /registro de 01\/10\/2026/ })
  expect(aparece('30/09/2026')).toBe(false)
})

// Pega os meses do endereço ignorados, que a volta da edição perderia
test('os meses abertos vêm do endereço', async () => {
  abrir('/historico?de=2026-09-01&ate=2026-10-31&abertos=2026-09', DOIS_MESES)

  await screen.findByRole('link', { name: /registro de 30\/09\/2026/ })
  expect(aparece('01/10/2026')).toBe(false)
})

// Pega o mês aberto guardado só na tela, fora do endereço, e o nenhum aberto
// gravado como sem escolha, que reabriria o mais recente
test('fechar o único mês aberto deixa todos fechados, também no endereço', async () => {
  abrir('/historico?de=2026-09-01&ate=2026-10-31', DOIS_MESES)
  const pessoa = userEvent.setup()

  await pessoa.click(await screen.findByRole('button', { name: /^outubro de 2026/ }))

  expect(aparece('01/10/2026')).toBe(false)
  expect(screen.getByText('Endereço ?de=2026-09-01&ate=2026-10-31&abertos=')).toBeTruthy()
})

// Pega o período do endereço ignorado, que a volta da edição perderia, e o
// período vazio tratado como conta vazia, com o convite ao primeiro registro
test('o período vem do endereço, e vazio pede outro período', async () => {
  abrir('/historico?de=2026-03-01&ate=2026-03-31', {
    'GET /registros?de=2026-03-01&ate=2026-03-31': [[200, []]],
    'GET /acompanhamento': [acompanhamento(5)],
  })

  expect(await screen.findByRole('heading', { name: 'Nenhum registro no período' })).toBeTruthy()
  expect(screen.queryByText('Registrar peso')).toBeNull()
})

// Pega a conta sem nenhum registro recebida com "escolha outro período", que
// não leva a lugar nenhum (como o UC-08 1b)
test('a conta sem registro recebe o convite ao primeiro', async () => {
  const hoje = dataDoDispositivo()
  abrir('/historico', {
    [`GET /registros?de=${somarDias(hoje, -89)}&ate=${hoje}`]: [[200, []]],
    'GET /acompanhamento': [acompanhamento(0)],
  })

  expect(await screen.findByRole('heading', { name: 'Nenhum registro ainda' })).toBeTruthy()
})
