# ADR-0011 Entrada por convite emitido pelo operador

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O sistema é para o interessado e alguns amigos. Contas são isoladas e ninguém
vê dado alheio (RN-50). Não existe serviço de e-mail no desenho.

## Decisão

A entrada acontece somente por convite, e emitir convite é ação exclusiva do
AT-02. O registro do convite é uma linha em tabela do banco, inserida por
comando de linha executado no servidor (RF-44).

O convidado é avisado por fora do sistema.

## Consequências

Não existe fluxo de convite com estado, expiração e recusa, que seria o custo de
permitir que usuário convide usuário.

Convidar é uma ação só do operador, o comando de linha (UC-13).

A lista de convidados é **dado**, não configuração, porque muda com o sistema no
ar. Por isso mora no banco e não em variável de ambiente.

## Alternativa recusada: cadastro aberto na internet

Obrigaria a resolver confirmação de e-mail, recuperação de senha, abuso e custo
de hospedagem que cresce sozinho, nada disso a serviço do produto.

## Alternativa recusada: convite emitido por usuário

Exigiria o fluxo de convite com estado, expiração e recusa que esta decisão
evita, num sistema feito para o interessado e alguns amigos.

## Alternativa recusada: lista de e-mails em variável de ambiente

Exigiria recriar o contêiner a cada convite, porque variável de ambiente é
copiada para dentro do processo na subida e não muda depois.
