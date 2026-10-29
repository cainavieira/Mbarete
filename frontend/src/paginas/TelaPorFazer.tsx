/** O lugar de uma tela da Etapa 9, para a navegação da casca já levar a algum lugar. */
export function TelaPorFazer({ titulo }: { titulo: string }) {
  return (
    <main className="mx-auto flex max-w-2xl flex-col gap-2 px-4 pt-6 md:px-8">
      <h1 className="text-2xl leading-tight font-bold">{titulo}</h1>
      <p className="text-muted-foreground">Esta tela chega na Etapa 9.</p>
    </main>
  )
}
