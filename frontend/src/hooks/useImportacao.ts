import { useMutation, useQueryClient } from '@tanstack/react-query'
import { chamar } from '@/api/cliente'
import type { Esquema } from '@/api/tipos'
import { invalidarOQueDependeDosRegistros } from '@/hooks/useRegistros'

export type Previsualizacao = Esquema<'PrevisualizacaoDto'>
export type LinhaValida = Esquema<'LinhaValida'>

/** Lê o arquivo e diz o que a gravação faria, sem gravar nada (UC-10, RN-58). */
export function usePrevisualizar() {
  return useMutation({
    mutationFn: (arquivo: File) => {
      const corpo = new FormData()
      corpo.append('arquivo', arquivo)
      return chamar<Previsualizacao>('/importacao/previsualizacao', { metodo: 'POST', corpo })
    },
  })
}

interface Importacao {
  arquivo: File
  /** As datas que a pré-visualização mostrou substituídas, que a pessoa confirmou (UC-10 3b). */
  substituir: string[]
}

/**
 * Grava o mesmo arquivo, reenviado com as datas confirmadas: o servidor não
 * guarda nada entre as duas chamadas, e responde 409 se o histórico mudou.
 */
export function useImportar() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: ({ arquivo, substituir }: Importacao) => {
      const corpo = new FormData()
      corpo.append('arquivo', arquivo)
      for (const data of substituir) {
        corpo.append('substituir', data)
      }
      return chamar<Previsualizacao>('/importacao', { metodo: 'POST', corpo })
    },
    onSuccess: () => invalidarOQueDependeDosRegistros(cliente),
  })
}
