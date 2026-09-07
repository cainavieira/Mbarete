# ADR-0033 Sobreposição de fase impedida no banco e na aplicação

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

A RN-19 proíbe sobreposição entre fases do mesmo usuário. Depois que período sem
fase passou a ser estado válido (RN-20), essa proibição virou o **único**
invariante estrutural das fases: não há mais obrigação de cobrir toda data, nem
de existir fase corrente.

Quatro caminhos diferentes podem criar sobreposição: criar fase (UC-05), editar
a data de início, editar a data de fim (UC-06) e encerrar fase (UC-14).

O Postgres consegue impor a regra sozinho, com restrição de exclusão sobre
intervalo de datas, o que exige a extensão `btree_gist`.

## Decisão

Nos dois lugares, com papéis diferentes.

**A aplicação verifica antes de gravar**, para devolver `FASE_SOBREPOSTA` com
campo, código e mensagem legível, conforme o formato de erro do
`convencoes-backend.md`, em vez de propagar erro cru do banco.

**O banco impõe por restrição de exclusão**, habilitada na primeira migração:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE fase ADD CONSTRAINT fase_sem_sobreposicao
  EXCLUDE USING gist (
    usuario_id WITH =,
    daterange(data_inicio, data_fim, '[]') WITH &&
  );
```

Intervalo com `data_fim` nula é aberto à direita, e o `daterange` trata isso
sozinho, sem caso especial no SQL.

## Consequências

Não é redundância, porque os dois fazem trabalhos diferentes. Sem a verificação
na aplicação, o usuário recebe erro de violação de restrição, sem campo e sem
código do catálogo. Sem a restrição no banco, um dos quatro caminhos vai passar
sem validação em algum momento, e estado inválido de fase é do tipo que não gera
erro: ele só produz número errado na tela, com aparência de certo.

É o mesmo raciocínio do ADR-0004, que pôs a RN-01 como chave única no banco em
vez de deixá-la só em código.

A primeira migração passa a depender de uma extensão. O `btree_gist` vem
distribuído com o Postgres e não é pacote externo, então isso não acrescenta
dependência de instalação, apenas uma linha de DDL.

## Alternativa recusada: só na aplicação

Dispensaria a extensão e concentraria a regra num lugar. Recusada pelos quatro
caminhos: validação espalhada por quatro operações é exatamente onde o
esquecimento de um caso vira estado inválido permanente, e ninguém descobre pelo
sintoma.

## Alternativa recusada: só no banco

Suficiente para a integridade e insuficiente para o usuário. A mensagem que
chega é violação de restrição, sem campo, sem código e sem texto legível, o que
contraria o formato de erro que o `convencoes-backend.md` fixa para todas as
respostas.
