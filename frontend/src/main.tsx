import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { ErroDaApi } from '@/api/cliente'
import { App } from '@/App'
import { acompanharOSistema } from '@/lib/tema'
import './index.css'

acompanharOSistema()

const consultas = new QueryClient({
  defaultOptions: {
    queries: {
      // A recusa do backend não muda repetindo; só a falha de rede ou do servidor tenta de novo
      retry: (tentativas, erro) =>
        !(erro instanceof ErroDaApi && erro.status < 500) && tentativas < 2,
    },
  },
})

const raiz = document.getElementById('root')
if (!raiz) {
  throw new Error('O index.html não tem o elemento #root.')
}

createRoot(raiz).render(
  <StrictMode>
    <QueryClientProvider client={consultas}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
