# ADR-0008 Identidade pelo Google, sem senha própria

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O sistema tem múltiplos usuários com dados isolados e entrada por convite
(ADR-0011). Não existe serviço de e-mail no desenho, e adicionar um exigiria
domínio verificado e configuração de DNS.

## Decisão

A autenticação é delegada ao Google. O sistema não armazena senha (RN-49). Os
escopos pedidos limitam-se a identidade: `openid`, e-mail e perfil (RNF-21).

O aplicativo permanece em modo Testing no console do Google, o que restringe o
login aos e-mails cadastrados manualmente, até 100.

## Consequências

Desaparecem do escopo: guardar senha com hash, recuperação de senha,
verificação de e-mail, bloqueio por tentativa repetida, e a responsabilidade de
ter senha de outras pessoas no banco.

Escopo mínimo tem um efeito documentado que decide a viabilidade: em modo
Testing o refresh token do Google expira em 7 dias, **exceto** quando os únicos
escopos pedidos são nome, e-mail e perfil. Como é o caso, a sessão não morre
semanalmente e o aplicativo não precisa passar por verificação.

Surge um portão duplo: o convite no sistema e a lista de usuários de teste no
console. Convidar alguém exige as duas ações, e esquecer a segunda produz uma
recusa do Google que o sistema não consegue explicar (UC-13, fluxo 3a).

Indisponibilidade do Google impede login novo. Sessão aberta não cai.

## Alternativa recusada: autenticação com senha própria

O trabalho de verdade não é a tela de login, é o que vem atrás dela, e mais da
metade desse trabalho depende de enviar e-mail, que este desenho não faz. Pelo
lado do aprendizado, ela ensinaria hash de senha em lugar de OAuth, e a
prioridade declarada do projeto favorece o produto.
