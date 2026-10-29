# Identidade visual

Como o sistema se parece: nome, modo, cor, fonte, tamanho, densidade e
logotipo. Vale para as telas, para o design delas e para qualquer peça fora do
sistema. Como isso vira código está no `convencoes-frontend.md`.

## Nome

**Mbarete**, "forte" em guarani. Em texto corrido, com M maiúsculo; no
logotipo, em minúsculas.

## Modo

Claro e escuro. O sistema segue o modo do sistema operacional até a pessoa
escolher outro, num seletor de três posições: sistema, claro e escuro. A
escolha fica no navegador (RF-51).

## Cor

| Nome | Claro | Escuro | Papel |
|:--|:--|:--|:--|
| fundo | `#FFFFFF` | `#131512` | a página |
| superfície | `#FFFFFF` | `#1B1E1A` | cartão e campo; no claro, o cartão se separa do fundo pela borda |
| borda | `#E4E8E1` | `#2E332C` | divisória e borda de cartão |
| borda de campo | `#8A9086` | `#6E746A` | contorno de campo e de controle |
| texto | `#121212` | `#EEF1EA` | texto |
| texto suave | `#5C625A` | `#A0A69C` | data, legenda, rótulo e o nome da fase no gráfico |
| ação | `#3E5F46`, verde militar | `#A7B89F`, sálvia | botão, link e foco |
| texto sobre a ação | `#FFFFFF` | `#131512` | o texto dentro do botão |
| seleção | `#E6EEE2` | `#263024` | período escolhido, botão secundário |
| texto sobre a seleção | `#3E5F46` | `#CFE0C4` | o texto do item selecionado |
| destaque | `#FFF3B0`, manteiga | `#2A2716` | o cartão do peso semanal |
| número no destaque | `#121212` | `#FFF3B0` | o peso semanal |
| texto suave no destaque | `#5C625A` | `#B9B49A` | rótulo e taxa dentro do cartão |
| linha do gráfico | `#6B8E5E`, oliva | `#A7B89F` | o peso semanal, e a barra de progresso |
| ponto do gráfico | `#788E6F` | `#7D8A77` | o peso de cada dia |
| área do gráfico | `#EEF3EA` | `#222A20` | sob a linha |
| fase | `#F1F3EF` | `#1F231E` | fundo do trecho com fase |
| trilho | `#EEF1EC` | `#2E332C` | fundo da barra de progresso |
| no ritmo | `#1F4E79` sobre `#E3EDF7` | `#A9C8E8` sobre `#1C2A38` | selo de "no ritmo" e "na faixa"; o fundo é também a faixa de tolerância no gráfico |
| borda da faixa | `#4F81AE` | `#6F9CC6` | os dois limites da faixa, tracejados |
| fora do ritmo | `#8A3B22` sobre `#F8E3DA` | `#F0B49C` sobre `#3A2620` | selo de abaixo e acima |
| dados insuficientes | texto suave, tracejado `#7C8378` | texto suave, tracejado `#6E746A` | selo sem fundo |
| erro | `#B42318`, texto sobre ele `#FFFFFF` | `#F08C7D`, texto sobre ele `#131512` | recusa no formulário e botão que apaga |
| sobreposição | `#121212` a 50% | `#131512` a 80% | o fundo escurecido atrás do diálogo |

Fundo e texto não são branco e preto puros. O par puro dá contraste de 21 para
1 e cansa a vista; `#121212` sobre branco dá 18,7 e continua muito acima do
mínimo.

**Contraste.** Texto comum tem razão de contraste de no mínimo 4,5 contra o
que está atrás dele; texto grande, borda de campo, foco e marca de gráfico, 3
(WCAG 2, nível AA). Todo par da tabela passa. Fundo de destaque e fundo de selo
não têm mínimo contra a página: o que conta é o texto em cima deles. Por isso
todo tom claro leva texto escuro: branco sobre verde-claro não chega a 3.

**O verde é ação e gráfico.** Botão, link, foco e seleção, e a linha e os
pontos do gráfico. Nunca indica estado: o "no ritmo" não é verde.

**A manteiga é identidade.** Só o cartão do peso semanal. Nunca indica estado
nem ação.

**O vermelho é erro e ação que apaga.** Nunca aparece no veredito: nada no
acompanhamento é erro.

## Estado

A cor segue o significado e reforça a palavra, nunca diz sozinha.

| Estado | Como aparece |
|:--|:--|
| no ritmo, na faixa | selo azul, com fundo |
| abaixo do ritmo, acima do ritmo, abaixo da faixa, acima da faixa | selo terracota, com fundo e a seta ↓ ou ↑; os dois lados têm a mesma cor, porque os dois são fora do ritmo (RN-37) |
| dados insuficientes | selo tracejado, sem fundo; na barra de progresso, o trilho tracejado |
| alvo atingido | aviso neutro, com ícone e sem cor de destaque; informa, não comemora (RN-45) |
| fase, no gráfico | o fundo neutro do trecho, com o tipo escrito no alto; trecho sem fase fica sem fundo (RF-47) |
| faixa de tolerância, no gráfico | o fundo do "no ritmo", entre os dois limites tracejados, do início da manutenção até hoje (RF-50) |

## Fonte e tamanho

Figtree, uma família só. A hierarquia vem do peso: negrito no número grande e
nos títulos, regular no texto. Os algarismos da Figtree são proporcionais por
padrão; com números tabulares ligados eles ficam da mesma largura, e todo número
que alinha ou muda no lugar usa essa opção.

| Tamanho | No padrão do navegador | Uso |
|:--|:--|:--|
| 3rem | 48 px | o número grande: o peso semanal e o peso no registro |
| 1,5rem | 24 px | título da tela |
| 1,125rem | 18 px | título de cartão |
| 1rem | 16 px | texto e campo |
| 0,875rem | 14 px | tabela no computador e selo |
| 0,75rem | 12 px | legenda |

O tamanho é em rem para crescer junto quando a pessoa aumenta a fonte padrão do
navegador. Só a espessura de borda fica em px. Campo nunca fica abaixo de 1rem:
abaixo disso, o Safari do iPhone aproxima a página ao tocar no campo.

O canto arredondado tem 0,75rem em campo e botão, 1rem em cartão, e é redondo
inteiro em selo e seletor. O alvo de toque tem no mínimo 2,75rem de altura.

## Densidade

Celular primeiro. Registrar o peso do dia cabe em uma tela e uma confirmação
(RNF-26): o peso é o número grande no centro, com teclado numérico, a data já
vem com hoje (RN-04) e a cintura, opcional, fica abaixo, menor.

A lista de registros e a pré-visualização da importação viram tabela compacta
em tela larga, porque dois anos de histórico passam de 700 linhas. No celular,
as duas são lista. As outras telas só ficam mais largas.

## Logotipo

Direção Traço: o M de Mbarete desenhado como a linha do peso semanal, que sobe
e desce e termina num ponto, o dia de hoje. É a linguagem do próprio gráfico do
produto, o peso de cada dia como ponto e o semanal como linha (RF-28). O nome
vai em minúsculas, Figtree em negrito, na cor da ação.

O símbolo funciona sozinho a partir de 16 px e é o ícone do navegador. No claro,
vai em verde militar; no escuro, em sálvia; numa cor só, em preto ou branco. Os
arquivos estão em `marca/`, ao lado deste documento: `logotipo` (símbolo e
nome, com o nome convertido em curvas) e `simbolo` (sozinho), cada um em `cor`,
`cor-escuro`, `preto` e `branco`.

## Recusado

| Recusado | Motivo |
|:--|:--|
| Um modo só | O interessado quis claro e escuro desde o início, com a escolha da pessoa |
| Fundo creme | O interessado preferiu fundo branco e texto quase preto |
| Preto puro sobre branco puro | 21 para 1 cansa a vista |
| Ação em quase preto ou em azul-petróleo | O interessado escolheu o verde |
| Botão amarelo | Manteiga contra o branco dá 1,1 para 1, e o botão some no modo claro |
| Texto branco sobre tom claro | Branco sobre verde-claro dá 2,2 para 1, abaixo do mínimo |
| Verde no veredito | O verde é ação, e selo verde ao lado de botão verde diz "toque aqui" |
| Uma cor por tipo de fase | Gastaria três tons e colidiria com as cores do veredito; o nome escrito basta |
| Vermelho no veredito | Viraria o alarme que as regras recusam |
| Comemorar o "no ritmo" ou o alvo atingido, e ilustração de personagem | Torna o veredito bom ou ruim, e é o visual genérico de aplicativo de dieta |
| Duas famílias de fonte | A personalidade já vem da cor e do número grande |
| Tamanho em px | Não cresce quando a pessoa aumenta a fonte do navegador |
| Símbolo Bloco, um bloco sólido com o m vazado | O ícone mais forte em 16 px, mas não diz nada do produto |
| Símbolo Fases, um anel em três arcos | Em 16 px os arcos viram um anel com um ponto, parecido com ícone de "carregando" |
| Os nomes Jaci, Rastro e TrackerPal | Jaci soa a nome de restaurante; Rastro era o mais fácil de dizer, e o interessado preferiu Mbarete; TrackerPal foi rótulo de trabalho |
