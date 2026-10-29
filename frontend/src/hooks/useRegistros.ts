import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { chamar } from '@/api/cliente'
import type { Esquema } from '@/api/tipos'

export type RegistroEnviado = Esquema<'RegistroEnviadoDto'>

/**
 * Registro gravado muda o acompanhamento, a lista de registros e a lista de
 * fases, que conta os registros de cada uma (ADR-0023).
 */
export function invalidarOQueDependeDosRegistros(cliente: QueryClient): Promise<void> {
  return Promise.all([
    cliente.invalidateQueries({ queryKey: ['acompanhamento'] }),
    cliente.invalidateQueries({ queryKey: ['registros'] }),
    cliente.invalidateQueries({ queryKey: ['fases'] }),
  ]).then(() => undefined)
}

/** Grava o peso de um dia (UC-02); a data ocupada volta 409 até o envio com a substituição. */
export function useRegistrar() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (registro: RegistroEnviado) =>
      chamar<Esquema<'RegistroDto'>>('/registros', { metodo: 'POST', corpo: registro }),
    onSuccess: () => invalidarOQueDependeDosRegistros(cliente),
  })
}
