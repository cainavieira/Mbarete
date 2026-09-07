# Requisitos funcionais

Cada requisito diz o que o sistema faz, citando o ator pelo identificador de
`atores.md` e a regra pelo identificador de `regras-de-negocio.md`. Requisito
não repete o texto da regra: ele aponta.

O identificador é estável pelas mesmas razões do documento de regras.

## Acesso e conta

- **RF-01** O sistema deve autenticar o AT-01 por meio do AT-03. (RN-49)
- **RF-02** O sistema deve recusar o acesso de quem não consta na lista de
  convidados. (RN-48)
- **RF-03** O sistema deve criar a conta do AT-01 no primeiro acesso
  bem-sucedido, já com a fase de manutenção inicial. (RN-21, RN-64)
- **RF-04** O sistema deve permitir que o AT-01 encerre a própria sessão.
- **RF-05** O sistema deve restringir toda leitura e escrita aos dados do
  próprio AT-01 autenticado. (RN-50)
- **RF-06** O sistema deve permitir que o AT-01 apague a própria conta,
  removendo registros, fases e dados de conta. (RN-51)

## Registro diário

- **RF-07** O sistema deve permitir que o AT-01 registre o peso do dia
  corrente. (RN-01, RN-02, RN-04)
- **RF-08** O sistema deve permitir que o AT-01 registre peso em data passada.
  (RN-06)
- **RF-09** O sistema deve permitir que o AT-01 informe a cintura junto com o
  peso, de forma opcional. (RN-10)
- **RF-10** O sistema deve avisar o AT-01 e pedir confirmação quando já
  existir registro na data informada. (RN-01)
- **RF-11** O sistema deve permitir que o AT-01 edite qualquer registro
  existente. (RN-07)
- **RF-12** O sistema deve permitir que o AT-01 apague qualquer registro
  existente. (RN-08)
- **RF-13** O sistema deve recusar valor fora de faixa e data futura,
  informando qual campo foi recusado e qual é o limite. (RN-03, RN-05, RN-11)
- **RF-14** O sistema deve listar os registros do AT-01 em ordem cronológica,
  com data, peso e cintura quando houver.

## Fase

- **RF-15** O sistema deve permitir que o AT-01 crie uma fase informando tipo,
  data de início, nível e peso alvo. (RN-18, RN-41)
- **RF-16** O sistema deve sugerir a taxa alvo a partir do tipo e do nível
  escolhidos. (RN-31, RN-32, RN-33)
- **RF-17** O sistema deve permitir que o AT-01 edite o valor e a unidade da
  taxa alvo. (RN-29)
- **RF-18** O sistema deve marcar o nível como personalizado quando o valor da
  taxa alvo for editado. (RN-30)
- **RF-19** O sistema deve encerrar a fase corrente ao criar uma fase nova.
  (RN-22)
- **RF-20** O sistema deve permitir que o AT-01 mova a fronteira entre duas
  fases, ajustando as duas ao mesmo tempo. (RN-24)
- **RF-21** O sistema deve recusar a exclusão de fase que contenha registro e
  da única fase da conta, informando o motivo. (RN-25, RN-26)
- **RF-22** O sistema deve permitir a exclusão de fase sem registro. (RN-26)
- **RF-23** O sistema deve apresentar as fases encerradas com variação total,
  duração e se o peso alvo foi atingido. (RN-40)
- **RF-24** O sistema deve dispensar peso alvo e nível quando o tipo for
  manutenção. (RN-33, RN-41)
- **RF-25** O sistema deve recusar peso alvo incoerente com o tipo da fase.
  (RN-42, RN-65)

## Acompanhamento

- **RF-26** O sistema deve calcular o peso semanal do AT-01 a cada dia.
  (RN-15)
- **RF-27** O sistema deve informar dados insuficientes quando a janela tiver
  menos registros que o mínimo. (RN-16)
- **RF-28** O sistema deve apresentar um gráfico com os pesos diários como
  pontos e o peso semanal como linha. (RN-09, RN-17)
- **RF-29** O sistema deve apresentar as três variações de peso com o
  intervalo de datas usado em cada uma. (RN-46, RN-47)
- **RF-30** O sistema deve apresentar o veredito de ritmo da fase corrente.
  (RN-36, RN-37, RN-38)
- **RF-31** O sistema deve informar dados insuficientes no lugar do veredito
  enquanto a fase não atingir o mínimo de dias e de registros. (RN-39)
- **RF-32** O sistema deve apresentar o equivalente em quilogramas da taxa
  alvo percentual da semana corrente. (RN-34, RN-35)
- **RF-33** O sistema deve apresentar a barra de progresso da fase corrente
  até o peso alvo. (RN-43, RN-44)
- **RF-34** O sistema deve avisar o AT-01 ao atingir o peso alvo, sem encerrar
  a fase. (RN-45)
- **RF-35** O sistema deve apresentar a cintura como último valor medido, data
  da medição, variação desde a medição anterior e variação desde o início da
  fase. (RN-12)
- **RF-36** O sistema deve apresentar números e datas no formato do idioma
  único do sistema. (RN-61, RN-62, RN-63)

## Importação e exportação

- **RF-37** O sistema deve receber um arquivo CSV do AT-01 e apresentar a
  pré-visualização antes de qualquer gravação. (RN-58)
- **RF-38** O sistema deve reportar na pré-visualização as linhas válidas, as
  recusadas com número de linha e motivo, e as datas que serão substituídas.
  (RN-58)
- **RF-39** O sistema deve permitir que o AT-01 escolha o tipo de fase da
  importação, com manutenção como padrão. (RN-56)
- **RF-40** O sistema deve gravar a importação inteira ou nenhuma linha, após
  confirmação. (RN-59)
- **RF-41** O sistema deve permitir que o AT-01 abandone a importação na
  pré-visualização, sem gravar nada.
- **RF-42** O sistema deve aceitar os separadores e os formatos de data
  previstos. (RN-54, RN-55)
- **RF-43** O sistema deve permitir que o AT-01 exporte o próprio histórico
  completo em CSV. (RN-52)

## Operação

- **RF-44** O sistema deve oferecer ao AT-02 um comando de linha que registre
  um e-mail na lista de convidados. (RN-48)
