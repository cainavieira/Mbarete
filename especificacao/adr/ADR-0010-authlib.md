# ADR-0010 Authlib para o fluxo OAuth

- **Data:** 07-09-2026
- **Situação:** superado por ADR-0041

## Contexto

A identidade é delegada ao Google (ADR-0008). O fluxo do OpenID Connect tem
quatro pontos que erram em silêncio: o parâmetro `state`, que impede alguém de
forçar o navegador da vítima a completar um login alheio; o `nonce`; a troca do
código pelo token; e a verificação da assinatura do `id_token` contra as chaves
públicas do provedor.

Errar qualquer um dos quatro ainda produz um login bem-sucedido, então o defeito
não aparece em teste manual.

## Decisão

Authlib.

## Consequências

Os quatro pontos acima ficam com a biblioteca, e não com código escrito uma vez
e nunca revisado.

O centro da Authlib é exatamente a dança do OAuth e do OIDC, que é 100% do que
o sistema precisa dela. Não há recurso a desligar nem rota a blindar.

Continua sendo responsabilidade de quem escreve entender o que a biblioteca faz
com o `state` e com o `id_token`, em vez de tratar como mágica, porque é o que
vai ser preciso na primeira vez que o fluxo quebrar.

## Alternativa recusada: fastapi-users

É framework de gestão de usuário, não biblioteca de OAuth. O centro dele é o
ciclo de vida da senha: cadastro com senha, login com senha, recuperação por
e-mail, verificação de e-mail. O sistema recusou a senha (RN-49), recusou o
cadastro aberto (RN-48) e não tem serviço de e-mail, então quatro das
funcionalidades principais dele ou foram recusadas ou não conseguiriam
funcionar.

O modelo de usuário dele traria para o banco colunas como `hashed_password`,
`is_verified` e `is_superuser`, que não significam nada neste sistema. Coluna
que existe e mente é pior que coluna ausente, porque alguém vai deduzir regra a
partir dela.

Framework paga quando se usa o centro dele. Aqui se carregaria o peso e se
usaria a borda.

## Alternativa recusada: implementação à mão

Seriam por volta de cem linhas e ensinaria o fluxo, e seria a escolha certa se
aprender OAuth fosse o objetivo. A prioridade declarada do projeto favorece o
produto.
