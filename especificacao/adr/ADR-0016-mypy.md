# ADR-0016 mypy, estrito apenas no módulo de regras

- **Data:** 07-09-2026
- **Situação:** superado por ADR-0042

## Contexto

FastAPI e Pydantic usam as anotações de tipo em tempo de execução, então elas
vão existir de qualquer forma. Ligar um verificador aproveita trabalho já
feito.

O caso concreto que justifica: pela RN-16, a janela com menos de 4 registros
**não tem** peso semanal, então a função que o calcula devolve `Decimal | None`,
e todo consumidor precisa tratar o vazio.

## Decisão

`mypy` no modo padrão para o projeto, e modo estrito apenas no módulo de regras
de agregação. Executado na integração contínua (ADR-0030).

## Consequências

O verificador pega quatro classes de defeito sem rodar o código: `None` não
tratado, tipo trocado (o que protege a decisão do ADR-0017 de se desfazer
sozinha), nome de campo errado, e chamada que ficou atrás de uma mudança de
assinatura.

Ele não confere lógica. Uma função declarada `-> Decimal` que devolve a soma em
vez da média passa sem reclamação, e é por isso que o teste do ADR-0020 existe
em paralelo e não em substituição.

Estrito só no núcleo porque o módulo de regras é código próprio com tipos
simples, onde o estrito sai de graça.

## Alternativa recusada: nenhum verificador

Deixaria as anotações como decoração, verificadas apenas quando o Pydantic as
usa em tempo de execução, ou seja, na borda e não no núcleo.

## Alternativa recusada: estrito no projeto inteiro

Código de integração lida com tipos de biblioteca de terceiro que não estão
completamente anotados, e o estrito ali produz reclamação que se silencia com
comentário de exceção. Comentário de exceção espalhado esvazia a ferramenta.
