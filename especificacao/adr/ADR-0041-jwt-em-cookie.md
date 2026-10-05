# ADR-0041 Autenticação com Spring Security e JWT em cookie

- **Data:** 05-10-2026
- **Situação:** aceito
- **Supera:** ADR-0009, ADR-0010

## Contexto

A identidade vem do Google (ADR-0008), sem senha (RN-49) e com entrada por
convite (ADR-0011). O fluxo do OpenID Connect tem quatro pontos que erram em
silêncio: o `state`, o `nonce`, a troca do código pelo token e a verificação da
assinatura do `id_token`. Errar qualquer um ainda produz um login bem-sucedido.

Depois do login, o sistema precisa reconhecer a pessoa a cada requisição. A
RNF-22 pede sessão de 30 dias renovada a cada uso, e a RF-04, encerramento
explícito. Frontend e api ficam na mesma origem (ADR-0038).

O ADR-0009 guardava a sessão numa linha do banco e recusou o JWT: um token não
se revoga por desenho, e revogar exigiria consultar o banco a cada requisição,
o que anula a vantagem dele. O motivo da troca é aprendizado, como no ADR-0035:
JWT é parte central do que se usa com Spring Security. O desenho abaixo
responde àquela objeção sem lista de tokens cancelados.

## Decisão

O login com o Google é feito pelo Spring Security, que cuida dos quatro pontos
do contexto. No fim do login, o sistema confere o convite (RN-48).

Para a pessoa convidada, o sistema emite dois tokens, cada um num cookie
`httpOnly`, `Secure` e `SameSite=Lax`:

- um **token de acesso**, JWT assinado com RS256, válido por 15 minutos e
  conferido só pela assinatura, sem consultar o banco;
- um **token de renovação**, aleatório, válido por 30 dias, guardado no banco
  como hash e enviado só para a rota de renovação. Cada uso empurra a validade
  para mais 30 dias, e ele não é trocado a cada uso.

Quando o token de acesso vence, o frontend usa o de renovação para pedir outro.
Sair apaga o token de renovação e expira os dois cookies.

Toda falha do retorno do login, inclusive a recusa por convite, volta para a
tela de entrada com um motivo, e nunca com um erro cru.

Não há token de proteção contra CSRF.

## Consequências

A objeção do ADR-0009 fica respondida pela duração curta: um token de acesso
copiado vale no máximo 15 minutos depois de a pessoa sair, e isso é aceito. O
banco é consultado só na renovação, uma vez a cada 15 minutos por pessoa, e não
em toda requisição.

`SameSite=Lax`, e não `Strict`, porque o retorno do login do Google é
navegação vinda de outro site, e `Strict` faria o cookie não ser enviado nesse
retorno. O `Lax` impede o navegador de mandar o cookie em POST, PATCH ou DELETE
vindo de outro site, e por isso nenhum GET muda dado.

O banco guarda só o hash do token de renovação, e um vazamento do banco não
entrega token utilizável.

O teste de ponta a ponta continua sem código de produção (ADR-0022): ele insere
um token de renovação no banco e põe o cookie no navegador.

Durante os segundos do login, o Spring guarda o `state` numa sessão em memória,
que é o único estado no servidor. Com uma réplica (ADR-0037), isso basta.

Custos aceitos: uma rota de renovação; o cliente HTTP do frontend renova e
repete a chamada que voltou 401; e um par de chaves guardado como Secret.

## Alternativa recusada: sessão no banco, como no ADR-0009

Tecnicamente basta neste sistema: revoga na hora, e o volume da RNF-25 não
sente a consulta por requisição. Recusada pelo motivo do contexto: aprender JWT.

## Alternativa recusada: JWT de 30 dias com lista de tokens cancelados

Junta o custo das duas opções: o token longo vale em qualquer cópia, e revogar
volta a consultar o banco em toda requisição.

## Alternativa recusada: trocar o token de renovação a cada uso

É a recomendação de segurança quando o JavaScript consegue ler o token. Aqui
ele fica em cookie `httpOnly`, e a troca tem um defeito concreto: com duas abas
abertas, as duas renovam ao mesmo tempo, a primeira invalida o token da
segunda, e a segunda cai na tela de login.

## Alternativa recusada: HS256

Um segredo só, que assina e confere, e basta com um serviço. Recusado porque,
com par de chaves, outro serviço pode conferir o token sem conseguir emitir um.

## Alternativa recusada: token guardado pelo JavaScript

Mandar o token no cabeçalho `Authorization`, guardado no `localStorage`, é
comum em tutorial. Recusado porque qualquer script injetado na página leria o
token, e em cookie `httpOnly` o JavaScript não o vê.

## Alternativa recusada: token de proteção contra CSRF

É o padrão do Spring Security. Recusado porque o `SameSite=Lax` e a origem única
já cobrem o caso, e nenhum GET muda dado.

## Alternativa recusada: o fluxo do Google escrito à mão

Seriam por volta de cem linhas, e ensinariam o fluxo. Recusado pelo motivo do
ADR-0010: os quatro pontos do contexto erram em silêncio, e o Spring Security
já os resolve.

## Quando rever

Mais de uma réplica da api: o `state` do login sai da memória e vai para um
cookie.
