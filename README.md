# Acompanhamento de peso

Sistema de acompanhamento de peso para ciclos de treino. Cada pessoa registra o
próprio peso, declara em que fase está (cutting, bulking ou manutenção) com uma
taxa alvo por semana, e o sistema responde a única pergunta que importa: **estou
no ritmo do meu ciclo?**

A unidade de decisão é o **peso semanal**, não o peso do dia. O peso diário
existe para alimentar a média; é o semanal que recalcula o alvo, mede o
progresso e dá o veredito. Peso corporal oscila um a dois quilos entre um dia e
outro, e decisão tomada em cima disso é decisão tomada em cima de ruído.

## Estado atual

**A especificação está completa. Não há código de aplicação ainda.**

Isso é deliberado. O projeto começou como um script que lia um CSV e tirava
média a cada sete linhas, e a primeira tentativa foi escolher tecnologia antes de
modelar requisito. Isso foi desfeito: o script original foi preservado, tudo o
mais foi apagado, e o levantamento recomeçou por atores, regras e casos de uso.

O que existe hoje:

- `especificacao/`: 64 regras de negócio, 46 requisitos funcionais, 14 casos de
  uso, 29 requisitos não funcionais, 36 ADR, arquitetura e convenções.
- `backend/pytho.py`: o script de origem, inalterado, preservado como ponto de
  partida.

## Onde ler o quê

| Se você quer saber | Abra |
|:--|:--|
| quem usa o sistema | [especificacao/atores.md](especificacao/atores.md) |
| o que é permitido, recusado e por quê | [especificacao/regras-de-negocio.md](especificacao/regras-de-negocio.md) |
| o que o sistema faz | [especificacao/requisitos-funcionais.md](especificacao/requisitos-funcionais.md) |
| como cada interação acontece, inclusive quando dá errado | [especificacao/casos-de-uso.md](especificacao/casos-de-uso.md) |
| o que o sistema tem que ser (custo, backup, privacidade) | [especificacao/requisitos-nao-funcionais.md](especificacao/requisitos-nao-funcionais.md) |
| por que cada tecnologia foi escolhida, e o que perdeu | [especificacao/adr/](especificacao/adr/) |
| o que roda onde, e por onde a requisição passa | [especificacao/arquitetura.md](especificacao/arquitetura.md) |
| como se escreve código aqui | [convenções do backend](especificacao/convencoes-backend.md) e [do frontend](especificacao/convencoes-frontend.md) |
| o contrato da API | [especificacao/endpoints.md](especificacao/endpoints.md) |
| como o trabalho entra no repositório | [especificacao/fluxo-de-trabalho.md](especificacao/fluxo-de-trabalho.md) |
| **como começar a implementar: armadilhas, recusas e o desenho do esquema** | [instrucao.md](instrucao.md) |

Cada documento cita os outros por identificador (`RN-15`, `RF-07`, `UC-02`,
`ADR-0009`) em vez de repetir texto. Identificador é estável: número aposentado
nunca volta.

A seção de **decisões recusadas** do documento de regras e a **alternativa
recusada** de cada ADR não são enfeite: são a parte que impede a mesma discussão
de recomeçar daqui a seis meses com o argumento já gasto.

## Stack

Decidida com alternativa registrada em cada caso. Um ADR por decisão.

- **Backend:** Python com FastAPI, SQLAlchemy 2.0, Pydantic, Alembic, Authlib
- **Frontend:** React com Vite e TypeScript, TanStack Query, React Router 8,
  Tailwind com shadcn/ui, React Hook Form com Zod, Recharts
- **Banco:** PostgreSQL
- **Identidade:** Google, sem senha própria, entrada por convite
- **Hospedagem:** servidor próprio, exposto por Cloudflare Tunnel
- **Qualidade:** ruff, mypy, pytest, Vitest, Playwright, tudo em integração
  contínua

## O que falta

O código. O ponto de partida para implementar está no
[instrucao.md](instrucao.md): ordem de leitura da especificação, o que já foi
recusado, armadilhas conhecidas, e o desenho do esquema e da camada de regras.
A ordem de construção, em macro tarefas, fica num documento de trabalho local.

O escopo da primeira versão é **tudo o que a especificação descreve**. Foi
proposta uma versão reduzida e recusada de propósito: import e export de CSV,
cintura e barra de progresso ficam dentro.
