# ADR-0027 Tipos do frontend gerados da especificação OpenAPI

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O FastAPI publica a especificação OpenAPI a partir do código que roda
(ADR-0001), e o frontend é TypeScript (ADR-0002). O contrato entre as duas
camadas é a única coisa que as duas precisam concordar.

## Decisão

Os tipos do contrato de API no frontend são **gerados** da especificação
OpenAPI, não escritos à mão.

## Consequências

O tipo não pode divergir do backend, porque não é uma segunda declaração da
mesma coisa, é uma tradução da primeira. Tipo escrito à mão divergiria na
primeira semana, e a divergência aparece em produção e não na compilação.

A geração é um passo que precisa ser executado quando o contrato muda, e o
resultado é versionado, para que quem clona o repositório compile sem subir o
backend.

Não substitui o Zod dos formulários (ADR-0026): são trabalhos diferentes. O tipo
gerado descreve o que a API troca; o Zod descreve o que o campo aceita enquanto
a pessoa digita.

## Alternativa recusada: tipos escritos à mão no frontend

Não depende de passo de geração e é o que a maioria faz no começo. Recusado por
criar duas declarações da mesma verdade, sem nada que force as duas a
concordarem.
