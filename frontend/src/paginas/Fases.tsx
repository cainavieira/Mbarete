import { ChevronRight, Plus, Target } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import { formatarData, formatarIntervalo, somarDias } from '@/formato/data'
import {
  ehFeminino,
  formatarDoTipo,
  formatarNivel,
  formatarTaxaAlvo,
  formatarTipo,
} from '@/formato/fase'
import { formatarDiferenca, formatarPeso } from '@/formato/numero'
import { useFases, type FaseListada } from '@/hooks/useFases'
import { cn } from '@/lib/utils'

/**
 * As fases da pessoa por situação: a corrente, as agendadas e as encerradas,
 * da mais recente para a mais antiga, com o histórico de cada uma (UC-09).
 * Cada fase abre a tela dela, onde se edita, encerra e apaga.
 */
export function Fases() {
  const consulta = useFases()

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col gap-6 px-4 pt-4 pb-6 md:px-8 md:pt-7">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-2xl leading-tight font-bold">Fases</h1>
        <Button asChild>
          <Link to="/fases/nova">
            <Plus aria-hidden="true" />
            Nova fase
          </Link>
        </Button>
      </div>
      {consulta.isError ? (
        <Alert>
          <AlertTitle>{consulta.error.message}</AlertTitle>
          <AlertDescription>Recarregue a página para tentar de novo.</AlertDescription>
        </Alert>
      ) : (
        consulta.data !== undefined && <Lista fases={consulta.data} />
      )}
    </main>
  )
}

function Lista({ fases }: { fases: FaseListada[] }) {
  if (fases.length === 0) {
    return (
      <section aria-labelledby="titulo-sem-fase" className={CARTAO}>
        <h2 id="titulo-sem-fase" className="text-lg leading-snug font-bold">
          Nenhuma fase ainda
        </h2>
        <p className="text-muted-foreground">
          Sem fase, o acompanhamento mostra o peso semanal, as variações e a cintura. A fase traz o
          plano: a taxa alvo, o veredito e a barra até o peso alvo.
        </p>
      </section>
    )
  }
  const corrente = fases.find(({ situacao }) => situacao === 'CORRENTE')
  const agendadas = fases
    .filter(({ situacao }) => situacao === 'FUTURA')
    .sort((a, b) => a.dataInicio.localeCompare(b.dataInicio))
  const encerradas = fases
    .filter(({ situacao }) => situacao === 'ENCERRADA')
    .sort((a, b) => b.dataInicio.localeCompare(a.dataInicio))
  // O fim no futuro da corrente nasceu da agendada que começa no dia seguinte (RN-67)
  const fimDaCorrente = corrente?.dataFim ?? null
  const queAcompanha =
    fimDaCorrente === null
      ? undefined
      : agendadas.find(({ dataInicio }) => dataInicio === somarDias(fimDaCorrente, 1))

  return (
    <>
      <Secao id="corrente" titulo="Corrente">
        {corrente === undefined ? (
          <p className="text-sm text-muted-foreground">Hoje não há fase.</p>
        ) : (
          <CartaoDaFase
            fase={corrente}
            rotulo={`${formatarTipo(corrente.tipo, { noInicio: true })} corrente, ${periodo(corrente)}, abrir a fase`}
          >
            <span className="tabular-nums">{periodo(corrente)}</span>
            {queAcompanha !== undefined && (
              <span className="text-muted-foreground">
                O fim acompanha o início {formatarDoTipo(queAcompanha.tipo)}{' '}
                {ehFeminino(queAcompanha.tipo) ? 'agendada' : 'agendado'}.
              </span>
            )}
            <Plano fase={corrente} />
          </CartaoDaFase>
        )}
      </Secao>

      {agendadas.length > 0 && (
        <Secao id="agendadas" titulo={agendadas.length === 1 ? 'Agendada' : 'Agendadas'}>
          {agendadas.map((fase) => (
            <CartaoDaFase
              key={fase.id}
              fase={fase}
              contar={false}
              rotulo={`${formatarTipo(fase.tipo, { noInicio: true })} ${ehFeminino(fase.tipo) ? 'agendada' : 'agendado'}, começa em ${formatarData(fase.dataInicio)}, abrir a fase`}
            >
              <span className="tabular-nums">começa em {formatarData(fase.dataInicio)}</span>
              <span className="text-muted-foreground">Não entra no acompanhamento até começar.</span>
            </CartaoDaFase>
          ))}
        </Secao>
      )}

      <Secao id="encerradas" titulo="Encerradas">
        {encerradas.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhuma fase encerrada ainda.</p>
        ) : (
          encerradas.map((fase) => (
            <CartaoDaFase
              key={fase.id}
              fase={fase}
              rotulo={`${formatarTipo(fase.tipo, { noInicio: true })} ${ehFeminino(fase.tipo) ? 'encerrada' : 'encerrado'}, ${periodo(fase)}, abrir a fase`}
            >
              <Historico fase={fase} />
            </CartaoDaFase>
          ))
        )}
      </Secao>
    </>
  )
}

const CARTAO = 'flex flex-col gap-3 rounded-xl border bg-card p-4 text-card-foreground'

function Secao({ id, titulo, children }: { id: string; titulo: string; children: ReactNode }) {
  return (
    <section aria-labelledby={`titulo-${id}`} className="flex flex-col gap-2.5">
      <h2 id={`titulo-${id}`} className="text-sm font-semibold text-muted-foreground">
        {titulo}
      </h2>
      {children}
    </section>
  )
}

interface PropsDoCartao {
  fase: FaseListada
  rotulo: string
  /** A agendada ainda não tem registro para contar. */
  contar?: boolean
  children: ReactNode
}

function CartaoDaFase({ fase, rotulo, contar = true, children }: PropsDoCartao) {
  return (
    <Link
      to={`/fases/${fase.id}`}
      aria-label={rotulo}
      className="flex flex-col gap-3 rounded-xl border bg-card p-4 text-card-foreground"
    >
      <div className="flex items-center gap-2">
        <h3 className="text-lg leading-snug font-bold capitalize">{formatarTipo(fase.tipo)}</h3>
        {contar && (
          <span className="ml-auto text-sm text-muted-foreground tabular-nums">
            {contarRegistros(fase.registros)}
          </span>
        )}
        <ChevronRight
          aria-hidden="true"
          className={cn('size-4.5 flex-none text-muted-foreground', !contar && 'ml-auto')}
        />
      </div>
      <div className="flex flex-col gap-1 text-sm">{children}</div>
    </Link>
  )
}

function Plano({ fase }: { fase: FaseListada }) {
  const { nivel, taxaAlvoValor, taxaAlvoUnidade, pesoAlvo } = fase
  if (nivel === null || taxaAlvoValor === null || taxaAlvoUnidade === null || pesoAlvo === null) {
    return null
  }
  return (
    <span className="tabular-nums">
      {formatarNivel(nivel)} · taxa alvo {formatarTaxaAlvo(taxaAlvoValor, taxaAlvoUnidade)} por
      semana · peso alvo {formatarPeso(pesoAlvo)} kg
    </span>
  )
}

/** O histórico da RN-40, ou o motivo de cada parte faltar (UC-09 2b, 2c, 2d). */
function Historico({ fase }: { fase: FaseListada }) {
  const historico = fase.historico
  const variacao = historico?.variacaoTotal ?? null
  const pesoAlvo = fase.pesoAlvo === null ? '' : `${formatarPeso(fase.pesoAlvo)} kg`

  return (
    <>
      <span className="tabular-nums">
        {periodo(fase)}
        {historico !== null && ` · ${historico.duracaoEmDias} dias`}
      </span>
      {variacao === null ? (
        <span className="text-muted-foreground">
          Sem variação total: a fase teve menos de dois registros.
        </span>
      ) : (
        <>
          <span className="tabular-nums">
            <span className="text-muted-foreground">variação total</span>{' '}
            <strong>{formatarDiferenca(variacao.valor)} kg</strong>{' '}
            <span className="text-muted-foreground">
              {formatarIntervalo(variacao.de, variacao.ate)}
            </span>
          </span>
          {variacao.poucosPontos && (
            <span className="text-muted-foreground">
              Do primeiro ao último registro, porque a fase teve poucos registros.
            </span>
          )}
        </>
      )}
      {historico?.alvo === 'ATINGIDO' && (
        <span className="inline-flex items-center gap-1.5 tabular-nums">
          <Target aria-hidden="true" className="size-4 flex-none" />
          Peso alvo de {pesoAlvo} atingido
          {historico.alvoAtingidoEm !== null && ` em ${formatarData(historico.alvoAtingidoEm)}`}.
        </span>
      )}
      {historico?.alvo === 'NAO_ATINGIDO' && (
        <span className="text-muted-foreground tabular-nums">
          Peso alvo de {pesoAlvo} não atingido.
        </span>
      )}
      {historico?.alvo === 'LADO_ERRADO' && (
        <span className="text-muted-foreground tabular-nums">
          Peso alvo de {pesoAlvo} do lado errado do peso de partida, sem avaliação.
        </span>
      )}
      {historico?.alvo === 'SEM_AVALIACAO' && (
        <span className="text-muted-foreground tabular-nums">
          Peso alvo de {pesoAlvo} sem avaliação: faltou peso semanal no início.
        </span>
      )}
    </>
  )
}

function periodo({ dataInicio, dataFim }: FaseListada): string {
  return dataFim === null
    ? `desde ${formatarData(dataInicio)}`
    : `de ${formatarData(dataInicio)} a ${formatarData(dataFim)}`
}

function contarRegistros(registros: number): string {
  if (registros === 0) {
    return 'nenhum registro'
  }
  return registros === 1 ? '1 registro' : `${registros} registros`
}
