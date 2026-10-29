import { ChevronLeft, Trash2 } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { dataDoDispositivo, ErroDaApi } from '@/api/cliente'
import { CampoData } from '@/componentes/CampoData'
import { DialogoConfirmacao } from '@/componentes/DialogoConfirmacao'
import { FormularioFase } from '@/componentes/FormularioFase'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/componentes/ui/dialog'
import { formatarData, somarDias } from '@/formato/data'
import { ehFeminino, formatarDoTipo, formatarOTipo, formatarTipo } from '@/formato/fase'
import { formatarPeso, formatarTaxa } from '@/formato/numero'
import {
  useApagarFase,
  useEditarFase,
  useEncerrarFase,
  useFases,
  useNiveis,
  type FaseEnviada,
  type FaseListada,
} from '@/hooks/useFases'
import { useConfirmacao } from '@/lib/confirmacao'
import { SEM_RESPOSTA } from '@/lib/forma'

interface Mudanca {
  mensagem: string
  editada: FaseEnviada
}

/**
 * A fase aberta da lista: editar (UC-06), encerrar a corrente (UC-14) e
 * apagar (UC-07). Mudar o início que leva junto o fim no futuro da anterior
 * pede o sim, como a criação (RN-67, UC-06 2i).
 */
export function EditarFase() {
  const id = Number(useParams().id)
  const fases = useFases()
  const niveis = useNiveis()

  if (fases.isError || niveis.isError) {
    return (
      <Moldura>
        <Alert>
          <AlertTitle>{(fases.error ?? niveis.error)?.message}</AlertTitle>
          <AlertDescription>Recarregue a página para tentar de novo.</AlertDescription>
        </Alert>
      </Moldura>
    )
  }
  if (fases.data === undefined || niveis.data === undefined) {
    return null
  }
  const fase = fases.data.find((daLista) => daLista.id === id)
  if (fase === undefined) {
    return (
      <Moldura>
        <p className="text-muted-foreground">Essa fase não existe mais.</p>
      </Moldura>
    )
  }
  return <Fase fase={fase} fases={fases.data} niveis={niveis.data} />
}

function Moldura({ children }: { children: ReactNode }) {
  return (
    <main className="mx-auto flex w-full max-w-md flex-col gap-3 px-4 pt-2 pb-6 md:px-0 md:pt-6">
      <Button asChild variant="ghost" className="-ml-2 self-start px-2 text-primary">
        <Link to="/fases">
          <ChevronLeft aria-hidden="true" />
          Fases
        </Link>
      </Button>
      {children}
    </main>
  )
}

interface PropsDaFase {
  fase: FaseListada
  fases: FaseListada[]
  niveis: NonNullable<ReturnType<typeof useNiveis>['data']>
}

function Fase({ fase, fases, niveis }: PropsDaFase) {
  const editar = useEditarFase(fase.id)
  const confirmacao = useConfirmacao<Mudanca>()
  const [encerrando, setEncerrando] = useState(false)
  const [apagando, setApagando] = useState(false)
  const navegar = useNavigate()

  async function aoEnviar(editada: FaseEnviada) {
    try {
      await editar.mutateAsync(editada)
    } catch (erro) {
      if (!(erro instanceof ErroDaApi && erro.codigo === 'FIM_DA_ANTERIOR_SERA_MOVIDO')) {
        throw erro
      }
      if (!(await confirmacao.pedir({ mensagem: erro.message, editada }))) {
        return
      }
      await editar.mutateAsync({ ...editada, moverFimDaAnterior: true })
    }
    toast.success(
      `${formatarOTipo(editada.tipo, { noInicio: true })} foi ${ehFeminino(editada.tipo) ? 'salva' : 'salvo'}.`,
    )
    navegar('/fases')
  }

  // A anterior colada, cujo fim no futuro acompanha o início desta (RN-67)
  const anterior = fases.find(({ dataFim }) => dataFim === somarDias(fase.dataInicio, -1))
  const mudanca = confirmacao.pedido

  return (
    <Moldura>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl leading-tight font-bold">
          {formatarTipo(fase.tipo, { noInicio: true })}
        </h1>
        <p className="text-sm text-muted-foreground tabular-nums">{subtitulo(fase)}</p>
      </div>
      <FormularioFase
        modo="editar"
        tipoGravado={fase.tipo}
        niveis={niveis}
        inicial={{
          tipo: fase.tipo,
          dataInicio: fase.dataInicio,
          terminou: fase.dataFim !== null,
          dataFim: fase.dataFim ?? dataDoDispositivo(),
          nivel: fase.nivel,
          taxaAlvoValor:
            fase.taxaAlvoValor === null ? '' : formatarTaxa(fase.taxaAlvoValor, { comSinal: false }),
          taxaAlvoUnidade:
            fase.taxaAlvoUnidade ??
            niveis.find(({ tipo }) => tipo === fase.tipo)?.unidade ??
            'PERCENTUAL_SEMANA',
          pesoAlvo: fase.pesoAlvo === null ? '' : formatarPeso(fase.pesoAlvo),
        }}
        rotuloEnviar="Salvar"
        aoEnviar={aoEnviar}
      >
        <div
          role="group"
          aria-label="Outras ações"
          className="mt-6 flex flex-col gap-2 border-t pt-5"
        >
          {fase.situacao === 'CORRENTE' && (
            <Button type="button" variant="outline" onClick={() => setEncerrando(true)}>
              Encerrar fase
            </Button>
          )}
          <Button
            type="button"
            variant="outline"
            className="text-destructive hover:text-destructive"
            onClick={() => setApagando(true)}
          >
            <Trash2 aria-hidden="true" />
            Apagar fase
          </Button>
        </div>
      </FormularioFase>

      <DialogoConfirmacao
        aberto={mudanca !== null}
        responder={confirmacao.responder}
        titulo={`Mudar também o fim ${anterior === undefined ? 'da fase anterior' : formatarDoTipo(anterior.tipo)}?`}
        confirmar="Salvar e mover"
        desistir="Cancelar"
      >
        {mudanca && (
          <>
            <p className="tabular-nums">{mudanca.mensagem}</p>
            <p className="text-sm text-muted-foreground tabular-nums">
              {formatarOTipo(mudanca.editada.tipo, { noInicio: true })} passa a começar em{' '}
              {formatarData(mudanca.editada.dataInicio)}.
            </p>
          </>
        )}
      </DialogoConfirmacao>

      {encerrando && <Encerrar fase={fase} fechar={() => setEncerrando(false)} />}
      <Apagar fase={fase} fases={fases} aberto={apagando} fechar={() => setApagando(false)} />
    </Moldura>
  )
}

function subtitulo(fase: FaseListada): string {
  const registros =
    fase.registros === 0
      ? 'nenhum registro'
      : fase.registros === 1
        ? '1 registro'
        : `${fase.registros} registros`
  const inicio = formatarData(fase.dataInicio)
  const fim = fase.dataFim === null ? null : formatarData(fase.dataFim)
  if (fase.situacao === 'FUTURA') {
    return `${ehFeminino(fase.tipo) ? 'agendada' : 'agendado'}, começa em ${inicio} · ${registros}`
  }
  const periodo = fim === null ? `desde ${inicio}` : `de ${inicio} a ${fim}`
  if (fase.situacao === 'CORRENTE') {
    return `corrente, ${periodo} · ${registros}`
  }
  return `${ehFeminino(fase.tipo) ? 'encerrada' : 'encerrado'}, ${periodo} · ${registros}`
}

/**
 * O encerramento pede a data, com hoje como padrão (UC-14). Data futura ou
 * antes do início é regra, e a recusa vem do backend, embaixo da data.
 */
function Encerrar({ fase, fechar }: { fase: FaseListada; fechar: () => void }) {
  const encerrar = useEncerrarFase(fase.id)
  const [dataFim, setDataFim] = useState(dataDoDispositivo())
  const [erro, setErro] = useState<string | undefined>()
  const navegar = useNavigate()

  async function confirmar() {
    setErro(undefined)
    try {
      await encerrar.mutateAsync(dataFim)
    } catch (recusa) {
      setErro(recusa instanceof ErroDaApi ? recusa.message : SEM_RESPOSTA)
      return
    }
    toast.success(
      `${formatarOTipo(fase.tipo, { noInicio: true })} foi ${ehFeminino(fase.tipo) ? 'encerrada' : 'encerrado'} em ${formatarData(dataFim)}.`,
    )
    navegar('/fases')
  }

  return (
    <Dialog open onOpenChange={(aberto) => !aberto && fechar()}>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>Encerrar {formatarOTipo(fase.tipo)}?</DialogTitle>
        </DialogHeader>
        <CampoData
          id="encerramento"
          rotulo="Data de encerramento"
          valor={dataFim}
          aoEscolher={setDataFim}
          erro={erro}
        />
        <DialogDescription asChild>
          <div className="flex flex-col gap-1.5">
            <p>
              A partir do dia seguinte não há fase, até você criar outra. Os registros depois dessa
              data ficam sem fase.
            </p>
            <p className="text-xs">
              Para terminar numa data futura, crie a fase seguinte começando no dia seguinte.
            </p>
          </div>
        </DialogDescription>
        <DialogFooter className="mx-0 mb-0 flex-col gap-2 rounded-none border-0 bg-transparent p-0 sm:flex-col *:w-full">
          <Button type="button" disabled={encerrar.isPending} onClick={() => void confirmar()}>
            Encerrar
          </Button>
          <Button type="button" variant="outline" onClick={fechar}>
            Cancelar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

interface PropsDoApagar {
  fase: FaseListada
  fases: FaseListada[]
  aberto: boolean
  fechar: () => void
}

/**
 * Apagar pede o sim sempre, com quantos registros perdem a fase e o que
 * acontece com o fim da anterior, que vem pronto no `aoApagar` (UC-07, RN-67).
 */
function Apagar({ fase, fases, aberto, fechar }: PropsDoApagar) {
  const apagar = useApagarFase(fase.id)
  const navegar = useNavigate()
  const dele = ehFeminino(fase.tipo) ? 'dela' : 'dele'

  async function responder(sim: boolean) {
    if (!sim) {
      fechar()
      return
    }
    try {
      await apagar.mutateAsync()
    } catch (erro) {
      fechar()
      toast.error(erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA)
      return
    }
    toast.success('A fase foi apagada.')
    navegar('/fases')
  }

  const titulo =
    fase.situacao === 'FUTURA'
      ? `Apagar ${formatarOTipo(fase.tipo)} ${ehFeminino(fase.tipo) ? 'agendada' : 'agendado'}?`
      : `Apagar ${formatarOTipo(fase.tipo)} ${fase.dataFim === null ? `desde ${formatarData(fase.dataInicio)}` : `de ${formatarData(fase.dataInicio)} a ${formatarData(fase.dataFim)}`}?`
  const registros =
    fase.registros === 0
      ? 'A fase não tem registro.'
      : fase.registros === 1
        ? `O registro ${dele} continua, e passa a não ter fase.`
        : `Os ${fase.registros} registros ${dele} continuam, e passam a não ter fase.`

  return (
    <DialogoConfirmacao
      aberto={aberto}
      responder={(sim) => void responder(sim)}
      titulo={titulo}
      confirmar="Apagar fase"
      desistir="Cancelar"
      apaga
    >
      <p className="tabular-nums">{registros}</p>
      <p className="text-sm text-muted-foreground tabular-nums">{consequencia(fase, fases)}</p>
    </DialogoConfirmacao>
  )
}

function consequencia(fase: FaseListada, fases: FaseListada[]): string {
  const recriar = 'Recriar a fase nas mesmas datas devolve a classificação.'
  const efeito = fase.aoApagar
  if (efeito !== null) {
    const anterior = fases.find(({ id }) => id === efeito.fase)
    if (anterior !== undefined && anterior.dataFim !== null) {
      const qual = `${formatarOTipo(anterior.tipo, { noInicio: true })}${anterior.situacao === 'CORRENTE' ? ' corrente' : ''}`
      return efeito.dataFim === null
        ? `${qual} perde o fim em ${formatarData(anterior.dataFim)} e volta a ficar em aberto.`
        : `${qual} passa a terminar em ${formatarData(efeito.dataFim)}.`
    }
  }
  if (fase.situacao === 'CORRENTE') {
    return `Sem fase corrente, o acompanhamento deixa de mostrar o veredito, a taxa alvo e a barra de progresso. ${recriar}`
  }
  return recriar
}
