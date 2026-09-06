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
- Cadastra o e-mail do convidado na lista de usuários de teste do AT-03.
  Passo manual enquanto o aplicativo estiver em modo Testing.
- Executa restauração de backup e manutenção do servidor.
- Não tem interface no sistema na v1. Age por fora, pelo console do Google e
  pelo terminal do servidor. Não existe tela de administração.
- Tem acesso técnico ao banco de dados. Isso é consequência do desenho, está
  declarado no RNF de privacidade, e não é prometido de outra forma.

## AT-03 Google

Sistema externo. Autentica o AT-01 e devolve nome, e-mail e foto de perfil.

- É a única fonte de identidade do sistema.
- Em modo Testing, autentica apenas os e-mails cadastrados pelo AT-02, até 100.
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
