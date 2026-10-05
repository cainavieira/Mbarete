# ADR-0039 Postgres no cluster com CloudNativePG, e dois caminhos de backup

- **Data:** 05-10-2026
- **Situação:** aceito

## Contexto

O banco é Postgres (ADR-0004) e roda na mesma máquina da api (ADR-0005). A
produção agora é Kubernetes (ADR-0037), e falta decidir se o banco entra no
cluster e, se entrar, quem cuida dele.

Perder histórico é a única falha irreversível do sistema (RNF-04). A RNF-05 pede
cópia em três lugares, duas mídias e uma fora de casa, e a RNF-09 pede alerta
quando um backup não roda.

Fatos conferidos em 04-10-2026:

- o CloudNativePG é um operador: um programa que roda no cluster e cuida do
  Postgres a partir de um YAML que descreve o que se quer;
- ele não usa StatefulSet, e gerencia Pods e discos direto, porque o
  StatefulSet não aumenta o disco nem distingue a instância principal das
  réplicas;
- o backup dele é físico, com o WAL (o diário de alterações do Postgres)
  arquivado continuamente num armazenamento de objetos do tipo S3, e restaurar
  é criar um banco novo a partir do backup, nunca por cima do que existe;
- o Cloudflare R2 dá 10 GB grátis e não cobra a saída dos dados.

## Decisão

Em produção, o Postgres roda no cluster, com uma instância, gerenciado pelo
CloudNativePG. O backup tem dois caminhos, e um não depende do outro:

- o do operador, físico e com o WAL contínuo, no Cloudflare R2, fora de casa;
- um `pg_dump` diário, feito por um CronJob, gravado no disco lento do
  servidor.

Um único aviso de homem morto, no fim do CronJob diário, confere os dois.

Fora de produção nada muda: em desenvolvimento, o Postgres vem do Compose.

## Consequências

A RNF-05 fica assim: o banco vivo no SSD, o `pg_dump` no disco lento e o backup
físico no R2. Com o WAL contínuo, a perda real cai das 24 horas que a RNF-06
aceita para minutos.

O `pg_dump` é o backup que quebra quando o esquema muda, porque guarda comandos
SQL, enquanto o físico copia bytes e não depende do esquema. Por isso é o
`pg_dump` que se restaura no teste a cada migração (RNF-08).

A retenção da RNF-07 se divide entre os dois caminhos: 90 dias de backup físico
no R2, 30 dias de `pg_dump` no disco lento, e um `pg_dump` mensal no R2 guardado
por 12 meses.

A cópia fora de casa continua num terceiro, como a RNF-05 já previa.

Custo aceito: o operador é mais um componente na VM de 4 GB, e uma camada a
mais entre o dado e o disco. O segundo caminho de backup existe por causa dessa
camada.

## Alternativa recusada: Postgres fora do cluster

Backup e restauração continuariam sendo comandos de Postgres, sem camada
nenhuma entre o dado e o disco. Recusado porque rodar banco em Kubernetes é
parte do que o ADR-0037 quer aprender, e operador é como isso se faz.

## Alternativa recusada: StatefulSet sem operador

Ensina pouco do que se usa, e deixa backup, restauração e atualização de versão
à mão.

## Alternativa recusada: só o backup do operador

Um mecanismo só. Se o operador ou a configuração dele falharem, todas as cópias
falham juntas, e a RNF-04 não admite isso.

## Alternativa recusada: só o pg_dump

Mais simples, e é o que a RNF-05 previa. Recusado porque perde a perda máxima
de minutos e o que o operador ensina.

## Quando rever

Se a memória medida na VM não comportar o operador, o Postgres sai do cluster.
