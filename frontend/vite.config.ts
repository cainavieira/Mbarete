/// <reference types="vitest/config" />
import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// O servidor de desenvolvimento fica na 5175, a porta do endereço de retorno
// registrado no console do Google. O proxy leva /api à api na máquina
// (ADR-0037) e mantém o Host, porque o endereço de retorno sai dele.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, 'src') },
  },
  server: {
    port: 5175,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: false },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['src/testes/preparar.ts'],
    // Um fuso atrás do UTC, para a data do dispositivo diferir da do UTC à noite
    env: { TZ: 'America/Sao_Paulo' },
  },
})
