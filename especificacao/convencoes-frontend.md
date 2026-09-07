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
| Estado de servidor | TanStack Query | lista de registros, fase corrente, peso semanal, variações |
| Estado de tela | `useState` no componente | diálogo aberto, aba selecionada, campo em digitação |
| Estado derivado | **em lugar nenhum** | o veredito, o progresso: calcula na renderização |

Depois de gravar, invalida-se a chave da consulta e os consumidores se
atualizam. Não se atualiza tela na mão.

## Formatação de número e data

Uma função para cada, em `formato/`, e nenhuma formatação solta em componente.

- Peso e cintura: exibidos com vírgula e uma casa decimal (RN-62).
- Entrada aceita vírgula e ponto, e envia com ponto (RN-62).
- Data: exibida em `DD/MM/AAAA`, trafegada em `AAAA-MM-DD` (RN-63).
- Variação: sempre acompanhada do intervalo de datas usado, no formato
  `-0,4 kg (12-09 / 19-09)` (RN-47).

O motivo de centralizar: a RN-62 é uma regra só, e regra que aparece em cinco
componentes divergiu em três deles antes do fim do mês.

## Validação de formulário

O front valida **forma**, o backend valida **regra** (ADR-0025).

- No front: campo obrigatório, é número, no máximo uma casa decimal, aceita
  vírgula.
- No backend: faixas, data futura, coerência de alvo.
- A mensagem de recusa de regra **vem do backend** e é exibida como veio. O
  front usa o `codigo` para decidir comportamento e o `campo` para destacar o
  input.

Nenhuma faixa numérica de regra de negócio aparece escrita no frontend.

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

## Acessibilidade mínima

Não é lista de boas intenções, são quatro itens verificáveis.

- Todo campo tem `label` associado, não apenas `placeholder`.
- Erro de campo é referenciado por `aria-describedby`, para que leitor de tela
  anuncie a recusa junto do campo.
- Componente interativo vem do shadcn/ui, que traz o comportamento do Radix.
- **O gráfico não é a única forma de ler o dado.** As três variações e a lista
  de registros existem também para quem não consegue ler o gráfico. Isso é
  requisito de leitura, não enfeite: gráfico é imagem, e imagem não se navega
  por teclado.
