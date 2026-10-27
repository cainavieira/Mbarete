# Casos de uso

Cada caso de uso descreve uma interação completa, do gatilho ao estado final.

O fluxo principal é o caminho sem erro. Os **fluxos alternativos** são o valor
deste documento: eles dizem o que acontece quando algo dá errado, e são o
lugar onde as regras foram testadas contra a realidade em vez de assumidas.

Atores em `atores.md`, requisitos em `requisitos-funcionais.md`, regras em
`regras-de-negocio.md`.

## UC-01 Entrar no sistema

**Ator:** AT-01
**Pré-condição:** o e-mail consta na lista de convidados.

**Fluxo principal**

1. A pessoa abre o endereço do sistema e escolhe entrar com a conta Google.
2. O AT-03 autentica e devolve nome, e-mail e foto. (RF-01)
3. O sistema confere o e-mail na lista de convidados. (RF-02, RN-48)
4. Existindo conta, o sistema abre a sessão e apresenta a tela de
   acompanhamento.
5. Não existindo conta, o sistema a cria. A conta nasce **sem fase alguma**, e
   o primeiro peso pode ser registrado antes de qualquer decisão de ciclo.
   (RF-03, RN-20, RN-66)

**Fluxos alternativos**

- **1a** O e-mail não está na lista de convidados: o sistema volta para a
  tela de entrada dizendo que o acesso é por convite, sem informar se aquele
  e-mail existe no sistema. (RF-02, RNF-23)
- **1c** O AT-03 está indisponível: nenhum login novo acontece. Sessão já
  aberta continua válida.
- **1d** O login não se completa no retorno do Google: a pessoa cancelou na
  tela de consentimento, ou a verificação do retorno falhou. O sistema volta
  para a tela de entrada dizendo que o login não foi concluído e que dá para
  tentar de novo, sem abrir sessão. (RF-01)

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
- **1c** A data escolhida não pertence a fase alguma: o registro é gravado
  normalmente. Ele conta para o peso semanal e para as variações, e não recebe
  veredito nem progresso. (RN-20, RN-66)
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
- **2b** A nova data pertence a outra fase, ou a nenhuma: o registro passa a
  pertencer à fase que cobre a nova data, ou a nenhuma, porque a fase é
  derivada e não guardada. (RN-20, RN-23)
- **2c** Valor fora de faixa ou data futura: recusado. (RF-13)

## UC-04 Apagar registro

**Ator:** AT-01

**Fluxo principal**

1. O usuário escolhe apagar um registro e confirma. (RF-12, RN-08)
2. O sistema remove o dado e recalcula o que dependia dele.

**Fluxos alternativos**

- **2a** A remoção derruba a janela para menos de 4 registros: o peso semanal
  passa a informar dados insuficientes em vez de exibir número. (RN-16)
- **2b** A remoção derruba uma das duas janelas comparadas para menos de 4
  registros: o veredito volta a dados insuficientes. (RN-36, RN-39)

## UC-05 Criar fase

**Ator:** AT-01

**Fluxo principal**

1. O usuário escolhe o tipo: cutting, bulking ou manutenção. (RN-18)
2. Informa a data de início, que pode ser passada, hoje ou futura, e a data de
   fim, se a fase já terminou. (RF-15, RN-69)
3. Escolhe o nível, e o sistema preenche a taxa alvo sugerida. (RF-16, RN-31,
   RN-32)
4. Informa o peso alvo. (RN-41)
5. Confirma.

**Fluxos alternativos**

- **1a** O tipo é manutenção: o sistema não pede nível nem peso alvo, e usa
  taxa alvo zero, com a faixa de tolerância de 1% em volta da base. (RF-24,
  RN-33)
- **2a** A data de início é passada ou hoje, cai dentro da fase **em aberto**
  depois do início dela, e a fase nova não tem data de fim: aceito, e a fase
  em aberto é encerrada na véspera. É o caso comum, de quem começou o ciclo
  antes de cadastrar. (RF-19, RN-19, RN-22)
- **2b** A data de início cai dentro de fase que já tem data de fim: recusado
  por sobreposição. (RF-46, RN-19)
- **2c** A fase nova cabe inteira num período **sem fase**: aceito, e é assim
  que se classifica histórico importado depois do fato, informando também a
  data de fim. (RN-57)
- **2d** A data de início é futura: aceito. A fase aparece na lista de fases e
  não entra no acompanhamento até começar. Se houver fase em aberto, ela é
  encerrada na véspera, e a tela avisa isso antes da confirmação. (RN-22,
  RN-69)
- **2e** A data de fim é informada e a fase invade a fase em aberto: recusado.
  Para classificar um período dentro do ciclo atual, a pessoa encerra a fase
  em aberto antes. Se o período começa no mesmo dia da fase em aberto,
  encerrá-la antes não é possível (UC-14 2b), e a pessoa adia antes o início
  dela (UC-06). (RF-46, RN-19)
- **2f** A data de fim é futura, ou anterior à data de início: recusado.
  (RF-15, RN-18, RN-67)
- **2g** A fase nova, sem data de fim, começa no mesmo dia da fase em aberto ou
  antes dela: recusado. Para adiantar o início de um ciclo, edita-se a data de
  início da fase existente (UC-06). (RF-46, RN-19)
- **2h** A data de início cai num período sem fase, mas a fase nova invade a
  seguinte, que já tem data de fim: recusado por sobreposição. (RF-46, RN-19)
- **3a** O usuário edita o valor ou a unidade da taxa alvo: o nível passa a
  personalizado. (RF-18, RN-30)
- **3b** O nível escolhido é personalizado: não há taxa sugerida, e o sistema
  exige o valor antes de aceitar. (RN-29, RN-31, RN-32)
- **3c** O valor da taxa alvo é digitado sem sinal, e o sistema aplica o do
  tipo; valor zero é recusado. (RN-29)
- **5a** O peso alvo está do lado errado da base: a fase é criada, e a barra
  de progresso não é exibida enquanto isso durar, em vez de aparecer em 100%.
  Se a base ainda não existe, a comparação acontece quando ela existir.
  (RN-44, RN-68)

**Pós-condição:** nenhuma sobreposição entre fases. Pode haver período sem
fase, e pode não haver fase corrente. (RN-19, RN-20, RN-27)

## UC-06 Editar fase

**Ator:** AT-01

**Fluxo principal**

1. O usuário abre uma fase.
2. Altera tipo, nível, taxa alvo, peso alvo, data de início ou data de fim.
3. Confirma.

**Fluxos alternativos**

- **2a** Uma das datas é alterada: os registros que entraram ou saíram do
  intervalo mudam de fase, porque a fase é derivada da data. Registro que saiu
  e não caiu em outra fase passa a não ter fase. (RF-20, RN-20, RN-23, RN-24)
- **2b** A alteração criaria sobreposição com outra fase, em aberto ou não:
  recusada, sem encerrar nenhuma fase, salvo o caso do 2i. Para adiantar o
  início de uma fase colada na anterior, encurta-se antes a anterior. (RF-46,
  RN-19)
- **2c** O tipo muda para manutenção: peso alvo, nível e taxa alvo deixam de
  existir para aquela fase e seus valores são descartados. (RF-24, RN-33)
- **2d** O tipo muda de manutenção para cutting ou bulking: o sistema passa a
  exigir nível, taxa alvo e peso alvo antes de aceitar. (RN-28, RN-29, RN-41)
- **2e** A taxa alvo muda de unidade: o valor é reinterpretado na nova
  unidade, sem conversão automática, porque 0,5% e 0,5 kg são alvos
  diferentes e adivinhar qual a pessoa quis é pior que perguntar. O nível
  passa a personalizado. (RF-18, RN-30)
- **2f** Uma das datas é alterada: o dia da base do progresso, da tolerância e
  da taxa real do primeiro mês é determinado de novo, porque o intervalo da
  fase passou a ser outro. (RN-36, RN-68)
- **2g** O peso alvo passa para o lado errado da base: a alteração é aceita, e
  a barra deixa de ser exibida enquanto isso durar. (RN-44)
- **2h** O valor da taxa alvo é editado: o nível passa a personalizado.
  (RF-18, RN-30)
- **2i** A data de início muda, e a fase anterior tem fim no futuro, na
  véspera desta: esse fim vai para a nova véspera, inclusive quando ela é hoje
  ou já passou, e os registros desses dias mudam de fase. Antecipar para o
  início da fase anterior, ou antes, é recusado. (RF-19, RN-67)
- **2j** O tipo muda entre cutting e bulking: a taxa alvo passa ao padrão do
  mesmo nível no tipo novo, com valor, unidade e sinal dele; no personalizado,
  o sistema exige o valor de novo antes de aceitar. O peso alvo continua, e
  vale o 2g se ele ficar do lado errado da base. (RN-28, RN-29, RN-44)

## UC-07 Apagar fase

**Ator:** AT-01

**Fluxo principal**

1. O usuário escolhe apagar uma fase.
2. O sistema informa quantos registros pertencem a ela e vão perder o rótulo do
   período, e pede confirmação. (RF-22, RN-70)
3. Confirmada, a fase é removida. Os registros permanecem e passam a não ter
   fase.

**Fluxos alternativos**

- **2a** A fase não tem registro: a confirmação é pedida do mesmo jeito, sem
  número para informar.
- **3a** A fase apagada era a corrente: passa a não haver fase corrente, e o
  acompanhamento deixa de exibir veredito, taxa alvo, barra e tolerância.
  (RN-27, RN-66)
- **3b** A fase anterior à apagada tem fim no futuro, na véspera dela: esse
  fim vai para a véspera da próxima fase; se não houver próxima, ele some, e a
  anterior volta a ser a fase em aberto. (RF-19, RN-67)

**Pós-condição:** nenhum registro é perdido. A operação é reversível criando
fase sobre o mesmo intervalo, salvo quando a fase apagada tinha fim no futuro:
aí o caminho é o da RN-70. (RN-70)

## UC-08 Acompanhar o ciclo corrente

**Ator:** AT-01
**Gatilho:** entrar no sistema, ou gravar qualquer registro.

**Fluxo principal**

1. O sistema apresenta o peso semanal. (RF-26, RN-15)
2. Apresenta o gráfico dos últimos 120 dias, com os pesos diários como pontos,
   o peso semanal como linha, e os trechos com fase distintos dos trechos sem
   fase. (RF-28, RF-47, RF-49)
3. Apresenta as três variações, cada uma com o intervalo de datas usado.
   (RF-29, RN-47)
4. Apresenta o veredito de ritmo da fase corrente, a taxa real com o
   intervalo de datas usado, e a taxa alvo convertida em quilogramas da
   semana. (RF-30, RF-32)
5. Apresenta a barra de progresso até o peso alvo. (RF-33)
6. Apresenta a cintura, se houver alguma medição. (RF-35)

**Fluxos alternativos**

- **1a** A janela tem menos de 4 registros: dados insuficientes no lugar do
  peso semanal, e sem peso semanal não há variações, veredito nem barra.
  (RN-16, RN-47)
- **2a** O usuário escolhe outro período, de 30, 60 ou 90 dias: o gráfico
  passa a mostrar só esse trecho. (RF-49)
- **2b** O primeiro registro é mais recente que o período escolhido: o gráfico
  começa no primeiro registro e se alarga conforme os dias passam, em vez de
  mostrar trecho em branco. (RF-49)
- **4a** Em cutting ou bulking, ainda não se passaram 14 dias desde o dia da
  base, ou um dos dois pesos semanais comparados não existe: dados
  insuficientes no lugar do veredito, mas o peso semanal e as variações
  continuam sendo exibidos. (RN-36, RN-39)
- **4b** A fase corrente é manutenção: o veredito diz se o peso semanal de
  hoje está abaixo, dentro ou acima da faixa de tolerância, que vai de 1%
  abaixo a 1% acima da base. A faixa aparece no gráfico, do início da fase até
  hoje, e em texto ao lado do veredito, com os dois limites, no lugar da taxa
  real, que a manutenção não usa. O veredito sai junto com a faixa, sem a
  espera de 14 dias, e sem base não há nenhum dos dois: o sistema informa
  dados insuficientes. (RF-50, RN-33, RN-37, RN-39, RN-68)
- **4c** **Não existe fase corrente**: veredito, taxa alvo, barra de progresso
  e faixa de tolerância não são exibidos. Peso semanal, variações e cintura
  continuam, e é o comportamento esperado, não estado de erro. (RN-27, RN-66)
- **4d** Existe fase futura cadastrada: ela não entra no acompanhamento, apenas
  na lista de fases. (RN-69)
- **4e** Em cutting ou bulking, o dia da base é mais recente que 28 dias
  atrás: a taxa real é medida contra a base, e o intervalo exibido ao lado
  dela mostra isso. (RN-36, RN-68)
- **5a** A fase corrente é manutenção: não existe barra de progresso, porque
  não existe peso alvo. (RN-41)
- **5c** A base do progresso não existe, porque nenhum peso semanal ocorreu
  dentro da fase, ou porque a janela do dia da base deixou de ter 4
  registros: a barra informa dados insuficientes em vez de aparecer vazia.
  (RN-68)
- **5b** O peso alvo foi atingido: o sistema avisa, a barra trava em 100%, e a
  fase continua aberta. (RF-34, RN-44, RN-45)
- **5d** O peso alvo está do lado errado da base: a barra não é exibida, e o
  aviso de alvo atingido não dispara. (RN-44)
- **6a** Nunca houve medição de cintura: a seção não aparece.
- **1b** A conta não tem nenhum registro: a tela convida ao primeiro registro
  em vez de exibir estrutura vazia.

## UC-09 Consultar fases encerradas

**Ator:** AT-01

**Fluxo principal**

1. O usuário abre o histórico de fases.
2. O sistema lista as fases encerradas, as que têm data de fim anterior a
   hoje, com tipo, período, duração, variação total e se o peso alvo foi
   atingido. (RF-23, RN-40)

**Fluxos alternativos**

- **2a** Nenhuma fase encerrada ainda: a lista informa isso, sem convite a
  ação, porque encerrar fase não é objetivo.
- **2b** A fase encerrada tinha menos registros que o mínimo: a variação total
  é exibida com a marca de que foi calculada sobre poucos pontos, em vez de
  omitida.
- **2c** O peso alvo da fase está do lado errado da base: ela não é marcada
  como atingida, e a lista indica que o alvo estava do lado errado. (RN-40,
  RN-44)

## UC-10 Importar histórico em CSV

**Ator:** AT-01

**Fluxo principal**

1. O usuário envia o arquivo. (RF-37)
2. O sistema lê, valida e apresenta a pré-visualização: linhas válidas, linhas
   recusadas com número e motivo, e datas já existentes que serão
   substituídas. (RF-38, RN-58)
3. O usuário confirma.
4. O sistema grava tudo em uma única operação, **sem criar fase**. As datas
   importadas ficam sem fase. (RF-40, RN-57, RN-59)

**Fluxos alternativos**

- **1a** Faltam as colunas obrigatórias: o arquivo é recusado inteiro antes da
  pré-visualização, porque nem dá para começar a ler. (RN-53)
- **1b** O separador não é reconhecido: mesma recusa, dizendo quais são
  aceitos. (RN-54)
- **2a** Algumas linhas são inválidas: elas aparecem na pré-visualização com
  linha e motivo, e as válidas seguem disponíveis para gravação. (RN-60)
- **2b** Nenhuma linha é válida: a confirmação fica indisponível.
- **3a** O usuário abandona: nada é gravado. (RF-41)
- **4a** A gravação falha no meio: nada é gravado, e o histórico continua como
  estava antes do envio. (RN-59)
- **4b** Depois de importar, o usuário quer classificar o período: cria fase
  com início e fim sobre o intervalo importado, que é operação normal do
  UC-05, porque aquelas datas estão sem fase. (RN-57)

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
2. O sistema explica o que será removido, avisa que as cópias de backup somem
   no fim da retenção, e exige que o usuário digite o próprio e-mail para
   confirmar.
3. O sistema remove registros, fases e dados de conta, e encerra a sessão.
   (RF-06, RN-51)

**Fluxos alternativos**

- **2a** O texto digitado não confere: a remoção não acontece.
- **3a** O e-mail continua na lista de convidados: a pessoa pode entrar
  novamente e receber uma conta nova e vazia. Apagar conta remove dado, não
  revoga convite.

**Pós-condição:** nenhum dado do usuário permanece no banco, e não há
anonimização. As cópias de backup somem no fim da retenção. (RN-51)

## UC-13 Convidar uma pessoa

**Ator:** AT-02
**Observação:** acontece fora do sistema, porque o AT-02 não tem interface.

**Fluxo principal**

1. O operador roda o comando de linha no servidor, informando o e-mail.
   (RF-44)
2. O comando insere o e-mail na lista de convidados.
3. O operador avisa a pessoa por fora, e passa o endereço do sistema.

**Fluxos alternativos**

- **2a** O e-mail já está na lista: o comando informa e não duplica.

**Limitação conhecida:** o sistema não envia e-mail de convite. Não existe
serviço de e-mail no desenho, e adicioná-lo custaria domínio verificado e
configuração de DNS para entregar uma mensagem que o operador manda em
segundos para um grupo pequeno.

## UC-14 Encerrar fase

**Ator:** AT-01
**Pré-condição:** existe fase corrente. (RN-27)

**Fluxo principal**

1. O usuário escolhe encerrar a fase corrente.
2. Informa a data de encerramento, com hoje como padrão.
3. Confirma. A fase passa a ter data de fim, e a partir do dia seguinte não há
   fase. (RF-45, RN-67)

**Fluxos alternativos**

- **2a** A data informada é futura: recusada. Para que a fase termine numa data
  futura, o caminho é criar a fase seguinte começando no dia seguinte, e a
  RN-22 encerra esta sozinha. (RN-67)
- **2b** A data informada é anterior ao início da fase: recusada. (RN-18)
- **3a** Existem registros depois da data de encerramento: eles permanecem e
  passam a não ter fase. (RN-20)

**Pós-condição:** a partir do dia seguinte à data de encerramento, não há fase
corrente, até que outra seja criada ou que a fase agendada comece. (RN-27,
RN-69)
