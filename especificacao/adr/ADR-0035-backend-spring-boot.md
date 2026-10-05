# ADR-0035 Backend em Spring Boot com Java 21

- **Data:** 04-10-2026
- **Situação:** aceito
- **Supera:** ADR-0001, ADR-0014

## Contexto

O backend estava decidido em Python, com FastAPI (ADR-0001) e pip com venv
(ADR-0014). Nada nessa escolha falhou, e a especificação inteira foi escrita
sobre ela.

O motivo da troca não é técnico. O interessado quer aprender Spring Boot até
ficar proficiente, e quer aprender num sistema com especificação completa e
usuários reais, não num exemplo. O projeto já se declara de aprendizado de
prática de indústria, além de produto (ADR-0014).

Versões conferidas em 03-10-2026:

| Linha | Suporte gratuito até |
|:--|:--|
| Spring Boot 3.5 | 30-06-2026, encerrado |
| Spring Boot 4.0 | 31-12-2026 |
| Spring Boot 4.1 | 31-07-2027; a 4.1.1 é a versão de produção atual |

O Spring Boot 4.1 exige Java 17 ou mais novo e funciona até o Java 26. O Java
21 é LTS, com suporte da Oracle até setembro de 2028 e do Temurin até pelo
menos dezembro de 2029.

## Decisão

Spring Boot 4.1, em Java 21, com Maven. A camada web é o Spring MVC.

O contrato de API continua gerado a partir do código que roda, agora pelo
springdoc-openapi, ligado só em desenvolvimento, onde os tipos do frontend são
gerados (ADR-0027).

## Consequências

O que pesava a favor do FastAPI no ADR-0001 continua: a regra de valor é
declarada no tipo, por Bean Validation, e o documento de endpoints sai do
código, sem dividir a verdade com ele. O springdoc traz o Swagger UI junto.

O Spring Boot fixa as versões de tudo o que gerencia numa lista testada em
conjunto, e o Maven não troca versão escrita. O defeito que o ADR-0014 evitava
com o `pip freeze`, dependência transitiva subindo de versão sozinha, não
aparece.

Custos aceitos em troca do aprendizado:

- a JVM ocupa mais memória no servidor de casa que o processo Python;
- em desenvolvimento, o DevTools reinicia a aplicação em segundos, em vez do
  recarregamento instantâneo do uvicorn;
- persistência, migração, autenticação, qualidade de código e o
  `convencoes-backend.md` são decididos de novo, cada um no seu ADR.

Nenhuma regra de negócio, requisito funcional, caso de uso ou rota do contrato
muda por causa desta decisão.

## Alternativa recusada: manter o FastAPI

Seria a escolha se o objetivo fosse só o produto: ele atende ao sistema e não
tem defeito aqui. Recusado porque o objetivo passou a incluir aprender Spring,
e isso só acontece escrevendo Spring.

## Alternativa recusada: Spring Boot 3.5

É a linha com mais material publicado e mais código em empresa. Recusada
porque o suporte gratuito acabou em 30-06-2026, e começar nela é começar sem
correção de segurança. O que muda da 3 para a 4 (Spring Framework 7, Jakarta EE
11, Jackson 3) troca nome de pacote e de dependência, não os conceitos que se
aprendem.

## Alternativa recusada: Kotlin

O Spring suporta bem, e seria uma segunda linguagem para aprender junto com o
framework. A maior parte da documentação e dos exemplos de Spring é em Java.

## Alternativa recusada: Gradle

Mais rápido e mais flexível, com a configuração escrita em código. A
flexibilidade é o custo: cada projeto Gradle se monta de um jeito, e seria mais
uma coisa a aprender junto. O Maven é declarativo e igual em todo projeto.

## Alternativa recusada: Spring WebFlux

Programação reativa, que paga com muita conexão simultânea esperando entrada e
saída. Cinco usuários com uma escrita por dia (RNF-25) não pedem isso, e ele
troca o modelo de programação inteiro.
