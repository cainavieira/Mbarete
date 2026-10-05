# ADR-0040 Flyway, com a migração antes da api e um ensaio antes do deploy

- **Data:** 05-10-2026
- **Situação:** aceito
- **Supera:** ADR-0013

## Contexto

O banco de produção tem estado: guarda histórico de meses, e perder histórico é
a única falha irreversível do sistema (RNF-04). Mudança de esquema precisa levar
o banco do estado antigo ao novo sem perder dado, em ordem, sabendo quais
migrações já rodaram em qual banco. A RNF-08 pede que a restauração do backup
seja testada a cada migração, antes de a migração chegar à produção.

O ADR-0013 resolvia isso com o Alembic, aplicado por comando no script de
deploy, nunca na subida da aplicação. Agora o backend é Spring (ADR-0035), o
esquema pertence às migrações escritas em SQL à mão (ADR-0036), a produção roda
em Kubernetes (ADR-0037) e o Postgres está no cluster (ADR-0039).

Fato conferido em 03-10-2026: com o Flyway no projeto, o Spring Boot aplica as
migrações sozinho toda vez que a aplicação sobe, a menos que isso seja
desligado.

## Decisão

Flyway, com as migrações escritas em SQL à mão e empacotadas na imagem da api.

A api nunca migra ao subir. A migração é um passo separado, que roda antes da
api nova: um initContainer, com a própria imagem da api em modo de migração,
aplica o que estiver pendente. Se ele falhar, a api nova não sobe.

Antes de tocar a produção, o deploy faz um ensaio num Job: restaura o
`pg_dump` mais recente num banco descartável, dentro do mesmo Postgres, e
aplica nele as migrações novas. Se a restauração ou a migração falhar, o deploy
para. O ensaio roda em todo deploy, com ou sem migração nova.

Nos testes, a migração roda na subida, sobre o banco que o teste cria do zero.

## Consequências

O Flyway faz o que o ADR-0013 pedia: aplica em ordem, anota no banco o que já
rodou, e roda cada migração numa transação, que no Postgres volta atrás inteira
se falhar. Ele também guarda a impressão digital de cada arquivo aplicado e
recusa rodar se um deles mudou, o que impõe pela ferramenta a regra de não
editar migração já aplicada.

O ensaio faz mais do que provar que o backup restaura: prova que a migração
roda sobre o dado real antes de rodar na produção.

O banco descartável fica no mesmo Postgres, e não num segundo, por causa da
memória da VM.

Não existe reversão automática: o Flyway gratuito não a tem, e reversão não é
plano de recuperação. O plano é o backup. Em desenvolvimento, desfazer um erro
é recriar o banco.

Com uma réplica, o initContainer roda uma vez por deploy. Com mais de uma, cada
Pod tenta migrar, e o Flyway trava o banco enquanto aplica, para que só um
aplique de fato.

## Alternativa recusada: migração na subida da própria api

É o padrão do Spring Boot e funciona. Recusada porque junta o passo que pode
destruir dado com a subida da aplicação: não sobra lugar para o ensaio antes, e
uma migração que falha vira uma api reiniciando e tentando de novo.

## Alternativa recusada: script de comando, como no ADR-0013

O script de deploy rodaria a migração e depois subiria a api. Funciona, e era o
desenho anterior. Recusado porque o initContainer é como o Kubernetes garante
que a api espera a migração terminar, e aprender isso é parte do ADR-0037.

## Alternativa recusada: o teste de restauração num segundo initContainer

Deixaria o teste junto da migração, na fila de inicialização do Pod. Recusado
porque initContainer roda toda vez que um Pod sobe, e não uma vez por deploy, e
porque o ensaio tem que acontecer antes de qualquer coisa tocar a produção.

## Alternativa recusada: Liquibase

Changelog em XML, YAML ou SQL, com reversão escrita e mais recursos. Recusado
porque o projeto já escreve SQL à mão, e o Flyway faz isso com menos cerimônia.

## Alternativa recusada: ensaio só quando há migração nova

Pouparia segundos de deploy. Recusado porque o banco é pequeno, e rodar sempre
testa o backup com mais frequência do que a RNF-08 pede.
