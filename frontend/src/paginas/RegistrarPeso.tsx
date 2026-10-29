import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { dataDoDispositivo, ErroDaApi } from '@/api/cliente'
import { DialogoConfirmacao } from '@/componentes/DialogoConfirmacao'
import { FormularioRegistro } from '@/componentes/FormularioRegistro'
import { formatarData } from '@/formato/data'
import { formatarCintura, formatarPeso } from '@/formato/numero'
import { useRegistrar, type RegistroEnviado } from '@/hooks/useRegistros'
import { useConfirmacao } from '@/lib/confirmacao'

interface Conflito {
  mensagem: string
  novo: RegistroEnviado
}

/**
 * Registrar o peso do dia em uma tela e uma confirmação (UC-02, RNF-26). A
 * data ocupada responde 409 com o registro gravado, e só o segundo envio, com
 * o sim da pessoa, substitui (RN-01).
 */
export function RegistrarPeso() {
  const registrar = useRegistrar()
  const confirmacao = useConfirmacao<Conflito>()
  const navegar = useNavigate()

  async function aoEnviar(novo: RegistroEnviado) {
    try {
      await registrar.mutateAsync(novo)
    } catch (erro) {
      if (!(erro instanceof ErroDaApi && erro.codigo === 'REGISTRO_JA_EXISTE')) {
        throw erro
      }
      if (!(await confirmacao.pedir({ mensagem: erro.message, novo }))) {
        return
      }
      await registrar.mutateAsync({ ...novo, substituir: true })
    }
    toast.success(`Peso de ${formatarData(novo.data)} registrado.`)
    navegar('/')
  }

  const conflito = confirmacao.pedido

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-3 px-4 pt-5 pb-5 md:px-0 md:pt-8">
      <h1 className="text-2xl leading-tight font-bold">Registrar peso</h1>
      <FormularioRegistro
        inicial={{ data: dataDoDispositivo(), peso: '', cintura: '' }}
        rotuloEnviar="Registrar"
        aoEnviar={aoEnviar}
      />
      <DialogoConfirmacao
        aberto={conflito !== null}
        responder={confirmacao.responder}
        titulo="Substituir o registro?"
        confirmar="Substituir"
        desistir="Manter o de antes"
      >
        {conflito && (
          <>
            <p className="tabular-nums">{conflito.mensagem}</p>
            <p className="text-muted-foreground tabular-nums">
              O novo fica no lugar dele: {formatarPeso(conflito.novo.peso)} kg
              {conflito.novo.cintura === undefined
                ? ', sem cintura.'
                : ` e ${formatarCintura(conflito.novo.cintura)} cm de cintura.`}
            </p>
          </>
        )}
      </DialogoConfirmacao>
    </main>
  )
}
