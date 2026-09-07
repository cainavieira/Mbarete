# ADR-0002 Frontend em React com Vite e TypeScript

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

Este ADR registra uma **premissa do projeto**, não uma escolha comparada. A
stack foi fixada pelo interessado antes do levantamento de requisitos, com
motivo declarado de aprendizado: são as duas tecnologias que ele quer praticar.

## Decisão

React com Vite e TypeScript.

## Consequências

Nenhuma alternativa foi avaliada, e este documento não finge que foram. Vue,
Svelte, Angular e renderização no servidor não entraram em comparação.

O compromisso com TypeScript torna proveitosa a geração de tipos a partir da
especificação OpenAPI (ADR-0027), que em JavaScript puro não teria efeito.

A escolha do Vite fixa o desenho de build e de desenvolvimento do ADR-0007 e
do ADR-0031.

## Observação

Registrar premissa como ADR é deliberado. Sem este arquivo, um leitor futuro
suporia que houve comparação e procuraria o critério que não existe.
