# ADR-0022 Sessão de teste inserida direto no banco

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O teste de ponta a ponta (ADR-0020, camada 3) precisa começar autenticado. O
login real do Google não é opção: o Google detecta e bloqueia navegador
automatizado, com a tela informando que o navegador pode não ser seguro. As
documentações do Playwright e do Cypress recomendam, cada uma por seu caminho,
pular o provedor de identidade.

## Decisão

O preparo do teste insere a linha de sessão direto no banco e injeta o cookie
correspondente no contexto do navegador. Nenhum código de produção participa.

## Consequências

Não existe superfície de produção para isso: o teste conversa com um banco que
ele já controla, e o código que vai para o servidor não tem um único desvio de
teste dentro.

É possível porque a sessão é uma linha de banco (ADR-0009). Com JWT, o teste
precisaria da chave de assinatura no ambiente de teste, o que empurraria para a
alternativa recusada abaixo.

Fica descoberto: a verificação da assinatura do `id_token` e a cadeia real de
redirecionamento. Coberto por teste de integração no backend contra `id_token`
fabricado (ADR-0020).

## Alternativa recusada: endpoint de teste protegido por variável de ambiente

Uma rota no backend que emite a mesma sessão que o retorno do Google emitiria,
ligada apenas quando uma variável de ambiente está presente.

Recusada porque coloca no código de produção uma rota que emite sessão **sem
autenticar ninguém**, cuja única proteção é configuração. Um deploy mal
configurado transforma isso em desvio de autenticação exposto na internet, e a
RNF-17 existe justamente porque o log de acesso mostra que há varredura
constante em serviço exposto.
