# ADR-0030 GitHub privado com Actions em runners hospedados

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O código existe apenas na máquina do interessado, e o backup da RNF-05 cobre o
banco, não o código. O projeto tem ruff, mypy, pytest, Vitest e um teste de
Playwright que só valem se rodarem sem depender de alguém lembrar.

O interessado cogita cobrar pelo sistema um dia (RNF-28).

Números verificados em 07-09-2026: GitHub Free dá 2.000 minutos de Actions por
mês em repositório privado, 500 MB de armazenamento compartilhado entre pacotes
e artefatos, e não oferece proteção de branch em repositório privado. GitLab
Free dá 400 minutos, 10 GiB por projeto com registro de contêiner fora da cota,
e proteção de branch no plano gratuito.

## Decisão

Repositório **privado** no GitHub, com GitHub Actions em runners hospedados
rodando ruff, mypy, pytest, Vitest e o teste de fumaça em cada envio.

Privado e não público por assimetria: abrir depois é um clique, e fechar depois
de ter sido público não desfaz clones que já existem.

## Consequências

2.000 minutos contra 400 é cinco vezes. Uma execução da suíte com Playwright
consome de 5 a 10 minutos, o que dá de 200 a 400 execuções por mês, o suficiente
para nunca precisar decidir quando rodar CI.

Os 500 MB compartilhados exigem cuidado concreto: rastro do Playwright sobe
apenas em falha, e com retenção curta de artefato. Sem isso, rastro compete com
imagem de contêiner na mesma cota.

Proteção de branch não existe no plano gratuito, e isso é aceito: num
repositório com uma pessoa, proteção de branch é cerimônia, não controle.

O remoto passa a ser também a cópia externa do código.

## Alternativa recusada: GitLab Free com runner próprio

Seria integração contínua praticamente sem limite, mais 10 GiB e proteção de
branch de graça, aproveitando o servidor doméstico que já existe. Recusada por
dois motivos: o servidor não fica ligado o tempo todo, então o runner não fica,
e esperar a máquina subir para ver resultado de teste gera o atrito que faz
parar de esperar o CI; e um runner morto não avisa, os jobs apenas ficam
pendentes, o que exigiria um segundo alerta de homem morto como o da RNF-09.

Anotado que, desde 01-03-2026, runner próprio em repositório privado do GitHub
passou a ser cobrado e a consumir a mesma cota, o que é o ponto em que o GitLab
voltaria a ganhar se essa rota fosse tomada.

## Alternativa recusada: GitLab Free com runners hospedados

400 minutos por mês dão de 40 a 80 execuções, menos de duas por dia útil, o que
obriga a escolher quando rodar teste.

## Alternativa recusada: repositório público

Melhor para portfólio e sem limite de minutos. Recusado pela possibilidade de
cobrança futura e por não ser reversível.
