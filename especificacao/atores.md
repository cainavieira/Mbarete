# Atores

Define quem interage com o sistema. Todo requisito funcional e todo caso de
uso referencia um ator daqui pelo identificador.

## AT-01 Usuário

Pessoa que registra o próprio peso e acompanha o próprio ciclo de treino.

- Único ator com interface no sistema.
- Vê exclusivamente os próprios dados. Não existe tela que mostre dado de
  outro usuário.
- Entra no sistema apenas por convite emitido pelo AT-02.
- Autentica-se pelo AT-03. O sistema nunca guarda senha.

## AT-02 Operador

Quem mantém o sistema no ar. Na v1 é uma pessoa só, a mesma que desenvolve.

- Emite convite. Convidar é ação exclusiva dele: o AT-01 não convida ninguém.
- Dispara o deploy, que também aplica a migração (ADR-0037, ADR-0040).
- Executa restauração de backup e manutenção do servidor.
- Não tem interface no sistema na v1. Age por fora, pelo console do Google,
  pelo terminal do servidor e pelo `kubectl`. Não existe tela de administração.
- Tem acesso técnico ao banco de dados. Isso é consequência do desenho, está
  declarado no RNF de privacidade, e não é prometido de outra forma.

## AT-03 Google

Sistema externo. Autentica o AT-01 e devolve nome, e-mail e foto de perfil.

- É a única fonte de identidade do sistema.
- Autentica qualquer conta Google. Quem decide quem entra é o convite (RN-48).
- Indisponibilidade dele impede login novo. Sessão já aberta não cai.

## Quem não é ator, e por quê

- **Convidado**: pessoa que recebeu convite e ainda não criou conta. É um
  estado do AT-01, não um ator. Ele não executa nenhum caso de uso que o
  usuário não execute, apenas os executa pela primeira vez.
- **Amigo ou grupo**: recusado. Contas são isoladas e ninguém vê dado alheio,
  então não existe ator que observe o peso de outro.
- **Administrador com tela**: recusado na v1. O que ele faria, convidar e
  restaurar backup, o AT-02 faz por fora do sistema. Construir tela para isso
  é trabalho que nenhum usuário vê.
