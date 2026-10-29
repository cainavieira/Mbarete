import { Navigate, Route, Routes } from 'react-router'
import { Casca } from '@/componentes/Casca'
import { Conta } from '@/paginas/Conta'
import { Entrada } from '@/paginas/Entrada'
import { TelaPorFazer } from '@/paginas/TelaPorFazer'

/**
 * As rotas, declaradas (ADR-0024). O servidor entrega o index.html para
 * qualquer caminho, então recarregar em /fases funciona (ADR-0007).
 */
export function App() {
  return (
    <Routes>
      <Route path="/entrar" element={<Entrada />} />
      <Route element={<Casca />}>
        <Route index element={<TelaPorFazer titulo="Acompanhamento" />} />
        <Route path="registrar" element={<TelaPorFazer titulo="Registrar peso" />} />
        <Route path="historico" element={<TelaPorFazer titulo="Histórico" />} />
        <Route path="fases" element={<TelaPorFazer titulo="Fases" />} />
        <Route path="conta" element={<Conta />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
