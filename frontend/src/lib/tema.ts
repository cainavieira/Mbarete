/**
 * O tema segue o do sistema operacional até a pessoa escolher outro, e a
 * escolha fica no navegador (RF-51). O script do index.html aplica o mesmo
 * antes da primeira pintura.
 */

export type Tema = 'sistema' | 'claro' | 'escuro'

const CHAVE = 'tema'

const escuroDoSistema = () => window.matchMedia('(prefers-color-scheme: dark)')

export function ehTema(valor: string): valor is Tema {
  return valor === 'sistema' || valor === 'claro' || valor === 'escuro'
}

export function lerTema(): Tema {
  try {
    const guardado = localStorage.getItem(CHAVE)
    return guardado === 'claro' || guardado === 'escuro' ? guardado : 'sistema'
  } catch {
    return 'sistema'
  }
}

function aplicarTema(tema: Tema): void {
  const escuro = tema === 'escuro' || (tema === 'sistema' && escuroDoSistema().matches)
  document.documentElement.classList.toggle('dark', escuro)
}

/** Guardar fora do navegador falha em janela privada, e o tema vale só nesta visita. */
export function escolherTema(tema: Tema): void {
  try {
    if (tema === 'sistema') {
      localStorage.removeItem(CHAVE)
    } else {
      localStorage.setItem(CHAVE, tema)
    }
  } catch {
    // Sem o armazenamento, o tema ainda vale até recarregar
  }
  aplicarTema(tema)
}

/** Enquanto o tema é o do sistema, acompanha a troca de modo do sistema operacional. */
export function acompanharOSistema(): void {
  escuroDoSistema().addEventListener('change', () => {
    if (lerTema() === 'sistema') {
      aplicarTema('sistema')
    }
  })
}
