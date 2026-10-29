import { ChevronLeft } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { dataDoDispositivo, ErroDaApi } from '@/api/cliente'
import { DialogoConfirmacao } from '@/componentes/DialogoConfirmacao'
import { FormularioFase } from '@/componentes/FormularioFase'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import { formatarData } from '@/formato/data'
import { ehFeminino, formatarOTipo } from '@/formato/fase'
import { formatarTaxa } from '@/formato/numero'
import { useCriarFase, useFases, useNiveis, type FaseEnviada } from '@/hooks/useFases'
import { useConfirmacao } from '@/lib/confirmacao'

interface Encerramento {
  mensagem: string
  nova: FaseEnviada
}

/**
 * Criar fase (UC-05). A fase sem fim que começa depois da em aberto a
 * encerra na véspera, e o backend responde 409 com qual fase termina e quando;
 * só o segundo envio, com o sim da pessoa, encerra (RN-22, UC-05 2a).
 */
export function NovaFase() {
  const niveis = useNiveis()
  const fases = useFases()
  const criar = useCriarFase()
  const confirmacao = useConfirmacao<Encerramento>()
  const navegar = useNavigate()

  async function aoEnviar(nova: FaseEnviada) {
    try {
      await criar.mutateAsync(nova)
    } catch (erro) {
      if (!(erro instanceof ErroDaApi && erro.codigo === 'FASE_EM_ABERTO_SERA_ENCERRADA')) {
        throw erro
      }
      if (!(await confirmacao.pedir({ mensagem: erro.message, nova }))) {
        return
      }
      await criar.mutateAsync({ ...nova, encerrarEmAberto: true })
    }
    toast.success(
      `${formatarOTipo(nova.tipo, { noInicio: true })} foi ${ehFeminino(nova.tipo) ? 'criada' : 'criado'}.`,
    )
    navegar('/fases')
  }

  // A em aberto é a que o 409 vai encerrar; o título a nomeia, e a mensagem diz o dia
  const emAberto = fases.data?.find((fase) => fase.dataFim === null)
  const encerramento = confirmacao.pedido
  const moderado = niveis.data?.find(
    (nivel) => nivel.tipo === 'CUTTING' && nivel.nivel === 'MODERADO',
  )

  return (
    <main className="mx-auto flex w-full max-w-md flex-col gap-3 px-4 pt-2 pb-6 md:px-0 md:pt-6">
      <div className="flex flex-col items-start gap-1">
        <Button asChild variant="ghost" className="-ml-2 px-2 text-primary">
          <Link to="/fases">
            <ChevronLeft aria-hidden="true" />
            Fases
          </Link>
        </Button>
        <h1 className="text-2xl leading-tight font-bold">Nova fase</h1>
      </div>
      {niveis.isError ? (
        <Alert>
          <AlertTitle>{niveis.error.message}</AlertTitle>
          <AlertDescription>Recarregue a página para tentar de novo.</AlertDescription>
        </Alert>
      ) : (
        niveis.data !== undefined && (
          <FormularioFase
            modo="criar"
            niveis={niveis.data}
            inicial={{
              tipo: 'CUTTING',
              dataInicio: dataDoDispositivo(),
              terminou: false,
              dataFim: dataDoDispositivo(),
              nivel: moderado === undefined ? null : 'MODERADO',
              taxaAlvoValor:
                moderado === undefined ? '' : formatarTaxa(moderado.valor, { comSinal: false }),
              taxaAlvoUnidade: moderado?.unidade ?? 'PERCENTUAL_SEMANA',
              pesoAlvo: '',
            }}
            rotuloEnviar="Criar fase"
            aoEnviar={aoEnviar}
          />
        )
      )}
      <DialogoConfirmacao
        aberto={encerramento !== null}
        responder={confirmacao.responder}
        titulo={`Encerrar ${emAberto === undefined ? 'a fase' : formatarOTipo(emAberto.tipo)} em aberto?`}
        confirmar="Criar e encerrar"
        desistir="Cancelar"
      >
        {encerramento && (
          <>
            <p className="tabular-nums">{encerramento.mensagem}</p>
            <p className="text-sm text-muted-foreground tabular-nums">
              {formatarOTipo(encerramento.nova.tipo, { noInicio: true })} começa em{' '}
              {formatarData(encerramento.nova.dataInicio)}.
            </p>
          </>
        )}
      </DialogoConfirmacao>
    </main>
  )
}
