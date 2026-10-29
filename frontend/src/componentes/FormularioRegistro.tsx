import { zodResolver } from '@hookform/resolvers/zod'
import type { ReactNode } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { ErroDaApi } from '@/api/cliente'
import { CampoData } from '@/componentes/CampoData'
import { Alert, AlertDescription } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import type { RegistroEnviado } from '@/hooks/useRegistros'
import { numeroDeForma, opcional, SEM_RESPOSTA } from '@/lib/forma'
import { cn } from '@/lib/utils'

const esquema = z.object({
  data: z.string(),
  peso: z.string().trim().min(1, 'Informe o peso.').transform(numeroDeForma(1, '78,4')),
  cintura: opcional(1, '88,5'),
})

export type ValoresDoRegistro = z.input<typeof esquema>

const CAMPOS = ['data', 'peso', 'cintura'] as const

function ehCampo(campo: string | null): campo is (typeof CAMPOS)[number] {
  return CAMPOS.some((nome) => nome === campo)
}

interface Props {
  inicial: ValoresDoRegistro
  rotuloEnviar: string
  /** Rejeita com a recusa do backend, que o formulário mostra embaixo do campo dela. */
  aoEnviar: (registro: RegistroEnviado) => Promise<void>
  /** O que vem embaixo do botão de enviar, como o de apagar na edição. */
  children?: ReactNode
}

/**
 * O registro de um dia, para registrar (UC-02) e editar (UC-03): a data já vem
 * com hoje (RN-04) e o calendário não bloqueia o futuro, porque a recusa é
 * regra e vem do backend (ADR-0025).
 */
export function FormularioRegistro({ inicial, rotuloEnviar, aoEnviar, children }: Props) {
  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(esquema), defaultValues: inicial })

  async function enviar({ data, peso, cintura }: z.output<typeof esquema>) {
    try {
      await aoEnviar(cintura === null ? { data, peso } : { data, peso, cintura })
    } catch (erro) {
      if (erro instanceof ErroDaApi && ehCampo(erro.campo)) {
        setError(erro.campo, { type: 'api', message: erro.message }, { shouldFocus: true })
      } else {
        setError('root.envio', {
          message: erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA,
        })
      }
    }
  }

  return (
    <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-1 flex-col gap-4">
      <Controller
        control={control}
        name="data"
        render={({ field }) => (
          <CampoData
            id="data"
            rotulo="Data"
            valor={field.value}
            aoEscolher={field.onChange}
            erro={errors.data?.message}
          />
        )}
      />

      <div className="flex flex-col items-center gap-2 rounded-xl border bg-card px-4 pt-6 pb-5">
        <label htmlFor="peso" className="text-sm font-semibold text-muted-foreground">
          Peso
        </label>
        <div
          className={cn(
            'flex items-baseline justify-center gap-2 rounded-sm border-b-2 border-primary px-2 pb-1 focus-within:outline-3 focus-within:outline-offset-2 focus-within:outline-ring',
            errors.peso && 'border-destructive',
          )}
        >
          <input
            id="peso"
            inputMode="decimal"
            autoComplete="off"
            aria-invalid={errors.peso ? true : undefined}
            aria-describedby={errors.peso ? 'erro-peso' : undefined}
            className="w-[4.8ch] bg-transparent text-center text-5xl font-bold tracking-tight tabular-nums outline-none"
            {...register('peso')}
          />
          <span className="text-lg font-semibold text-muted-foreground">kg</span>
        </div>
        {errors.peso && (
          <span id="erro-peso" className="text-center text-sm text-destructive">
            {errors.peso.message}
          </span>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="cintura" className="text-sm font-semibold">
          Cintura <span className="font-normal text-muted-foreground">(opcional)</span>
        </label>
        <div
          className={cn(
            'flex min-h-11 items-center gap-2 rounded-lg border border-input bg-card px-3 focus-within:outline-3 focus-within:outline-offset-2 focus-within:outline-ring',
            errors.cintura && 'border-2 border-destructive',
          )}
        >
          <input
            id="cintura"
            inputMode="decimal"
            autoComplete="off"
            aria-invalid={errors.cintura ? true : undefined}
            aria-describedby={errors.cintura ? 'erro-cintura' : undefined}
            className="w-full min-w-0 bg-transparent py-2.5 text-base tabular-nums outline-none"
            {...register('cintura')}
          />
          <span className="text-sm text-muted-foreground">cm</span>
        </div>
        {errors.cintura && (
          <span id="erro-cintura" className="text-sm text-destructive">
            {errors.cintura.message}
          </span>
        )}
      </div>

      {errors.root?.envio && (
        <Alert variant="destructive" className="rounded-xl px-4 py-3 text-base">
          <AlertDescription className="text-base text-destructive">
            {errors.root.envio.message}
          </AlertDescription>
        </Alert>
      )}

      <div className="mt-auto flex flex-col gap-2 pt-2 md:mt-4">
        <Button type="submit" disabled={isSubmitting} className="w-full">
          {rotuloEnviar}
        </Button>
        {children}
      </div>
    </form>
  )
}
