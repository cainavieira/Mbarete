# ADR-0025 Front valida forma, backend valida regra

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O sistema tem regras de valor escritas: faixa de peso de 20,0 a 400,0 (RN-03),
faixa de cintura (RN-11), data não futura (RN-05), coerência do peso alvo com o
tipo da fase (RN-65).

O backend precisa validar de qualquer forma, porque o navegador pode ser
contornado. A pergunta é se o frontend repete essas regras para dar resposta
imediata.

## Decisão

Divisão por natureza, não por conveniência.

- **O frontend valida forma**: campo obrigatório, é número, no máximo uma casa
  decimal, aceita vírgula (RN-62). É sobre digitação e precisa ser instantâneo.
- **O backend valida regra de negócio**: faixas, data futura, coerência de
  alvo. O frontend exibe a recusa que ele devolve.

## Consequências

Regra de negócio tem uma casa só. Nenhuma faixa aparece escrita em dois lugares,
então não existe o dia em que alguém muda uma e esquece a outra.

O usuário descobre que 500 kg é inválido depois de uma ida ao servidor. Na
prática isso é imperceptível, e o custo é aceito em troca de não duplicar regra.

A validação de forma continua sendo instantânea, que é onde a resposta imediata
tem valor real.

## Alternativa recusada: duplicar as faixas nos dois lados

Daria resposta imediata para tudo. Recusado porque a duplicação de regra de
negócio entre Pydantic e o esquema do frontend é garantia de divergência
futura, e divergência de faixa é o tipo de defeito que se manifesta como recusa
inexplicável para o usuário.
