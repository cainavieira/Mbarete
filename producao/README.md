# Produção

O que a produção tem e nenhum arquivo do repositório mostra: a VM, os ajustes
feitos nela à mão e o que cada peça pede para subir. A configuração está nos
arquivos desta pasta, e as decisões, na especificação.

## A VM

| Item | Valor | Por quê |
|:--|:--|:--|
| Sistema | Ubuntu Server 26.04 LTS | está na matriz de testes do k3s, e o suporte grátis vai até 2031 |
| CPU | 2 núcleos, tipo `host` | o processador do servidor não tem AES-NI, que o tipo padrão do Proxmox exige |
| Memória | 4 GiB, com o balloon ligado e o mínimo igual ao máximo | memória fixa para o Kubernetes e a JVM, e o Proxmox enxerga o uso por dentro |
| Disco | 32 GB, qcow2, com discard, sem LVM | o Proxmox já aumenta o disco e tira snapshot |
| Rede | IP fixo na rede de casa, sem IPv6 | o IPv6 daria à VM um endereço público |

## Depois de instalar

Ajustes feitos à mão no Ubuntu:

- IPv6 desligado no netplan, com `accept-ra: false` na interface.
- kdump removido, o que devolve os 512 MB que ele reserva.
- Swap desligada.
- `qemu-guest-agent` instalado.

### AppArmor

`kernel.apparmor_restrict_unprivileged_unconfined = 0`, em
`/etc/sysctl.d/99-k3s-apparmor.conf`. Com 1, o padrão do Ubuntu 26.04, o
containerd do k3s empilha o perfil dos contêineres
(`cri-containerd.apparmor.d//&unconfined`), os processos de um mesmo contêiner
não conseguem mandar sinal uns aos outros, e o Postgres trava na primeira
conexão.

- Desfazer quando o k3s trouxer o containerd com a correção.
- As alternativas eram um perfil próprio em cada contêiner com vários
  processos, ou tirar o AppArmor deles.

## k3s

O k3s v1.36.5+k3s1, com a versão fixa na instalação para registrar o que está
em produção. Os dois arquivos de `k3s/` vão para o lugar antes de instalar, e
de novo a cada mudança: o `config.yaml` em `/etc/rancher/k3s/`, e o
`k3s-traefik-config.yaml` em `/var/lib/rancher/k3s/server/manifests/`.

## Postgres

O banco roda pelo operador CloudNativePG 1.30.1, e o `Cluster` está em
`manifestos/`.

- A ordem é o namespace, o Secret e o banco, porque o banco lê a senha ao
  nascer.
- Depois de recriar o operador, o banco só se aplica quando ele fica pronto:
  antes disso, o webhook dele não responde, e o Kubernetes recusa o `Cluster`.

O Secret `banco-app` é criado por comando na VM, do tipo
`kubernetes.io/basic-auth`, com duas chaves:

- `username`: `mbarete`, o mesmo dono do banco no `banco.yaml`.
- `password`: gerada na própria VM.

## Imagens

A api e o frontend têm cada um a sua imagem no GitHub Container Registry:
`ghcr.io/cainavieira/mbarete-api` e `ghcr.io/cainavieira/mbarete-frontend`. A
etiqueta de cada versão é o hash curto do commit de onde ela saiu, e nunca
`latest`: assim se sabe o que está rodando, e voltar à versão anterior é trocar
a etiqueta.

O build:

- parte da árvore limpa, porque lê a pasta, e não o commit: uma mudança não
  commitada entraria numa imagem com a etiqueta de um commit que não a tem.
- não roda os testes, que ficam com a integração contínua.

O envio:

- pede um token clássico do GitHub com o escopo `write:packages`, porque o GHCR
  não aceita o token fino.
- na primeira vez, a imagem nasce privada, e a visibilidade muda para pública na
  página do pacote no GitHub.
- a prova de que deu certo é a VM baixar as duas imagens sem login.

## A api

O Secret `api` é criado por comando na VM, como o do banco, com três chaves:

- `MBARETE_GOOGLE_ID` e `MBARETE_GOOGLE_SEGREDO`, do cliente de produção no
  console do Google, separado do de desenvolvimento: o segredo daquele fica no
  `.env` de quem desenvolve. O endereço de volta registrado nele é
  `https://mbarete.com.br/api/v1/auth/callback`.
- `MBARETE_JWT_CHAVE`, a chave do token, gerada na própria VM: quem tivesse a de
  desenvolvimento conseguiria forjar sessão em produção.

## O domínio e o túnel

O `mbarete.com.br` está registrado no Registro.br, com o DNS na Cloudflare e sem
DNSSEC. Além do site, a zona tem os registros que avisam que o domínio não manda
nem recebe e-mail: o MX nulo, o SPF `-all` e o DMARC `reject`.

O túnel `mbarete` é configurado no painel da Cloudflare, e não em arquivo:

- A rota pública leva `mbarete.com.br` para `http://traefik.kube-system:80`.
- O Always Use HTTPS manda quem chega por `http://` para `https://`.

O Secret `cloudflared` é criado por comando na VM, com uma chave,
`TUNNEL_TOKEN`: o token do túnel, que aparece no comando de instalação, na
página dele no painel.

## Os logs

O log da api e o de acesso do Traefik são copiados pelo Vector para
`/var/log/mbarete/`, no disco da VM, com um arquivo por dia de Brasília e por
contêiner. A pasta só abre para o root. O log do banco, o do `cloudflared` e o
do próprio Traefik ficam só com o kubelet, enquanto o pod existe.

- No log de acesso, o IP de quem acessa é o `request_Cf-Connecting-Ip`. O
  `ClientHost` traz o `X-Forwarded-For` inteiro, cujo começo o visitante pode
  inventar.
- O painel do túnel na Cloudflare mostra ao vivo, no Live logs, os pedidos que
  passam por ele, sem entrar na VM.
