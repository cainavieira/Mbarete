/** Entrega o arquivo para o navegador salvar, como o link de download faria. */
export function salvarArquivo(conteudo: Blob, nome: string): void {
  const endereco = URL.createObjectURL(conteudo)
  const link = document.createElement('a')
  link.href = endereco
  link.download = nome
  link.click()
  URL.revokeObjectURL(endereco)
}
