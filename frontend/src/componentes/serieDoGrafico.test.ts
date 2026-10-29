import { expect, test } from 'vitest'
import { trechosDeFase, type Ponto } from './serieDoGrafico'

function pontos(...fases: (number | null)[]): Ponto[] {
  return fases.map((faseId, indice) => ({
    data: `2026-10-${String(indice + 1).padStart(2, '0')}`,
    peso: 80,
    pesoSemanal: 80,
    faseId,
  }))
}

// Pega o bloco que junta fases encostadas, escondendo o início da segunda, e
// o que junta a mesma fase por cima de dias sem fase (RF-47)
test('cada fase é um bloco de dias seguidos', () => {
  expect(trechosDeFase(pontos(null, 1, 1, 2, 2, null, 2))).toEqual([
    { faseId: 1, de: 1, ate: 2 },
    { faseId: 2, de: 3, ate: 4 },
    { faseId: 2, de: 6, ate: 6 },
  ])
})
