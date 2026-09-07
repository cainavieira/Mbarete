# ADR-0029 Recharts para o gráfico

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O gráfico do UC-08 tem uma série e uma linha: pesos diários como pontos, peso
semanal como linha. E tem uma exigência que vem da RN-09: dia sem registro é
ausência de dado, e o sistema não repete nem interpola.

Consequência disso no desenho: a linha do peso semanal precisa aparecer
**interrompida** onde não houve dado suficiente, em vez de ligar dois pontos
distantes com uma reta que sugere uma trajetória que não aconteceu.

Além disso, a fronteira entre fases (ADR do domínio, RN-24) é informação que
faz sentido marcar sobre o gráfico.

## Decisão

Recharts.

## Consequências

Valor nulo quebra a linha de fábrica, que é exatamente o comportamento que a
RN-09 exige. Não é configuração nem contorno.

Marcar a fronteira entre fases usa um componente pronto de linha de referência.

É declarativo em React: o gráfico é composto de componentes, não desenhado por
chamadas imperativas.

## Alternativa recusada: Chart.js

Desenha em canvas e se configura por objeto de opções. Recusado por ficar
desconfortável assim que se quer anotação sobre o gráfico, que é o caso da
fronteira de fase.

## Alternativa recusada: D3 com visx

Dá controle total sobre cada elemento. Recusado pelo custo: muito código para um
gráfico de uma série e uma linha, e o controle extra não tem uso previsto aqui.
