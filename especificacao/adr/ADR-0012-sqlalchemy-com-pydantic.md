# ADR-0012 SQLAlchemy 2.0 com Pydantic em camada separada

- **Data:** 07-09-2026
- **Situação:** superado por ADR-0036

## Contexto

O banco é relacional (ADR-0004) e a API é FastAPI com Pydantic (ADR-0001).
Falta decidir se tabela e contrato de API compartilham a mesma declaração.

Eles divergem de fato. O registro guarda identificador interno, identificador
de usuário e instante de criação, e nenhum dos três sai na API: o usuário está
implícito na sessão, e expor identificador sequencial permite contar quantos
registros o sistema tem. Na direção contrária, o peso semanal **sai** na API e
não existe em coluna nenhuma, porque é calculado (RN-15).

## Decisão

SQLAlchemy 2.0 descreve as tabelas. Modelos Pydantic separados descrevem
entrada e saída de cada rota.

## Consequências

Cada declaração afirma apenas o que é verdade sobre ela. Nenhum campo precisa
ser marcado como opcional só porque é opcional em um dos três papéis.

Existe conversão explícita entre as duas camadas. É código a mais, e é o preço
de não vazar estrutura de banco para dentro do contrato público.

Os modelos do SQLAlchemy tornam possível o autogenerate do Alembic (ADR-0013).

## Alternativa recusada: SQLModel

Promete um modelo servindo de tabela e de esquema de API, e é do mesmo autor do
FastAPI. Recusado porque a fusão é justamente onde dói: a divergência descrita
no contexto não desaparece, ela se transforma em campos opcionais e classes
filhas espalhados pelo modelo.

## Alternativa recusada: SQL puro com psycopg

Defensável, porque o sistema tem poucas consultas. Recusado pela migração: sem
modelos no código, não existe esquema desejado para o Alembic comparar com o
banco, e todo DDL passaria a ser escrito e sequenciado à mão, incluindo o
controle de quais migrações já rodaram em qual banco.
