/**
 * Data é exibida em DD/MM/AAAA e trafegada em AAAA-MM-DD (RN-63). A conta é
 * sobre o texto, e não sobre um Date: o Date lê AAAA-MM-DD como meia-noite no
 * UTC, e atrás do UTC o dia exibido seria o anterior.
 */

function partes(iso: string): [string, string, string] {
  const [ano, mes, dia] = iso.split('-')
  return [ano, mes, dia]
}

/** O dia de um Date no calendário do dispositivo, e não no do UTC, em AAAA-MM-DD (RN-04). */
export function isoDoDia(dia: Date): string {
  const mes = String(dia.getMonth() + 1).padStart(2, '0')
  const numero = String(dia.getDate()).padStart(2, '0')
  return `${dia.getFullYear()}-${mes}-${numero}`
}

/** O dia, à meia-noite do dispositivo, para o calendário, que trabalha com Date. */
export function diaDoIso(iso: string): Date {
  const [ano, mes, dia] = partes(iso)
  return new Date(Number(ano), Number(mes) - 1, Number(dia))
}

/** O dia tantos dias depois, ou antes, com o número negativo, em AAAA-MM-DD. */
export function somarDias(iso: string, dias: number): string {
  const dia = diaDoIso(iso)
  dia.setDate(dia.getDate() + dias)
  return isoDoDia(dia)
}

/** 07/09/2026. */
export function formatarData(iso: string): string {
  const [ano, mes, dia] = partes(iso)
  return `${dia}/${mes}/${ano}`
}

/** O dia e o mês, nas pontas do eixo do gráfico: 07/09. */
export function formatarDiaEMes(iso: string): string {
  const [, mes, dia] = partes(iso)
  return `${dia}/${mes}`
}

/** O intervalo das variações e da taxa real: (12-09 / 19-09) (RN-47). */
export function formatarIntervalo(de: string, ate: string): string {
  const [, mesDe, diaDe] = partes(de)
  const [, mesAte, diaAte] = partes(ate)
  return `(${diaDe}-${mesDe} / ${diaAte}-${mesAte})`
}
