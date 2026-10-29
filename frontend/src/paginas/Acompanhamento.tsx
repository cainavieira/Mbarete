import { ArrowDown, ArrowUp, Target } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { dataDoDispositivo } from '@/api/cliente'
import type { Esquema } from '@/api/tipos'
import { GraficoEvolucao } from '@/componentes/GraficoEvolucao'
import { Simbolo } from '@/componentes/Logotipo'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import { formatarData, formatarIntervalo } from '@/formato/data'
import { formatarDoTipo, formatarTipo } from '@/formato/fase'
import {
  formatarCintura,
  formatarDiferenca,
  formatarInteiro,
  formatarPeso,
  formatarTaxa,
} from '@/formato/numero'
import { useAcompanhamento, type Acompanhamento as Dados } from '@/hooks/useAcompanhamento'
import { cn } from '@/lib/utils'

type Fase = Esquema<'FaseDto'>

const CARTAO = 'flex flex-col rounded-xl border bg-card p-4 text-card-foreground'
const LINK = 'font-semibold text-primary underline underline-offset-3'

/**
 * Estou no ritmo do meu ciclo? (UC-08). Tudo vem de uma resposta só, e a tela
 * só decide o que mostrar com o que veio: sem peso semanal hoje, não há
 * veredito, barra nem variações (UC-08 1a).
 */
export function Acompanhamento() {
  const consulta = useAcompanhamento()

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col gap-3 px-4 pt-5 pb-6 md:px-8 md:pt-7 lg:max-w-6xl lg:gap-5">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2">
        <h1 className="text-2xl leading-tight font-bold">Acompanhamento</h1>
        <span className="text-sm text-muted-foreground tabular-nums">
          {formatarData(dataDoDispositivo())}
        </span>
      </div>
      {consulta.isError ? (
        <Alert>
          <AlertTitle>{consulta.error.message}</AlertTitle>
          <AlertDescription>Recarregue a página para tentar de novo.</AlertDescription>
        </Alert>
      ) : (
        consulta.data !== undefined && <Conteudo dados={consulta.data} />
      )}
    </main>
  )
}

function Conteudo({ dados }: { dados: Dados }) {
  if (dados.serie.length === 0) {
    return <PrimeiroRegistro />
  }
  const fase = dados.faseCorrente
  const temPeso = dados.pesoSemanal !== null
  const progresso = temPeso ? dados.progresso : null

  return (
    // No computador, o gráfico ocupa a coluna da direita do alto até embaixo, e a última
    // linha absorve a altura dele, para os cartões da esquerda não se afastarem
    <div className="flex flex-col gap-3 lg:grid lg:grid-cols-[minmax(0,26rem)_minmax(0,1fr)] lg:grid-rows-[repeat(5,auto)_1fr] lg:items-start lg:gap-x-5 lg:gap-y-4">
      <PesoSemanal dados={dados} />
      {progresso?.atingido && fase?.pesoAlvo != null && <AlvoAtingido alvo={fase.pesoAlvo} faseId={fase.id} />}
      {progresso !== null && fase?.pesoAlvo != null && (
        <Progresso progresso={progresso} alvo={fase.pesoAlvo} faseId={fase.id} />
      )}
      <GraficoEvolucao
        serie={dados.serie}
        faseCorrente={fase}
        faixa={dados.faixa}
        className="lg:col-start-2 lg:row-span-full lg:row-start-1"
      />
      {temPeso && <Variacoes variacoes={dados.variacoes} />}
      {dados.cintura !== null && <Cintura cintura={dados.cintura} fase={fase} />}
    </div>
  )
}

function PesoSemanal({ dados }: { dados: Dados }) {
  const { pesoSemanal, faseCorrente: fase, veredito } = dados
  const janela = dados.registrosNaJanela

  return (
    <section
      aria-labelledby="titulo-semanal"
      className="flex flex-col gap-1.5 rounded-xl bg-destaque px-4.5 pt-4 pb-4.5 text-destaque-foreground lg:col-start-1"
    >
      <h2 id="titulo-semanal" className="text-sm font-semibold text-destaque-suave">
        Peso semanal ·{' '}
        {fase === null
          ? 'sem fase'
          : `${formatarTipo(fase.tipo)} desde ${formatarData(fase.dataInicio)}`}
      </h2>
      {pesoSemanal === null ? (
        <>
          <Selo estado="DADOS_INSUFICIENTES" manutencao={false} />
          <p className="text-sm text-destaque-suave tabular-nums">
            {janela === 0
              ? 'Nenhum registro nos últimos 7 dias.'
              : `Só ${janela} ${janela === 1 ? 'registro' : 'registros'} nos últimos 7 dias.`}
          </p>
        </>
      ) : (
        <p className="text-5xl leading-none font-bold tracking-tight tabular-nums">
          {formatarPeso(pesoSemanal)}{' '}
          <small className="text-lg font-semibold tracking-normal text-destaque-suave">kg</small>
        </p>
      )}
      {pesoSemanal !== null && fase !== null && veredito !== null && (
        <Veredito veredito={veredito} fase={fase} dados={dados} />
      )}
    </section>
  )
}

interface PropsDoVeredito {
  veredito: Esquema<'VereditoDto'>
  fase: Fase
  dados: Dados
}

function Veredito({ veredito, fase, dados }: PropsDoVeredito) {
  const manutencao = fase.tipo === 'MANUTENCAO'
  const { taxaReal, aPartirDe } = veredito
  const taxaAlvo = dados.taxaAlvoKgSemana
  const linhas: { rotulo: string; valor: string; extra?: string }[] = []

  if (taxaReal !== null) {
    linhas.push({
      rotulo: 'taxa real',
      valor: `${formatarTaxa(taxaReal.valor, { comSinal: true })} kg por semana`,
      extra: formatarIntervalo(taxaReal.de, taxaReal.ate),
    })
  }
  // A manutenção não tem taxa alvo: o lugar dela é da faixa (UC-08 4b)
  if (!manutencao && taxaAlvo !== null) {
    linhas.push({
      rotulo: 'taxa alvo',
      valor: `${formatarTaxa(taxaAlvo, { comSinal: true })} kg por semana`,
      // O percentual trafega sem sinal; o sinal é o da taxa em quilos, que já vem com ele
      extra:
        fase.taxaAlvoUnidade === 'PERCENTUAL_SEMANA' && fase.taxaAlvoValor !== null
          ? `(${formatarTaxa(Math.sign(taxaAlvo) * fase.taxaAlvoValor, { comSinal: true })}%)`
          : undefined,
    })
  }
  if (dados.faixa !== null) {
    linhas.push({
      rotulo: 'faixa',
      valor: `de ${formatarPeso(dados.faixa.minimo)} a ${formatarPeso(dados.faixa.maximo)} kg`,
    })
  }

  return (
    <div className="mt-1.5 flex flex-col gap-2">
      <Selo estado={veredito.estado} manutencao={manutencao} />
      {aPartirDe !== null && (
        <p className="text-sm text-destaque-suave tabular-nums">
          O veredito aparece a partir de {formatarData(aPartirDe)}.
        </p>
      )}
      {linhas.length > 0 && (
        <dl className="flex flex-col gap-0.5 text-sm tabular-nums">
          {linhas.map(({ rotulo, valor, extra }) => (
            <div key={rotulo} className="grid grid-cols-[4.75rem_minmax(0,1fr)] gap-2">
              <dt className="text-destaque-suave">{rotulo}</dt>
              <dd>
                {valor} {extra !== undefined && <span className="text-destaque-suave">{extra}</span>}
              </dd>
            </div>
          ))}
        </dl>
      )}
    </div>
  )
}

const TEXTOS = {
  ritmo: { ABAIXO: 'abaixo do ritmo', NO_RITMO: 'no ritmo', ACIMA: 'acima do ritmo' },
  faixa: { ABAIXO: 'abaixo da faixa', NO_RITMO: 'na faixa', ACIMA: 'acima da faixa' },
}

/**
 * O selo diz o estado em palavra, e a cor só reforça; fora do ritmo leva a
 * seta (convencoes-frontend.md). Na manutenção, as palavras são da faixa (RN-37).
 */
function Selo({
  estado,
  manutencao,
}: {
  estado: Esquema<'VereditoDto'>['estado']
  manutencao: boolean
}) {
  const base =
    'inline-flex items-center gap-1 self-start rounded-full border-[1.5px] border-transparent px-3 py-0.5 text-sm font-semibold whitespace-nowrap'
  if (estado === 'DADOS_INSUFICIENTES') {
    return (
      <span className={cn(base, 'mt-1 border-dashed border-insuficiente-borda text-destaque-suave')}>
        dados insuficientes
      </span>
    )
  }
  const texto = TEXTOS[manutencao ? 'faixa' : 'ritmo'][estado]
  if (estado === 'NO_RITMO') {
    return <span className={cn(base, 'bg-ritmo-fundo text-ritmo')}>{texto}</span>
  }
  const Seta = estado === 'ABAIXO' ? ArrowDown : ArrowUp
  return (
    <span className={cn(base, 'bg-fora-fundo text-fora')}>
      <Seta aria-hidden="true" className="size-4" />
      {texto}
    </span>
  )
}

function AlvoAtingido({ alvo, faseId }: { alvo: number; faseId: number }) {
  return (
    <div
      role="status"
      className="flex items-start gap-3 rounded-xl border bg-card px-4 py-3.5 text-card-foreground lg:col-start-1"
    >
      <Target aria-hidden="true" className="mt-0.5 size-5 flex-none" />
      <div className="flex flex-col gap-0.5">
        <strong className="tabular-nums">O peso semanal chegou ao peso alvo de {formatarPeso(alvo)} kg.</strong>
        <span className="text-muted-foreground">A fase continua aberta até você encerrá-la.</span>
        <Link to={`/fases/${faseId}`} className={cn(LINK, 'mt-1 self-start')}>
          Encerrar a fase
        </Link>
      </div>
    </div>
  )
}

interface PropsDoProgresso {
  progresso: Esquema<'ProgressoDto'>
  alvo: number
  faseId: number
}

function Progresso({ progresso, alvo, faseId }: PropsDoProgresso) {
  const { estado, percentual } = progresso

  return (
    <section aria-labelledby="titulo-progresso" className={cn(CARTAO, 'gap-2 lg:col-start-1')}>
      <div className="flex items-baseline justify-between gap-2">
        <h2 id="titulo-progresso" className="text-sm font-semibold tabular-nums">
          Progresso até {formatarPeso(alvo)} kg
        </h2>
        {estado === 'BARRA' && percentual !== null && (
          <span className="text-sm font-bold tabular-nums">{formatarInteiro(percentual)}%</span>
        )}
        {estado === 'DADOS_INSUFICIENTES' && (
          <span className="text-sm text-muted-foreground">dados insuficientes</span>
        )}
      </div>
      {estado === 'BARRA' && percentual !== null && (
        <div
          role="progressbar"
          aria-labelledby="titulo-progresso"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={percentual}
          className="h-2.5 overflow-hidden rounded-full bg-trilho"
        >
          <div className="h-full rounded-full bg-grafico-linha" style={{ width: `${percentual}%` }} />
        </div>
      )}
      {estado === 'DADOS_INSUFICIENTES' && (
        <div className="h-2.5 rounded-full border-[1.5px] border-dashed border-insuficiente-borda" />
      )}
      {estado === 'NAO_EXIBIDO' && (
        <p className="text-sm text-muted-foreground">
          A barra fica escondida enquanto o peso alvo estiver do lado errado do peso de partida da
          fase.{' '}
            <Link to={`/fases/${faseId}`} className={LINK}>
            Editar a fase
          </Link>
        </p>
      )}
    </section>
  )
}

function Variacoes({ variacoes }: { variacoes: Esquema<'Variacoes'> }) {
  const linhas = [
    { rotulo: '7 dias', variacao: variacoes.semana },
    { rotulo: '30 dias', variacao: variacoes.mes },
    { rotulo: 'desde o início', variacao: variacoes.desdeOInicio },
  ]

  return (
    <section aria-labelledby="titulo-variacoes" className={cn(CARTAO, 'gap-2.5 lg:col-start-1')}>
      <h2 id="titulo-variacoes" className="text-lg leading-snug font-bold">
        Variação do peso semanal
      </h2>
      <dl>
        {linhas.map(({ rotulo, variacao }) => (
          <Linha
            key={rotulo}
            rotulo={rotulo}
            valor={variacao === null ? 'dados insuficientes' : `${formatarDiferenca(variacao.valor)} kg`}
          >
            {variacao !== null && (
              <dd className="col-span-full text-xs text-muted-foreground tabular-nums">
                {formatarIntervalo(variacao.de, variacao.ate)}
              </dd>
            )}
          </Linha>
        ))}
      </dl>
    </section>
  )
}

function Cintura({ cintura, fase }: { cintura: Esquema<'ResumoCintura'>; fase: Fase | null }) {
  const { desdeAAnterior, desdeOInicioDaFase } = cintura

  return (
    <section aria-labelledby="titulo-cintura" className={cn(CARTAO, 'gap-2 lg:col-start-1')}>
      <div className="flex items-baseline justify-between gap-2">
        <h2 id="titulo-cintura" className="text-lg leading-snug font-bold">
          Cintura
        </h2>
        <span className="text-sm text-muted-foreground tabular-nums">em {formatarData(cintura.data)}</span>
      </div>
      <p className="text-2xl leading-tight font-bold tabular-nums">{formatarCintura(cintura.valor)} cm</p>
      {(desdeAAnterior !== null || desdeOInicioDaFase !== null) && (
        <dl className="text-sm">
          {desdeAAnterior !== null && (
            <Linha rotulo="desde a medição anterior" valor={`${formatarDiferenca(desdeAAnterior)} cm`} />
          )}
          {desdeOInicioDaFase !== null && fase !== null && (
            <Linha
              rotulo={`desde a primeira medição ${formatarDoTipo(fase.tipo)}`}
              valor={`${formatarDiferenca(desdeOInicioDaFase)} cm`}
            />
          )}
        </dl>
      )}
    </section>
  )
}

function Linha({
  rotulo,
  valor,
  children,
}: {
  rotulo: string
  valor: string
  children?: ReactNode
}) {
  return (
    <div className="grid grid-cols-[minmax(0,1fr)_auto] items-baseline gap-x-3 border-t py-2.5 first:border-t-0 first:pt-0">
      <dt>{rotulo}</dt>
      <dd className="text-right font-bold tabular-nums">{valor}</dd>
      {children}
    </div>
  )
}

/** A conta sem registro: um convite, em vez de estrutura vazia (UC-08 1b). */
function PrimeiroRegistro() {
  return (
    <section
      aria-labelledby="titulo-primeiro"
      className={cn(CARTAO, 'items-start gap-4 px-5 py-6 lg:max-w-lg lg:p-8')}
    >
      <span aria-hidden="true" className="text-primary">
        <Simbolo className="size-10" />
      </span>
      <h2 id="titulo-primeiro" className="text-lg leading-snug font-bold">
        Registre o primeiro peso
      </h2>
      <p className="text-muted-foreground">
        O peso semanal, o gráfico e as variações aparecem conforme os registros chegam.
      </p>
      <Button asChild className="w-full lg:w-auto">
        <Link to="/registrar">Registrar peso</Link>
      </Button>
      <p className="text-sm text-muted-foreground">
        Já tem um histórico em planilha?{' '}
        <Link to="/importar" className={LINK}>
          Importar CSV
        </Link>
      </p>
    </section>
  )
}
