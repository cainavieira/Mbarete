import { useMutation, useQuery } from '@tanstack/react-query'
import { baixar, chamar } from '@/api/cliente'
import type { Esquema } from '@/api/tipos'
import { salvarArquivo } from '@/lib/arquivo'

/** Nome, e-mail e foto vindos do Google (UC-12); é também a prova de que a sessão vale. */
export function useConta() {
  return useQuery({ queryKey: ['conta'], queryFn: () => chamar<Esquema<'ContaDto'>>('/conta') })
}

/** O histórico inteiro em CSV, no formato que a importação lê de volta (UC-11, RF-43). */
export function useExportar() {
  return useMutation({
    mutationFn: async () => salvarArquivo(await baixar('/conta/exportar'), 'historico.csv'),
  })
}

/** Encerra a sessão: o backend apaga o token de renovação e expira os dois cookies (RF-04). */
export function useSair() {
  return useMutation({ mutationFn: () => chamar<void>('/auth/logout', { metodo: 'POST' }) })
}

/**
 * Apaga a conta e tudo dela, com o e-mail digitado como confirmação; o que
 * não confere volta recusado, no campo (UC-12, RN-51).
 */
export function useApagarConta() {
  return useMutation({
    mutationFn: (email: string) =>
      chamar<void>('/conta/exclusao', { metodo: 'POST', corpo: { email } }),
  })
}
