# ADR-0013 Alembic aplicado por comando explícito

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O banco de produção tem estado: já contém histórico de meses. Mudança de
esquema precisa transformar o banco do estado antigo no novo sem perder dado,
porque perder histórico é a única falha irreversível do sistema (RNF-04).

Migrações têm ordem, precisam saber quais já rodaram em qual banco, e a
RNF-08 exige testar a restauração do backup a cada mudança de esquema.

## Decisão

Alembic, com a aplicação feita por comando (`alembic upgrade head`) executado
como passo do script de deploy (ADR-0031). Nunca automaticamente na subida do
contêiner.

## Consequências

O passo de migração é observável e interrompível, e cabe entre ele e a subida
da aplicação o teste de restauração que a RNF-08 pede.

O rascunho gerado pelo autogenerate é sempre revisado antes de aplicado. Ele
erra em renomeação: vê uma coluna que desapareceu e outra que apareceu, e gera
remoção mais criação, o que apagaria dado. Renomear coluna se corrige à mão.

As funções de reversão são escritas, mas não são plano de recuperação: reversão
que remove coluna apaga dado. O plano de recuperação é o backup. A reversão
serve para desfazer erro em desenvolvimento.

## Alternativa recusada: migração automática na subida do contêiner

É conveniente e comum. Recusada porque um deploy que falha no meio da migração
deixa o banco num estado que ninguém escolheu e que não é fácil de identificar,
e porque tornaria impossível encaixar o teste de restauração antes da aplicação
subir.
