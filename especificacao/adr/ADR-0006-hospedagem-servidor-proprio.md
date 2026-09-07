# ADR-0006 Servidor próprio exposto por Cloudflare Tunnel

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O interessado tem um servidor doméstico com Proxmox. A conexão é residencial,
provavelmente atrás de CGNAT, sem IP fixo, o que torna redirecionamento de
porta inviável. Não há compromisso de disponibilidade (RNF-02).

## Decisão

Backend, banco e nginx rodam em LXC no Proxmox doméstico, expostos por
Cloudflare Tunnel, com domínio `.com.br` próprio a R$40 por ano.

## Consequências

Nenhuma porta aberta na rede doméstica, e o endereço IP residencial não fica
visível. O túnel é conexão de saída, o que resolve o CGNAT sem depender de IP
fixo.

O domínio é ativo próprio: trocar de máquina depois não muda o endereço que os
usuários conhecem, nem o URI de redirecionamento registrado no Google.

A aplicação depende da energia e da conexão da casa (RNF-03), e a energia é o
maior custo do projeto (RNF-01). Esse custo foi levantado, discutido e aceito.

## Alternativa recusada: camadas gratuitas de PaaS

Nenhuma serve. Render hiberna o serviço após 15 minutos sem tráfego e apaga o
Postgres gratuito em 30 dias. Railway concede US$1 por mês de crédito contra um
custo tabelado de US$10 por GB de memória por mês. Fly.io descontinuou as
allowances gratuitas em 07-10-2024. Vercel é serverless com teto de 300
segundos por função e proíbe uso comercial no plano gratuito.

## Alternativa recusada: Tailscale Funnel

Sairia por R$0 e dispensaria o domínio, com hostname estável e HTTPS. Recusado
por dois motivos: o plano Personal é declarado não comercial, e o interessado
considera cobrar um dia; e o limite de banda não é divulgado, então não há como
planejar contra ele.

## Alternativa recusada: Oracle Cloud Always Free

Ofereceria 2 OCPU ARM, 12 GB de memória, IP público e independência da energia
da casa, por R$0. Recusada porque a cota já foi cortada pela metade em 2026 sem
anúncio, apenas com edição da documentação. Camada gratuita que muda sozinha é
dívida que vence quando o fornecedor quiser, e aqui ela guardaria o dado cuja
perda é a única falha irreversível do sistema (RNF-04).
