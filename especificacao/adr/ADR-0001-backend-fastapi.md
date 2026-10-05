# ADR-0001 Backend em FastAPI

- **Data:** 07-09-2026
- **Situação:** superado por ADR-0035

## Contexto

O backend é consumido exclusivamente por um frontend React (ADR-0002), então
não há renderização de página no servidor. As regras de negócio são em boa
parte validação de valor: faixa de peso (RN-03), faixa de cintura (RN-11),
data não futura (RN-05), unidade da taxa alvo (RN-29).

O projeto também precisa de um documento de endpoints que não envelheça.

## Decisão

FastAPI, com Pydantic para validação de entrada e saída.

## Consequências

As regras de valor são declaradas no tipo, em vez de escritas como verificação
manual em cada rota.

A especificação OpenAPI é gerada a partir do código que roda, e é ela que serve
de documento de endpoints. Documento gerado não divide a verdade com o código.
Essa mesma especificação alimenta a geração dos tipos do frontend (ADR-0027).

Não existe painel administrativo pronto. Isso é aceito porque o AT-02 não tem
interface no sistema.

## Alternativa recusada: Django

Traz pronto o que aqui não se usa: motor de templates, renderização de página,
sistema de autenticação com senha (recusado no ADR-0008) e painel
administrativo. Entregaria de graça as migrações e um lugar para olhar o banco,
e ainda assim mais da metade do que ele traz ficaria sem uso.

## Alternativa recusada: Flask

Não valida entrada de fábrica e não gera especificação OpenAPI, então as duas
coisas que mais pesam a favor do FastAPI aqui teriam que ser montadas à mão.
