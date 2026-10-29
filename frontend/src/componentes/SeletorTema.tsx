import { useState } from 'react'
import { ToggleGroup, ToggleGroupItem } from '@/componentes/ui/toggle-group'
import { ehTema, escolherTema, lerTema, type Tema } from '@/lib/tema'

const OPCOES: { valor: Tema; rotulo: string }[] = [
  { valor: 'sistema', rotulo: 'Sistema' },
  { valor: 'claro', rotulo: 'Claro' },
  { valor: 'escuro', rotulo: 'Escuro' },
]

/** O seletor de três posições do RF-51: sistema, claro e escuro. */
export function SeletorTema() {
  const [tema, setTema] = useState<Tema>(lerTema)

  return (
    <ToggleGroup
      type="single"
      value={tema}
      onValueChange={(valor) => {
        // Tocar de novo na opção marcada desmarca no Radix, e o tema não pode ficar vazio
        if (ehTema(valor)) {
          escolherTema(valor)
          setTema(valor)
        }
      }}
      aria-label="Tema"
      spacing={0}
      className="grid w-full grid-cols-3 rounded-full border bg-card p-0.5"
    >
      {OPCOES.map(({ valor, rotulo }) => (
        <ToggleGroupItem
          key={valor}
          value={valor}
          className="min-h-11 rounded-full! text-sm text-muted-foreground data-[state=on]:bg-secondary data-[state=on]:font-bold data-[state=on]:text-secondary-foreground"
        >
          {rotulo}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  )
}
