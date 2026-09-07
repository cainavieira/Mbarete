# ADR-0018 Data pura, sem hora e sem fuso

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

A RN-01 permite um registro por usuário por data, e a RN-15 calcula o peso
semanal sobre uma janela de 7 dias de calendário. As duas dependem de alguém
dizer qual dia é hoje.

Se o servidor operar em UTC e a pessoa registrar às 23h30 em Brasília, o
registro cai no dia seguinte e a janela fecha errado.

## Decisão

O registro guarda **data pura**, sem hora e sem fuso. O dia corrente vem do
dispositivo do usuário, enviado junto com a requisição (RN-04).

## Consequências

Todo cálculo de janela vira aritmética de datas, sem conversão de fuso em
lugar nenhum do sistema.

Quem viajar registra o dia que é no lugar onde está, e o sistema não precisa
saber que algo mudou.

O cliente pode enviar data errada com relógio errado. A defesa já existe: data
futura é recusada (RN-05). Data passada errada é indistinguível de registro
retroativo legítimo, que é livre (RN-06), e isso é aceito.

O dado não registra a que hora a pessoa se pesou. Aceito: a RN-01 já trata
pesagem comparável como uma por dia.

## Alternativa recusada: instante em UTC com conversão no servidor

Guarda precisão que ninguém usa e cria o defeito de fronteira de dia descrito
no contexto, além de exigir saber o fuso de cada usuário.

## Alternativa recusada: fuso fixo America/Sao_Paulo

Resolve o caso comum e quebra para quem viajar, além de embutir no esquema uma
suposição sobre onde os usuários estão.
