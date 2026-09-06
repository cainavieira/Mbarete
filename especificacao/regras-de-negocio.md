# Regras de negócio

Regras que valem independentemente de tela, rota ou tecnologia. Requisitos
funcionais e casos de uso citam estas regras pelo identificador em vez de
repetir o texto.

O identificador é estável: regra removida tem o número aposentado, nunca
reaproveitado por outra regra.

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
  apresenta o último valor medido, a data da medição, a variação desde a
  medição anterior e a variação desde o início da fase.
- **RN-13** O campo cintura existe em qualquer tipo de fase. Destacá-lo
  durante cutting é comportamento de tela, não regra.
- **RN-14** Dado medido não é apagado nem escondido por mudança de
  configuração posterior. Vale para toda a aplicação, não só para a cintura.

## Peso semanal

- **RN-15** O peso semanal é a média aritmética dos registros existentes nos
  últimos 7 dias de calendário, recalculada a cada dia.
- **RN-16** A janela exige no mínimo 4 registros. Com menos, não existe peso
  semanal para aquele dia.
- **RN-17** O peso semanal é a unidade de decisão do sistema. Comparação de
  ritmo, recálculo de taxa alvo e progresso até o peso alvo usam o peso
  semanal, nunca o peso bruto de um dia isolado.

## Fase

- **RN-18** Uma fase tem tipo, data de início, data de fim, nível e taxa alvo.
  Os tipos são cutting, bulking e manutenção.
- **RN-19** Existe no máximo uma fase ativa por usuário. Fases não se
  sobrepõem.
- **RN-20** Toda data com registro pertence a uma fase. Não existe registro
  fora de fase.
- **RN-21** Conta nova nasce com uma fase de manutenção aberta, para que o
  primeiro peso possa ser registrado antes de qualquer decisão de ciclo.
- **RN-22** Iniciar uma fase nova encerra a fase corrente no dia anterior à
  data de início da nova.
- **RN-23** A fase de um registro é derivada da data do registro. A fase não é
  gravada dentro do registro.
- **RN-24** As datas de uma fase são editáveis. Editá-las reclassifica
  automaticamente os registros do intervalo afetado.
- **RN-25** Fase que contenha registro não pode ser apagada. Erro de criação
  se corrige alterando tipo, nível, taxa alvo, peso alvo ou datas.
- **RN-26** Fase sem nenhum registro pode ser apagada, exceto quando for a
  única fase da conta.
- **RN-27** Fase não tem data de fim planejada. A data de fim registra quando
  a fase efetivamente encerrou.

## Nível e taxa alvo

- **RN-28** O nível é conservador, moderado, agressivo ou personalizado, e
  determina a taxa alvo padrão da fase.
- **RN-29** A taxa alvo tem valor e unidade, e ambos são editáveis pelo
  usuário. As unidades possíveis são porcentagem do peso corporal por semana e
  quilogramas por semana.
- **RN-30** Editar o valor da taxa alvo muda o nível da fase para
  personalizado.
- **RN-31** Padrões de cutting, em porcentagem do peso corporal por semana:
  conservador -0,25%, moderado -0,50%, agressivo -1,00%.
- **RN-32** Padrões de bulking, em quilogramas por semana: conservador
  +0,20 kg, moderado +0,30 kg, agressivo +0,40 kg.
- **RN-33** Manutenção tem taxa alvo zero e não tem nível. Sua tolerância é de
  0,5% do peso semanal do início da fase, para mais ou para menos.
- **RN-34** Taxa alvo expressa em porcentagem incide sobre o peso semanal
  corrente e é reconvertida a cada semana. Nunca incide sobre o peso do início
  da fase.
- **RN-35** Sempre que a taxa alvo estiver em porcentagem, o sistema exibe
  também o valor equivalente em quilogramas para a semana corrente.

## Veredito de ritmo

- **RN-36** A taxa real é a diferença entre o peso semanal de hoje e o peso
  semanal de 7 dias atrás.
- **RN-37** O veredito tem três estados: abaixo do ritmo, no ritmo e acima do
  ritmo.
- **RN-38** Considera-se no ritmo a taxa real que fique dentro de 20% do valor
  da taxa alvo, para mais ou para menos.
- **RN-39** Não há veredito antes de 14 dias completos de fase com no mínimo
  8 registros. Até lá o sistema informa dados insuficientes.
- **RN-40** O veredito existe apenas para a fase corrente. Fase encerrada
  apresenta variação total, duração e se o peso alvo foi atingido, sem receber
  veredito.

## Peso alvo e progresso

- **RN-41** O peso alvo é obrigatório em cutting e bulking, e não existe em
  manutenção.
- **RN-42** A direção do peso alvo vem do tipo da fase: em cutting é o valor a
  ser alcançado para baixo, em bulking é o teto a não ser ultrapassado.
- **RN-43** A barra de progresso vai do peso semanal do início da fase até o
  peso alvo.
- **RN-44** A barra trava em 100% quando o alvo é atingido ou ultrapassado, e
  em 0% quando o movimento é na direção oposta. Não exibe valor negativo nem
  acima de 100%.
- **RN-45** Ao atingir o peso alvo o sistema avisa o usuário. A fase nunca é
  encerrada automaticamente.

## Variações exibidas

- **RN-46** O sistema exibe três variações de peso: 7 dias, 30 dias e desde o
  primeiro registro.
- **RN-47** Cada variação exibe o intervalo de datas efetivamente usado no
  cálculo, no formato (DD-MM / DD-MM).

## Conta, acesso e dados

- **RN-48** A entrada no sistema acontece somente por convite emitido pelo
  operador.
- **RN-49** A identidade do usuário vem do Google. O sistema não armazena
  senha.
- **RN-50** Cada usuário acessa exclusivamente os próprios dados.
- **RN-51** Apagar a conta remove todos os dados do usuário, sem anonimização
  e sem retenção.
- **RN-52** Exportar o próprio histórico em CSV está disponível ao usuário
  desde a primeira versão.

## Importação de CSV

- **RN-53** As colunas data e peso são obrigatórias. A coluna cintura é
  opcional.
- **RN-54** O separador pode ser ponto e vírgula ou vírgula, detectado a
  partir do conteúdo do arquivo.
- **RN-55** A data aceita os formatos AAAA-MM-DD e DD/MM/AAAA.
- **RN-56** O tipo de fase escolhido na importação vale para o arquivo
  inteiro. O padrão é manutenção.
- **RN-57** A importação não divide o arquivo em fases distintas.
  Reclassificar o histórico se faz depois, editando as datas das fases.
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

## Decisões recusadas

Registradas para que não voltem como novidade.

| Recusado | Motivo |
|:--|:--|
| IMC | Não distingue massa magra de gordura e entrega rótulo clínico que o sistema não tem competência para dar |
| Altura | Sem o IMC, nenhum cálculo a consome. Campo obrigatório sem consumidor é atrito puro |
| Medidas além da cintura | Cintura é a única com leitura útil durante cutting. As demais multiplicam campos sem multiplicar decisão |
| Meta de peso global no perfil | Durante bulking ela anda para trás e acusa falha enquanto o plano funciona |
| Projeção de data estimada | Extrapola tendência ruidosa e transforma o sistema em promessa que ele não controla |
| Fechamento por semana ISO | Duplicaria o peso semanal com número diferente para o mesmo período |
| Janela dos últimos 7 registros | Com dias em branco a janela estica e faz a taxa parecer mais lenta do que foi |
| Repetir ou interpolar peso ausente | Inventa medição que não houve e a entrega ao gráfico com aparência de dado real |
| Sequência de dias registrando | Mede constância de registro, não progresso. Quem quebra a sequência para de registrar de vez |
| Compartilhamento entre usuários | Contradiz privacidade simples e obriga a decidir o que o outro vê e o que acontece ao sair |
| Encerramento automático de fase | Tira do usuário a decisão de terminar o ciclo, e datas editáveis poderiam disparar encerramento indevido |
| Alerta de variação suspeita | Não há calibragem confiável para o limiar, e alerta mal calibrado ensina o usuário a ignorar avisos |
| Data de fim planejada | Serviria apenas a um alerta de prazo que ninguém pediu |
| Janela de tempo para edição | Não há auditoria a proteger, e a janela só gera frustração |
| Importação parcial silenciosa | Destrói a confiança no histórico: a pessoa não sabe o que entrou |
