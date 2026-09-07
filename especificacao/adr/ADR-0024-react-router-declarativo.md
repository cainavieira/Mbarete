# ADR-0024 React Router 8 em modo declarativo

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O sistema tem poucas telas: acompanhamento, registros, fases, importar, conta, e
a rota de retorno do OAuth. O estado de servidor já tem dono (ADR-0023). O build
e o desenvolvimento já estão desenhados (ADR-0007, ADR-0031).

Situação das versões em 07-09-2026: a 8.3.1 é a corrente, lançada em 17-06-2026;
a linha 7 está em manutenção; a linha 6 tem fim de vida declarado, com dez
avisos de segurança publicados em 2026, incluindo redirecionamento aberto
levando a XSS.

## Decisão

React Router 8, em modo declarativo. Import de `react-router`, e de
`react-router/dom` para o que é específico de navegador.

## Consequências

O `<BrowserRouter>` exige que o servidor entregue `index.html` para qualquer
caminho, que é o `try_files` do ADR-0007. Sem isso, recarregar em `/fases` dá
404.

O pacote `react-router-dom` não é usado: ele parou na linha 7 e não tem versão
8. O npm não avisa nada ao instalá-lo, porque não está marcado como obsoleto.

A versão 8 é somente ESM e exige Node 22.22+, React 19.2.7+ e Vite 7+.

Perde-se a divisão automática de código por rota. Com poucas telas, o pacote
inteiro é pequeno e isso não paga.

## Alternativa recusada: modo framework

É a recomendação oficial da equipe do React Router para projeto novo, inclusive
para SPA com `ssr: false`. Recusado por dois motivos próprios deste projeto: os
carregadores de rota dele buscam dado, e o estado de servidor já pertence ao
TanStack Query (ADR-0023), então haveria duas mecânicas respondendo à mesma
pergunta; e o plugin do Vite que ele instala toma conta de parte do build, que
já está desenhado de outra forma.

## Alternativa recusada: linha 6

Fim de vida declarado, sem atualização de segurança. Rodar hoje é pergunta de
segurança, não de preferência.

## Alternativa recusada: linha 7

Está em manutenção desde junho de 2026. Começar nela é nascer com uma migração
pendente sem ganho em troca.
