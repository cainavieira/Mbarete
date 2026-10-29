import { useRef, useState } from 'react'

export interface Confirmacao<T> {
  /** O que o diálogo mostra enquanto espera a resposta; nulo com o diálogo fechado. */
  pedido: T | null
  /** Abre o diálogo e espera: verdadeiro no sim, falso no não ou no Escape. */
  pedir: (pedido: T) => Promise<boolean>
  responder: (sim: boolean) => void
}

/**
 * A confirmação que o backend pede por conflito (endpoints.md) no meio de um
 * envio: o envio espera a resposta da pessoa e segue dali, e o erro do segundo
 * envio volta ao formulário pelo mesmo caminho do primeiro.
 */
export function useConfirmacao<T>(): Confirmacao<T> {
  const [pedido, setPedido] = useState<T | null>(null)
  const resolver = useRef<((sim: boolean) => void) | null>(null)

  return {
    pedido,
    pedir: (novo) =>
      new Promise<boolean>((resolve) => {
        resolver.current = resolve
        setPedido(novo)
      }),
    responder: (sim) => {
      resolver.current?.(sim)
      resolver.current = null
      setPedido(null)
    },
  }
}
