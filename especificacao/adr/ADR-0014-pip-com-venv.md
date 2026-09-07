# ADR-0014 pip com venv e requirements.txt

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O projeto é declaradamente de aprendizado de prática de indústria, além de
produto.

## Decisão

`venv` para isolamento, `pip` para instalação, e dois arquivos: `requirements.txt`
gerado por `pip freeze` para produção, e `requirements-dev.txt` para as
ferramentas de qualidade e teste, que não entram na imagem de produção.

## Consequências

Escolha feita por aprendizado explícito: são as ferramentas que qualquer
projeto Python usa, e conhecê-las é pré-requisito para entender o que as mais
novas escondem.

O `pip freeze` grava a árvore inteira com versão exata, incluindo dependências
de dependências. Isso é o que evita o defeito clássico do `requirements.txt`
escrito à mão, em que uma dependência transitiva sobe de versão sozinha e o
projeto quebra sem ninguém ter mudado nada.

Não existe arquivo separando o que se pediu do que veio por arrasto. Consequência
aceita: quando fizer falta, o caminho é manter um arquivo de origem à mão.

## Alternativa recusada: uv

Substitui pip, venv e pip-tools de uma vez, com travamento de versões
transitivas em arquivo de bloqueio, e é ordens de magnitude mais rápido.
Recusado por decisão de aprendizado, não por defeito técnico.

## Alternativa recusada: poetry

Traz gestão de dependência e empacotamento juntos, e o empacotamento não serve
a uma aplicação que roda em contêiner e não é publicada como biblioteca.
