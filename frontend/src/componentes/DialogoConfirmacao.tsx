import type { ReactNode } from 'react'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/componentes/ui/alert-dialog'

interface Props {
  aberto: boolean
  responder: (sim: boolean) => void
  titulo: string
  /** A mensagem do backend como veio, e a linha do front que diz o efeito. */
  children: ReactNode
  confirmar: string
  desistir: string
  /** Só a ação que apaga vai em vermelho; substituir e encerrar não apagam nada. */
  apaga?: boolean
}

/**
 * O diálogo do shadcn/ui (ADR-0028), com o foco preso e o Escape valendo como
 * desistir.
 */
export function DialogoConfirmacao({
  aberto,
  responder,
  titulo,
  children,
  confirmar,
  desistir,
  apaga = false,
}: Props) {
  return (
    <AlertDialog open={aberto} onOpenChange={(abrindo) => !abrindo && responder(false)}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{titulo}</AlertDialogTitle>
          <AlertDialogDescription asChild>
            <div>{children}</div>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogAction
            onClick={() => responder(true)}
            variant={apaga ? 'destructive' : 'default'}
          >
            {confirmar}
          </AlertDialogAction>
          <AlertDialogCancel>{desistir}</AlertDialogCancel>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
