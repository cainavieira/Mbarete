# ADR-0031 Deploy manual por script que para no primeiro erro

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O deploy tem passos com ordem obrigatória: subir o código, aplicar migração
(ADR-0013), e testar a restauração do backup, que a RNF-08 exige a cada mudança
de esquema.

A integração contínua executa a árvore de dependências de npm e pip inteira, ou
seja, código de terceiro não auditado.

## Decisão

Um script de deploy, disparado por quem opera, que executa os passos na ordem e
**para no primeiro erro**.

Em produção, Compose com quatro serviços: nginx, api, banco e o conector do
túnel. A imagem do frontend é construída em dois estágios: o primeiro roda o
build do Vite, o segundo é a imagem do nginx e copia apenas os arquivos
gerados. A imagem final não contém Node.

Em desenvolvimento **não se usa a imagem de produção**: `npm run dev` e
`uvicorn --reload` rodam direto na máquina, e apenas o Postgres vem do Compose.

## Consequências

Parar no primeiro erro é o que impede o terceiro passo de rodar quando o
segundo falhou, que é o cenário em que se sobe aplicação nova contra esquema
antigo.

O desenvolvimento tem recarregamento instantâneo. Reconstruir imagem a cada
ajuste de CSS é o caminho mais curto para abandonar o Docker.

O proxy embutido do servidor de desenvolvimento do Vite repassa `/api` ao
uvicorn, então o navegador vê **uma origem só** também em desenvolvimento.
Resultado: não há configuração de CORS em nenhum ambiente, e o cookie
`SameSite=Lax` do ADR-0009 se comporta igual nos dois.

## Alternativa recusada: deploy disparado pela integração contínua

É o padrão em equipe e seria conveniente aqui, já que CI e produção poderiam
conviver. Recusado porque daria a código de terceiro acesso ao ambiente de
produção: um pacote comprometido durante a instalação de dependências receberia
credencial do banco que guarda o dado cuja perda é a única falha irreversível do
sistema (RNF-04).

## Alternativa recusada: comandos digitados soltos no servidor

Depende de lembrar a ordem e de perceber que um passo falhou antes de executar
o seguinte.
