# ADR-0036 Spring Data JPA, com entidade separada do DTO

- **Data:** 05-10-2026
- **Situação:** aceito
- **Supera:** ADR-0012

## Contexto

O backend passou a ser Spring Boot (ADR-0035). Falta decidir como o código
conversa com o Postgres (ADR-0004), e se tabela e contrato de API compartilham
a mesma declaração.

Eles divergem de fato, pelo motivo que o ADR-0012 registrou. O registro guarda
identificador interno, identificador de usuário e instante de criação, e
nenhum dos três sai na API: o usuário está implícito na autenticação, e expor
identificador sequencial permite contar quantos registros o sistema tem. Na
direção contrária, o peso semanal sai na API e não existe em coluna nenhuma,
porque é calculado (RN-15).

A premissa que pesava contra SQL puro no ADR-0012 mudou. O Alembic gerava o
rascunho da migração comparando os modelos com o banco, e sem modelo não havia
o que comparar. A migração agora é SQL escrito à mão em qualquer opção, então
esse argumento não decide mais, e a escolha passa a ser de aprendizado.

## Decisão

Spring Data JPA, com o Hibernate por baixo, descreve as tabelas como
entidades. Records Java separados descrevem a entrada e a saída de cada rota, e
uma camada de mapeamento escrita à mão converte entre os dois.

Duas regras fazem parte da decisão. O esquema pertence às migrações: o
Hibernate confere na subida se as entidades batem com as tabelas, e nunca cria
nem altera tabela. E o banco só é consultado dentro do service, nunca enquanto
a resposta é montada.

## Consequências

Cada declaração afirma só o que é verdade sobre ela, como no ADR-0012. A
conversão explícita é código a mais, e é o preço de não vazar estrutura de
banco para o contrato público.

O repositório é uma interface, e o Spring Data gera a consulta a partir do nome
do método. É o centro do que se usa em Spring, que é o motivo do ADR-0035.

O JPA traz duas armadilhas, e as duas produzem consulta que ninguém escreveu:
o carregamento preguiçoso, em que ler um campo de relação dispara uma consulta,
e o N+1, uma consulta para a lista e mais uma por item. O N+1 é o antipadrão de
consulta dentro de laço do `convencoes-backend.md`, acontecendo sem laço
visível. Como o banco só é consultado dentro do service, o carregamento
preguiçoso fora de lugar aparece como erro durante o desenvolvimento, em vez de
consulta escondida.

A conferência da subida pega entidade que divergiu da tabela, mas não confere
restrição. A chave única da RN-01 e a exclusão de sobreposição do ADR-0033
existem só no SQL das migrações.

## Alternativa recusada: a entidade como contrato de API

Devolver a entidade direto na rota dispensa o mapeamento. Recusada pelo mesmo
motivo que o ADR-0012 recusou o SQLModel: a divergência do contexto não
desaparece, ela vira campo escondido por anotação e campo opcional espalhado.

## Alternativa recusada: Spring Data JDBC ou JdbcClient

SQL mais explícito, sem carregamento preguiçoso nem sessão de persistência, e
defensável no volume deste sistema (RNF-25). Recusado porque o JPA é o que se
encontra em quase todo projeto Spring, e aprender Spring é o motivo da troca.

## Alternativa recusada: jOOQ

SQL com tipo conferido pelo compilador. Poderoso e de nicho, e exige gerar
código a partir do esquema do banco a cada migração.

## Alternativa recusada: MapStruct

Gera o código de mapeamento a partir de interfaces anotadas, e é comum em
projeto Spring. Recusado porque, com poucas entidades, o mapeamento à mão é
curto e visível, e um gerador a mais seria outra coisa para aprender junto.
