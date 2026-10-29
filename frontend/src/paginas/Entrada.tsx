import { Check, Info, LogIn } from 'lucide-react'
import { useLocation, useSearchParams } from 'react-router'
import { Logotipo } from '@/componentes/Logotipo'
import { Alert, AlertDescription, AlertTitle } from '@/componentes/ui/alert'
import { Button } from '@/componentes/ui/button'

/** O que a tela de conta passa ao voltar para cá depois de apagar tudo (UC-12). */
export interface EstadoDaEntrada {
  contaApagada?: boolean
}

/**
 * A entrada (UC-01). O retorno do login chega aqui com o motivo na URL quando
 * falha (endpoints.md), e a tela explica: as duas recusas num aviso neutro, e
 * não em vermelho, porque não são erro de formulário.
 */
export function Entrada() {
  const [busca] = useSearchParams()
  const motivo = busca.get('motivo')
  const estado = useLocation().state as EstadoDaEntrada | null
  const contaApagada = estado?.contaApagada === true

  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-between gap-10 px-6 pt-18 pb-8">
      <div className="flex flex-col gap-10">
        <Logotipo className="h-8.5 w-auto self-start text-primary" />

        <div className="flex flex-col gap-3">
          <h1 className="text-2xl leading-tight font-bold">Estou no ritmo do meu ciclo?</h1>
          <p className="text-muted-foreground">
            Registre o peso de cada dia e veja, pelo peso semanal, se o seu cutting, bulking ou
            manutenção está no ritmo do plano.
          </p>
        </div>

        {motivo === 'NAO_CONVIDADO' && (
          <Aviso
            titulo="O acesso ao Mbarete é por convite."
            texto="Se você recebeu o convite, entre com a conta Google do e-mail convidado."
          />
        )}
        {motivo === 'LOGIN_NAO_CONCLUIDO' && (
          <Aviso titulo="O login não foi concluído." texto="Nada mudou. Pode tentar de novo." />
        )}
        {contaApagada && (
          <Alert role="status" className="rounded-xl px-4 py-3.5 text-base">
            <Check aria-hidden="true" />
            <AlertTitle>A conta foi apagada.</AlertTitle>
            <AlertDescription>
              Os registros, as fases e os dados da conta saíram do banco. Entrando de novo, você
              começa com uma conta vazia.
            </AlertDescription>
          </Alert>
        )}
      </div>

      <div className="flex flex-col gap-4">
        {/* Navegação de página inteira: o login passa pelo Google e volta pela api */}
        <Button asChild className="w-full">
          <a href="/api/v1/auth/login">
            <LogIn aria-hidden="true" />
            Entrar com o Google
          </a>
        </Button>
        {!motivo && !contaApagada && (
          <p className="text-center text-sm text-muted-foreground">O acesso é por convite.</p>
        )}
      </div>
    </main>
  )
}

function Aviso({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <Alert className="rounded-xl px-4 py-3.5 text-base">
      <Info aria-hidden="true" />
      <AlertTitle>{titulo}</AlertTitle>
      <AlertDescription>{texto}</AlertDescription>
    </Alert>
  )
}
