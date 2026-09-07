# ADR-0003 Monorepo com backend, frontend e especificação

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O sistema tem duas camadas que conversam por um contrato de API, e uma pasta de
especificação que descreve as duas.

## Decisão

Um repositório só, com `backend/`, `frontend/` e `especificacao/` lado a lado.

## Consequências

Mudança que atravessa as camadas cabe num commit. Acrescentar um campo mexe no
modelo, na rota, no cliente HTTP e na tela, e as quatro alterações entram
juntas ou não entram.

A especificação fica versionada ao lado do código que ela descreve, que é o
propósito de tê-la escrito antes.

O histórico mistura mudança de backend e de frontend. Aceito: o projeto tem uma
pessoa, e o filtro por caminho resolve quando fizer falta.

## Alternativa recusada: dois repositórios

Separar paga quando os dois lados têm ciclo de vida independente, e aqui eles
têm o mesmo. Custaria dois pull requests por mudança de contrato, com ordem
obrigatória entre eles e uma janela no meio em que o sistema está quebrado.
