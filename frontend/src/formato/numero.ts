import { formatarIntervalo } from './data'

/**
 * Número decimal é exibido com vírgula e trafegado com ponto (RN-62). Os
 * números do veredito já chegam arredondados do backend, e aqui só se escreve
 * a quantidade de casas que eles têm (RN-37).
 */

const UMA_CASA = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
  useGrouping: false,
})

const DUAS_CASAS = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  useGrouping: false,
})

const UMA_CASA_COM_SINAL = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
  useGrouping: false,
  signDisplay: 'exceptZero',
})

const DUAS_CASAS_COM_SINAL = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  useGrouping: false,
  signDisplay: 'exceptZero',
})

/** Peso em quilogramas, com uma casa: 82,4. */
export function formatarPeso(valor: number): string {
  return UMA_CASA.format(valor)
}

/** Cintura em centímetros, com uma casa, como o peso: 91,0. */
export function formatarCintura(valor: number): string {
  return UMA_CASA.format(valor)
}

/**
 * Taxa em quilogramas por semana, ou em porcentagem, com duas casas (RN-29). No
 * formulário da fase vai sem sinal, como se digita; no acompanhamento, com o
 * sinal (convencoes-frontend.md).
 */
export function formatarTaxa(valor: number, { comSinal }: { comSinal: boolean }): string {
  return (comSinal ? DUAS_CASAS_COM_SINAL : DUAS_CASAS).format(valor)
}

/** A variação com o intervalo de datas usado: -0,4 kg (12-09 / 19-09) (RN-47). */
export function formatarVariacao(valor: number, de: string, ate: string): string {
  return `${UMA_CASA_COM_SINAL.format(valor)} kg ${formatarIntervalo(de, ate)}`
}

const DIGITADO = /^\d+([.,]\d+)?$/

/**
 * O número como a pessoa digita, com vírgula ou ponto, pronto para trafegar
 * (RN-62). Sem sinal: o sinal da taxa alvo vem do tipo da fase (RN-29). O que
 * não for número volta nulo, em vez de virar o pedaço antes da vírgula.
 */
export function lerNumero(texto: string): number | null {
  const limpo = texto.trim()
  return DIGITADO.test(limpo) ? Number(limpo.replace(',', '.')) : null
}
