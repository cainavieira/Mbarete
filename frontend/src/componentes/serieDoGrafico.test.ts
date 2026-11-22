import { expect, test } from 'vitest'
import { escalaDoPeso, trechosDeFase, type Ponto } from './serieDoGrafico'

const TIPOS = { 1: 'CUTTING', 2: 'MANUTENCAO' } as const

function pontos(...fases: (1 | 2 | null)[]): Ponto[] {
  return fases.map((faseId, indice) => ({
    data: `2026-10-${String(indice + 1).padStart(2, '0')}`,
    peso: 80,
    pesoSemanal: 80,
    faseId,
    faseTipo: faseId === null ? null : TIPOS[faseId],
  }))
}

// Pega o bloco que junta fases encostadas, escondendo o início da segunda, o
// que junta a mesma fase por cima de dias sem fase, e o bloco com o tipo de
// outra fase (RF-47)
test('cada fase é um bloco de dias seguidos, com o tipo dela', () => {
  expect(trechosDeFase(pontos(null, 1, 1, 2, 2, null, 2))).toEqual([
    { faseId: 1, tipo: 'CUTTING', de: 1, ate: 2 },
    { faseId: 2, tipo: 'MANUTENCAO', de: 3, ate: 4 },
    { faseId: 2, tipo: 'MANUTENCAO', de: 6, ate: 6 },
  ])
})

// Pega a conta em quilos, que deixa as marcas tortas (79,60000000000001) e
// perde a última, e a volta das pontas ao quilo inteiro, que achata a linha
test('com o peso mexendo pouco, as marcas vão de 0,2 em 0,2 e as pontas seguem o passo', () => {
  expect(escalaDoPeso([79.7, 80, 80.3])).toEqual({
    dominio: [79.4, 80.6],
    marcas: [79.4, 79.6, 79.8, 80, 80.2, 80.4, 80.6],
    passo: 0.2,
  })
})

// Pega o passo que não cresce com a variação, que empilharia 35 marcas na coluna
test('com o peso mexendo muito, o passo cresce e a coluna fica em até sete marcas', () => {
  expect(escalaDoPeso([75, 81])).toEqual({ dominio: [74, 82], marcas: [74, 76, 78, 80, 82], passo: 2 })
})
