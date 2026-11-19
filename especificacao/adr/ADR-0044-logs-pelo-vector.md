# ADR-0044 Logs copiados pelo Vector

- **Data:** 18-11-2026
- **Situação:** aceito

## Contexto

A RNF-18 pede 7 dias do log da api e do log de acesso do Traefik. Os dois saem
na saída padrão dos contêineres, e o kubelet guarda essa saída em
`/var/log/pods`. Só que ele apaga a pasta do pod quando o pod é trocado, e todo
deploy troca o pod. Um erro na sexta seguido de um deploy no sábado sumiria
antes de o usuário relatar na segunda.

Fatos conferidos em 18-11-2026, na documentação do Vector, num teste com a
versão 0.62.0 e na VM:

- A fonte de Kubernetes do Vector lê `/var/log/pods` e pede `list` e `watch`
  em pods, namespaces e nós, para saber de que contêiner veio cada linha.
- A saída em arquivo põe a data no nome, tirada da hora em que a linha foi
  escrita, no fuso escolhido.
- A codificação `text` grava a linha como o programa escreveu.
- Os arquivos do kubelet são do root, e só o root e o grupo root os leem.

As características do Fluent Bit e do Loki, nas alternativas, são de memória:
a documentação deles não foi lida.

## Decisão

O Vector roda no cluster e copia para `/var/log/mbarete/`, no disco da VM, as
linhas dos dois contêineres do pod da api (a api e a migração) e as do log de
acesso do Traefik. Cada linha vai como foi escrita, num arquivo por dia de
Brasília e por contêiner. Uma tarefa agendada do cluster apaga o que passa de 7
dias.

## Consequências

Ler o log é entrar por SSH e abrir o arquivo do dia. Não há tela de busca.

O Vector é a primeira peça com permissão no cluster inteiro: lê nomes e
etiquetas de pods, namespaces e nós, sem acesso a Secret.

Ele roda como root, sem nenhuma capacidade do Linux, e do disco da VM monta só
três pastas: a dos logs dos pods, só para leitura, a da posição de leitura e a
de saída. Um usuário comum no grupo root leria os arquivos, mas não escreveria
nas pastas que o Kubernetes cria, e prepará-las pediria um passo à mão na VM.

O log do banco, o do `cloudflared` e o do próprio Traefik ficam só com o que o
kubelet guarda enquanto o pod existe. A RNF-18 pede o da aplicação e o de
acesso.

O código de uso único do login do Google entra no log de acesso, porque o
endereço gravado inclui a consulta. Fica assim: a api gasta o código no mesmo
segundo, e quem lê o arquivo é o root da VM, que já lê o segredo do cliente.

## Alternativa recusada: só o kubelet

Não guardar nada além do que o kubelet guarda. Recusado porque a troca do pod
apaga o log, e todo deploy troca o pod.

## Alternativa recusada: o Live logs da Cloudflare

O painel do túnel mostra os pedidos que passam por ele. Recusado porque não vê
o log da api, que diz por que um pedido falhou.

## Alternativa recusada: a fonte `file` do Vector

Lê `/var/log/pods` direto, sem permissão no cluster. Recusado porque
reescreveria à mão o que a fonte de Kubernetes já faz: ler o formato do
containerd e emendar as linhas que ele parte em pedaços.

## Alternativa recusada: Fluent Bit com o logrotate

Grava num arquivo que só cresce. Cortar por dia e apagar o velho ficaria com o
`logrotate` da VM, num arquivo copiado à mão, fora do `apply` dos manifestos.

## Alternativa recusada: Loki com Grafana

Dá tela de busca e apaga sozinho o que passa da retenção. Recusado pela memória
que pede numa VM de 4 GiB, e por serem duas peças a mais para manter.
