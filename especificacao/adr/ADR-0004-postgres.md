# ADR-0004 Postgres como banco de dados

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O script de origem lia um CSV, e o primeiro desenho do sistema mantinha CSV
como armazenamento. O sistema tem múltiplos usuários com dados isolados
(RN-50), restrição de um registro por usuário por data (RN-01) e peso com uma
casa decimal exata (RN-02, ADR-0017).

## Decisão

PostgreSQL.

## Consequências

A restrição da RN-01 passa a ser garantida pelo banco, por chave única sobre
usuário e data, em vez de por código que precisa ser escrito e testado.

`numeric(4,1)` guarda o peso com exatidão decimal (ADR-0017).

Aparece a necessidade de migração de esquema versionada (ADR-0013) e de dump
agendado com restauração testada (RNF-05 a RNF-09).

## Alternativa recusada: CSV

Era o desenho inicial e não sobrevive ao multiusuário: sem transação, escrita
concorrente corrompe o arquivo, e a restrição de um registro por dia teria que
ser reimplementada a cada leitura.

## Alternativa recusada: SQLite

Tecnicamente adequado à carga (RNF-25) e tornaria o backup quase trivial, já
que é um arquivo. Recusado por uma diferença concreta: SQLite guarda número
como ponto flutuante e não reproduz o arredondamento de `numeric(4,1)`, o que
colidiria com a RN-02. Consequência ligada: SQLite também não serve como
substituto do Postgres em teste (ADR-0020).
