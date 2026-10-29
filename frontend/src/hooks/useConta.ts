import { useQuery } from '@tanstack/react-query'
import { chamar } from '@/api/cliente'
import type { Esquema } from '@/api/tipos'

/** Nome, e-mail e foto vindos do Google (UC-12); é também a prova de que a sessão vale. */
export function useConta() {
  return useQuery({ queryKey: ['conta'], queryFn: () => chamar<Esquema<'ContaDto'>>('/conta') })
}
