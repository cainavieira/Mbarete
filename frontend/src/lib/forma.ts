import { z } from 'zod'
import { lerNumero } from '@/formato/numero'

export type Lido = { valor: number } | { erro: string }

/**
 * A forma do número digitado (ADR-0025): é número, com no máximo as casas
 * decimais do campo (RN-62). Faixa e obrigatoriedade de regra vêm do backend.
 * O exemplo entra na mensagem: "Use uma casa decimal, como 78,4.". O campo em
 * que o sinal tem dono, como a taxa alvo (RN-29), diz isso em vez de "não é
 * número".
 */
export function lerForma(texto: string, casas: 1 | 2, exemplo: string, sinal?: string): Lido {
  if (sinal !== undefined && /^[-+]/.test(texto)) {
    return { erro: sinal }
  }
  const valor = lerNumero(texto)
  if (valor === null) {
    return { erro: `Digite um número, como ${exemplo}.` }
  }
  if (!new RegExp(`^\\d+([.,]\\d{1,${casas}})?$`).test(texto)) {
    const regra = casas === 1 ? 'uma casa decimal' : 'até duas casas decimais'
    return { erro: `Use ${regra}, como ${exemplo}.` }
  }
  return { valor }
}

/** A forma como transformação do Zod, para o campo de um formulário. */
export function numeroDeForma(casas: 1 | 2, exemplo: string) {
  return (texto: string, ctx: z.RefinementCtx): number => {
    const lido = lerForma(texto, casas, exemplo)
    if ('erro' in lido) {
      ctx.addIssue({ code: 'custom', message: lido.erro })
      return z.NEVER
    }
    return lido.valor
  }
}

/** O campo que pode ficar vazio: vazio vira nulo, e o resto passa pela forma. */
export function opcional(casas: 1 | 2, exemplo: string) {
  const forma = numeroDeForma(casas, exemplo)
  return z
    .string()
    .trim()
    .transform((texto, ctx) => (texto === '' ? null : forma(texto, ctx)))
}

/** Quando o servidor não responde, não há recusa para mostrar. */
export const SEM_RESPOSTA = 'Sem resposta do servidor. Confira a conexão e tente de novo.'
