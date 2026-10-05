# ADR-0038 Entrada pelo Traefik, com nginx só no estático

- **Data:** 05-10-2026
- **Situação:** aceito
- **Supera:** ADR-0007

## Contexto

O build do Vite produz arquivos estáticos, não um programa, e a api é um
processo separado. O ADR-0007 pôs o nginx na frente de tudo: ele entregava o
estático e repassava `/api` à api. A produção agora roda em Kubernetes
(ADR-0037), onde quem recebe o tráfego de fora e decide para onde cada caminho
vai é um controlador de entrada.

Fatos conferidos em 05-10-2026:

- o k3s já vem com o Traefik, que entende tanto o Ingress quanto a Gateway
  API;
- o ingress-nginx, o controlador de entrada mais usado, foi aposentado em março
  de 2026 e não recebe mais correção de segurança;
- a documentação do Kubernetes diz que a API do Ingress está congelada, sem
  plano de remoção, e recomenda a Gateway API no lugar dela.

## Decisão

O Traefik recebe toda requisição que chega pelo túnel, e o roteamento é
declarado pela Gateway API. Caminhos sob `/api` vão para a api. Todo outro
caminho vai para o nginx do frontend, que só entrega arquivo estático, com
`index.html` como resposta para o caminho que não é arquivo, para as rotas do
lado do cliente (ADR-0024) funcionarem ao recarregar a página. O conector
`cloudflared` roda dentro do cluster e entrega ao Traefik.

A api só é alcançável de dentro do cluster.

## Consequências

A origem única do ADR-0007 continua: frontend e api compartilham o mesmo
endereço. Isso elimina a configuração de origem cruzada, deixa o cookie
funcionar sem marca de uso entre sites e mantém um único URI de
redirecionamento no console do Google.

O nginx perde o papel de proxy e fica só com o estático.

A Gateway API não vem ligada no Traefik do k3s: ligar é uma configuração do
próprio k3s, feita uma vez.

## Alternativa recusada: Ingress

É o que o k3s traz ligado, e o que mais se encontra em cluster existente.
Recusado porque está congelado, e a documentação do Kubernetes recomenda a
Gateway API para o que é novo. Os conceitos passam de um para o outro.

## Alternativa recusada: nginx fazendo o repasse

O túnel apontaria para o nginx, que repassaria `/api` como no ADR-0007.
Funciona, e deixaria de lado a peça que o Kubernetes tem para isso, que é parte
do que o ADR-0037 quer aprender.

## Alternativa recusada: ingress-nginx

Foi o controlador de entrada mais usado. Recusado porque foi aposentado em março
de 2026 e não recebe mais correção de segurança.

## Alternativa recusada: a api servindo o estático

Funciona e é mais simples de montar. Recusada pelo motivo do ADR-0007: põe o
processo da aplicação a entregar arquivo estático, que é desperdício e complica
o controle de cache.

## Alternativa recusada: frontend em CDN separada

Cloudflare Pages ou equivalente daria distribuição global aos arquivos. O ganho
é de dezenas de milissegundos para um público de poucas pessoas no Brasil, e o
custo é reintroduzir duas origens, com CORS, cookie entre sites, dois lugares
de deploy e duas entradas no Google. Além disso, o Cloudflare Tunnel já põe a
borda da Cloudflare na frente da aplicação, com cache do estático.
