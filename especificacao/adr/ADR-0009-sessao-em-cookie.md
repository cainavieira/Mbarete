# ADR-0009 Sessão em cookie com estado no servidor

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

A identidade vem do Google (ADR-0008) e o sistema não guarda senha (RN-49).
Depois que o Google devolve quem a pessoa é, o sistema precisa manter isso
válido entre requisições. A RNF-22 fixou sessão de 30 dias renovada a cada uso,
e a RF-04 exige encerramento explícito.

Frontend e API ficam na mesma origem (ADR-0007), então cookie funciona sem
configuração de origem cruzada.

## Decisão

A sessão é uma linha em tabela do banco. O navegador recebe um cookie com
identificador aleatório, marcado `httpOnly`, `Secure` e `SameSite=Lax`.

`SameSite=Lax` e não `Strict` porque o retorno do login do Google é navegação
vinda de outro site, e `Strict` faria o cookie não ser enviado nesse retorno,
impedindo a sessão de se estabelecer.

## Consequências

Toda requisição autenticada consulta a tabela de sessão. No volume da RNF-25
isso é irrelevante.

Encerrar sessão apaga a linha **e** instrui o navegador a descartar o cookie.
Os dois passos: apagar só o cookie não revoga nada, porque uma cópia do valor
continuaria válida.

O teste de ponta a ponta insere a linha de sessão direto no banco e injeta o
cookie, sem tocar no Google nem no código de produção (ADR-0022). Esse ganho
não estava previsto quando a decisão foi tomada.

## Alternativa recusada: JWT

O token carrega os dados assinados e o servidor valida sem consultar o banco,
o que permite escalar autenticação por várias máquinas sem estado
compartilhado. O ganho existe e não é observável no volume deste sistema.

O custo é que JWT não é revogável por desenho. Encerrar sessão apagaria o token
do navegador sem invalidar cópias, e sessão de 30 dias sem revogação é troca
ruim. Revogar de verdade exigiria lista de tokens invalidados no banco, que é
exatamente a tabela que o JWT dispensava, consultada em toda requisição, o que
anula a única vantagem.
