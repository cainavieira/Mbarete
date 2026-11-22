import type { Esquema } from '@/api/tipos'
import type { TipoDeFase } from '@/formato/fase'

/**
 * As contas de tela do gráfico (ADR-0025): o trecho do período, os blocos de
 * fase e a escala. A série chega pronta do backend, um ponto por dia até hoje.
 */

export type Ponto = Esquema<'PontoDaSerieDto'>

export const PERIODOS = [30, 60, 90, 120] as const

export type Periodo = (typeof PERIODOS)[number]

/**
 * Os últimos dias do período, hoje incluído (RF-49). A série tem um ponto por
 * dia, inclusive o sem dado, então os dias são as últimas entradas; e, como
 * ela começa no primeiro registro, o recorte mais curto que o período já
 * começa nele, sem trecho em branco.
 */
export function recortar(serie: Ponto[], dias: Periodo): Ponto[] {
  return serie.slice(-dias)
}

export interface TrechoDeFase {
  faseId: number
  tipo: TipoDeFase
  /** Posição do primeiro e do último dia do trecho no recorte. */
  de: number
  ate: number
}

/**
 * Os dias seguidos sob a mesma fase, cada um pintado como um bloco (RF-47).
 * Duas fases encostadas são dois blocos, para o início da segunda aparecer.
 */
export function trechosDeFase(pontos: Ponto[]): TrechoDeFase[] {
  const trechos: TrechoDeFase[] = []
  pontos.forEach(({ faseId, faseTipo }, indice) => {
    if (faseId === null || faseTipo === null) {
      return
    }
    const ultimo = trechos.at(-1)
    if (ultimo !== undefined && ultimo.faseId === faseId && ultimo.ate === indice - 1) {
      ultimo.ate = indice
    } else {
      trechos.push({ faseId, tipo: faseTipo, de: indice, ate: indice })
    }
  })
  return trechos
}

/** De quanto em quanto pulam as marcas, em décimos de quilo: de 0,2 kg a 50 kg. */
const PASSOS = [2, 5, 10, 20, 40, 50, 100, 200, 500]

/** O respiro abaixo do menor valor e acima do maior, em décimos de quilo. */
const FOLGA = 3

export interface EscalaDoPeso {
  dominio: [number, number]
  marcas: number[]
  /** Em quilos: abaixo de 1, as marcas levam uma casa. */
  passo: number
}

/**
 * O eixo do peso com o menor passo que deixe no máximo seis intervalos, para
 * as marcas não se encostarem no celular, e as pontas alinhadas ao passo. A
 * conta é em décimos de quilo: em quilos, somar 0,2 dá 79,60000000000001, e a
 * última marca passa da ponta e some.
 */
export function escalaDoPeso(valores: number[]): EscalaDoPeso {
  const menor = Math.floor(Math.min(...valores) * 10) - FOLGA
  const maior = Math.ceil(Math.max(...valores) * 10) + FOLGA
  const passo = PASSOS.find((candidato) => (maior - menor) / candidato <= 6) ?? 1000
  const de = Math.floor(menor / passo) * passo
  const ate = Math.ceil(maior / passo) * passo
  const marcas: number[] = []
  for (let marca = de; marca <= ate; marca += passo) {
    marcas.push(marca / 10)
  }
  return { dominio: [de / 10, ate / 10], marcas, passo: passo / 10 }
}
