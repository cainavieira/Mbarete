import { AlertCircle, ChevronLeft, Download, FileText, Info } from 'lucide-react'
import { useRef, useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { ErroDaApi } from '@/api/cliente'
import { Button } from '@/componentes/ui/button'
import { ToggleGroup, ToggleGroupItem } from '@/componentes/ui/toggle-group'
import { formatarData } from '@/formato/data'
import { formatarCintura, formatarPeso } from '@/formato/numero'
import { useExportar } from '@/hooks/useConta'
import {
  useImportar,
  usePrevisualizar,
  type LinhaValida,
  type Previsualizacao,
} from '@/hooks/useImportacao'
import { SEM_RESPOSTA } from '@/lib/forma'
import { cn } from '@/lib/utils'

/** Quantas datas novas a lista mostra antes de "Ver as N". */
const NOVAS_A_VISTA = 4

type Etapa =
  | { tipo: 'escolhendo'; erro?: string }
  | { tipo: 'previa'; arquivo: File; previa: Previsualizacao; mudou: boolean; falhou?: string }
  | { tipo: 'importado'; gravadas: LinhaValida[] }

/**
 * Importar o histórico de uma planilha (UC-10) e exportá-lo (UC-11). Nada é
 * gravado antes da pré-visualização e do sim; a confirmação reenvia o mesmo
 * arquivo com as datas que a pessoa viu substituídas, e se o histórico mudou
 * entre as duas chamadas, a tela refaz a pré-visualização (UC-10 3b).
 */
export function Importar() {
  const [etapa, setEtapa] = useState<Etapa>({ tipo: 'escolhendo' })
  const previsualizar = usePrevisualizar()
  const importar = useImportar()
  const seletor = useRef<HTMLInputElement>(null)

  async function ler(arquivo: File, mudou = false) {
    try {
      const previa = await previsualizar.mutateAsync(arquivo)
      setEtapa({ tipo: 'previa', arquivo, previa, mudou })
    } catch (erro) {
      setEtapa({ tipo: 'escolhendo', erro: erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA })
    }
  }

  async function gravar(arquivo: File, previa: Previsualizacao) {
    const substituir = previa.validas.filter((linha) => linha.substitui !== null).map(({ data }) => data)
    try {
      const gravado = await importar.mutateAsync({ arquivo, substituir })
      setEtapa({ tipo: 'importado', gravadas: gravado.validas })
    } catch (erro) {
      if (erro instanceof ErroDaApi && erro.codigo === 'PREVISUALIZACAO_DESATUALIZADA') {
        await ler(arquivo, true)
        return
      }
      setEtapa({
        tipo: 'previa',
        arquivo,
        previa,
        mudou: false,
        falhou: erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA,
      })
    }
  }

  function trocar() {
    setEtapa({ tipo: 'escolhendo' })
    seletor.current?.click()
  }

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-col gap-3 px-4 pt-2 pb-6 md:px-8 md:pt-6 lg:max-w-6xl">
      <div className="flex flex-col items-start gap-1">
        <Button asChild variant="ghost" className="-ml-2 px-2 text-primary">
          <Link to="/historico">
            <ChevronLeft aria-hidden="true" />
            Histórico
          </Link>
        </Button>
        <h1 className="text-2xl leading-tight font-bold">Importar e exportar</h1>
      </div>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <section aria-labelledby="titulo-importar" className="flex min-w-0 flex-col gap-3 lg:flex-[999_1_36rem]">
          <h2 id="titulo-importar" className="text-lg leading-snug font-bold">
            Importar CSV
          </h2>

          {/* O seletor fica sempre na página, para o "Trocar" abri-lo sem passar pela primeira tela */}
          <input
            ref={seletor}
            id="arquivo"
            type="file"
            accept=".csv,text/csv"
            aria-describedby={
              etapa.tipo === 'escolhendo' && etapa.erro ? 'erro-arquivo ajuda-arquivo' : 'ajuda-arquivo'
            }
            className="sr-only"
            onChange={(evento) => {
              const arquivo = evento.target.files?.[0]
              evento.target.value = ''
              if (arquivo !== undefined) {
                void ler(arquivo)
              }
            }}
          />

          {etapa.tipo === 'escolhendo' && (
            <>
              <p className="text-sm text-muted-foreground">
                Traga o histórico de uma planilha. Nada é gravado antes de você ver o que entra e
                confirmar.
              </p>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="arquivo" className="text-sm font-semibold">
                  Arquivo
                </label>
                <Arquivo
                  nome={previsualizar.isPending ? 'Lendo o arquivo…' : 'Nenhum arquivo escolhido'}
                >
                  <Button asChild variant="outline" className="px-3 text-sm">
                    <label htmlFor="arquivo" className="cursor-pointer">
                      Escolher
                    </label>
                  </Button>
                </Arquivo>
                {etapa.erro && (
                  <span id="erro-arquivo" className="text-sm text-destructive">
                    {etapa.erro}
                  </span>
                )}
              </div>
              <Formato id="ajuda-arquivo" className="lg:hidden" />
            </>
          )}

          {etapa.tipo === 'previa' && (
            <Previa
              etapa={etapa}
              gravando={importar.isPending || previsualizar.isPending}
              trocar={trocar}
              cancelar={() => setEtapa({ tipo: 'escolhendo' })}
              gravar={() => void gravar(etapa.arquivo, etapa.previa)}
            />
          )}

          {etapa.tipo === 'importado' && <Importado gravadas={etapa.gravadas} />}
        </section>

        <aside className="flex flex-col gap-4 border-t pt-5 lg:flex-[1_1_18rem] lg:border-t-0 lg:pt-0">
          <section
            aria-labelledby="titulo-formato"
            className="hidden flex-col gap-3 rounded-xl border bg-card p-4 text-card-foreground lg:flex"
          >
            <h2 id="titulo-formato" className="text-lg leading-snug font-bold">
              O formato
            </h2>
            <Formato id={etapa.tipo === 'escolhendo' ? undefined : 'ajuda-arquivo'} />
          </section>
          <Exportar />
        </aside>
      </div>
    </main>
  )
}

interface PropsDoArquivo {
  nome: string
  children: ReactNode
  /** O nome do arquivo escolhido sai forte; o "nenhum arquivo", suave. */
  forte?: boolean
}

function Arquivo({ nome, children, forte = false }: PropsDoArquivo) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-dashed border-input bg-card px-4 py-3">
      <FileText aria-hidden="true" className="size-5 flex-none text-muted-foreground" />
      <span className={cn('flex-1 text-sm', forte ? 'font-semibold' : 'text-muted-foreground')}>{nome}</span>
      {children}
    </div>
  )
}

/** O formato que a importação lê, que é o mesmo que a exportação gera (RN-53, RN-54, RN-55). */
function Formato({ id, className }: { id?: string; className?: string }) {
  return (
    <div id={id} className={cn('flex flex-col gap-2', className)}>
      <p className="text-sm text-muted-foreground">
        Colunas data e peso, e cintura se quiser, separadas por ponto e vírgula ou por vírgula. Data
        em AAAA-MM-DD ou DD/MM/AAAA; número com ponto decimal. É o formato que a exportação gera:
      </p>
      <pre className="overflow-x-auto rounded-lg bg-muted px-3 py-2.5 font-mono text-sm leading-normal">
        {'data;peso;cintura\n2026-09-07;82.4;91.0\n2026-09-08;82.1;'}
      </pre>
    </div>
  )
}

interface PropsDaPrevia {
  etapa: Extract<Etapa, { tipo: 'previa' }>
  gravando: boolean
  trocar: () => void
  cancelar: () => void
  gravar: () => void
}

function Previa({ etapa, gravando, trocar, cancelar, gravar }: PropsDaPrevia) {
  const { arquivo, previa, mudou, falhou } = etapa
  const substituem = previa.validas.filter((linha) => linha.substitui !== null)
  const novas = previa.validas.filter((linha) => linha.substitui === null)
  const validas = previa.validas.length
  const acoes = (
    <>
      <Button type="button" disabled={validas === 0 || gravando} onClick={gravar}>
        {validas === 0 ? 'Gravar' : `Gravar ${contarRegistros(validas)}`}
      </Button>
      <Button type="button" variant="outline" onClick={cancelar}>
        Cancelar
      </Button>
    </>
  )

  return (
    <>
      <Arquivo nome={arquivo.name} forte>
        <Button type="button" variant="outline" className="px-3 text-sm" onClick={trocar}>
          Trocar
        </Button>
      </Arquivo>

      {mudou && (
        <Aviso titulo="O histórico mudou desde a pré-visualização: nada foi gravado.">
          Esta é a lista de agora. Confira as datas que já têm registro e grave de novo.
        </Aviso>
      )}

      <dl aria-label="Resumo do arquivo" className="grid grid-cols-3 gap-2">
        <Numero valor={validas} rotulo="linhas válidas" />
        <Numero valor={previa.recusadas.length} rotulo="recusadas" />
        <Numero valor={substituem.length} rotulo="datas que já têm registro" />
      </dl>

      <div className="flex flex-col gap-3 lg:hidden">
        {previa.recusadas.length > 0 && (
          <Lista titulo="Recusadas, ficam de fora">
            {previa.recusadas.map(({ linha, motivo }) => (
              <LinhaDoArquivo key={linha}>
                <strong>Linha {linha}</strong>
                <span className="text-muted-foreground">{motivo}</span>
              </LinhaDoArquivo>
            ))}
          </Lista>
        )}
        {substituem.length > 0 && (
          <Lista titulo="Substituem o registro que já existe">
            {substituem.map((linha) => (
              <LinhaDoArquivo key={linha.linha}>
                <span>
                  <strong>{formatarData(linha.data)}</strong> · {novo(linha)}
                </span>
                <span className="text-muted-foreground">no lugar de {gravado(linha)}</span>
              </LinhaDoArquivo>
            ))}
          </Lista>
        )}
        {novas.length > 0 && <Novas novas={novas} />}
      </div>

      {falhou && (
        <Aviso titulo={falhou} erro>
          Nada foi gravado: o histórico está como antes do envio.
        </Aviso>
      )}

      <p className="text-sm text-muted-foreground">
        {validas === 0
          ? 'Nenhuma linha pode ser gravada. Corrija o arquivo e envie de novo.'
          : 'A importação não cria fase: as datas importadas ficam sem fase.'}
      </p>
      {/* No computador, os botões ficam no alto da tabela, que pode ser longa */}
      <div className="flex flex-col gap-2 lg:hidden">{acoes}</div>
      <TabelaDoArquivo previa={previa} acoes={acoes} />
    </>
  )
}

function Novas({ novas }: { novas: LinhaValida[] }) {
  const [todas, setTodas] = useState(false)
  const visiveis = todas ? novas : novas.slice(0, NOVAS_A_VISTA)

  return (
    <Lista titulo={`Novas: ${novas.length === 1 ? '1 data' : `${novas.length} datas`}`}>
      {visiveis.map((linha) => (
        <LinhaDoArquivo key={linha.linha}>
          <span>
            <strong>{formatarData(linha.data)}</strong> · {novo(linha)}
          </span>
        </LinhaDoArquivo>
      ))}
      {novas.length > NOVAS_A_VISTA && (
        <Button
          type="button"
          variant="link"
          aria-expanded={todas}
          className="self-start px-0"
          onClick={() => setTodas(!todas)}
        >
          {todas ? 'Ver menos' : `Ver as ${novas.length}`}
        </Button>
      )}
    </Lista>
  )
}

type Filtro = 'todas' | 'recusadas' | 'substituem'

/** No computador, as linhas do arquivo numa tabela só, com o filtro do canvas largo. */
function TabelaDoArquivo({ previa, acoes }: { previa: Previsualizacao; acoes: ReactNode }) {
  const [filtro, setFiltro] = useState<Filtro>('todas')
  const linhas = [
    ...previa.validas.map((linha) => ({
      linha: linha.linha,
      data: formatarData(linha.data),
      peso: `${formatarPeso(linha.peso)} kg`,
      cintura: linha.cintura === null ? '' : `${formatarCintura(linha.cintura)} cm`,
      tipo: linha.substitui === null ? ('nova' as const) : ('substitui' as const),
      situacao: linha.substitui === null ? 'nova' : `substitui ${gravado(linha)}`,
    })),
    ...previa.recusadas.map(({ linha, motivo }) => ({
      linha,
      data: '',
      peso: '',
      cintura: '',
      tipo: 'recusada' as const,
      situacao: `recusada: ${motivo}`,
    })),
  ].sort((a, b) => a.linha - b.linha)
  const mostradas = linhas.filter(
    ({ tipo }) =>
      filtro === 'todas' ||
      (filtro === 'recusadas' && tipo === 'recusada') ||
      (filtro === 'substituem' && tipo === 'substitui'),
  )
  const contar = (tipo: string) => linhas.filter((linha) => linha.tipo === tipo).length

  return (
    <div className="hidden flex-col gap-3 lg:flex">
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-3">
        <ToggleGroup
          type="single"
          value={filtro}
          onValueChange={(valor) => {
            if (valor === 'todas' || valor === 'recusadas' || valor === 'substituem') {
              setFiltro(valor)
            }
          }}
          aria-label="Mostrar"
          spacing={0}
          className="rounded-full border bg-card p-0.5"
        >
          {(
            [
              ['todas', `Todas (${linhas.length})`],
              ['recusadas', `Recusadas (${contar('recusada')})`],
              ['substituem', `Substituem (${contar('substitui')})`],
            ] as const
          ).map(([valor, rotulo]) => (
            <ToggleGroupItem
              key={valor}
              value={valor}
              className="min-h-10 rounded-full! px-3.5 text-sm text-muted-foreground tabular-nums data-[state=on]:bg-secondary data-[state=on]:font-bold data-[state=on]:text-secondary-foreground"
            >
              {rotulo}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
        <div className="flex flex-row-reverse gap-2">{acoes}</div>
      </div>
      <div className="overflow-hidden rounded-xl border bg-card">
        <table className="w-full text-sm tabular-nums">
          <caption className="sr-only">Linhas do arquivo</caption>
          <thead className="text-left font-semibold text-muted-foreground">
            <tr className="border-b">
              <th className="px-3 py-2 font-semibold">Linha</th>
              <th className="px-3 py-2 font-semibold">Data</th>
              <th className="px-3 py-2 text-right font-semibold">Peso</th>
              <th className="px-3 py-2 text-right font-semibold">Cintura</th>
              <th className="px-3 py-2 font-semibold">Situação</th>
            </tr>
          </thead>
          <tbody>
            {mostradas.map((linha) => (
              <tr key={linha.linha} className="border-t first:border-t-0">
                <td className="px-3 py-2 text-muted-foreground">{linha.linha}</td>
                <td className="px-3 py-2 whitespace-nowrap">{linha.data}</td>
                <td className="px-3 py-2 text-right font-bold whitespace-nowrap">{linha.peso}</td>
                <td className="px-3 py-2 text-right whitespace-nowrap">{linha.cintura}</td>
                <td
                  className={cn(
                    'px-3 py-2',
                    linha.tipo === 'recusada' && 'text-destructive',
                    linha.tipo === 'nova' && 'text-muted-foreground',
                  )}
                >
                  {linha.situacao}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

/** Depois de gravar, o período importado e o caminho para classificá-lo (UC-10 4b). */
function Importado({ gravadas }: { gravadas: LinhaValida[] }) {
  const datas = gravadas.map(({ data }) => data).sort()
  const de = datas[0]
  const ate = datas[datas.length - 1]
  const quantos = `${contarRegistros(gravadas.length)} ${gravadas.length === 1 ? 'gravado' : 'gravados'}`

  return (
    <>
      <Aviso
        titulo={`${quantos}, de ${formatarData(de)} a ${formatarData(ate)}.`}
      >
        As datas importadas ficam sem fase. Para classificar o período, crie uma fase com início e
        fim sobre ele.
      </Aviso>
      <div className="flex flex-col gap-2 lg:flex-row">
        <Button asChild variant="outline">
          <Link to={`/fases/nova?inicio=${de}&fim=${ate}`}>Criar fase para o período</Link>
        </Button>
        <Button asChild variant="ghost" className="text-primary">
          <Link to="/historico">Ver o histórico</Link>
        </Button>
      </div>
    </>
  )
}

function Exportar() {
  const exportar = useExportar()

  return (
    <section aria-labelledby="titulo-exportar" className="flex flex-col gap-3 lg:rounded-xl lg:border lg:bg-card lg:p-4">
      <h2 id="titulo-exportar" className="text-lg leading-snug font-bold">
        Exportar CSV
      </h2>
      <p className="text-sm text-muted-foreground">
        Baixa todos os registros, com data, peso e cintura, no mesmo formato que a importação aceita.
      </p>
      <Button
        type="button"
        variant="outline"
        disabled={exportar.isPending}
        onClick={() =>
          exportar.mutate(undefined, {
            onError: (erro) => toast.error(erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA),
          })
        }
      >
        <Download aria-hidden="true" />
        Baixar historico.csv
      </Button>
    </section>
  )
}

interface PropsDoAviso {
  titulo: string
  /** A falha da gravação; o resto informa, sem cor de estado. */
  erro?: boolean
  children: ReactNode
}

function Aviso({ titulo, erro = false, children }: PropsDoAviso) {
  const Icone = erro ? AlertCircle : Info
  return (
    <div
      role={erro ? 'alert' : 'status'}
      className={cn(
        'flex items-start gap-3 rounded-xl border bg-card px-4 py-3.5 text-card-foreground',
        erro && 'border-destructive',
      )}
    >
      <Icone aria-hidden="true" className={cn('mt-0.5 size-5 flex-none', erro && 'text-destructive')} />
      <div className="flex flex-col gap-0.5">
        <strong className="tabular-nums">{titulo}</strong>
        <span className="text-muted-foreground">{children}</span>
      </div>
    </div>
  )
}

function Numero({ valor, rotulo }: { valor: number; rotulo: string }) {
  return (
    <div className="flex flex-col-reverse gap-0.5 rounded-xl border bg-card p-3">
      <dt className="text-xs text-muted-foreground">{rotulo}</dt>
      <dd className="text-2xl leading-tight font-bold tabular-nums">{valor}</dd>
    </div>
  )
}

function Lista({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-2 rounded-xl border bg-card p-4 text-card-foreground">
      <h3 className="text-sm font-bold tabular-nums">{titulo}</h3>
      <div className="flex flex-col">{children}</div>
    </section>
  )
}

function LinhaDoArquivo({ children }: { children: ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5 border-t py-2.5 text-sm tabular-nums first:border-t-0 first:pt-0">
      {children}
    </div>
  )
}

function novo({ peso, cintura }: LinhaValida): string {
  return `${formatarPeso(peso)} kg${cintura === null ? '' : ` e ${formatarCintura(cintura)} cm`}`
}

function gravado({ substitui }: LinhaValida): string {
  if (substitui === null) {
    return ''
  }
  return `${formatarPeso(substitui.peso)} kg${
    substitui.cintura === null ? '' : ` e ${formatarCintura(substitui.cintura)} cm de cintura`
  }`
}

function contarRegistros(registros: number): string {
  return registros === 1 ? '1 registro' : `${registros} registros`
}
