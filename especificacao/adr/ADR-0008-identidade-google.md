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

O aplicativo fica em modo Testing no console do Google, sem lista de usuários
de teste. Conferido em 10-10-2026 na documentação do Google: o app que pede só
esses escopos não precisa da lista nem em modo Testing. Quem decide quem entra
é o convite (ADR-0011).

## Consequências

Desaparecem do escopo: guardar senha com hash, recuperação de senha,
verificação de e-mail, bloqueio por tentativa repetida, e a responsabilidade de
ter senha de outras pessoas no banco.

Escopo mínimo é também o que dispensa o aplicativo da verificação do Google
(RNF-21). O sistema não usa o refresh token do Google: depois do login, a
sessão é o token de renovação próprio (ADR-0041), e as regras do Google sobre a
validade daquele token não a alcançam.

O portão é um só, o convite: o Google autentica qualquer conta, e o sistema
recusa quem não está na lista de convidados (UC-01, fluxo 1a). Sem a
verificação do Google, que é opcional, a tela de login dele pode não mostrar o
nome e o logo do aplicativo.

Indisponibilidade do Google impede login novo. Sessão aberta não cai.

## Alternativa recusada: autenticação com senha própria

O trabalho de verdade não é a tela de login, é o que vem atrás dela, e mais da
metade desse trabalho depende de enviar e-mail, que este desenho não faz. Pelo
lado do aprendizado, ela ensinaria hash de senha em lugar de OAuth, e a
prioridade declarada do projeto favorece o produto.
