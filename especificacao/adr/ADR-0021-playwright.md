# ADR-0021 Playwright para o teste de ponta a ponta

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

A camada 3 do ADR-0020 pede um teste de fumaça em navegador real.

## Decisão

Playwright.

## Consequências

O paralelismo é orquestrado pelo próprio executor, com processos locais e
divisão entre máquinas por parâmetro. Nenhum serviço, conta ou assinatura
envolvidos.

A imagem oficial do Playwright já traz os navegadores instalados, o que evita
baixar centenas de megabytes em cada execução da integração contínua.

## Alternativa recusada: Cypress

Duas razões de peso.

A primeira é arquitetural: o Cypress roda o código de teste **dentro** do
navegador, no mesmo ambiente da aplicação. É isso que dá a ele o depurador com
viagem no tempo, e é a origem das limitações: sendo código de navegador, ele
obedece à política de mesma origem. O `cy.origin()` existe e mantém restrições
severas (não aninha, não é closure, proíbe interceptação dentro dele, exige
serialização dos argumentos), e a opção que permite importar dependência dentro
dele segue marcada como experimental desde 2022. Playwright dirige o navegador
de fora, então o problema não existe.

A segunda é de cota: o paralelismo hoje está no plano gratuito, mas continua
exigindo a nuvem do fornecedor e a marca de gravação, com teto de 500 resultados
de teste por mês, e paralelizar não reduz essa contagem.

Verificado em 07-09-2026: `playwright` teve 87,5 milhões de downloads semanais
contra 7,6 milhões do `cypress`. Comparando apenas o executor de teste, a razão
é de 7,7 vezes.
