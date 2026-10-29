import { useQueryClient } from '@tanstack/react-query'
import { LogOut, Trash2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { ErroDaApi } from '@/api/cliente'
import { SeletorTema } from '@/componentes/SeletorTema'
import { Button } from '@/componentes/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/componentes/ui/dialog'
import { useApagarConta, useConta, useSair } from '@/hooks/useConta'
import { SEM_RESPOSTA } from '@/lib/forma'
import { cn } from '@/lib/utils'
import type { EstadoDaEntrada } from '@/paginas/Entrada'

/**
 * A conta (UC-12): os dados que vêm do Google, o tema (RF-51), sair (RF-04) e
 * apagar tudo, com o e-mail digitado como confirmação (RF-06, RN-51).
 */
export function Conta() {
  const conta = useConta()
  const sair = useSair()
  const cliente = useQueryClient()
  const navegar = useNavigate()
  const [apagando, setApagando] = useState(false)

  /**
   * Volta à entrada sem nada da conta em memória, para outra pessoa entrar
   * limpa. Navega antes de limpar: a casca, ainda montada, buscaria a conta de
   * novo, e o 401 levaria à entrada sem o aviso da exclusão.
   */
  function irParaAEntrada(estado?: EstadoDaEntrada) {
    navegar('/entrar', { replace: true, state: estado })
    cliente.clear()
  }

  async function aoSair() {
    try {
      await sair.mutateAsync()
    } catch (erro) {
      toast.error(erro instanceof ErroDaApi ? erro.message : SEM_RESPOSTA)
      return
    }
    irParaAEntrada()
  }

  // A casca só mostra as telas com a conta carregada
  if (conta.data === undefined) {
    return null
  }
  const { nome, email, foto } = conta.data

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-6 px-4 pt-5 pb-6 md:px-0 md:pt-7">
      <h1 className="text-2xl leading-tight font-bold">Conta</h1>

      <section
        aria-label="Seus dados"
        className="flex items-center gap-3.5 rounded-xl border bg-card p-4 text-card-foreground"
      >
        {foto === null ? (
          <span
            aria-hidden="true"
            className="grid size-12 flex-none place-items-center rounded-full bg-secondary font-bold text-secondary-foreground"
          >
            {iniciais(nome)}
          </span>
        ) : (
          <img src={foto} alt="" className="size-12 flex-none rounded-full object-cover" />
        )}
        <div className="flex min-w-0 flex-col">
          <strong className="truncate">{nome}</strong>
          <span className="truncate text-sm text-muted-foreground">{email}</span>
          <span className="text-xs text-muted-foreground">Nome, e-mail e foto vêm da conta Google.</span>
        </div>
      </section>

      <section aria-labelledby="titulo-tema" className="flex flex-col gap-1.5">
        <h2 id="titulo-tema" className="text-sm font-semibold">
          Tema
        </h2>
        <SeletorTema />
        <p className="text-xs text-muted-foreground">
          A escolha fica neste navegador. Em Sistema, o tema segue o do aparelho.
        </p>
      </section>

      <Button type="button" variant="outline" disabled={sair.isPending} onClick={() => void aoSair()}>
        <LogOut aria-hidden="true" />
        Sair
      </Button>

      <section
        aria-labelledby="titulo-apagar-conta"
        className="mt-auto flex flex-col gap-2.5 border-t pt-5"
      >
        <h2 id="titulo-apagar-conta" className="text-lg leading-snug font-bold">
          Apagar a conta
        </h2>
        <p className="text-sm text-muted-foreground">
          Tira do banco, na hora, todos os registros, as fases e os dados da conta. Não dá para
          desfazer.
        </p>
        <Button
          type="button"
          variant="outline"
          className="text-destructive hover:text-destructive"
          onClick={() => setApagando(true)}
        >
          <Trash2 aria-hidden="true" />
          Apagar a conta
        </Button>
      </section>

      {apagando && (
        <ApagarConta
          email={email}
          fechar={() => setApagando(false)}
          apagada={() => irParaAEntrada({ contaApagada: true })}
        />
      )}
    </main>
  )
}

interface PropsDoApagar {
  email: string
  fechar: () => void
  apagada: () => void
}

/**
 * A confirmação explica o que sai, o backup e o convite, e pede o e-mail. Se o
 * digitado não confere, a recusa vem do backend, embaixo do campo (UC-12 2a).
 */
function ApagarConta({ email, fechar, apagada }: PropsDoApagar) {
  const apagar = useApagarConta()
  const [digitado, setDigitado] = useState('')
  const [erro, setErro] = useState<string | undefined>()

  async function confirmar(evento: FormEvent) {
    evento.preventDefault()
    setErro(undefined)
    try {
      await apagar.mutateAsync(digitado)
    } catch (recusa) {
      setErro(recusa instanceof ErroDaApi ? recusa.message : SEM_RESPOSTA)
      return
    }
    apagada()
  }

  return (
    <Dialog open onOpenChange={(aberto) => !aberto && fechar()}>
      <DialogContent showCloseButton={false}>
        <form noValidate onSubmit={(evento) => void confirmar(evento)} className="flex flex-col gap-4">
          <DialogHeader>
            <DialogTitle>Apagar a conta?</DialogTitle>
          </DialogHeader>
          <DialogDescription asChild>
            <div className="flex flex-col gap-2 text-base text-popover-foreground">
              <p>
                Saem do banco, na hora, todos os registros, as fases e os dados da conta. Não há como
                desfazer.
              </p>
              <p className="text-sm text-muted-foreground">
                As cópias de backup não são editadas e somem no fim da retenção, em até 12 meses.
              </p>
              <p className="text-sm text-muted-foreground">
                O convite continua: entrando de novo, você começa com uma conta vazia.
              </p>
              <p className="text-sm">
                Quer guardar o histórico?{' '}
                <Link to="/importar" className="font-semibold text-primary underline underline-offset-3">
                  Exporte o CSV antes
                </Link>
                .
              </p>
            </div>
          </DialogDescription>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="email" className="text-sm font-semibold">
              Para confirmar, digite seu e-mail: {email}
            </label>
            <div
              className={cn(
                'flex min-h-11 items-center rounded-lg border border-input bg-card px-3 focus-within:outline-3 focus-within:outline-offset-2 focus-within:outline-ring',
                erro && 'border-2 border-destructive',
              )}
            >
              <input
                id="email"
                type="email"
                autoComplete="off"
                value={digitado}
                onChange={(evento) => setDigitado(evento.target.value)}
                aria-invalid={erro ? true : undefined}
                aria-describedby={erro ? 'erro-email' : undefined}
                className="w-full min-w-0 bg-transparent py-2.5 text-base outline-none"
              />
            </div>
            {erro && (
              <span id="erro-email" className="text-sm text-destructive">
                {erro}
              </span>
            )}
          </div>
          <DialogFooter className="mx-0 mb-0 flex-col gap-2 rounded-none border-0 bg-transparent p-0 sm:flex-col *:w-full">
            <Button type="submit" variant="destructive" disabled={apagar.isPending}>
              Apagar a conta
            </Button>
            <Button type="button" variant="outline" onClick={fechar}>
              Cancelar
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** As iniciais do nome, no lugar da foto que o Google não mandou: Marina Costa, MC. */
function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/)
  const primeira = partes[0]?.charAt(0) ?? ''
  const ultima = partes.length > 1 ? partes[partes.length - 1].charAt(0) : ''
  return `${primeira}${ultima}`.toUpperCase()
}
