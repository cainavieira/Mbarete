import { zodResolver } from '@hookform/resolvers/zod'
import { Info } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { ErroDaApi } from '@/api/cliente'
import { CampoData } from '@/componentes/CampoData'
import { Alert, AlertDescription } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import { RadioGroup, RadioGroupItem } from '@/componentes/ui/radio-group'
import { Switch } from '@/componentes/ui/switch'
import { ToggleGroup, ToggleGroupItem } from '@/componentes/ui/toggle-group'
import {
  formatarNivel,
  formatarTaxaAlvo,
  formatarTipo,
  type NivelDaFase,
  type TipoDeFase,
  type UnidadeDaTaxa,
} from '@/formato/fase'
import { formatarPeso, formatarTaxa, lerNumero } from '@/formato/numero'
import type { FaseEnviada, Nivel } from '@/hooks/useFases'
import { lerForma, SEM_RESPOSTA } from '@/lib/forma'
import { cn } from '@/lib/utils'

const TIPOS: TipoDeFase[] = ['CUTTING', 'BULKING', 'MANUTENCAO']
const NIVEIS: NivelDaFase[] = ['CONSERVADOR', 'MODERADO', 'AGRESSIVO', 'PERSONALIZADO']
const UNIDADES: UnidadeDaTaxa[] = ['PERCENTUAL_SEMANA', 'KG_SEMANA']

/** O número do campo, ou nada quando vazio; a forma errada vira recusa no campo (ADR-0025). */
function ler(
  texto: string,
  campo: 'taxaAlvoValor' | 'pesoAlvo',
  ctx: z.RefinementCtx,
): number | undefined {
  const limpo = texto.trim()
  if (limpo === '') {
    return undefined
  }
  const lido =
    campo === 'taxaAlvoValor'
      ? lerForma(limpo, 2, '0,50', 'Digite sem sinal: o tipo da fase dá o sinal.')
      : lerForma(limpo, 1, '75,0')
  if ('erro' in lido) {
    ctx.addIssue({ code: 'custom', message: lido.erro, path: [campo] })
    return undefined
  }
  return lido.valor
}

/**
 * O que falta do plano não é recusado aqui: nível, taxa e peso alvo são
 * obrigatórios por regra, e a recusa vem do backend, no campo dela (RN-28,
 * RN-29, RN-41). A manutenção não leva plano nenhum (RN-33).
 */
const esquema = z
  .object({
    tipo: z.enum(['CUTTING', 'BULKING', 'MANUTENCAO']),
    dataInicio: z.string(),
    terminou: z.boolean(),
    dataFim: z.string(),
    nivel: z.enum(['CONSERVADOR', 'MODERADO', 'AGRESSIVO', 'PERSONALIZADO']).nullable(),
    taxaAlvoValor: z.string(),
    taxaAlvoUnidade: z.enum(['PERCENTUAL_SEMANA', 'KG_SEMANA']),
    pesoAlvo: z.string(),
  })
  .transform((valores, ctx): FaseEnviada => {
    const fase: FaseEnviada = { tipo: valores.tipo, dataInicio: valores.dataInicio }
    if (valores.terminou) {
      fase.dataFim = valores.dataFim
    }
    if (valores.tipo === 'MANUTENCAO') {
      return fase
    }
    if (valores.nivel !== null) {
      fase.nivel = valores.nivel
    }
    const taxa = ler(valores.taxaAlvoValor, 'taxaAlvoValor', ctx)
    if (taxa !== undefined) {
      fase.taxaAlvoValor = taxa
      fase.taxaAlvoUnidade = valores.taxaAlvoUnidade
    }
    const pesoAlvo = ler(valores.pesoAlvo, 'pesoAlvo', ctx)
    if (pesoAlvo !== undefined) {
      fase.pesoAlvo = pesoAlvo
    }
    return fase
  })

export type ValoresDaFase = z.input<typeof esquema>

const CAMPOS = ['tipo', 'dataInicio', 'dataFim', 'nivel', 'taxaAlvoValor', 'pesoAlvo'] as const

/** O campo da recusa do backend; a unidade da taxa aparece embaixo do valor. */
function campoDaRecusa(campo: string | null): (typeof CAMPOS)[number] | undefined {
  return CAMPOS.find((nome) => nome === (campo === 'taxaAlvoUnidade' ? 'taxaAlvoValor' : campo))
}

interface Props {
  inicial: ValoresDaFase
  niveis: Nivel[]
  modo: 'criar' | 'editar'
  /** Na edição, o tipo gravado, que decide os avisos da troca de tipo (UC-06 2c, 2j). */
  tipoGravado?: TipoDeFase
  rotuloEnviar: string
  /** Rejeita com a recusa do backend, que o formulário mostra embaixo do campo dela. */
  aoEnviar: (fase: FaseEnviada) => Promise<void>
  children?: ReactNode
}

/**
 * A fase, para criar (UC-05) e editar (UC-06). Escolher o nível preenche a
 * taxa com o padrão dele, e mexer na taxa passa o nível a personalizado,
 * como o backend grava (RN-30, RN-31).
 */
export function FormularioFase({
  inicial,
  niveis,
  modo,
  tipoGravado,
  rotuloEnviar,
  aoEnviar,
  children,
}: Props) {
  const {
    control,
    register,
    handleSubmit,
    setError,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(esquema), defaultValues: inicial })
  const [unidadeTrocada, setUnidadeTrocada] = useState(false)
  const tipo = useWatch({ control, name: 'tipo' })
  const nivel = useWatch({ control, name: 'nivel' })
  const terminou = useWatch({ control, name: 'terminou' })
  const pesoAlvo = lerNumero(useWatch({ control, name: 'pesoAlvo' }))

  const padrao = (doTipo: TipoDeFase, doNivel: NivelDaFase | null) =>
    niveis.find((candidato) => candidato.tipo === doTipo && candidato.nivel === doNivel)

  function aplicarPadrao(doTipo: TipoDeFase, doNivel: NivelDaFase | null) {
    const taxa = padrao(doTipo, doNivel)
    if (taxa !== undefined) {
      setValue('taxaAlvoValor', formatarTaxa(taxa.valor, { comSinal: false }))
      setValue('taxaAlvoUnidade', taxa.unidade)
      setUnidadeTrocada(false)
    }
  }

  async function enviar(fase: FaseEnviada) {
    try {
      await aoEnviar(fase)
    } catch (erro) {
      const campo = erro instanceof ErroDaApi ? campoDaRecusa(erro.campo) : undefined
      if (erro instanceof ErroDaApi && campo !== undefined) {
        setError(campo, { type: 'api', message: erro.message }, { shouldFocus: true })
      } else {
        setError('root.envio', { message: erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA })
      }
    }
  }

  const comPlano = tipo !== 'MANUTENCAO'
  const aviso = avisoDoTipo(modo, tipoGravado, tipo, nivel, pesoAlvo)
  const ajudaDaTaxa = unidadeTrocada
    ? 'O valor não é convertido: 0,50% e 0,50 kg são alvos diferentes. Confira o número na unidade nova.'
    : `Sem sinal: o ${formatarTipo(tipo)} ${tipo === 'BULKING' ? 'ganha' : 'perde'}.${modo === 'criar' ? ' Mudar o valor ou a unidade passa o nível a personalizado.' : ''}`
  // Sem fim, a falta dele pode invadir a seguinte: a recusa fica embaixo do interruptor
  const erroDoFim = errors.dataFim?.message

  return (
    <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5">
      <div className="flex flex-col gap-1.5">
        <span id="rotulo-tipo" className="text-sm font-semibold">
          Tipo
        </span>
        <Controller
          control={control}
          name="tipo"
          render={({ field }) => (
            <ToggleGroup
              type="single"
              value={field.value}
              onValueChange={(valor) => {
                const novo = TIPOS.find((candidato) => candidato === valor)
                if (novo !== undefined) {
                  field.onChange(novo)
                  if (nivel !== 'PERSONALIZADO') {
                    aplicarPadrao(novo, nivel)
                  }
                }
              }}
              aria-labelledby="rotulo-tipo"
              aria-describedby={errors.tipo ? 'erro-tipo' : undefined}
              spacing={0}
              className="grid w-full grid-cols-3 rounded-full border bg-card p-0.5"
            >
              {TIPOS.map((opcao) => (
                <ToggleGroupItem key={opcao} value={opcao} className={SEGMENTO}>
                  {formatarTipo(opcao)}
                </ToggleGroupItem>
              ))}
            </ToggleGroup>
          )}
        />
        {errors.tipo && (
          <span id="erro-tipo" className="text-sm text-destructive">
            {errors.tipo.message}
          </span>
        )}
      </div>

      <Controller
        control={control}
        name="dataInicio"
        render={({ field }) => (
          <CampoData
            id="inicio"
            rotulo="Início"
            valor={field.value}
            aoEscolher={field.onChange}
            erro={errors.dataInicio?.message}
            ajuda={
              modo === 'editar'
                ? 'Os registros seguem as datas: mudar o período muda a fase deles.'
                : undefined
            }
          />
        )}
      />

      <div className="flex flex-col gap-2">
        <div className="flex min-h-11 items-center justify-between gap-3">
          <div className="flex flex-col gap-0.5">
            <label htmlFor="terminou" className="text-sm font-semibold">
              A fase já terminou
            </label>
            {modo === 'criar' && (
              <span id="ajuda-terminou" className="text-xs text-muted-foreground">
                Para classificar um período passado, como um histórico importado.
              </span>
            )}
          </div>
          <Controller
            control={control}
            name="terminou"
            render={({ field }) => (
              <Switch
                id="terminou"
                checked={field.value}
                onCheckedChange={field.onChange}
                aria-describedby={
                  [modo === 'criar' && 'ajuda-terminou', !terminou && erroDoFim && 'erro-fim']
                    .filter(Boolean)
                    .join(' ') || undefined
                }
              />
            )}
          />
        </div>
        {terminou ? (
          <Controller
            control={control}
            name="dataFim"
            render={({ field }) => (
              <CampoData
                id="fim"
                rotulo="Fim"
                valor={field.value}
                aoEscolher={field.onChange}
                erro={erroDoFim}
              />
            )}
          />
        ) : (
          erroDoFim && (
            <span id="erro-fim" className="text-sm text-destructive">
              {erroDoFim}
            </span>
          )
        )}
      </div>

      {aviso !== null && (
        <div
          role="status"
          className="flex items-start gap-3 rounded-xl border bg-card px-4 py-3.5 text-card-foreground"
        >
          <Info aria-hidden="true" className="mt-0.5 size-5 flex-none" />
          <div className="flex flex-col gap-0.5">
            <strong className="tabular-nums">{aviso.titulo}</strong>
            <span className="text-muted-foreground tabular-nums">{aviso.texto}</span>
          </div>
        </div>
      )}

      {comPlano && (
        <>
          <div className="flex flex-col gap-1.5">
            <span id="rotulo-nivel" className="text-sm font-semibold">
              Nível
            </span>
            <Controller
              control={control}
              name="nivel"
              render={({ field }) => (
                <RadioGroup
                  value={field.value ?? ''}
                  onValueChange={(valor) => {
                    const novo = NIVEIS.find((candidato) => candidato === valor)
                    if (novo !== undefined) {
                      field.onChange(novo)
                      if (novo !== 'PERSONALIZADO') {
                        aplicarPadrao(tipo, novo)
                      }
                    }
                  }}
                  aria-labelledby="rotulo-nivel"
                  aria-invalid={errors.nivel ? true : undefined}
                  aria-describedby={errors.nivel ? 'erro-nivel' : undefined}
                  className={cn(
                    'gap-0 overflow-hidden rounded-xl border bg-card',
                    errors.nivel && 'border-2 border-destructive',
                  )}
                >
                  {NIVEIS.map((opcao) => {
                    const taxa = padrao(tipo, opcao)
                    return (
                      <label
                        key={opcao}
                        className="flex min-h-12 cursor-pointer items-center gap-3 border-t px-3.5 py-2 first:border-t-0"
                      >
                        <RadioGroupItem value={opcao} />
                        <span className="capitalize">{formatarNivel(opcao)}</span>
                        <span className="ml-auto text-sm text-muted-foreground tabular-nums">
                          {taxa === undefined
                            ? 'você digita a taxa'
                            : formatarTaxaAlvo(taxa.valor, taxa.unidade)}
                        </span>
                      </label>
                    )
                  })}
                </RadioGroup>
              )}
            />
            {errors.nivel && (
              <span id="erro-nivel" className="text-sm text-destructive">
                {errors.nivel.message}
              </span>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="taxa" className="text-sm font-semibold">
              Taxa alvo por semana
            </label>
            <div className="flex items-stretch gap-2">
              <div className={cn(CAIXA, 'flex-1', errors.taxaAlvoValor && INVALIDA)}>
                <input
                  id="taxa"
                  inputMode="decimal"
                  autoComplete="off"
                  aria-invalid={errors.taxaAlvoValor ? true : undefined}
                  aria-describedby={errors.taxaAlvoValor ? 'ajuda-taxa erro-taxa' : 'ajuda-taxa'}
                  className={ENTRADA}
                  {...register('taxaAlvoValor', {
                    onChange: () => setValue('nivel', 'PERSONALIZADO'),
                  })}
                />
              </div>
              <Controller
                control={control}
                name="taxaAlvoUnidade"
                render={({ field }) => (
                  <ToggleGroup
                    type="single"
                    value={field.value}
                    onValueChange={(valor) => {
                      const nova = UNIDADES.find((candidata) => candidata === valor)
                      if (nova !== undefined && nova !== field.value) {
                        field.onChange(nova)
                        setValue('nivel', 'PERSONALIZADO')
                        setUnidadeTrocada(true)
                      }
                    }}
                    aria-label="Unidade da taxa alvo"
                    spacing={0}
                    className="flex-none rounded-full border bg-card p-0.5"
                  >
                    <ToggleGroupItem
                      value="PERCENTUAL_SEMANA"
                      aria-label="por cento do peso"
                      className={cn(SEGMENTO, 'px-4 normal-case')}
                    >
                      %
                    </ToggleGroupItem>
                    <ToggleGroupItem
                      value="KG_SEMANA"
                      aria-label="quilos"
                      className={cn(SEGMENTO, 'px-4 normal-case')}
                    >
                      kg
                    </ToggleGroupItem>
                  </ToggleGroup>
                )}
              />
            </div>
            <span id="ajuda-taxa" className="text-xs text-muted-foreground tabular-nums">
              {ajudaDaTaxa}
            </span>
            {errors.taxaAlvoValor && (
              <span id="erro-taxa" className="text-sm text-destructive">
                {errors.taxaAlvoValor.message}
              </span>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="peso-alvo" className="text-sm font-semibold">
              Peso alvo
            </label>
            <div className={cn(CAIXA, errors.pesoAlvo && INVALIDA)}>
              <input
                id="peso-alvo"
                inputMode="decimal"
                autoComplete="off"
                aria-invalid={errors.pesoAlvo ? true : undefined}
                aria-describedby={
                  [modo === 'criar' && 'ajuda-peso-alvo', errors.pesoAlvo && 'erro-peso-alvo']
                    .filter(Boolean)
                    .join(' ') || undefined
                }
                className={ENTRADA}
                {...register('pesoAlvo')}
              />
              <span className="text-sm text-muted-foreground">kg</span>
            </div>
            {modo === 'criar' && (
              <span id="ajuda-peso-alvo" className="text-xs text-muted-foreground">
                {tipo === 'BULKING'
                  ? 'No bulking, o teto a não passar.'
                  : 'No cutting, o peso a alcançar descendo.'}
              </span>
            )}
            {errors.pesoAlvo && (
              <span id="erro-peso-alvo" className="text-sm text-destructive">
                {errors.pesoAlvo.message}
              </span>
            )}
          </div>
        </>
      )}

      {errors.root?.envio && (
        <Alert variant="destructive" className="rounded-xl px-4 py-3 text-base">
          <AlertDescription className="text-base text-destructive">
            {errors.root.envio.message}
          </AlertDescription>
        </Alert>
      )}

      <div className="flex flex-col gap-2 pt-1">
        <Button type="submit" disabled={isSubmitting} className="w-full">
          {rotuloEnviar}
        </Button>
        {children}
      </div>
    </form>
  )
}

const SEGMENTO =
  'min-h-10 rounded-full! text-sm text-muted-foreground capitalize data-[state=on]:bg-secondary data-[state=on]:font-bold data-[state=on]:text-secondary-foreground'
const CAIXA =
  'flex min-h-11 items-center gap-2 rounded-lg border border-input bg-card px-3 focus-within:outline-3 focus-within:outline-offset-2 focus-within:outline-ring'
const INVALIDA = 'border-2 border-destructive'
const ENTRADA = 'w-full min-w-0 bg-transparent py-2.5 text-base tabular-nums outline-none'

/**
 * O que a troca de tipo faz com o plano, antes de salvar: a manutenção nova
 * não tem plano; na edição, virar manutenção descarta o plano (UC-06 2c), e
 * trocar entre cutting e bulking leva a taxa ao padrão do nível (UC-06 2j).
 */
function avisoDoTipo(
  modo: 'criar' | 'editar',
  tipoGravado: TipoDeFase | undefined,
  tipo: TipoDeFase,
  nivel: NivelDaFase | null,
  pesoAlvo: number | null,
): { titulo: string; texto: string } | null {
  if (modo === 'criar') {
    return tipo === 'MANUTENCAO'
      ? {
          titulo: 'A manutenção não tem nível, taxa alvo nem peso alvo.',
          texto:
            'O veredito diz se o peso semanal está dentro de uma faixa em volta do peso de partida da fase.',
        }
      : null
  }
  if (tipoGravado === undefined || tipoGravado === 'MANUTENCAO' || tipo === tipoGravado) {
    return null
  }
  if (tipo === 'MANUTENCAO') {
    return {
      titulo: 'Nível, taxa alvo e peso alvo desta fase serão descartados.',
      texto: 'A manutenção não tem nenhum dos três. Os registros continuam como estão.',
    }
  }
  if (nivel === null || nivel === 'PERSONALIZADO') {
    return null
  }
  return {
    titulo: `A taxa alvo foi para o padrão do ${formatarNivel(nivel)} no ${formatarTipo(tipo)}.`,
    texto: `${pesoAlvo === null ? '' : `O peso alvo continua ${formatarPeso(pesoAlvo)} kg. `}Se ele ficar do lado errado do peso de partida, a barra some até você corrigir.`,
  }
}
