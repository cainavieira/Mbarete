# Acompanhamento de peso

Sistema de acompanhamento de peso para ciclos de treino (cutting, bulking,
manutenção). A pergunta que o produto responde é "estou no ritmo do meu ciclo?",
e a unidade de decisão é o **peso semanal**, nunca o peso de um dia.

**Estado: a especificação está completa e não existe código de aplicação.** O
script de origem em `backend/pytho.py` está preservado e inalterado.

## Onde está a verdade

`especificacao/` é a fonte. Nenhum comportamento se decide fora dela.

Os identificadores são estáveis e se citam entre documentos: `RN-15`, `RF-07`,
`RNF-22`, `UC-02`, `ADR-0009`. **Número aposentado nunca volta**, e cada
documento tem seção listando os seus aposentados com o motivo.

## Antes de propor qualquer coisa

`especificacao/adr/` tem 39 decisões, cada uma com a alternativa que perdeu e o
motivo. A seção **Decisões recusadas** do `regras-de-negocio.md` faz o mesmo
para regra de produto.

Se você sentir vontade de propor SQLite, JWT, camadas de
Clean Architecture, segurança por linha do Postgres, IMC ou sequência de dias
registrando, a recusa já está escrita com o motivo. Decisão nova é do
interessado, nunca sua.

## O padrão que achou tudo

Dezesseis defeitos de coerência foram encontrados nesta especificação, e
**nenhum apareceu lendo um documento sozinho.** Todos apareceram quando dois
documentos ficaram lado a lado: duas regras corretas separadas produzindo um
sistema errado juntas, uma condição que parecia suficiente e não era, uma porta
descrita sem maçaneta.

Consequência prática, detalhada no `fluxo-de-trabalho.md`: ao mexer numa regra,
leia todos os documentos que a citam, contra ela. Ler só o que você mudou não
acha nada.

## Como trabalhar aqui

- Mostre o texto antes de gravar. Commit depois da mensagem aprovada. **Push é
  sempre dele.**
- Não busque na web sem ele pedir.
- Sem travessão, em documento, commit ou conversa.
- Mensagem de commit sem acento; documento com acento.
- `especificacao/` é versionada de propósito. `.env`, `.venv/`, `node_modules/`
  e `.claude/` não entram.
