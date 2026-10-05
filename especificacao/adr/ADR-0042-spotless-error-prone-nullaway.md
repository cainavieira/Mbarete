# ADR-0042 Spotless, Error Prone e NullAway só nas regras

- **Data:** 05-10-2026
- **Situação:** aceito
- **Supera:** ADR-0015, ADR-0016

## Contexto

Qualidade de código aqui são três trabalhos diferentes. Formatação decide
aparência e não muda comportamento; o valor dela é que ninguém volta a discutir
o assunto e que o diff mostra só mudança real. Lint procura defeito que compila
sem erro. E a checagem de nulo procura o valor ausente que ninguém tratou.

O caso que justifica a terceira é a RN-16: a janela com menos de 4 registros
não tem peso semanal, e todo consumidor precisa tratar o vazio. O compilador
Java confere tipo, mas não confere nulo: um `BigDecimal` pode chegar nulo sem
aviso.

O ADR-0015 resolvia os dois primeiros trabalhos com o ruff, e o ADR-0016
resolvia o terceiro com o mypy estrito só no módulo de regras. Com o backend em
Java (ADR-0035), as ferramentas mudam.

Fato conferido em 03-10-2026: o Spring 7 marca nas próprias APIs o que pode ser
nulo, com as anotações JSpecify.

## Decisão

- O Spotless formata o código num estilo pronto.
- O Error Prone roda junto da compilação e acusa defeito, não estilo.
- O NullAway, extensão do Error Prone, acusa nulo não tratado e `Optional`
  aberto sem checagem, só no pacote de regras.

Os três rodam na integração contínua (ADR-0032), e a compilação falha quando
um deles acusa.

## Consequências

O NullAway restrito às regras repete o desenho do ADR-0016: o núcleo é código
próprio com tipos simples, onde a checagem estrita sai de graça, e a ausência
do peso semanal (RN-16) não passa sem tratamento. Como o Spring 7 já marca o que
pode ser nulo, o NullAway entende também o que vem do framework.

Fora das regras, o compilador confere o tipo, e o nulo fica por convenção
(`convencoes-backend.md`) e por teste.

O Error Prone faz o papel de lint que o ruff fazia: pega defeito que compila.
Ele não confere lógica: uma função que devolve a soma em vez da média passa
sem reclamação, e é por isso que o teste do ADR-0020 existe em paralelo.

Custo aceito: configuração no build e uma compilação um pouco mais lenta.

## Alternativa recusada: só o Spotless

Formataria e deixaria o nulo só por convenção. Recusado porque perde o lint e a
checagem que o ADR-0016 existia para fazer.

## Alternativa recusada: Checkstyle

Muito comum em empresa. Recusado porque confere estilo, não defeito, e o estilo
já fica com o Spotless.

## Alternativa recusada: NullAway no projeto inteiro

Código de integração lida com tipo de biblioteca de terceiro, e a checagem
estrita ali produz reclamação que se silencia com anotação de exceção. Exceção
espalhada esvazia a ferramenta.

## Alternativa recusada: nenhuma checagem de nulo

Deixaria a ausência do peso semanal dependendo de alguém lembrar de tratar.
