import { expect, test } from 'vitest'
import { trechosDeFase, type Ponto } from './serieDoGrafico'

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
