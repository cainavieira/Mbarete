/**
 * O único lugar que monta requisição (convencoes-frontend.md). Põe o dia do
 * dispositivo no Data-Local de toda chamada (RN-04) e, ao receber 401, renova
 * a sessão uma vez e repete a chamada; se a renovação também voltar 401, a
 * pessoa vai para a tela de entrada (endpoints.md).
 */

const PREFIXO = '/api/v1'

/** A recusa no formato único do backend (convencoes-backend.md). */
export class ErroDaApi extends Error {
  readonly status: number
  readonly codigo: string
  readonly campo: string | null
  readonly regra: string | null

  constructor(
    status: number,
    codigo: string,
    campo: string | null,
    regra: string | null,
    mensagem: string,
  ) {
    super(mensagem)
    this.name = 'ErroDaApi'
    this.status = status
    this.codigo = codigo
    this.campo = campo
    this.regra = regra
  }
}

type Metodo = 'GET' | 'POST' | 'PATCH' | 'DELETE'

export interface Opcoes {
  metodo?: Metodo
  /** JSON, ou o formulário do arquivo da importação. */
  corpo?: unknown
}

/** O dia do calendário do dispositivo, e não o do UTC, em AAAA-MM-DD (RN-04). */
export function dataDoDispositivo(agora: Date = new Date()): string {
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  const dia = String(agora.getDate()).padStart(2, '0')
  return `${agora.getFullYear()}-${mes}-${dia}`
}

let aoPerderSessao: () => void = () => window.location.assign('/entrar')

/** Troca o que acontece quando nem a renovação devolve a sessão; existe para o teste. */
export function definirAoPerderSessao(acao: () => void): void {
  aoPerderSessao = acao
}

/**
 * Chamadas que voltam 401 ao mesmo tempo esperam a mesma renovação: duas
 * renovações seguidas com o mesmo token disputariam a sessão.
 */
let renovacaoEmCurso: Promise<boolean> | null = null

function renovar(): Promise<boolean> {
  renovacaoEmCurso ??= fetch(`${PREFIXO}/auth/renovacao`, {
    method: 'POST',
    headers: { 'Data-Local': dataDoDispositivo() },
    credentials: 'same-origin',
  })
    .then((resposta) => resposta.ok)
    .catch(() => false)
    .finally(() => {
      renovacaoEmCurso = null
    })
  return renovacaoEmCurso
}

function enviar(caminho: string, { metodo = 'GET', corpo }: Opcoes): Promise<Response> {
  const cabecalhos: Record<string, string> = { 'Data-Local': dataDoDispositivo() }
  let body: BodyInit | undefined
  if (corpo instanceof FormData) {
    body = corpo
  } else if (corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json'
    body = JSON.stringify(corpo)
  }
  return fetch(`${PREFIXO}${caminho}`, {
    method: metodo,
    headers: cabecalhos,
    body,
    credentials: 'same-origin',
  })
}

/**
 * A recusa como o backend a escreveu. Resposta sem o formato único só vem de
 * fora da api, como o proxy com a api fora do ar.
 */
async function erroDe(resposta: Response): Promise<ErroDaApi> {
  try {
    const { erro } = (await resposta.json()) as {
      erro: { codigo: string; campo: string | null; mensagem: string; regra: string | null }
    }
    return new ErroDaApi(resposta.status, erro.codigo, erro.campo, erro.regra, erro.mensagem)
  } catch {
    return new ErroDaApi(
      resposta.status,
      'ERRO_INTERNO',
      null,
      null,
      'Erro inesperado. Tente de novo em instantes.',
    )
  }
}

export async function chamar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  let resposta = await enviar(caminho, opcoes)
  if (resposta.status === 401) {
    if (await renovar()) {
      resposta = await enviar(caminho, opcoes)
    }
    if (resposta.status === 401) {
      aoPerderSessao()
      throw await erroDe(resposta)
    }
  }
  if (!resposta.ok) {
    throw await erroDe(resposta)
  }
  if (resposta.status === 204) {
    return undefined as T
  }
  return (await resposta.json()) as T
}
