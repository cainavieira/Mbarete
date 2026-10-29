// O jsdom não tem ResizeObserver, e o interruptor e as opções do Radix medem o
// próprio tamanho com ele. Nos testes, medir não importa: só existir.
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
}
