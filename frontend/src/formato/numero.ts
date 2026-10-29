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

/**
 * A diferença de peso ou de cintura, com o sinal e uma casa: -0,4. Na
 * variação do peso, vai sempre com o intervalo de datas usado (RN-47).
 */
export function formatarDiferenca(valor: number): string {
  return UMA_CASA_COM_SINAL.format(valor)
}

const INTEIRO = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0, useGrouping: false })

/** A marca do eixo do peso no gráfico, que é sempre um quilo inteiro: 78. */
export function formatarInteiro(valor: number): string {
  return INTEIRO.format(valor)
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
