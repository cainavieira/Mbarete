# ADR-0037 Produção em Kubernetes, com k3s numa VM

- **Data:** 05-10-2026
- **Situação:** aceito
- **Supera:** ADR-0031

## Contexto

Em produção, o ADR-0031 roda quatro serviços num Compose (nginx, api, banco e
o conector do túnel), e o deploy é um script que para no primeiro erro. Nada
nesse desenho falhou.

O motivo da troca é o mesmo do ADR-0035: aprendizado. O interessado quer
aprender a operar a aplicação em Kubernetes, inclusive o desenho em que a
migração roda num contêiner separado, antes de a api subir.

O servidor é um só, doméstico, com Proxmox (ADR-0006), e tem 7 GB de memória
livres. Fatos conferidos em 04-10-2026: o k3s se declara "a fully compliant
Kubernetes distribution", empacotada para rodar com um servidor só, e pede no
mínimo 2 núcleos e 2 GB de memória para o nó. Rodar o k3s dentro de LXC exige
contêiner privilegiado e módulos de kernel no Proxmox, e a recomendação da
comunidade é usar VM.

## Decisão

Produção em Kubernetes, com o k3s numa VM do Proxmox. A api roda com uma
réplica.

As duas imagens que o projeto constrói são feitas em dois estágios: a da api
compila com Maven e leva só o Java de execução e o `.jar`; a do frontend roda o
build do Vite e entrega só os arquivos ao nginx. Elas vão para o GitHub
Container Registry com etiqueta de versão, e o cluster as baixa de lá. Os
manifestos são YAML com Kustomize, versionados no repositório.

O deploy continua manual e para no primeiro erro: um script disparado pelo
interessado constrói e envia as imagens, aplica os manifestos e espera a versão
nova ficar pronta. A integração contínua testa e não faz deploy.

Desenvolvimento continua sem Kubernetes e sem imagem de produção: a api roda
direto na máquina, e só o Postgres vem do Compose.

## Consequências

Kubernetes ensina o que o Compose esconde: Deployment, Service, Ingress, Job,
initContainer, Secret e sonda de saúde. O k3s usa a mesma API e o mesmo
`kubectl` do Kubernetes tradicional. O que ele não ensina é montar o cluster à
mão, e isso é aceito: é trabalho de quem administra cluster, e é comum o
cluster vir pronto da nuvem.

Em desenvolvimento, o proxy do Vite repassa `/api` para a api na máquina, e o
navegador vê uma origem só, como em produção.

O repositório é público (ADR-0032), e por isso as imagens no registro não levam
segredo: senha, chave e token entram só no cluster, como Secrets criados por
comando, nunca no git.

Custos aceitos em troca do aprendizado:

- a VM reserva 4 GB dos 7 livres no servidor, e o próprio k3s consome parte
  disso;
- passam a existir mais peças para operar: o cluster, o registro de imagens e
  os Secrets.

## Alternativa recusada: manter o Compose

Mais simples, mais leve, e nada nele falhou. Recusado pelo motivo do contexto:
o objetivo inclui aprender Kubernetes.

## Alternativa recusada: kubeadm

O Kubernetes montado peça por peça. Mais pesado num servidor só, e o que ele
ensina a mais é administrar o cluster, não operar a aplicação.

## Alternativa recusada: k3s em LXC

Aproveitaria o LXC em que a produção rodava antes. Recusado porque exige
contêiner privilegiado e módulos de kernel no Proxmox.

## Alternativa recusada: Docker Hub

Grátis para imagem pública, com limite de 100 downloads por hora com login e 10
sem login, desde abril de 2025. Recusado porque o GitHub Container Registry
fica junto do repositório, com o mesmo login, e não cobra armazenamento nem
tráfego de pacote público.

## Alternativa recusada: imagem construída na VM, sem registro

Dispensaria o registro. Recusada porque a etiqueta num registro é o que permite
voltar à versão anterior trocando só a etiqueta, e porque construir na VM gasta
a memória que o cluster usa.

## Alternativa recusada: Helm

Transforma o YAML em modelo com variáveis, e é o padrão para distribuir
aplicação a terceiros. Aqui há uma aplicação e um ambiente, e o Kustomize, que
já vem no `kubectl`, resolve o caso concreto: trocar a etiqueta da imagem a
cada deploy.

## Alternativa recusada: deploy pela integração contínua

Mesmo motivo do ADR-0031: a integração contínua executa código de terceiros, e
esse código ganharia acesso à produção. Além disso, a casa não tem porta aberta
(RNF-19), e o GitHub Actions não alcança o cluster.

## Alternativa recusada: GitOps com Argo CD ou Flux

O cluster buscaria no repositório o que mudou, sem ninguém de fora entrar nele,
o que resolveria o problema acima. Recusado por agora pela memória: é mais um
componente rodando numa VM de 4 GB.

## Alternativa recusada: comandos digitados soltos

Depende de lembrar a ordem e de perceber que um passo falhou antes de executar
o seguinte.

## Quando rever

Um segundo servidor reabre o número de réplicas. Memória sobrando na VM, depois
de medida, reabre o GitOps.
