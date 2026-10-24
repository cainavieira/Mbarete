# Convenções do frontend

Regras de escrita, não de negócio. As decisões de ferramenta estão nos ADR.

## Idioma dos identificadores

Mesma regra do backend: domínio em português, vocabulário técnico em inglês.

| O que | Como | Exemplos |
|:--|:--|:--|
| Componente de domínio | PascalCase em português | `FormularioRegistro`, `GraficoEvolucao`, `BarraProgresso`, `CartaoVeredito` |
| Gancho de consulta | português, prefixo `use` | `useRegistros`, `useFaseCorrente`, `useAcompanhamento` |
| Vocabulário de React | inglês | `props`, `useState`, `onSubmit`, `handleChange` |
| Arquivo de componente | igual ao componente | `FormularioRegistro.tsx` |

## Estrutura de pasta

```
src/
  api/           cliente HTTP e tipos gerados do OpenAPI
  paginas/       uma por rota
  componentes/   reutilizados entre paginas
  hooks/         ganchos de consulta e mutacao do TanStack Query
  formato/       formatacao de numero e data
  lib/           utilidades sem dominio
```

O cliente HTTP em `api/` é o único lugar que monta requisição. É ele que põe
o cabeçalho `Data-Local`, com a data do dispositivo, em toda chamada (RN-04),
para nenhuma rota nova nascer sem ela. E é ele que, ao receber 401, chama a
renovação uma vez e repete a chamada (`endpoints.md`).

## TypeScript

- `strict` ligado no `tsconfig`.
- `any` proibido. Quando o tipo é genuinamente desconhecido, `unknown` mais
  verificação.
- Os tipos do contrato de API **não se escrevem à mão**: são gerados da
  especificação OpenAPI e versionados (ADR-0027).
- Retorno de função exportada é anotado, mesmo quando a inferência acerta.

## Onde mora o estado

Três lugares, e a fronteira entre eles é a regra mais importante deste
documento (ADR-0023).

| Tipo de estado | Onde mora | Exemplos |
|:--|:--|:--|
| Estado de servidor | TanStack Query | lista de registros, fase corrente, peso semanal, variações, veredito, progresso |
| Estado de tela | `useState` no componente | diálogo aberto, aba selecionada, campo em digitação |
| Estado derivado | **em lugar nenhum** | o trecho da série do período escolhido (RF-49), se o formulário pode ser enviado: calcula na renderização |

Depois de gravar, invalida-se a chave da consulta e os consumidores se
atualizam. Não se atualiza tela na mão.

## Formatação de número e data

Uma função para cada, em `formato/`, e nenhuma formatação solta em componente.

- Peso e cintura: exibidos com vírgula e uma casa decimal (RN-62).
- Taxa alvo: exibida com vírgula e duas casas decimais (RN-29, RN-62). No
  formulário da fase, sem sinal, como se digita; no acompanhamento, com o
  sinal do tipo.
- Taxa real: exibida com vírgula, sinal e duas casas decimais, em quilogramas
  por semana, com o intervalo de datas usado (RN-36, RN-47). Os números do
  veredito chegam arredondados do backend, e o formato não arredonda de novo
  (RN-37).
- Entrada aceita vírgula e ponto, e envia com ponto (RN-62).
- Data: exibida em `DD/MM/AAAA`, trafegada em `AAAA-MM-DD` (RN-63).
- Variação: sempre acompanhada do intervalo de datas usado, no formato
  `-0,4 kg (12-09 / 19-09)` (RN-47).

O motivo de centralizar: a RN-62 é uma regra só, e regra que aparece em cinco
componentes divergiu em três deles antes do fim do mês.

## Validação de formulário

O front valida **forma**, o backend valida **regra** (ADR-0025).

- No front: campo obrigatório, é número, no máximo uma casa decimal no peso e
  na cintura e duas na taxa alvo, sem sinal na taxa alvo, aceita vírgula.
- No backend: faixas, data futura, sobreposição de fase.
- A mensagem de recusa de regra **vem do backend** e é exibida como veio. O
  front usa o `codigo` para decidir comportamento e o `campo` para destacar o
  input.

Nenhuma faixa numérica de regra de negócio aparece escrita no frontend.

## Identidade visual

Cada cor, fonte e tamanho, com o valor, está no `identidade-visual.md`. Aqui
fica como isso vira código.

- Cada cor é uma variável CSS com o nome que o shadcn/ui usa, definida duas
  vezes: no `:root`, com o valor do modo claro, e no bloco `.dark`, com o do
  escuro. O seletor de tema (RF-51) só põe ou tira a classe `dark` do `<html>`.
- Componente usa o token, nunca o valor. Trocar uma cor é trocar a variável.
- As cores que o shadcn/ui não traz entram nos dois blocos e no tema do
  Tailwind, para virar classe (`bg-destaque`, `text-ritmo`). O gráfico do
  Recharts recebe a variável, não o valor.
- Tamanho de fonte e de espaço sai da escala do Tailwind, que é em rem.
- Número que alinha ou muda no lugar usa a classe `tabular-nums`.

| Cor | Token |
|:--|:--|
| fundo | `--background` |
| superfície | `--card`, `--popover` |
| borda | `--border` |
| borda de campo | `--input` |
| texto | `--foreground` e os pares `-foreground` que não aparecem abaixo |
| texto suave | `--muted-foreground` |
| ação | `--primary`, `--ring` |
| texto sobre a ação | `--primary-foreground` |
| seleção | `--secondary`, `--accent` |
| texto sobre a seleção | `--secondary-foreground`, `--accent-foreground` |
| fase | `--muted` |
| erro | `--destructive` |
| texto sobre o erro | `--destructive-foreground` |
| destaque, número no destaque, texto suave no destaque | `--destaque`, `--destaque-foreground`, `--destaque-suave` |
| linha, ponto e área do gráfico | `--grafico-linha`, `--grafico-ponto`, `--grafico-area` |
| trilho | `--trilho` |
| no ritmo, texto e fundo | `--ritmo`, `--ritmo-fundo` |
| borda da faixa | `--faixa-borda` |
| fora do ritmo, texto e fundo | `--fora`, `--fora-fundo` |
| tracejado de dados insuficientes | `--insuficiente-borda` |

A barra de progresso usa `--grafico-linha`, e a faixa de tolerância usa
`--ritmo-fundo`: a mesma cor nos dois lugares é decisão do design, não
coincidência de valor. O canto arredondado é a variável `--radius`. Nome da
tabela que a versão instalada do shadcn/ui não trouxer entra como as cores
próprias, nos dois blocos e no tema do Tailwind.

## Antipadrões proibidos

1. `useEffect` para buscar dado do servidor. É estado de servidor tratado como
   estado de tela, exatamente o que o ADR-0023 recusou.
2. `any`.
3. Guardar em `useState` valor que se deriva de outro. Duas fontes para a mesma
   verdade divergem, e a que está na tela é a errada.
4. Índice do array como `key` em lista que muda de ordem ou de tamanho.
5. `fetch` chamado direto no componente, em vez de pelo gancho de consulta.
6. Formatar número ou data fora de `formato/`.
7. Escrever à mão tipo do contrato de API (ADR-0027).
8. Repetir no front a mensagem de erro que o backend já devolve.
9. Diálogo feito à mão. Usa o do shadcn/ui, que trata foco preso, fechamento
   por Escape e retorno de foco (ADR-0028).
10. Cor escrita no componente, seja o valor ou uma classe de cor do Tailwind
    (`text-green-700`), fora dos tokens.
11. Verde ou manteiga em selo de estado, e vermelho no veredito.
12. Número que alinha sem `tabular-nums`. Os algarismos da Figtree são
    proporcionais por padrão, e a coluna de pesos desalinha.
13. Tamanho de fonte ou de espaço em px, fora da espessura de borda, e campo
    com fonte abaixo de 1rem.

## Acessibilidade mínima

Não é lista de boas intenções, são cinco itens verificáveis.

- Todo campo tem `label` associado, não apenas `placeholder`.
- Erro de campo é referenciado por `aria-describedby`, para que leitor de tela
  anuncie a recusa junto do campo.
- Componente interativo vem do shadcn/ui, que traz o comportamento do Radix.
- O estado nunca é dito só pela cor: o selo do veredito traz a palavra, e o de
  fora do ritmo, a seta.
- **O gráfico não é a única forma de ler o dado.** As três variações e a lista
  de registros existem também para quem não consegue ler o gráfico. Isso é
  requisito de leitura, não enfeite: gráfico é imagem, e imagem não se navega
  por teclado.
