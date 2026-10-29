/**
 * Data é exibida em DD/MM/AAAA e trafegada em AAAA-MM-DD (RN-63). A conta é
 * sobre o texto, e não sobre um Date: o Date lê AAAA-MM-DD como meia-noite no
 * UTC, e atrás do UTC o dia exibido seria o anterior.
 */

function partes(iso: string): [string, string, string] {
  const [ano, mes, dia] = iso.split('-')
  return [ano, mes, dia]
}

/** 07/09/2026. */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = partes(iso)
  return `${dia}/${mes}/${ano}`
}

/** O intervalo das variações e da taxa real: (12-09 / 19-09) (RN-47). */
export function formatarIntervalo(de: string, ate: string): string {
  const [, mesDe, diaDe] = partes(de)
  const [, mesAte, diaAte] = partes(ate)
  return `(${diaDe}-${mesDe} / ${diaAte}-${mesAte})`
}
