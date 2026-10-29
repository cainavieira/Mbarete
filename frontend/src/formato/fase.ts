import type { Esquema } from '@/api/tipos'

export type TipoDeFase = Esquema<'FaseDto'>['tipo']

const NOMES: Record<TipoDeFase, string> = {
  CUTTING: 'cutting',
  BULKING: 'bulking',
  MANUTENCAO: 'manutenção',
}

const COM_ARTIGO: Record<TipoDeFase, string> = {
  CUTTING: 'do cutting',
  BULKING: 'do bulking',
  MANUTENCAO: 'da manutenção',
}

/** O tipo da fase como a tela escreve, em minúscula no meio da frase: manutenção. */
export function formatarTipo(tipo: TipoDeFase): string {
  return NOMES[tipo]
}

/** O tipo depois de "desde a primeira medição": da manutenção. */
export function formatarTipoComArtigo(tipo: TipoDeFase): string {
  return COM_ARTIGO[tipo]
}
