import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
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

export type Registro = Esquema<'RegistroDto'>

/** Os registros do intervalo, as duas pontas inclusive, do mais recente para o mais antigo (RF-14). */
export function useListaDeRegistros(de: string, ate: string) {
  return useQuery({
    queryKey: ['registros', de, ate],
    queryFn: () => chamar<Registro[]>(`/registros?de=${de}&ate=${ate}`),
  })
}

/**
 * Edita o registro da data, inclusive movendo-o (UC-03); a data nova ocupada
 * volta 409 até o envio com a substituição, como no registro.
 */
export function useEditarRegistro(data: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (registro: RegistroEnviado) =>
      chamar<Registro>(`/registros/${data}`, { metodo: 'PATCH', corpo: registro }),
    // Sem esperar: o registro movido sumiria da tela dele antes de ela sair
    onSuccess: () => {
      void invalidarOQueDependeDosRegistros(cliente)
    },
  })
}

/** Apaga o registro da data, sem volta (UC-04, RN-08). */
export function useApagarRegistro(data: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: () => chamar<void>(`/registros/${data}`, { metodo: 'DELETE' }),
    // Sem esperar: a tela do registro apagado sai antes de a lista voltar sem ele
    onSuccess: () => {
      void invalidarOQueDependeDosRegistros(cliente)
    },
  })
}
