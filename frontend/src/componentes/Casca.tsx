import { useQuery } from '@tanstack/react-query'
import { CalendarRange, ChartLine, List, Plus, UserRound, type LucideIcon } from 'lucide-react'
import { Link, NavLink, Outlet } from 'react-router'
import { chamar } from '@/api/cliente'
import { Logotipo } from '@/componentes/Logotipo'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'
import { cn } from '@/lib/utils'

interface Destino {
  para: string
  rotulo: string
  rotuloLargo: string
  Icone: LucideIcon
}

const INICIO: Destino = { para: '/', rotulo: 'Início', rotuloLargo: 'Acompanhamento', Icone: ChartLine }
const HISTORICO: Destino = { para: '/historico', rotulo: 'Histórico', rotuloLargo: 'Histórico', Icone: List }
const FASES: Destino = { para: '/fases', rotulo: 'Fases', rotuloLargo: 'Fases', Icone: CalendarRange }
const CONTA: Destino = { para: '/conta', rotulo: 'Conta', rotuloLargo: 'Conta', Icone: UserRound }

/**
 * A moldura das telas de quem entrou: no celular, a barra embaixo com o
 * registro no meio; no computador, a navegação no alto com o registro à
 * direita. A conta é a prova de sessão: sem ela, o cliente HTTP leva à entrada.
 */
export function Casca() {
  const sessao = useQuery({ queryKey: ['conta'], queryFn: () => chamar<unknown>('/conta') })

  if (sessao.isPending) {
    return null
  }
  if (sessao.isError) {
    return (
      <main className="mx-auto max-w-md p-4">
        <Alert>
          <AlertTitle>{sessao.error.message}</AlertTitle>
          <AlertDescription>Recarregue a página para tentar de novo.</AlertDescription>
        </Alert>
      </main>
    )
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="hidden border-b md:block">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-8 gap-y-3 px-8 py-3">
          <Link to="/" aria-label="Acompanhamento" className="text-primary">
            <Logotipo className="h-7 w-auto" />
          </Link>
          <nav aria-label="Principal" className="flex flex-1 flex-wrap items-center gap-1">
            {[INICIO, HISTORICO, FASES, CONTA].map(({ para, rotuloLargo }) => (
              <NavLink
                key={para}
                to={para}
                end={para === '/'}
                className="inline-flex min-h-11 items-center rounded-lg px-3.5 font-semibold text-muted-foreground aria-[current=page]:bg-secondary aria-[current=page]:text-secondary-foreground"
              >
                {rotuloLargo}
              </NavLink>
            ))}
          </nav>
          <Button asChild>
            <Link to="/registrar">
              <Plus aria-hidden="true" />
              Registrar peso
            </Link>
          </Button>
        </div>
      </header>

      <div className="flex-1 pb-24 md:pb-0">
        <Outlet />
      </div>

      <nav
        aria-label="Principal"
        className="fixed inset-x-0 bottom-0 grid grid-cols-5 border-t bg-background px-1 pt-1.5 pb-4 md:hidden"
      >
        {[INICIO, HISTORICO].map((destino) => (
          <ItemDaBarra key={destino.para} {...destino} />
        ))}
        <ItemDaBarra para="/registrar" rotulo="Registrar" rotuloLargo="Registrar" Icone={Plus} registrar />
        {[FASES, CONTA].map((destino) => (
          <ItemDaBarra key={destino.para} {...destino} />
        ))}
      </nav>
    </div>
  )
}

function ItemDaBarra({ para, rotulo, Icone, registrar = false }: Destino & { registrar?: boolean }) {
  return (
    <NavLink
      to={para}
      end={para === '/'}
      className={cn(
        'group flex min-h-13 flex-col items-center justify-center gap-0.5 text-xs font-semibold text-muted-foreground aria-[current=page]:text-foreground',
        registrar && 'text-primary',
      )}
    >
      <span
        className={cn(
          'grid h-7.5 w-13 place-items-center rounded-full group-aria-[current=page]:bg-secondary group-aria-[current=page]:text-secondary-foreground',
          registrar && 'bg-primary text-primary-foreground',
        )}
      >
        <Icone aria-hidden="true" className="size-5.5" />
      </span>
      {rotulo}
    </NavLink>
  )
}
