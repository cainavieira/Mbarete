import { Navigate, Route, Routes } from 'react-router'
import { Casca } from '@/componentes/Casca'
import { Toaster } from '@/componentes/ui/sonner'
import { Acompanhamento } from '@/paginas/Acompanhamento'
import { Conta } from '@/paginas/Conta'
import { Entrada } from '@/paginas/Entrada'
import { Fases } from '@/paginas/Fases'
import { NovaFase } from '@/paginas/NovaFase'
import { RegistrarPeso } from '@/paginas/RegistrarPeso'
import { TelaPorFazer } from '@/paginas/TelaPorFazer'

/**
 * As rotas, declaradas (ADR-0024). O servidor entrega o index.html para
 * qualquer caminho, então recarregar em /fases funciona (ADR-0007).
 */
export function App() {
  return (
    <>
      <Routes>
        <Route path="/entrar" element={<Entrada />} />
        <Route element={<Casca />}>
          <Route index element={<Acompanhamento />} />
          <Route path="registrar" element={<RegistrarPeso />} />
          <Route path="historico" element={<TelaPorFazer titulo="Histórico" />} />
          <Route path="fases" element={<Fases />} />
          <Route path="fases/nova" element={<NovaFase />} />
          <Route path="fases/:id" element={<TelaPorFazer titulo="Fase" />} />
          <Route path="importar" element={<TelaPorFazer titulo="Importar e exportar" />} />
          <Route path="conta" element={<Conta />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      {/* No alto, para não cobrir a barra de navegação do celular */}
      <Toaster position="top-center" />
    </>
  )
}
