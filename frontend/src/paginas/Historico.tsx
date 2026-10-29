import { CalendarDays, ChevronDown, ChevronRight, Download, Upload } from 'lucide-react'
import { useState } from 'react'
import type { DateRange } from 'react-day-picker'
import { ptBR } from 'react-day-picker/locale'
import { Link, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { dataDoDispositivo, ErroDaApi } from '@/api/cliente'
import { Simbolo } from '@/componentes/Logotipo'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import { Calendar } from '@/componentes/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/componentes/ui/popover'
import {
  diaDoIso,
  formatarData,
  formatarDiaDaSemana,
  formatarMes,
  isoDoDia,
  somarDias,
} from '@/formato/data'
import { formatarCintura, formatarPeso } from '@/formato/numero'
import { useAcompanhamento } from '@/hooks/useAcompanhamento'
import { useExportar } from '@/hooks/useConta'
import { useListaDeRegistros, type Registro } from '@/hooks/useRegistros'
import { SEM_RESPOSTA } from '@/lib/forma'

/** Sem período escolhido, os últimos 90 dias, como o backend sem parâmetro (endpoints.md). */
const DIAS_DO_PERIODO = 90

/**
 * Os registros do período, do mais recente para o mais antigo, agrupados por
 * mês (RF-14). Cada um abre a edição (UC-03). O período fica no endereço, para
 * a volta da edição cair no mesmo lugar.
 */
export function Historico() {
  const [parametros, setParametros] = useSearchParams()
  const hoje = dataDoDispositivo()
  const ate = parametros.get('ate') ?? hoje
  const de = parametros.get('de') ?? somarDias(ate, -(DIAS_DO_PERIODO - 1))
  const lista = useListaDeRegistros(de, ate)
  const exportar = useExportar()

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col gap-3 px-4 pt-4 pb-6 md:max-w-3xl md:gap-5 md:px-8 md:pt-7">
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <h1 className="text-2xl leading-tight font-bold">Histórico</h1>
        <div className="flex flex-wrap gap-2">
          <Button asChild variant="outline" className="px-3 text-sm">
            <Link to="/importar">
              <Upload aria-hidden="true" />
              Importar
            </Link>
          </Button>
          <Button
            type="button"
            variant="outline"
            className="px-3 text-sm"
            disabled={exportar.isPending}
            onClick={() =>
              exportar.mutate(undefined, {
                onError: (erro) =>
                  toast.error(erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA),
              })
            }
          >
            <Download aria-hidden="true" />
            Exportar
          </Button>
        </div>
      </div>
      {lista.isError ? (
        <Alert>
          <AlertTitle>{lista.error.message}</AlertTitle>
          <AlertDescription>Recarregue a página para tentar de novo.</AlertDescription>
        </Alert>
      ) : (
        <>
          <CampoPeriodo
            de={de}
            ate={ate}
            escolher={(novoDe, novoAte) => setParametros({ de: novoDe, ate: novoAte })}
          />
          {lista.data !== undefined &&
            (lista.data.length === 0 ? (
              <Vazio de={de} ate={ate} />
            ) : (
              <Lista registros={lista.data} />
            ))}
        </>
      )}
    </main>
  )
}

interface PropsDoPeriodo {
  de: string
  ate: string
  escolher: (de: string, ate: string) => void
}

/** O período em um calendário de intervalo: o segundo toque fecha e aplica. */
function CampoPeriodo({ de, ate, escolher }: PropsDoPeriodo) {
  const [aberto, setAberto] = useState(false)
  const [rascunho, setRascunho] = useState<DateRange | undefined>()
  const [toques, setToques] = useState(0)

  return (
    <div className="flex flex-col gap-1.5 md:max-w-xs">
      <span id="rotulo-periodo" className="text-sm font-semibold">
        Período
      </span>
      <Popover
        open={aberto}
        onOpenChange={(abrindo) => {
          setAberto(abrindo)
          setRascunho({ from: diaDoIso(de), to: diaDoIso(ate) })
          setToques(0)
        }}
      >
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            aria-labelledby="rotulo-periodo valor-periodo"
            className="w-full justify-start border-input bg-card px-3 font-normal dark:bg-card"
          >
            <CalendarDays aria-hidden="true" className="text-muted-foreground" />
            <span id="valor-periodo" className="tabular-nums">
              {formatarData(de)} a {formatarData(ate)}
            </span>
            <ChevronDown aria-hidden="true" className="ml-auto text-muted-foreground" />
          </Button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-auto rounded-xl p-2">
          <Calendar
            mode="range"
            locale={ptBR}
            selected={rascunho}
            defaultMonth={diaDoIso(ate)}
            onSelect={(intervalo, dia) => {
              // O primeiro toque começa um intervalo novo; o segundo o fecha
              const novo = toques === 0 ? { from: dia, to: undefined } : intervalo
              setRascunho(novo)
              setToques(toques + 1)
              if (toques === 1 && novo?.from !== undefined && novo.to !== undefined) {
                escolher(isoDoDia(novo.from), isoDoDia(novo.to))
                setAberto(false)
              }
            }}
          />
        </PopoverContent>
      </Popover>
    </div>
  )
}

function Lista({ registros }: { registros: Registro[] }) {
  const meses: { chave: string; registros: Registro[] }[] = []
  for (const registro of registros) {
    const chave = registro.data.slice(0, 7)
    const ultimo = meses.at(-1)
    if (ultimo?.chave === chave) {
      ultimo.registros.push(registro)
    } else {
      meses.push({ chave, registros: [registro] })
    }
  }

  return (
    <div className="flex flex-col">
      <p className="text-sm text-muted-foreground tabular-nums md:mb-2">
        {contar(registros.length)}, do mais recente para o mais antigo
      </p>
      <div className="md:rounded-xl md:border md:bg-card md:py-2">
        <div
          aria-hidden="true"
          className="hidden grid-cols-[minmax(7rem,1.2fr)_minmax(5rem,1fr)_minmax(5rem,1fr)_minmax(5rem,1fr)] gap-x-4 border-b px-3 pb-2 text-sm font-semibold text-muted-foreground md:grid"
        >
          <span>Data</span>
          <span>Dia</span>
          <span className="text-right">Peso</span>
          <span className="text-right">Cintura</span>
        </div>
        {meses.map((mes) => (
          <section key={mes.chave} aria-labelledby={`mes-${mes.chave}`}>
            <h2
              id={`mes-${mes.chave}`}
              className="flex items-baseline justify-between gap-2 pt-4 pb-1.5 text-sm font-semibold text-muted-foreground md:px-3 md:pt-3.5"
            >
              <span>{formatarMes(`${mes.chave}-01`)}</span>
              <span className="font-normal tabular-nums">{contar(mes.registros.length)}</span>
            </h2>
            <ul>
              {mes.registros.map((registro) => (
                <li key={registro.data}>
                  <Item registro={registro} />
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  )
}

function Item({ registro }: { registro: Registro }) {
  const { data, peso, cintura } = registro
  const rotulo = `Editar o registro de ${formatarData(data)}, ${formatarPeso(peso)} kg${
    cintura === null ? '' : `, cintura ${formatarCintura(cintura)} cm`
  }`

  return (
    <Link
      to={`/historico/${data}`}
      aria-label={rotulo}
      className="flex min-h-14 items-center gap-3 border-t py-2 text-sm tabular-nums md:grid md:min-h-10 md:grid-cols-[minmax(7rem,1.2fr)_minmax(5rem,1fr)_minmax(5rem,1fr)_minmax(5rem,1fr)] md:gap-x-4 md:px-3 md:py-0 md:hover:bg-muted"
    >
      <span className="flex flex-col md:contents">
        <span className="text-base font-semibold md:text-sm">{formatarData(data)}</span>
        <span className="text-xs text-muted-foreground md:text-sm">{formatarDiaDaSemana(data)}</span>
      </span>
      <span className="ml-auto flex flex-col items-end md:contents">
        <span className="text-base font-bold md:text-right md:text-sm">{formatarPeso(peso)} kg</span>
        <span className="text-xs text-muted-foreground md:text-right md:text-sm md:text-foreground">
          {cintura === null ? '' : `${formatarCintura(cintura)} cm`}
        </span>
      </span>
      <ChevronRight aria-hidden="true" className="size-4.5 flex-none text-muted-foreground md:hidden" />
    </Link>
  )
}

/** Período sem registro, ou a conta sem nenhum, que recebe o convite (como o UC-08 1b). */
function Vazio({ de, ate }: { de: string; ate: string }) {
  const acompanhamento = useAcompanhamento()
  if (acompanhamento.data === undefined) {
    return null
  }
  if (acompanhamento.data.serie.length === 0) {
    return (
      <section
        aria-labelledby="titulo-vazio"
        className="flex flex-col items-start gap-4 rounded-xl border bg-card px-5 py-6 text-card-foreground"
      >
        <span aria-hidden="true" className="text-primary">
          <Simbolo className="size-10" />
        </span>
        <h2 id="titulo-vazio" className="text-lg leading-snug font-bold">
          Nenhum registro ainda
        </h2>
        <p className="text-muted-foreground">
          Os pesos que você registrar aparecem aqui, do mais recente para o mais antigo.
        </p>
        <Button asChild className="w-full md:w-auto">
          <Link to="/registrar">Registrar peso</Link>
        </Button>
        <p className="text-sm text-muted-foreground">
          Já tem um histórico em planilha?{' '}
          <Link to="/importar" className="font-semibold text-primary underline underline-offset-3">
            Importar CSV
          </Link>
        </p>
      </section>
    )
  }
  return (
    <section
      aria-labelledby="titulo-vazio"
      className="flex flex-col gap-3 rounded-xl border bg-card p-4 text-card-foreground"
    >
      <h2 id="titulo-vazio" className="text-lg leading-snug font-bold">
        Nenhum registro no período
      </h2>
      <p className="text-muted-foreground tabular-nums">
        Entre {formatarData(de)} e {formatarData(ate)} não há peso registrado. Escolha outro período.
      </p>
    </section>
  )
}

function contar(registros: number): string {
  return registros === 1 ? '1 registro' : `${registros} registros`
}
