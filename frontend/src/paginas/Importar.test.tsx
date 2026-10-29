import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, expect, test, vi } from 'vitest'
import { apiFalsa, descricao, recusa, type Resposta } from '@/testes/apiFalsa'
import { Importar } from './Importar'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const PREVIA = {
  validas: [
    { linha: 2, data: '2026-03-09', peso: 83.1, cintura: null, substitui: { peso: 83, cintura: 92.4 } },
    { linha: 3, data: '2026-03-10', peso: 82.9, cintura: null, substitui: null },
  ],
  recusadas: [{ linha: 4, motivo: 'O peso deve ser um número com ponto decimal, como 85.7.' }],
}

const ARQUIVO = new File(['data;peso\n2026-03-09;83.1\n'], 'pesos.csv', { type: 'text/csv' })

function abrir(rotas: Record<string, Resposta[]>) {
  const fetch = apiFalsa(rotas)
  render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <Importar />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { fetch, pessoa: userEvent.setup() }
}

/** Os campos de cada envio da importação, que vai em formulário com o arquivo. */
function envios(fetch: ReturnType<typeof apiFalsa>, caminho: string): FormData[] {
  return fetch.mock.calls
    .filter(([url]) => url === `/api/v1${caminho}`)
    .map(([, init]) => init?.body as FormData)
}

// Pega a confirmação sem as datas que a pessoa viu substituídas, que o
// backend recusaria, e o arquivo trocado por outro no segundo envio (UC-10 3b)
test('gravar reenvia o arquivo com as datas que vão ser substituídas', async () => {
  const { fetch, pessoa } = abrir({
    'POST /importacao/previsualizacao': [[200, PREVIA]],
    'POST /importacao': [[200, PREVIA]],
  })

  await pessoa.upload(screen.getByLabelText('Arquivo'), ARQUIVO)
  await pessoa.click((await screen.findAllByRole('button', { name: 'Gravar 2 registros' }))[0])

  const [envio] = envios(fetch, '/importacao')
  expect(envio.get('arquivo')).toBe(ARQUIVO)
  expect(envio.getAll('substituir')).toEqual(['2026-03-09'])
  expect(await screen.findByText('2 registros gravados, de 09/03/2026 a 10/03/2026.')).toBeTruthy()
})

// Pega a substituição mostrada sem o que está gravado, que ela apaga, como a
// cintura (UC-10)
test('a linha que substitui mostra o que está gravado na data', async () => {
  const { pessoa } = abrir({ 'POST /importacao/previsualizacao': [[200, PREVIA]] })

  await pessoa.upload(screen.getByLabelText('Arquivo'), ARQUIVO)

  expect(await screen.findByText('no lugar de 83,0 kg e 92,4 cm de cintura')).toBeTruthy()
})

// Pega o 409 tratado como falha, que deixaria a pessoa sem saber o que mudou,
// ou a gravação repetida sem nova pré-visualização (UC-10 3b)
test('se o histórico mudou, refaz a pré-visualização sem gravar', async () => {
  const { fetch, pessoa } = abrir({
    'POST /importacao/previsualizacao': [[200, PREVIA]],
    'POST /importacao': [
      recusa(409, 'PREVISUALIZACAO_DESATUALIZADA', null, 'O histórico mudou desde a pré-visualização: nada foi gravado.'),
    ],
  })

  await pessoa.upload(screen.getByLabelText('Arquivo'), ARQUIVO)
  await pessoa.click((await screen.findAllByRole('button', { name: 'Gravar 2 registros' }))[0])

  expect(
    await screen.findByText('O histórico mudou desde a pré-visualização: nada foi gravado.'),
  ).toBeTruthy()
  expect(envios(fetch, '/importacao/previsualizacao')).toHaveLength(2)
  expect(envios(fetch, '/importacao')).toHaveLength(1)
})

// Pega a recusa do arquivo inteiro longe do campo, onde o leitor de tela não
// a anuncia (UC-10 1a)
test('o arquivo sem as colunas é recusado no campo dele', async () => {
  const { pessoa } = abrir({
    'POST /importacao/previsualizacao': [
      recusa(400, 'CSV_COLUNA_AUSENTE', 'arquivo', 'O arquivo precisa das colunas data e peso.'),
    ],
  })

  await pessoa.upload(screen.getByLabelText('Arquivo'), ARQUIVO)

  expect(await screen.findByText('O arquivo precisa das colunas data e peso.')).toBeTruthy()
  expect(descricao(screen.getByLabelText('Arquivo'))).toContain(
    'O arquivo precisa das colunas data e peso.',
  )
})

// Pega a gravação oferecida sem nenhuma linha válida (UC-10 2b)
test('sem linha válida, gravar fica indisponível', async () => {
  const { pessoa } = abrir({
    'POST /importacao/previsualizacao': [[200, { validas: [], recusadas: PREVIA.recusadas }]],
  })

  await pessoa.upload(screen.getByLabelText('Arquivo'), ARQUIVO)

  for (const botao of await screen.findAllByRole('button', { name: 'Gravar' })) {
    expect((botao as HTMLButtonElement).disabled).toBe(true)
  }
})
