# Regras de negócio

Regras que valem independentemente de tela, rota ou tecnologia. Requisitos
funcionais e casos de uso citam estas regras pelo identificador em vez de
repetir o texto.

O identificador é estável: regra removida tem o número aposentado, nunca
reaproveitado por outra regra. As aposentadas estão listadas no fim, com o
motivo.

## Registro de peso

- **RN-01** Existe no máximo um registro por usuário por data. Uma segunda
  pesagem na mesma data substitui a primeira, e a substituição exige
  confirmação explícita do usuário.
- **RN-02** O peso é expresso em quilogramas, com uma casa decimal.
- **RN-03** O peso aceito vai de 20,0 a 400,0 kg. Fora dessa faixa o registro
  é recusado.
- **RN-04** A data do registro é data pura, sem hora e sem fuso horário. O dia
  corrente é o do dispositivo do usuário, enviado junto com a requisição.
- **RN-05** Data futura é recusada.
- **RN-06** Registro retroativo é livre para qualquer data passada, sem limite
  de janela.
- **RN-07** Editar registro existente é livre, sem janela de tempo.
- **RN-08** Apagar registro é permitido e remove o dado. Não há exclusão
  lógica nem histórico de alterações.
- **RN-09** Dia sem registro é ausência de dado. O sistema não repete o último
  peso conhecido nem interpola entre registros vizinhos.

## Cintura

- **RN-10** A cintura é campo opcional do registro do dia, em centímetros, com
  uma casa decimal.
- **RN-11** A cintura aceita vai de 40,0 a 200,0 cm. Fora dessa faixa é
  recusada.
- **RN-12** A cintura não tem valor semanal nem janela móvel. O sistema
  apresenta o último valor medido, a data da medição e a variação desde a
  medição anterior. Havendo fase na data, apresenta também a variação desde a
  primeira medição dentro dessa fase.
- **RN-13** O campo cintura existe em qualquer tipo de fase, e também em data
  sem fase. Destacá-lo durante cutting é comportamento de tela, não regra.
- **RN-14** Dado medido não é apagado nem escondido por mudança de
  configuração posterior. Vale para toda a aplicação, não só para a cintura.

## Peso semanal

- **RN-15** O peso semanal é a média aritmética dos registros existentes nos
  últimos 7 dias de calendário, recalculada a cada dia.
- **RN-16** A janela exige no mínimo 4 registros. Com menos, não existe peso
  semanal para aquele dia.
- **RN-17** O peso semanal é a unidade de decisão do sistema. Comparação de
  ritmo, recálculo de taxa alvo, variações e progresso até o peso alvo usam o
  peso semanal, nunca o peso bruto de um dia isolado.

## Fase

- **RN-18** Uma fase tem tipo, data de início, data de fim, nível e taxa alvo.
  Os tipos são cutting, bulking e manutenção. Data de fim vazia significa fase
  em aberto, e data de fim preenchida nunca é anterior à de início.
- **RN-19** Fases do mesmo usuário não se sobrepõem, e a reação à tentativa de
  sobreposição depende de com quem ela colide. Criar fase que invada o
  intervalo de uma fase **com data de fim** é recusado. Criar fase **sem data
  de fim** que invada a fase **em aberto**, começando depois dela, encerra essa
  fase na véspera do novo início, em vez de recusar: é o caso de quem começou o
  ciclo antes de cadastrar. Se a fase nova começa no mesmo dia da fase em
  aberto, ou antes dela, é recusado: a véspera cairia antes do início da fase
  em aberto, e quem quer adiantar o início de um ciclo edita a data da fase
  existente (RN-24). Criar fase **com data de fim** que invada a fase em aberto
  é recusado: ela classifica um período, não começa um ciclo, e encerrar a fase
  em aberto seria efeito que a pessoa não pediu. Editar fase nunca encerra
  outra: a edição que criaria sobreposição com qualquer fase, em aberto ou não,
  é recusada, porque recusa é reversível e encerrar a fase que a pessoa está
  vivendo, por efeito de corrigir outra, não é. A exceção é o fim no futuro
  da RN-67: mexer na fase seguinte move um fim que ela mesma já tinha posto
  na anterior.
- **RN-20** Data pode não pertencer a fase nenhuma. Período sem fase é estado
  válido e esperado, não defeito a corrigir.
- **RN-22** Iniciar uma fase nova sem data de fim, começando depois da fase em
  aberto, encerra a fase em aberto no dia anterior à data de início da nova.
- **RN-23** A fase de um registro é derivada da data do registro. A fase não é
  gravada dentro do registro.
- **RN-24** As datas de início e de fim de uma fase são editáveis, sujeitas à
  proibição de sobreposição da RN-19, à ordem entre as datas da RN-18 e à
  proibição de data de fim futura da RN-67. Editá-las reclassifica os
  registros dos intervalos afetados, porque a fase é derivada da data.
- **RN-27** Fase corrente é a que cobre a data de hoje, e **pode não existir**.
  Ela não se confunde com a fase **em aberto**, a que não tem data de fim
  (RN-18): as duas costumam ser a mesma, e deixam de ser quando a próxima fase
  já está agendada. Aí a corrente tem data de fim, posta pela RN-22, e a em
  aberto é a futura (RN-69).
- **RN-66** Data sem fase exibe peso semanal, as três variações e a cintura
  medida, e nada além disso. Não há taxa alvo, veredito de ritmo, barra de
  progresso nem faixa de tolerância, porque os quatro dependem de um plano
  declarado, e sem fase não há plano.
- **RN-67** Encerrar fase é ação do usuário, e a data de encerramento não pode
  ser futura. Para que uma fase termine numa data futura, cria-se a fase
  seguinte começando no dia seguinte, e a RN-22 encerra a atual. Data de fim no
  futuro existe apenas como consequência de outra fase começar, nunca como
  declaração isolada. Por isso, enquanto o fim de uma fase está no futuro, ele
  é sempre a véspera da fase seguinte e a acompanha: mudar o início da
  seguinte move esse fim para a nova véspera, como edição de datas da RN-24,
  inclusive quando a véspera nova é hoje ou já passou; e apagar a seguinte
  leva o fim para a véspera da próxima fase, ou o tira, se não houver
  próxima. Antecipar a seguinte para o início da anterior, ou antes, é
  recusado pela RN-19.
- **RN-69** A fase pode começar em data futura. Isso difere da RN-05, que
  recusa registro em data futura, e a diferença é deliberada: registro afirma
  uma medição que não aconteceu, fase declara uma intenção que ainda não
  começou. Fase futura aparece na lista de fases e não entra no acompanhamento.
- **RN-70** Qualquer fase pode ser apagada, inclusive com registros dentro. Os
  registros permanecem e apenas perdem o rótulo do período. A operação é
  reversível: recriar a fase sobre o mesmo intervalo devolve a classificação.
  A exceção é a fase com fim no futuro, que não se recria sobre o mesmo
  intervalo, porque fim no futuro só nasce de outra fase começar (RN-67): a
  classificação dos registros volta recriando a fase até hoje, e o intervalo
  inteiro volta apagando as fases agendadas depois dela, recriando-a sem fim e
  agendando-as de novo.

## Nível e taxa alvo

- **RN-28** O nível é conservador, moderado, agressivo ou personalizado, é
  obrigatório em cutting e bulking, e determina a taxa alvo padrão da fase.
  Trocar a fase entre cutting e bulking aplica o padrão do mesmo nível no tipo
  novo, com valor e unidade; no personalizado, que não tem padrão, o valor é
  pedido de novo.
- **RN-29** A taxa alvo é obrigatória em cutting e bulking. Ela tem valor, com
  até duas casas decimais, e unidade, e ambos são editáveis pelo usuário. As
  unidades possíveis são porcentagem do peso corporal por semana e
  quilogramas por semana. O valor é digitado sem sinal, e o sinal vem do tipo:
  negativo em cutting, positivo em bulking. Zero é recusado. Assim o plano
  nunca aponta contra o próprio tipo, e o sinal de menos esquecido não inverte
  o veredito (RN-37).
- **RN-30** Editar o valor ou a unidade da taxa alvo muda o nível da fase para
  personalizado.
- **RN-31** Padrões de cutting, em porcentagem do peso corporal por semana:
  conservador -0,25%, moderado -0,50%, agressivo -1,00%.
- **RN-32** Padrões de bulking, em quilogramas por semana: conservador
  +0,20 kg, moderado +0,30 kg, agressivo +0,40 kg.
- **RN-33** Manutenção tem taxa alvo zero e não tem nível. O zero é constante
  da regra e não valor gravado, e por isso a taxa alvo de uma fase de
  manutenção fica vazia. O veredito da manutenção compara a **posição** do
  peso semanal de hoje com a faixa de tolerância, que vai de 1% abaixo a 1%
  acima da base da RN-68, com as bordas dentro. Posição, e não taxa, porque
  manter é segurar o peso: uma taxa pequena a cada semana passaria no veredito
  enquanto o peso escorrega. E 1%, porque a água e o sal movem o peso semanal
  o bastante para tirar de uma faixa mais estreita quem mantém o peso à risca.
- **RN-34** Taxa alvo expressa em porcentagem incide sobre o peso semanal
  corrente e é reconvertida a cada semana. Nunca incide sobre o peso do início
  da fase.
- **RN-35** Sempre que a taxa alvo estiver em porcentagem, o sistema exibe
  também o valor equivalente em quilogramas para a semana corrente.

## Veredito de ritmo

- **RN-36** A taxa real é a variação do peso semanal convertida em
  quilogramas por semana, medida entre hoje e 28 dias atrás: 78,4 hoje contra
  80,0 há 28 dias dá -0,40 kg por semana. Enquanto o dia da base da RN-68 for
  mais recente que 28 dias atrás, a medida é entre hoje e a base. O mês, e não
  a semana, porque a água e o sal movem o peso semanal tanto quanto o ritmo
  planejado: comparando com 7 dias atrás, quem segue o plano veria fora do
  ritmo na maioria das semanas. No mês, a água de uma semana compensa a da
  outra e o ritmo se acumula; no começo da fase, o veredito fica mais
  confiável a cada dia, até o mês fechar.
- **RN-37** O veredito tem três estados: abaixo do ritmo, no ritmo e acima do
  ritmo. Em cutting e bulking, ritmo é velocidade na direção do plano, que é a
  do tipo (RN-29): abaixo do ritmo é perder, ou ganhar, mais devagar que a
  taxa alvo, inclusive andar na direção contrária, e acima do ritmo é mais
  rápido. Na manutenção, que compara posição (RN-33), os mesmos três estados
  se exibem como abaixo da faixa, na faixa e acima da faixa, pelo peso. A
  palavra é ritmo, e não taxa, porque taxa tem sinal: em cutting, -0,10 contra
  um alvo de -0,50 é número maior e perda mais lenta. O veredito compara os
  números como a tela os mostra: o peso semanal e os limites da faixa com uma
  casa decimal, a taxa real e a taxa alvo em quilogramas por semana com duas,
  com o meio arredondado para longe do zero. A conta por trás continua exata,
  e só o resultado é arredondado: comparando o valor exato, a tela mostraria,
  ao lado de um veredito de fora, um número igual à borda.
- **RN-38** Em cutting e bulking, considera-se no ritmo a taxa real que fique
  dentro de 50% do valor da taxa alvo, para mais ou para menos, com as bordas
  dentro: com alvo de -0,40 kg por semana, está no ritmo a taxa real entre
  -0,20 e -0,60. A tolerância é larga porque, mesmo medida no mês (RN-36), a
  taxa real ainda carrega água e sal, e veredito que acusa fora do ritmo quem
  segue o plano ensina a pessoa a ignorá-lo. Com alvo pequeno, como os
  conservadores, o veredito oscila mais, e isso é aceito: em um mês, a balança
  de casa não separa 0,2 kg por semana de peso parado.
- **RN-39** Em cutting e bulking, não há veredito antes de 14 dias completos
  desde o dia da base (RN-68), nem sem os dois pesos semanais que a RN-36
  compara. Até lá o sistema informa dados insuficientes. A condição de
  liberação é a de cálculo: sem os dois pesos semanais não existe taxa real, e
  com menos de 14 dias ela ainda é mais água do que ritmo. A manutenção é a
  exceção, porque não compara taxa (RN-33): o veredito dela existe sempre que
  existem a base e o peso semanal de hoje, que é a mesma condição da faixa de
  tolerância. Esperar os 14 dias mostraria a linha dentro da faixa, no
  gráfico, ao lado de um veredito de dados insuficientes.
- **RN-40** O veredito existe apenas para a fase corrente. Fase encerrada é a
  que tem data de fim anterior a hoje; a corrente com fim agendado segue
  corrente, com veredito, até o último dia. A fase encerrada apresenta
  variação total, duração e se o peso alvo foi atingido, sem receber veredito.
  Quando o peso alvo está do lado errado da base, ela não é marcada como
  atingida, e o histórico indica isso, pelo mesmo motivo da RN-44. Quando a
  variação total dessa fase foi calculada sobre menos pontos que o mínimo da
  RN-16, o número é exibido com indicação disso, em vez de omitido: variação
  sobre poucos pontos informa mais que variação ausente, desde que a pessoa
  saiba qual das duas está olhando.

## Peso alvo e progresso

- **RN-41** O peso alvo é obrigatório em cutting e bulking, e não existe em
  manutenção.
- **RN-42** A direção do peso alvo vem do tipo da fase: em cutting é o valor a
  ser alcançado para baixo, em bulking é o teto a não ser ultrapassado.
- **RN-43** A barra de progresso vai da base da RN-68 até o peso alvo.
- **RN-44** A barra trava em 100% quando o alvo é atingido ou ultrapassado, e
  em 0% quando o movimento é na direção oposta. Não exibe valor negativo nem
  acima de 100%. E ela não é exibida enquanto o peso alvo estiver do lado
  errado da base, porque ali apareceria em 100% anunciando progresso completo
  sem nada ter acontecido; pelo mesmo motivo, nesse estado não há o aviso da
  RN-45. A comparação é refeita a cada exibição, e não num momento só, porque
  o peso alvo é editável e a base acompanha a correção de registro (RN-68).
- **RN-45** Ao atingir o peso alvo o sistema avisa o usuário. Atingir se mede
  pelo peso semanal e nunca pelo peso bruto de um dia, por coerência com a
  RN-17: aviso disparado por peso bruto desaparece no dia seguinte e perde
  crédito. A fase nunca é encerrada automaticamente.
- **RN-68** A base do progresso, da faixa de tolerância e da taxa real do
  primeiro mês (RN-36) é o peso semanal do **dia da base**. O dia da base é o dia de início da fase ou, quando aquele dia não
  tinha peso semanal (RN-16), o primeiro dia dentro da fase que tiver. Ele é
  fixado na primeira vez e nunca muda depois: escolher de novo conforme chegam
  registros retroativos faria a base saltar para outra semana, e a barra andar
  para trás sem o peso ter mudado. O valor, ao contrário, acompanha os
  registros da janela daquele dia, e corrigir um peso digitado errado corrige
  a base. Enquanto nenhum dia da fase tiver peso semanal, ou quando a janela do
  dia da base deixar de ter os 4 registros da RN-16, não há barra nem faixa de
  tolerância, e o sistema informa dados insuficientes, sem repetir o último
  valor conhecido (RN-09). A exceção é editar as datas da fase (RN-24): ali o
  intervalo passou a ser outro, e o dia da base é determinado de novo sobre o
  intervalo novo.

## Variações exibidas

- **RN-46** O sistema exibe três variações, todas calculadas sobre peso
  semanal e nunca sobre peso bruto de um dia: 7 dias, 30 dias, e desde o
  primeiro peso semanal disponível.
- **RN-47** Cada variação exibe o intervalo de datas das janelas efetivamente
  usadas no cálculo, no formato (DD-MM / DD-MM).

## Conta, acesso e dados

- **RN-48** A entrada no sistema acontece somente por convite emitido pelo
  operador.
- **RN-49** A identidade do usuário vem do Google. O sistema não armazena
  senha.
- **RN-50** Cada usuário acessa exclusivamente os próprios dados.
- **RN-51** Apagar a conta remove do banco, na hora, todos os dados do
  usuário, sem anonimização. As cópias de backup não são editadas e somem
  sozinhas no fim da retenção. Restaurar uma cópia anterior à exclusão traz a
  conta de volta.
- **RN-52** Exportar o próprio histórico em CSV está disponível ao usuário
  desde a primeira versão.

## Importação de CSV

- **RN-53** As colunas data e peso são obrigatórias. A coluna cintura é
  opcional.
- **RN-54** O separador pode ser ponto e vírgula ou vírgula, detectado a
  partir do conteúdo do arquivo.
- **RN-55** A data aceita os formatos AAAA-MM-DD e DD/MM/AAAA.
- **RN-57** A importação não cria fase. As datas importadas ficam sem fase
  (RN-20), e classificar o histórico depois se faz criando fase com início e
  fim sobre o intervalo, que é operação normal pela RN-19. Isso evita que um período
  importado antigo se torne a fase corrente por não ter nada depois dele.
- **RN-58** A gravação exige pré-visualização prévia, que informa quantas
  linhas são válidas, quantas foram recusadas com número da linha e motivo, e
  quantas datas já existentes serão substituídas.
- **RN-59** A gravação é tudo ou nada, e só ocorre após confirmação explícita
  do usuário.
- **RN-60** Todas as regras de validação de registro valem igualmente para as
  linhas importadas.

## Idioma e formato

- **RN-61** O sistema opera apenas em português do Brasil.
- **RN-62** Número decimal é digitado e exibido com vírgula, e trafegado e
  armazenado com ponto.
- **RN-63** Data é exibida em DD/MM/AAAA, e trafegada e armazenada em
  AAAA-MM-DD.

## Regras aposentadas

Os números não voltam. Estão aqui para que uma citação antiga possa ser
entendida, e para que ninguém reaproveite o número.

| Número | O que dizia | Por que saiu |
|:--|:--|:--|
| RN-21 | Conta nova nasce com fase de manutenção aberta | Só existia para satisfazer a obrigatoriedade de fase. Sem ela, conta nova simplesmente não tem fase |
| RN-25 | Fase com registro não pode ser apagada | O motivo era deixar registro sem fase possível, e período sem fase passou a ser estado válido |
| RN-26 | A única fase da conta não pode ser apagada | Mesmo motivo da RN-25 |
| RN-56 | O tipo de fase escolhido na importação vale para o arquivo inteiro | A importação deixou de criar fase |
| RN-64 | A primeira fase da conta não tem data de início e cobre todo o passado | Existia para que registro retroativo tivesse fase. Sem obrigatoriedade de fase, não é mais necessária |
| RN-65 | O peso alvo deve ser coerente com o tipo da fase | Não era avaliável em conta sem registro, e a consequência que ela evitava passou a ser tratada pela RN-44, que deixa de exibir a barra |

## Decisões recusadas

Registradas para que não voltem como novidade.

| Recusado | Motivo |
|:--|:--|
| IMC | Não distingue massa magra de gordura e entrega rótulo clínico que o sistema não tem competência para dar |
| Altura | Sem o IMC, nenhum cálculo a consome. Campo obrigatório sem consumidor é atrito puro |
| Medidas além da cintura | Cintura é a única com leitura útil durante cutting. As demais multiplicam campos sem multiplicar decisão |
| Meta de peso global no perfil | Durante bulking ela anda para trás e acusa falha enquanto o plano funciona |
| Projeção de data estimada | Extrapola tendência ruidosa e transforma o sistema em promessa que ele não controla |
| Corredor do plano no gráfico de cutting e bulking, saindo da base | Mede a posição acumulada, e o veredito mede o ritmo recente: depois de um começo lento, o gráfico diria fora enquanto o veredito diz no ritmo |
| Linha do peso alvo no gráfico | Estica o eixo até o alvo e achata a curva do peso semanal. A barra de progresso já mostra a distância (RF-33) |
| Fechamento por semana ISO | Duplicaria o peso semanal com número diferente para o mesmo período |
| Janela dos últimos 7 registros | Com dias em branco a janela estica e faz a taxa parecer mais lenta do que foi |
| Taxa real sobre a semana, com tolerância de 20% | A água e o sal movem o peso semanal tanto quanto o ritmo planejado: quem seguia o plano via fora do ritmo na maioria das semanas, e quem estava parado via no ritmo quase com a mesma frequência. Trocado pelo mês da RN-36 e pelos 50% da RN-38 |
| Tolerância mínima em quilos para alvo pequeno | Troca um erro por outro: quem segue o plano passa a ver no ritmo mais vezes, e quem está parado também |
| Repetir ou interpolar peso ausente | Inventa medição que não houve e a entrega ao gráfico com aparência de dado real |
| Sequência de dias registrando | Mede constância de registro, não progresso. Quem quebra a sequência para de registrar de vez |
| Compartilhamento entre usuários | Contradiz privacidade simples e obriga a decidir o que o outro vê e o que acontece ao sair |
| Encerramento automático de fase | Tira do usuário a decisão de terminar o ciclo |
| Alerta de variação suspeita | Não há calibragem confiável para o limiar, e alerta mal calibrado ensina o usuário a ignorar avisos |
| Data de fim declarada isoladamente para o futuro | Seria prazo declarado sem plano do que vem depois. Agendar o fim se faz criando a fase seguinte (RN-67) |
| Janela de tempo para edição | Não há auditoria a proteger, e a janela só gera frustração |
| Importação parcial silenciosa | Destrói a confiança no histórico: a pessoa não sabe o que entrou |
| Obrigatoriedade de fase em toda data | Forçava manutenção como preenchimento e produzia fase que a pessoa nunca viveu. Trocado pela RN-20 e pela RN-66 |
| Lista de contas apagadas, para refazer a exclusão depois de restaurar backup | Guardaria rastro da conta, contra a RN-51, para um caso raro num sistema por convite. A restauração vai ao ponto mais recente (RNF-14) |
