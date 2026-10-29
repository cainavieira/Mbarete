import { SeletorTema } from '@/componentes/SeletorTema'

/** A conta (UC-12). Por enquanto, só o tema (RF-51); o resto chega na T-32. */
export function Conta() {
  return (
    <main className="mx-auto flex max-w-2xl flex-col gap-6 px-4 pt-6 md:px-8">
      <h1 className="text-2xl leading-tight font-bold">Conta</h1>
      <section aria-labelledby="titulo-tema" className="flex flex-col gap-2">
        <h2 id="titulo-tema" className="text-lg font-bold">
          Tema
        </h2>
        <SeletorTema />
        <p className="text-xs text-muted-foreground">A escolha fica neste navegador.</p>
      </section>
    </main>
  )
}
