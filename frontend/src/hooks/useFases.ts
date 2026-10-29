import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { chamar } from '@/api/cliente'
import type { Esquema } from '@/api/tipos'

export type FaseListada = Esquema<'FaseListadaDto'>
export type FaseEnviada = Esquema<'FaseEnviadaDto'>
export type Nivel = Esquema<'NivelDto'>

/** Todas as fases, com a situação, a contagem de registros e o efeito de apagar (endpoints.md). */
export function useFases() {
  return useQuery({ queryKey: ['fases'], queryFn: () => chamar<FaseListada[]>('/fases') })
}

/** A taxa padrão de cada nível, que não muda enquanto a aplicação está aberta (RN-31, RN-32). */
export function useNiveis() {
  return useQuery({
    queryKey: ['niveis'],
    queryFn: () => chamar<Nivel[]>('/niveis'),
    staleTime: Infinity,
  })
}

/** Mudar fase muda a lista, o acompanhamento e a fase de cada registro (RN-23). */
function invalidarOQueDependeDasFases(cliente: QueryClient): Promise<void> {
  return Promise.all([
    cliente.invalidateQueries({ queryKey: ['fases'] }),
    cliente.invalidateQueries({ queryKey: ['acompanhamento'] }),
    cliente.invalidateQueries({ queryKey: ['registros'] }),
  ]).then(() => undefined)
}

/** Cria a fase (UC-05); a que encerraria a em aberto volta 409 até o envio com a confirmação. */
export function useCriarFase() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (fase: FaseEnviada) =>
      chamar<Esquema<'FaseDto'>>('/fases', { metodo: 'POST', corpo: fase }),
    onSuccess: () => invalidarOQueDependeDasFases(cliente),
  })
}
