import { expect, test } from 'vitest'
import { formatarData } from './data'
import { formatarPeso, formatarTaxa, formatarVariacao, lerNumero } from './numero'

// Pega a data lida como Date: AAAA-MM-DD vira meia-noite no UTC, e no fuso
// dos testes, atrás do UTC, o dia exibido seria o anterior (RN-63)
test('a data sai no dia em que trafegou', () => {
  expect(formatarData('2026-09-07')).toBe('07/09/2026')
})

// Pega o ponto no lugar da vírgula e o peso redondo sem a casa (RN-62)
test('o peso sai com vírgula e uma casa', () => {
  expect(formatarPeso(82)).toBe('82,0')
  expect(formatarPeso(78.4)).toBe('78,4')
})

// Pega a taxa do acompanhamento sem o sinal, que diria ganho num cutting, e
// a do formulário com ele (convencoes-frontend.md)
test('a taxa leva o sinal só quando pedido', () => {
  expect(formatarTaxa(-0.4, { comSinal: true })).toBe('-0,40')
  expect(formatarTaxa(0.3, { comSinal: true })).toBe('+0,30')
  expect(formatarTaxa(0.5, { comSinal: false })).toBe('0,50')
})

// Pega a variação sem o intervalo, ou com o intervalo fora do formato da RN-47
test('a variação vem com o intervalo de datas usado', () => {
  expect(formatarVariacao(-0.4, '2026-09-12', '2026-09-19')).toBe('-0,4 kg (12-09 / 19-09)')
})

// Pega o número digitado com vírgula lido até a vírgula, como o parseFloat
// faz: 82,4 viraria 82 sem aviso (RN-62)
test('o número digitado aceita vírgula e ponto, e recusa o resto', () => {
  expect(lerNumero('82,4')).toBe(82.4)
  expect(lerNumero(' 82.4 ')).toBe(82.4)
  expect(lerNumero('82,4,1')).toBeNull()
  expect(lerNumero('abc')).toBeNull()
  expect(lerNumero('')).toBeNull()
})
