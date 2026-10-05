# ADR-0015 ruff para lint e formatação

- **Data:** 07-09-2026
- **Situação:** superado por ADR-0042

## Contexto

São dois trabalhos diferentes. Formatação decide aparência e não muda
comportamento; o valor dela é que ninguém volta a discutir o assunto e que o
diff de um commit passa a mostrar só mudança real. Lint procura defeito que
passa sem erro de sintaxe.

O script de origem do projeto tem dois exemplos do segundo tipo: um nome de
variável usado ao mesmo tempo como dicionário e como variável de laço, que
funciona uma vez e destrói o nome; e um `except` largo que engole erro de
arquivo inexistente e deixa o programa continuar com lista vazia, reportando
resultado vazio em vez de falha.

## Decisão

`ruff`, cobrindo lint e formatação, configurado no `pyproject.toml` e executado
na integração contínua (ADR-0030).

## Consequências

Uma ferramenta, uma configuração, uma linha no `requirements-dev.txt`.

Execução em fração de segundo no projeto inteiro, o que permite rodar no
editor a cada gravação sem incômodo.

## Alternativa recusada: black com flake8 e isort

É o conjunto histórico e continua funcionando. Recusado por ser três
instalações e três configurações para o mesmo resultado, com o agravante
conhecido de a ordenação do isort conflitar com o tamanho de linha do black,
exigindo perfil de compatibilidade.
