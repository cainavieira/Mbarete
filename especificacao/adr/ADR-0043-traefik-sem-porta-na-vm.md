# ADR-0043 Traefik sem porta na VM

- **Data:** 02-11-2026
- **Situação:** aceito

## Contexto

O Traefik recebe o tráfego que chega pelo túnel (ADR-0038), e a casa não tem
porta aberta para a internet (RNF-19). Falta decidir se ele também fica
alcançável pela rede da casa, nas portas da VM.

Fatos conferidos em 02-11-2026 na documentação do k3s: o k3s traz o
ServiceLB, que publica todo Service do tipo `LoadBalancer` nas portas da
máquina, e o Service do Traefik vem desse tipo, nas portas 80 e 443. Com o
ServiceLB desligado, um Service `LoadBalancer` continua abrindo uma porta alta
em todo nó. A documentação indica `ClusterIP` quando o Traefik é alcançado por
outro meio, como um túnel que chega a ele de dentro do cluster.

## Decisão

O ServiceLB fica desligado, e o Service do Traefik é `ClusterIP`. O Traefik
não ocupa porta da VM, e só o `cloudflared`, de dentro do cluster, chega a ele.

## Consequências

O túnel é a única entrada, também para quem está na rede da casa.

Antes de o túnel existir, abrir o site pede `kubectl port-forward`: um atalho
temporário, que só quem tem a credencial do cluster abre, e que por padrão só
escuta na própria VM.

As duas configurações ficam em `producao/k3s/`: o `disable: servicelb` no
`config.yaml` e o `service.spec.type` no `k3s-traefik-config.yaml`. A chave
antiga, `service.type`, é ignorada sem aviso desde o chart v40 do Traefik.

## Alternativa recusada: o Traefik nas portas da VM, pelo ServiceLB

É o padrão do k3s, e deixaria abrir o site pelo endereço da VM antes de o túnel
existir. Recusado porque seria uma entrada por fora do túnel, alcançável de
qualquer aparelho da casa.

## Alternativa recusada: só desligar o ServiceLB

O Traefik perderia as portas 80 e 443, mas o Service do tipo `LoadBalancer`
continuaria abrindo uma porta alta em todo nó: a mesma entrada, por outro
número.
