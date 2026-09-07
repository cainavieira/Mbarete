# ADR-0023 TanStack Query para estado de servidor

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

Há dois tipos de estado no frontend, e tratá-los igual é o erro comum. Estado
de tela só existe no navegador: qual aba está aberta, se o diálogo apareceu, o
que está digitado. Estado de servidor é **cópia** de algo que mora no banco, e
cópia envelhece.

O caso concreto vem da RN-17: ao gravar o peso do dia (UC-02), mudam de uma vez
o peso semanal, as três variações, o veredito e a barra de progresso.

## Decisão

TanStack Query cuida do estado de servidor. `useState` cuida do estado de tela.
Nenhuma biblioteca de estado global.

## Consequências

Depois de gravar, invalida-se uma chave e tudo que depende dela se atualiza. Com
estado escrito à mão seria preciso lembrar de rebuscar cada consumidor, e
esquecer uma invalidação é o defeito que ninguém vê, porque a tela mostra
número plausível e velho.

Vêm de fábrica: cache por chave, controle de dado velho, nova busca ao voltar o
foco da janela, e eliminação de buscas simultâneas idênticas.

Consequência de desenho: os carregadores de rota do React Router responderiam à
mesma pergunta, e é por isso que o modo framework foi recusado (ADR-0024).

## Alternativa recusada: fetch com useState

Funciona com cinco telas e ensina o que a biblioteca faz. Recusado pelo
argumento da invalidação em cascata do contexto, que é onde o erro é silencioso.

## Alternativa recusada: Redux ou Zustand

Servem para estado de tela compartilhado por muitos componentes, e o estado de
tela deste sistema é quase nada. Usá-los para estado de servidor significa
escrever o cache à mão dentro de uma loja.
