# ADR-0026 React Hook Form com Zod

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O sistema tem poucos formulários: registro de peso, criação e edição de fase, e
importação. O frontend valida forma e não regra (ADR-0025).

## Decisão

React Hook Form para o formulário, Zod para declarar a forma dos campos.

## Consequências

A contabilidade chata fica com a biblioteca: qual campo foi tocado, qual tem
erro, se está enviando. É a parte que sai malfeita quando escrita à mão, e é o
motivo real da escolha, não desempenho.

O Zod declara a forma uma vez e produz o tipo TypeScript e a verificação em
tempo de execução da mesma declaração.

Os valores dos campos ficam fora do estado do React, então digitar não
re-renderiza o formulário inteiro. Com formulários deste tamanho isso é
irrelevante, e está registrado para não ser confundido com a justificativa.

## Alternativa recusada: formulário controlado à mão

Três formulários é pouco o suficiente para justificar, e ensinaria campos
controlados. Recusado por deixar a contabilidade de erro e envio na mão de quem
escreve, que é justamente onde ela é feita pela metade.
