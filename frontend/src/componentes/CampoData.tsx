import { CalendarDays, ChevronDown } from 'lucide-react'
import { useState } from 'react'
import { ptBR } from 'react-day-picker/locale'
import { dataDoDispositivo } from '@/api/cliente'
import { Button } from '@/componentes/ui/button'
import { Calendar } from '@/componentes/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/componentes/ui/popover'
import { diaDoIso, formatarData, isoDoDia } from '@/formato/data'

interface Props {
  /** Dá os ids do rótulo, do valor, da ajuda e do erro: rotulo-{id}, erro-{id}. */
  id: string
  rotulo: string
  valor: string
  aoEscolher: (iso: string) => void
  erro: string | undefined
  ajuda?: string
}

/**
 * Uma data num botão com cara de campo, que abre o calendário do shadcn/ui. O
 * calendário não bloqueia dia nenhum: data futura ou fora de faixa é regra, e
 * a recusa vem do backend, embaixo do campo (ADR-0025).
 */
export function CampoData({ id, rotulo, valor, aoEscolher, erro, ajuda }: Props) {
  const [aberto, setAberto] = useState(false)
  const hoje = dataDoDispositivo()
  const descricao = [erro && `erro-${id}`, ajuda && `ajuda-${id}`].filter(Boolean).join(' ')

  return (
    <div className="flex flex-col gap-1.5">
      <span id={`rotulo-${id}`} className="text-sm font-semibold">
        {rotulo}
      </span>
      <Popover open={aberto} onOpenChange={setAberto}>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            aria-labelledby={`rotulo-${id} valor-${id}`}
            aria-invalid={erro ? true : undefined}
            aria-describedby={descricao || undefined}
            className="w-full justify-start border-input bg-card px-3 font-normal aria-invalid:border-2 aria-invalid:border-destructive dark:bg-card"
          >
            <CalendarDays aria-hidden="true" className="text-muted-foreground" />
            <span id={`valor-${id}`} className="tabular-nums">
              {formatarData(valor)}
            </span>
            {valor === hoje && <span className="text-sm text-muted-foreground">hoje</span>}
            <ChevronDown aria-hidden="true" className="ml-auto text-muted-foreground" />
          </Button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-auto rounded-xl p-2">
          <Calendar
            mode="single"
            locale={ptBR}
            required
            selected={diaDoIso(valor)}
            defaultMonth={diaDoIso(valor)}
            onSelect={(dia) => {
              aoEscolher(isoDoDia(dia))
              setAberto(false)
            }}
          />
        </PopoverContent>
      </Popover>
      {erro && (
        <span id={`erro-${id}`} className="text-sm text-destructive">
          {erro}
        </span>
      )}
      {ajuda && (
        <span id={`ajuda-${id}`} className="text-xs text-muted-foreground">
          {ajuda}
        </span>
      )}
    </div>
  )
}
