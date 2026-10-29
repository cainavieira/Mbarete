import { useQuery } from '@tanstack/react-query'
import { chamar } from '@/api/cliente'
import type { Esquema } from '@/api/tipos'

export type Acompanhamento = Esquema<'AcompanhamentoDto'>

/**
 * A tela principal numa resposta só, porque tudo nela deriva do mesmo peso
 * semanal (endpoints.md). Gravar registro invalida esta chave (ADR-0023).
 */
export function useAcompanhamento() {
  return useQuery({
    queryKey: ['acompanhamento'],
    queryFn: () => chamar<Acompanhamento>('/acompanhamento'),
  })
}
