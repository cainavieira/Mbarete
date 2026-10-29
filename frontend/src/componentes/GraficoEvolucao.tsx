import { useState } from 'react'
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Line,
  ReferenceArea,
  ReferenceLine,
  XAxis,
  YAxis,
  type LabelProps,
  type XAxisTickContentProps,
} from 'recharts'
import type { Esquema } from '@/api/tipos'
import { ToggleGroup, ToggleGroupItem } from '@/componentes/ui/toggle-group'
import { formatarData, formatarDiaEMes } from '@/formato/data'
import { formatarTipo } from '@/formato/fase'
import { formatarInteiro, formatarPeso } from '@/formato/numero'
import { cn } from '@/lib/utils'
import {
  escalaDoPeso,
  PERIODOS,
  recortar,
  trechosDeFase,
  type Periodo,
  type Ponto,
} from './serieDoGrafico'

interface Props {
  /** Nunca vazia: sem registro, a tela convida ao primeiro (UC-08 1b). */
  serie: Ponto[]
  faseCorrente: Esquema<'FaseDto'> | null
  faixa: Esquema<'FaixaDeTolerancia'> | null
  className?: string
}

/**
 * O gráfico do UC-08: o peso do dia em pontos, o peso semanal em linha, que
 * se interrompe onde ele é nulo (RN-09, ADR-0029), os trechos com fase
 * pintados (RF-47) e a faixa da manutenção do início da fase até hoje
 * (RF-50). O período recorta a série na tela, sem nova chamada (RF-49).
 * Gráfico é imagem: a descrição diz o que ele mostra, e as variações e o
 * histórico dão os números a quem não o lê.
 */
export function GraficoEvolucao({ serie, faseCorrente, faixa, className }: Props) {
  const [periodo, setPeriodo] = useState<Periodo>(120)
  const pontos = recortar(serie, periodo)
  const ultimo = pontos.length - 1
  const trechos = trechosDeFase(pontos)
  const pesos = pontos
    .flatMap(({ peso, pesoSemanal }) => [peso, pesoSemanal])
    .filter((valor) => valor !== null)

  const faixaNoGrafico =
    faixa !== null && faseCorrente !== null
      ? { ...faixa, de: pontos.findIndex(({ data }) => data >= faseCorrente.dataInicio) }
      : null

  const dominioX: [number, number] = ultimo > 0 ? [0, ultimo] : [-1, 1]
  // Cada dia ocupa meia distância para cada lado, e o bloco não passa das bordas
  const dentro = (posicao: number) => Math.min(Math.max(posicao, dominioX[0]), dominioX[1])
  const escala = escalaDoPeso(
    faixaNoGrafico === null ? pesos : [...pesos, faixaNoGrafico.minimo, faixaNoGrafico.maximo],
  )

  const titulo =
    pontos.length < periodo
      ? `Peso desde ${formatarData(pontos[0].data)}`
      : `Peso nos últimos ${periodo} dias`
  const descricao = [
    `Gráfico de ${formatarData(pontos[0].data)} a ${formatarData(pontos[ultimo].data)}, com o peso de cada dia em pontos e o peso semanal em linha`,
    ...(trechos.length === 0
      ? ['sem fase no período']
      : trechos.map(
          ({ tipo, de, ate }) =>
            `${formatarTipo(tipo)} de ${formatarDiaEMes(pontos[de].data)} a ${formatarDiaEMes(pontos[ate].data)}`,
        )),
    ...(faixaNoGrafico === null
      ? []
      : [
          `faixa de tolerância de ${formatarPeso(faixaNoGrafico.minimo)} a ${formatarPeso(faixaNoGrafico.maximo)} kg`,
        ]),
  ].join('; ')

  return (
    <section
      aria-labelledby="titulo-grafico"
      className={cn(
        'flex min-w-0 flex-col gap-3 rounded-xl border bg-card p-4 text-card-foreground lg:px-6 lg:py-5',
        className,
      )}
    >
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-3">
        <h2 id="titulo-grafico" className="text-lg leading-snug font-bold">
          {titulo}
        </h2>
        <ToggleGroup
          type="single"
          value={String(periodo)}
          onValueChange={(valor) => {
            // Tocar de novo no período marcado desmarca no Radix, e o gráfico não fica sem período
            const escolhido = PERIODOS.find((dias) => String(dias) === valor)
            if (escolhido !== undefined) {
              setPeriodo(escolhido)
            }
          }}
          aria-label="Período do gráfico"
          spacing={0}
          className="grid w-full grid-cols-4 rounded-full border bg-card p-0.5 lg:w-auto"
        >
          {PERIODOS.map((dias) => (
            <ToggleGroupItem
              key={dias}
              value={String(dias)}
              aria-label={`${dias} dias`}
              className="min-h-11 rounded-full! px-3 text-sm text-muted-foreground tabular-nums data-[state=on]:bg-secondary data-[state=on]:font-bold data-[state=on]:text-secondary-foreground"
            >
              {dias === 120 ? '120 dias' : dias}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </div>

      {pesos.length === 0 ? (
        <p className="text-muted-foreground">Nenhum registro nos últimos {periodo} dias.</p>
      ) : (
        <div role="img" aria-label={`${descricao}.`}>
          <ComposedChart
            responsive
            accessibilityLayer={false}
            data={pontos.map(({ peso, pesoSemanal }, indice) => ({ indice, peso, pesoSemanal }))}
            margin={{ top: 4, right: 8, bottom: 0, left: 0 }}
            className="aspect-[326/210] w-full lg:aspect-[680/320]"
          >
            {trechos.map(({ tipo, de, ate }) => (
              <ReferenceArea
                key={`fase-${de}`}
                x1={dentro(de - 0.5)}
                x2={dentro(ate + 0.5)}
                fill="var(--muted)"
                fillOpacity={1}
                zIndex={-300}
                label={{
                  content: (props: LabelProps) => rotuloDaFase(props, formatarTipo(tipo)),
                  zIndex: -250,
                }}
              />
            ))}
            {trechos
              .filter(({ de }) => de > 0)
              .map(({ de }) => (
                <ReferenceLine key={`inicio-${de}`} x={de - 0.5} stroke="var(--input)" zIndex={-250} />
              ))}
            {faixaNoGrafico !== null && (
              <ReferenceArea
                x1={dentro(faixaNoGrafico.de - 0.5)}
                x2={dominioX[1]}
                y1={faixaNoGrafico.minimo}
                y2={faixaNoGrafico.maximo}
                fill="var(--ritmo-fundo)"
                fillOpacity={1}
                zIndex={-200}
              />
            )}
            <CartesianGrid vertical={false} stroke="var(--border)" />
            {faixaNoGrafico !== null &&
              [faixaNoGrafico.minimo, faixaNoGrafico.maximo].map((limite) => (
                <ReferenceLine
                  key={`limite-${limite}`}
                  segment={[
                    { x: dentro(faixaNoGrafico.de - 0.5), y: limite },
                    { x: dominioX[1], y: limite },
                  ]}
                  stroke="var(--faixa-borda)"
                  strokeWidth={1.5}
                  strokeDasharray="5 4"
                  zIndex={-90}
                />
              ))}
            <XAxis
              type="number"
              dataKey="indice"
              domain={dominioX}
              ticks={ultimo > 0 ? [0, ultimo] : [0]}
              interval={0}
              axisLine={false}
              tickLine={false}
              height={22}
              tick={(props: XAxisTickContentProps) => (
                <text
                  x={props.x}
                  y={props.y}
                  dy="0.9em"
                  textAnchor={ultimo === 0 ? 'middle' : props.payload.value === 0 ? 'start' : 'end'}
                  className="fill-muted-foreground text-xs tabular-nums"
                >
                  {formatarDiaEMes(pontos[Number(props.payload.value)].data)}
                </text>
              )}
            />
            <YAxis
              type="number"
              domain={escala.dominio}
              ticks={escala.marcas}
              interval={0}
              tickFormatter={formatarInteiro}
              axisLine={false}
              tickLine={false}
              width={30}
              tick={{ className: 'fill-muted-foreground text-xs tabular-nums' }}
            />
            <Area
              dataKey="pesoSemanal"
              type="linear"
              stroke="none"
              fill="var(--grafico-area)"
              fillOpacity={0.85}
              dot={false}
              activeDot={false}
              isAnimationActive={false}
            />
            <Line
              dataKey="peso"
              type="linear"
              stroke="none"
              dot={{ r: 2, fill: 'var(--grafico-ponto)', stroke: 'none' }}
              activeDot={false}
              isAnimationActive={false}
            />
            <Line
              dataKey="pesoSemanal"
              type="linear"
              stroke="var(--grafico-linha)"
              strokeWidth={2.5}
              strokeLinecap="round"
              strokeLinejoin="round"
              dot={false}
              activeDot={false}
              isAnimationActive={false}
              zIndex={700}
            />
          </ComposedChart>
        </div>
      )}

      <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
        <li className="inline-flex items-center gap-1.5">
          <svg aria-hidden="true" viewBox="0 0 10 10" className="size-2.5">
            <circle cx="5" cy="5" r="3" fill="var(--grafico-ponto)" />
          </svg>
          peso do dia
        </li>
        <li className="inline-flex items-center gap-1.5">
          <svg aria-hidden="true" viewBox="0 0 18 10" className="h-2.5 w-4.5">
            <line x1="2" y1="5" x2="16" y2="5" stroke="var(--grafico-linha)" strokeWidth="2.5" strokeLinecap="round" />
          </svg>
          peso semanal
        </li>
        {trechos.length > 0 && (
          <li className="inline-flex items-center gap-1.5">
            <svg aria-hidden="true" viewBox="0 0 14 10" className="h-2.5 w-3.5">
              <rect x="0.5" y="0.5" width="13" height="9" rx="2" fill="var(--muted)" stroke="var(--input)" />
            </svg>
            com fase
          </li>
        )}
        {faixaNoGrafico !== null && (
          <li className="inline-flex items-center gap-1.5">
            <svg aria-hidden="true" viewBox="0 0 14 10" className="h-2.5 w-3.5">
              <rect x="0" y="1" width="14" height="8" fill="var(--ritmo-fundo)" />
              {[1, 9].map((y) => (
                <line
                  key={y}
                  x1="0"
                  y1={y}
                  x2="14"
                  y2={y}
                  stroke="var(--faixa-borda)"
                  strokeWidth="1.5"
                  strokeDasharray="3 2"
                />
              ))}
            </svg>
            faixa de tolerância
          </li>
        )}
      </ul>
    </section>
  )
}

/** O nome da fase no alto do bloco, só quando cabe nele. */
function rotuloDaFase({ viewBox }: LabelProps, nome: string) {
  if (viewBox === undefined || !('width' in viewBox) || viewBox.width < nome.length * 6.2 + 6) {
    return null
  }
  return (
    <text x={viewBox.x + 5} y={viewBox.y + 12} className="fill-muted-foreground text-xs font-semibold">
      {nome}
    </text>
  )
}
