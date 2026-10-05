# ADR-0017 Peso como BigDecimal e numeric(4,1)

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

A RN-02 fixa peso em quilogramas com uma casa decimal, faixa de 20,0 a 400,0
(RN-03). O peso semanal é média de até 7 valores (RN-15), e o veredito compara
a taxa real com a taxa alvo dentro de uma tolerância de 20% (RN-38).

Número de ponto flutuante binário não representa 0,1 exatamente.

## Decisão

`BigDecimal` no Java, `numeric(4,1)` no Postgres. A cintura segue a mesma regra.

## Consequências

A média de sete pesos não produz valores como `82.40000000000001`, e a
comparação com a tolerância da RN-38 depende da regra e não de erro de
arredondamento.

Cuidado que morde na primeira vez: `BigDecimal` só é exato se construído a
partir de texto, de inteiro ou por `BigDecimal.valueOf`. `new BigDecimal("82.4")`
vale 82,4; `new BigDecimal(82.4)` carrega o erro do ponto flutuante para dentro
e vale 82,400000000000005684341886080801486968994140625.

Consequência ligada: SQLite não reproduz esse arredondamento, o que reforça a
recusa dele no ADR-0004 e no ADR-0020.

## Alternativa recusada: double

Mais simples e sem conversão. Recusado pelo motivo do contexto: o erro não
aparece no valor gravado, aparece na média e na comparação, que é onde o
sistema toma decisão.

## Alternativa recusada: inteiro em decigramas

Guardar 824 para 82,4 kg também é exato, e é o que sistema financeiro faz.
Recusado porque obriga a converter em toda leitura e escrita, em troca de
nenhuma vantagem sobre `numeric` neste contexto.
