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

Além disso, o gráfico tem três estados a distinguir, e não dois: trecho dentro
de uma fase, trecho sem fase alguma (RN-20, RN-66), e trecho sem dado. O início
e o fim de cada fase são informação que faz sentido marcar sobre a linha.

## Decisão

Recharts.

## Consequências

Valor nulo quebra a linha de fábrica, que é exatamente o comportamento que a
RN-09 exige. Não é configuração nem contorno.

Marcar início e fim de fase usa um componente pronto de linha de referência, e
destacar o trecho sem fase usa área de referência, também pronta.

É declarativo em React: o gráfico é composto de componentes, não desenhado por
chamadas imperativas.

## Alternativa recusada: Chart.js

Desenha em canvas e se configura por objeto de opções. Recusado por ficar
desconfortável assim que se quer anotação sobre o gráfico, que é o caso das
marcas de início e fim de fase e do destaque do trecho sem fase.

## Alternativa recusada: D3 com visx

Dá controle total sobre cada elemento. Recusado pelo custo: muito código para um
gráfico de uma série e uma linha, e o controle extra não tem uso previsto aqui.
