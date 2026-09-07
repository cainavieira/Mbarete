# Requisitos não funcionais

O que o sistema tem que ser, em contraste com o que ele faz. Todo item traz um
número quando existe número, e diz explicitamente quando não existe meta e por
quê. RNF sem número é adjetivo.

## Custo

- **RNF-01** O custo de operação não excede R$40 por ano, correspondentes ao
  domínio. Servidor, banco e túnel custam zero por rodarem em hardware próprio.

## Disponibilidade

- **RNF-02** Não há compromisso de disponibilidade. Ficar fora do ar não é
  incidente, porque o registro retroativo é livre e nada se perde. (RN-06)
- **RNF-03** A aplicação depende da energia e da conexão residencial do AT-02.
  É limitação aceita, não defeito a corrigir.

## Preservação do dado

- **RNF-04** Perder histórico é a única falha irreversível deste sistema. Peso
  de dois anos atrás não se reconstrói e não existe fonte para recuperá-lo.
  Todo o resto é reversível, e é por isso que os requisitos desta seção são os
  mais duros do documento.
- **RNF-05** O banco tem cópia diária em três lugares, duas mídias, uma fora
  de casa: banco vivo no SSD, dump no disco lento do mesmo servidor, dump em
  armazenamento externo gratuito.
- **RNF-06** A perda máxima aceitável é de 24 horas de dado.
- **RNF-07** Retenção: 30 dias no disco lento, 90 dias na cópia externa, e uma
  cópia mensal guardada por 12 meses.
- **RNF-08** A restauração é testada a cada migração de esquema do banco, não
  por calendário. Dump antigo deixa de restaurar exatamente quando o esquema
  muda, e teste trimestral pode passar semanas depois da quebra.
- **RNF-09** Backup que não executou gera alerta, por interruptor de homem
  morto: o script avisa um serviço externo ao terminar, e o silêncio no
  horário esperado dispara o aviso. O verificador fica fora da casa do AT-02,
  porque monitor no mesmo servidor cai junto e fica quieto.

## Privacidade

- **RNF-10** O AT-02 tem acesso técnico ao banco. Não há criptografia ponta a
  ponta, e nada no sistema promete o contrário.
- **RNF-11** Cada usuário acessa apenas os próprios dados, e não existe tela
  que apresente dado alheio. (RN-50)
- **RNF-12** O sistema não armazena senha. (RN-49)
- **RNF-13** O log nunca registra valor de peso nem de cintura.
- **RNF-14** Apagar a conta remove tudo, sem retenção e sem anonimização.
  (RN-51)
- **RNF-15** O usuário pode levar o próprio histórico embora em CSV, para que
  sair do sistema não custe o dado. (RN-52)

## Registro operacional

- **RNF-16** O log de aplicação registra apenas nível erro e acima. Operação
  bem-sucedida não gera linha: ela não responde pergunta nenhuma e aumenta o
  volume onde dado sensível pode escapar.
- **RNF-17** O log de acesso do servidor web é mantido, porque é o que revela
  varredura e tentativa de entrada num serviço exposto na internet.
- **RNF-18** Retenção de 7 dias para os dois. O caso de uso do log é o usuário
  relatando problema no dia seguinte ou depois do fim de semana.

## Segurança

- **RNF-19** A rede doméstica do AT-02 não tem porta aberta para a internet. A
  exposição acontece por túnel de saída, e o endereço residencial não fica
  visível.
- **RNF-20** O banco não é alcançável de fora da rede interna da aplicação.
- **RNF-21** Os escopos pedidos ao AT-03 se limitam a identidade: openid,
  e-mail e perfil. Escopo mínimo dispensa verificação do aplicativo e reduz o
  que o sistema tem permissão de ver.
- **RNF-22** A sessão dura 30 dias e renova a cada uso, com encerramento
  explícito disponível. Sessão curta é atrito que faz a pessoa parar de
  registrar, que é o único jeito real de este produto falhar.
- **RNF-23** A recusa de acesso não informa se um e-mail existe no sistema.

## Idioma

- **RNF-24** Português do Brasil apenas, sem estrutura de tradução. (RN-61)

## Desempenho

- **RNF-25** Não há meta de latência, e a ausência é deliberada. Cinco usuários
  com uma escrita diária cada produzem por volta de 9.000 linhas em cinco anos,
  volume que o banco atende em milissegundos sem índice além da chave primária.
  Fixar número aqui seria inventar restrição.
- **RNF-26** A meta que existe é de interação: registrar o peso do dia cabe em
  uma tela e uma confirmação, sem passo intermediário. O atrito que faz alguém
  parar de registrar é de cliques, não de milissegundos.

## Manutenibilidade

- **RNF-27** As regras de agregação (peso semanal, taxa real, veredito de
  ritmo e progresso) são cobertas por teste automatizado. São a parte do
  sistema onde o erro passa despercebido: o número sai errado e continua
  parecendo plausível, ao contrário de uma tela que quebra e aparece.

## Premissa de fim econômico

- **RNF-28** O sistema nasce sem fim econômico. Nessa condição ele se encaixa
  na exclusão da LGPD para tratamento realizado por pessoa natural com fins
  exclusivamente particulares e não econômicos.
- **RNF-29** No dia em que houver cobrança, esta premissa cai e reabre três
  frentes: base legal e consentimento para dado de saúde, política de
  privacidade, e as licenças de ferramenta que proíbem uso comercial. Nada
  disso é implementado agora. Está escrito para não ser descoberto tarde.
