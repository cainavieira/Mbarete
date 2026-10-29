import { ChevronLeft, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { ErroDaApi } from '@/api/cliente'
import { DialogoConfirmacao } from '@/componentes/DialogoConfirmacao'
import { FormularioRegistro } from '@/componentes/FormularioRegistro'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import { formatarData } from '@/formato/data'
import { formatarCintura, formatarPeso } from '@/formato/numero'
import {
  useApagarRegistro,
  useEditarRegistro,
  useListaDeRegistros,
  type Registro,
  type RegistroEnviado,
} from '@/hooks/useRegistros'
import { useConfirmacao } from '@/lib/confirmacao'
import { SEM_RESPOSTA } from '@/lib/forma'

interface Conflito {
  mensagem: string
  editado: RegistroEnviado
}

/**
 * Editar e apagar o registro de um dia (UC-03, UC-04). Mover para uma data
 * ocupada responde 409 como no registro, e o sim faz dos dois registros um.
 */
export function EditarRegistro() {
  const data = useParams().data ?? ''
  // Não há rota de um registro só: o intervalo de um dia o traz
  const lista = useListaDeRegistros(data, data)

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-3 px-4 pt-2 pb-5 md:px-0 md:pt-6">
      <div className="flex flex-col items-start gap-1">
        <Button asChild variant="ghost" className="-ml-2 px-2 text-primary">
          <Link to="/historico">
            <ChevronLeft aria-hidden="true" />
            Histórico
          </Link>
        </Button>
        <h1 className="text-2xl leading-tight font-bold">Editar registro</h1>
      </div>
      {lista.isError ? (
        <Alert>
          <AlertTitle>{lista.error.message}</AlertTitle>
          <AlertDescription>Recarregue a página para tentar de novo.</AlertDescription>
        </Alert>
      ) : (
        lista.data !== undefined &&
        (lista.data.length === 0 ? (
          <p className="text-muted-foreground">Esse registro não existe mais.</p>
        ) : (
          <Edicao registro={lista.data[0]} />
        ))
      )}
    </main>
  )
}

function Edicao({ registro }: { registro: Registro }) {
  const editar = useEditarRegistro(registro.data)
  const apagar = useApagarRegistro(registro.data)
  const confirmacao = useConfirmacao<Conflito>()
  const [apagando, setApagando] = useState(false)
  const navegar = useNavigate()

  async function aoEnviar(editado: RegistroEnviado) {
    try {
      await editar.mutateAsync(editado)
    } catch (erro) {
      if (!(erro instanceof ErroDaApi && erro.codigo === 'REGISTRO_JA_EXISTE')) {
        throw erro
      }
      if (!(await confirmacao.pedir({ mensagem: erro.message, editado }))) {
        return
      }
      await editar.mutateAsync({ ...editado, substituir: true })
    }
    toast.success(`Registro de ${formatarData(editado.data)} salvo.`)
    navegar('/historico')
  }

  async function responderApagar(sim: boolean) {
    setApagando(false)
    if (!sim) {
      return
    }
    try {
      await apagar.mutateAsync()
    } catch (erro) {
      toast.error(erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA)
      return
    }
    toast.success(`Registro de ${formatarData(registro.data)} apagado.`)
    navegar('/historico')
  }

  const conflito = confirmacao.pedido
  const antes = formatarData(registro.data)

  return (
    <>
      <FormularioRegistro
        inicial={{
          data: registro.data,
          peso: formatarPeso(registro.peso),
          cintura: registro.cintura === null ? '' : formatarCintura(registro.cintura),
        }}
        rotuloEnviar="Salvar"
        aoEnviar={aoEnviar}
      >
        <Button
          type="button"
          variant="outline"
          className="text-destructive hover:text-destructive"
          onClick={() => setApagando(true)}
        >
          <Trash2 aria-hidden="true" />
          Apagar registro
        </Button>
      </FormularioRegistro>

      <DialogoConfirmacao
        aberto={conflito !== null}
        responder={confirmacao.responder}
        titulo={`Substituir o registro de ${conflito === null ? '' : formatarData(conflito.editado.data)}?`}
        confirmar="Substituir"
        desistir="Manter os dois"
      >
        {conflito && (
          <>
            <p className="tabular-nums">{conflito.mensagem}</p>
            <p className="text-muted-foreground tabular-nums">
              O registro de {antes} vai para essa data e fica no lugar dele, e {antes} fica sem
              registro.
            </p>
          </>
        )}
      </DialogoConfirmacao>

      <DialogoConfirmacao
        aberto={apagando}
        responder={(sim) => void responderApagar(sim)}
        titulo={`Apagar o registro de ${antes}?`}
        confirmar="Apagar"
        desistir="Cancelar"
        apaga
      >
        <p className="text-muted-foreground tabular-nums">
          {formatarPeso(registro.peso)} kg
          {registro.cintura === null ? '' : ` e ${formatarCintura(registro.cintura)} cm de cintura`}.
          Apagar não se desfaz.
        </p>
      </DialogoConfirmacao>
    </>
  )
}
