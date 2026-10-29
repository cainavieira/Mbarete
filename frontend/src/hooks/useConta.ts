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
