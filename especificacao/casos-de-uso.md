# Casos de uso

Cada caso de uso descreve uma interação completa, do gatilho ao estado final.

O fluxo principal é o caminho sem erro. Os **fluxos alternativos** são o valor
deste documento: eles dizem o que acontece quando algo dá errado, e são o
lugar onde as regras foram testadas contra a realidade em vez de assumidas.

Atores em `atores.md`, requisitos em `requisitos-funcionais.md`, regras em
`regras-de-negocio.md`.

## UC-01 Entrar no sistema

**Ator:** AT-01
**Pré-condição:** o e-mail consta na lista de convidados e na lista de
usuários de teste do AT-03.

**Fluxo principal**

1. A pessoa abre o endereço do sistema e escolhe entrar com a conta Google.
2. O AT-03 autentica e devolve nome, e-mail e foto. (RF-01)
3. O sistema confere o e-mail na lista de convidados. (RF-02, RN-48)
4. Existindo conta, o sistema abre a sessão e apresenta a tela de
   acompanhamento.
5. Não existindo conta, o sistema a cria junto com a primeira fase, de
   manutenção e sem data de início. (RF-03, RN-21, RN-64)

**Fluxos alternativos**

- **1a** O e-mail não está na lista de convidados: o sistema recusa dizendo
  que o acesso é por convite, sem informar se aquele e-mail existe no sistema.
- **1b** O e-mail não está na lista de usuários de teste do AT-03: a recusa
  acontece no Google, antes de o sistema ser alcançado, e a pessoa vê a tela
  do Google dizendo que o aplicativo não está autorizado. O sistema não tem
  como melhorar essa mensagem, e essa é uma limitação conhecida do modo
  Testing.
- **1c** O AT-03 está indisponível: nenhum login novo acontece. Sessão já
  aberta continua válida.

**Pós-condição:** sessão aberta, ou nenhuma mudança de estado.

## UC-02 Registrar peso

**Ator:** AT-01
**Pré-condição:** sessão aberta.

**Fluxo principal**

1. O usuário abre o formulário, que já vem com a data do dia corrente do
   dispositivo. (RN-04)
2. Informa o peso. (RF-07)
3. Informa a cintura, se quiser. (RF-09, RN-10)
4. Confirma.
5. O sistema valida, grava, e recalcula peso semanal, variações, veredito e
   barra de progresso.

**Fluxos alternativos**

- **1a** O usuário troca para uma data passada: aceito sem restrição de
  janela. (RF-08, RN-06)
- **1b** O usuário troca para data futura: recusado. (RN-05)
- **1c** A data escolhida é anterior a qualquer fase existente: nada de
  especial acontece, porque a primeira fase cobre todo o passado. (RN-64)
- **2a** Peso fora de 20,0 a 400,0 kg: recusado, informando o campo e o
  limite. (RF-13, RN-03)
- **3a** Cintura fora de 40,0 a 200,0 cm: recusada do mesmo modo. (RN-11)
- **4a** Já existe registro naquela data: o sistema mostra o valor gravado,
  pede confirmação, e substitui apenas com o sim. (RF-10, RN-01)
- **4b** O usuário recusa a substituição: nada é gravado e o registro anterior
  permanece intacto.

**Pós-condição:** no máximo um registro naquela data. (RN-01)

## UC-03 Editar registro

**Ator:** AT-01

**Fluxo principal**

1. O usuário abre um registro da lista. (RF-14)
2. Altera peso, cintura ou data. (RF-11, RN-07)
3. Confirma e o sistema revalida e recalcula.

**Fluxos alternativos**

- **2a** A nova data já tem registro: cai na confirmação de substituição do
  UC-02, e aceitar significa que dois registros passam a ser um.
- **2b** A nova data pertence a outra fase: o registro passa a pertencer à
  fase daquela data, porque a fase é derivada e não guardada. (RN-23)
- **2c** Valor fora de faixa ou data futura: recusado. (RF-13)

## UC-04 Apagar registro

**Ator:** AT-01

**Fluxo principal**

1. O usuário escolhe apagar um registro e confirma. (RF-12, RN-08)
2. O sistema remove o dado e recalcula o que dependia dele.

**Fluxos alternativos**

- **2a** A remoção derruba a janela para menos de 4 registros: o peso semanal
  passa a informar dados insuficientes em vez de exibir número. (RN-16)
- **2b** A remoção derruba a fase para menos de 8 registros: o veredito volta
  a dados insuficientes. (RN-39)

## UC-05 Criar fase

**Ator:** AT-01

**Fluxo principal**

1. O usuário escolhe o tipo: cutting, bulking ou manutenção. (RN-18)
2. Informa a data de início.
3. Escolhe o nível, e o sistema preenche a taxa alvo sugerida. (RF-16, RN-31,
   RN-32)
4. Informa o peso alvo. (RN-41)
5. Confirma. O sistema encerra a fase corrente na véspera e a nova passa a ser
   a corrente. (RF-19, RN-22)

**Fluxos alternativos**

- **1a** O tipo é manutenção: o sistema não pede nível nem peso alvo, e usa
  taxa alvo zero com tolerância de 0,5%. (RF-24, RN-33)
- **2a** A data de início é passada e cai dentro da fase corrente: aceito, e a
  fase corrente é recortada. É o caso comum, quando a pessoa começou o ciclo
  antes de cadastrar.
- **2b** A data de início cai dentro de fase já encerrada: recusado. Para
  reclassificar histórico, o caminho é mover a fronteira das fases
  existentes. (UC-06)
- **2c** A data de início é futura: recusada, porque deixaria o intervalo
  entre hoje e ela sem fase. (RN-20)
- **3a** O usuário edita o valor da taxa alvo: o nível passa a personalizado.
  (RF-18, RN-30)
- **4a** O peso alvo é incoerente com o tipo, como cutting com alvo acima do
  peso corrente: recusado. (RF-25, RN-65)

**Pós-condição:** exatamente uma fase corrente, sem sobreposição e sem
intervalo descoberto. (RN-19, RN-20)

## UC-06 Editar fase

**Ator:** AT-01

**Fluxo principal**

1. O usuário abre uma fase.
2. Altera tipo, nível, taxa alvo, peso alvo ou a data de início.
3. Confirma.

**Fluxos alternativos**

- **2a** A data de início é alterada: a fronteira se move e a fase anterior
  tem o fim ajustado no mesmo ato. Os registros do intervalo passam a
  pertencer à fase que agora cobre suas datas. (RF-20, RN-24)
- **2b** A fase é a primeira da conta: ela não tem data de início editável,
  porque cobre todo o passado. O que se move é a fronteira dela com a
  seguinte. (RN-64)
- **2c** O tipo muda para manutenção: peso alvo e nível deixam de existir para
  aquela fase e seus valores são descartados. (RF-24)
- **2d** O tipo muda de manutenção para cutting ou bulking: o sistema passa a
  exigir nível e peso alvo antes de aceitar. (RN-41)
- **2e** A taxa alvo muda de unidade: o valor é reinterpretado na nova
  unidade, sem conversão automática, porque 0,5% e 0,5 kg são alvos
  diferentes e adivinhar qual a pessoa quis é pior que perguntar.

## UC-07 Apagar fase

**Ator:** AT-01

**Fluxo principal**

1. O usuário escolhe apagar uma fase sem registros e confirma. (RF-22, RN-26)
2. A fronteira desaparece e a fase anterior absorve o intervalo.

**Fluxos alternativos**

- **1a** A fase contém registro: recusado, informando que o caminho é corrigir
  tipo, nível, alvo ou fronteira. (RF-21, RN-25)
- **1b** É a única fase da conta: recusado, porque deixaria registros sem
  fase possível. (RN-26)

## UC-08 Acompanhar o ciclo corrente

**Ator:** AT-01
**Gatilho:** entrar no sistema, ou gravar qualquer registro.

**Fluxo principal**

1. O sistema apresenta o peso semanal. (RF-26, RN-15)
2. Apresenta o gráfico com os pesos diários como pontos e o peso semanal como
   linha. (RF-28)
3. Apresenta as três variações, cada uma com o intervalo de datas usado.
   (RF-29, RN-47)
4. Apresenta o veredito de ritmo da fase corrente e a taxa alvo convertida em
   quilogramas da semana. (RF-30, RF-32)
5. Apresenta a barra de progresso até o peso alvo. (RF-33)
6. Apresenta a cintura, se houver alguma medição. (RF-35)

**Fluxos alternativos**

- **1a** A janela tem menos de 4 registros: dados insuficientes no lugar do
  peso semanal, e sem peso semanal não há veredito nem barra. (RN-16)
- **4a** A fase tem menos de 14 dias ou menos de 8 registros: dados
  insuficientes no lugar do veredito, mas o peso semanal e as variações
  continuam sendo exibidos. (RN-39)
- **4b** A fase corrente é manutenção: o veredito compara contra a tolerância
  de 0,5% em vez de contra uma taxa alvo. (RN-33)
- **5a** A fase corrente é manutenção: não existe barra de progresso, porque
  não existe peso alvo. (RN-41)
- **5b** O peso alvo foi atingido: o sistema avisa, a barra trava em 100%, e a
  fase continua aberta. (RF-34, RN-44, RN-45)
- **6a** Nunca houve medição de cintura: a seção não aparece.
- **1b** A conta não tem nenhum registro: a tela convida ao primeiro registro
  em vez de exibir estrutura vazia.

## UC-09 Consultar fases encerradas

**Ator:** AT-01

**Fluxo principal**

1. O usuário abre o histórico de fases.
2. O sistema lista as fases encerradas com tipo, período, duração, variação
   total e se o peso alvo foi atingido. (RF-23, RN-40)

**Fluxos alternativos**

- **2a** Nenhuma fase encerrada ainda: a lista informa isso, sem convite a
  ação, porque encerrar fase não é objetivo.
- **2b** A fase encerrada tinha menos registros que o mínimo: a variação total
  é exibida com a marca de que foi calculada sobre poucos pontos, em vez de
  omitida.

## UC-10 Importar histórico em CSV

**Ator:** AT-01

**Fluxo principal**

1. O usuário envia o arquivo. (RF-37)
2. Escolhe o tipo de fase para o período importado, com manutenção como
   padrão. (RF-39, RN-56)
3. O sistema lê, valida e apresenta a pré-visualização: linhas válidas, linhas
   recusadas com número e motivo, e datas já existentes que serão
   substituídas. (RF-38, RN-58)
4. O usuário confirma.
5. O sistema grava tudo em uma única operação. (RF-40, RN-59)

**Fluxos alternativos**

- **1a** Faltam as colunas obrigatórias: o arquivo é recusado inteiro antes da
  pré-visualização, porque nem dá para começar a ler. (RN-53)
- **1b** O separador não é reconhecido: mesma recusa, dizendo quais são
  aceitos. (RN-54)
- **3a** Algumas linhas são inválidas: elas aparecem na pré-visualização com
  linha e motivo, e as válidas seguem disponíveis para gravação. (RN-60)
- **3b** Nenhuma linha é válida: a confirmação fica indisponível.
- **4a** O usuário abandona: nada é gravado. (RF-41)
- **5a** A gravação falha no meio: nada é gravado, e o histórico continua como
  estava antes do envio. (RN-59)

**Pós-condição:** todas as linhas gravadas, ou nenhuma.

## UC-11 Exportar histórico em CSV

**Ator:** AT-01

**Fluxo principal**

1. O usuário pede a exportação. (RF-43, RN-52)
2. O sistema entrega um arquivo com data, peso e cintura de todos os
   registros, no formato que a própria importação aceita.

**Fluxos alternativos**

- **2a** A conta não tem registros: o arquivo sai apenas com o cabeçalho, em
  vez de erro. Arquivo vazio é resposta válida para histórico vazio.

## UC-12 Apagar a conta

**Ator:** AT-01

**Fluxo principal**

1. O usuário escolhe apagar a conta na tela de conta.
2. O sistema explica o que será removido e exige que o usuário digite o
   próprio e-mail para confirmar.
3. O sistema remove registros, fases e dados de conta, e encerra a sessão.
   (RF-06, RN-51)

**Fluxos alternativos**

- **2a** O texto digitado não confere: a remoção não acontece.
- **3a** O e-mail continua na lista de convidados: a pessoa pode entrar
  novamente e receber uma conta nova e vazia. Apagar conta remove dado, não
  revoga convite.

**Pós-condição:** nenhum dado do usuário permanece. Não há retenção nem
anonimização. (RN-51)

## UC-13 Convidar uma pessoa

**Ator:** AT-02
**Observação:** acontece fora do sistema, porque o AT-02 não tem interface.

**Fluxo principal**

1. O operador roda o comando de linha no servidor, informando o e-mail.
   (RF-44)
2. O comando insere o e-mail na lista de convidados.
3. O operador adiciona o mesmo e-mail na lista de usuários de teste do AT-03,
   pelo console do Google.
4. O operador avisa a pessoa por fora, e passa o endereço do sistema.

**Fluxos alternativos**

- **2a** O e-mail já está na lista: o comando informa e não duplica.
- **3a** O passo 3 é esquecido: a pessoa recebe recusa do Google no login, com
  a mensagem do fluxo 1b do UC-01, e sem pista do motivo. Os dois passos
  precisam andar juntos, e essa é a limitação conhecida do modo Testing.

**Limitação conhecida:** o sistema não envia e-mail de convite. Não existe
serviço de e-mail no desenho, e adicioná-lo custaria domínio verificado e
configuração de DNS para entregar uma mensagem que o operador manda em
segundos para um grupo pequeno.
