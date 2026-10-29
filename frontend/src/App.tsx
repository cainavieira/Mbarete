import { Navigate, Route, Routes } from 'react-router'
import { Casca } from '@/componentes/Casca'
import { Toaster } from '@/componentes/ui/sonner'
import { Acompanhamento } from '@/paginas/Acompanhamento'
import { Conta } from '@/paginas/Conta'
import { EditarFase } from '@/paginas/EditarFase'
import { EditarRegistro } from '@/paginas/EditarRegistro'
import { Entrada } from '@/paginas/Entrada'
import { Fases } from '@/paginas/Fases'
import { Historico } from '@/paginas/Historico'
import { Importar } from '@/paginas/Importar'
import { NovaFase } from '@/paginas/NovaFase'
import { RegistrarPeso } from '@/paginas/RegistrarPeso'

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
          <Route path="historico" element={<Historico />} />
          <Route path="historico/:data" element={<EditarRegistro />} />
          <Route path="fases" element={<Fases />} />
          <Route path="fases/nova" element={<NovaFase />} />
          <Route path="fases/:id" element={<EditarFase />} />
          <Route path="importar" element={<Importar />} />
          <Route path="conta" element={<Conta />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      {/* No alto, para não cobrir a barra de navegação do celular */}
      <Toaster position="top-center" />
    </>
  )
}
