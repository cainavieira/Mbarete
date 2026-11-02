# Produção

A configuração do cluster e o roteiro para montar a VM de novo.

## A VM

| Item | Valor | Por quê |
|:--|:--|:--|
| Sistema | Ubuntu Server 26.04 LTS | está na matriz de testes do k3s, e o suporte grátis vai até 2031 |
| CPU | 2 núcleos, tipo `host` | o processador do servidor não tem AES-NI, que o tipo padrão do Proxmox exige |
| Memória | 4 GiB, com o balloon ligado e o mínimo igual ao máximo | memória fixa para o Kubernetes e a JVM, e o Proxmox enxerga o uso por dentro |
| Disco | 32 GB, qcow2, com discard, sem LVM | o Proxmox já aumenta o disco e tira snapshot |
| Rede | IP fixo na rede de casa, sem IPv6 | o IPv6 daria à VM um endereço público |

## Depois de instalar

- IPv6: `accept-ra: false` na interface, no arquivo do netplan, aplicado com
  `sudo netplan try`.
- kdump: `sudo apt purge kdump-tools`, `sudo update-grub` e reboot. Devolve os
  512 MB que ele reserva.
- Swap: linha do `/swap.img` comentada no `/etc/fstab`, e o arquivo apagado.
- `qemu-guest-agent` instalado.

## k3s

1. Copiar `k3s/config.yaml` para `/etc/rancher/k3s/` e
   `k3s/k3s-traefik-config.yaml` para `/var/lib/rancher/k3s/server/manifests/`,
   antes de instalar.
2. `curl -sfL https://get.k3s.io -o install-k3s.sh`, ler, e
   `sudo INSTALL_K3S_VERSION='v1.36.5+k3s1' sh install-k3s.sh`. A versão é fixa
   para registrar o que está em produção. Sem ela, o instalador consulta o
   `update.k3s.io`, que em 02-11 não respondia da rede de casa.
3. Conferir: o nó `Ready`, nenhum Pod `svclb-`, e o Service do Traefik em
   `ClusterIP`.
