import type { Esquema } from '@/api/tipos'
import { formatarTaxa } from './numero'

export type TipoDeFase = Esquema<'FaseDto'>['tipo']
export type NivelDaFase = NonNullable<Esquema<'FaseDto'>['nivel']>
export type UnidadeDaTaxa = NonNullable<Esquema<'FaseDto'>['taxaAlvoUnidade']>

const NOMES: Record<TipoDeFase, string> = {
  CUTTING: 'cutting',
  BULKING: 'bulking',
  MANUTENCAO: 'manutenção',
}

const NIVEIS: Record<NivelDaFase, string> = {
  CONSERVADOR: 'conservador',
  MODERADO: 'moderado',
  AGRESSIVO: 'agressivo',
  PERSONALIZADO: 'personalizado',
}

/** A manutenção é a única fase feminina, e a frase concorda com ela: "agendada". */
export function ehFeminino(tipo: TipoDeFase): boolean {
  return tipo === 'MANUTENCAO'
}

/** O tipo da fase em minúscula no meio da frase, manutenção, e em maiúscula no começo. */
export function formatarTipo(tipo: TipoDeFase, { noInicio = false } = {}): string {
  const nome = NOMES[tipo]
  return noInicio ? nome.charAt(0).toUpperCase() + nome.slice(1) : nome
}

/** O tipo com o artigo: "o cutting", ou "O cutting" no começo da frase. */
export function formatarOTipo(tipo: TipoDeFase, { noInicio = false } = {}): string {
  const artigo = ehFeminino(tipo) ? 'a' : 'o'
  return `${noInicio ? artigo.toUpperCase() : artigo} ${NOMES[tipo]}`
}

/** O tipo depois de "de": "desde a primeira medição da manutenção". */
export function formatarDoTipo(tipo: TipoDeFase): string {
  return `${ehFeminino(tipo) ? 'da' : 'do'} ${NOMES[tipo]}`
}

/** O nível em minúscula: moderado. */
export function formatarNivel(nivel: NivelDaFase): string {
  return NIVEIS[nivel]
}

/** A taxa alvo como se digita, sem sinal, com a unidade: 0,50% ou 0,30 kg (RN-29). */
export function formatarTaxaAlvo(valor: number, unidade: UnidadeDaTaxa): string {
  const numero = formatarTaxa(valor, { comSinal: false })
  return unidade === 'PERCENTUAL_SEMANA' ? `${numero}%` : `${numero} kg`
}
