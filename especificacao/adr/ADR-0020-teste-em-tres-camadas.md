# ADR-0020 Teste em três camadas

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

A RNF-27 exige cobertura das regras de agregação. As regras não importam
framework (ADR-0019), o que abre a possibilidade de testá-las sem banco.

## Decisão

Três camadas, com critério de corte explícito em cada uma.

1. **Regras de agregação**, sem banco algum: função recebe lista de registros e
   devolve valor. É onde mora a maior parte dos testes.
2. **Rotas**, contra Postgres real e descartável, que o Testcontainers sobe para
   a execução dos testes. A rota é chamada pelo MockMvc, sem abrir porta de
   rede, e cada teste roda dentro de uma transação revertida no fim. O login é
   simulado pelo suporte de teste do Spring Security, sem o Google.
3. **Um teste de fumaça de ponta a ponta** com Playwright (ADR-0021): token de
   renovação injetado (ADR-0022), registra o peso de hoje, o valor aparece no
   gráfico.

No frontend, testa-se componente que **decide**, não componente que desenha: o
que escolhe entre número e "dados insuficientes" (RN-16, RN-39), e o que trata
vírgula na entrada e na exibição (RN-62). A trava da barra em 0% e 100%
(RN-44) é regra do backend: o progresso chega pronto (`endpoints.md`), e a
trava se testa na camada 1.

## Consequências

A camada 1 roda em milissegundos e cobre o que mais importa.

A camada 3 pega a única classe de defeito que as outras duas não pegam de jeito
nenhum: fiação. Botão chamando endpoint errado, campo enviado com nome errado,
invalidação de cache que não aconteceu. Teste de unidade e de rota passam
verdes com a fiação trocada.

Fica descoberto de propósito: a verificação da assinatura do `id_token` e a
cadeia real de redirecionamento do OAuth, consequência da decisão do ADR-0022.
Isso se cobre por teste de integração no backend contra um `id_token`
fabricado, nunca por ponta a ponta.

## Alternativa recusada: SQLite como substituto do Postgres nos testes

Seria mais rápido e dispensaria banco de pé. Recusado porque SQLite difere do
Postgres em tipo, restrição e função de data, e no caso concreto deste sistema
não reproduz o arredondamento de `numeric(4,1)` que a RN-02 exige. Teste que
passa no substituto e falha em produção é o pior tipo de teste, porque entrega
confiança falsa.

## Alternativa recusada: o Postgres do Compose de desenvolvimento

O teste usaria o banco que já roda na máquina. Recusado porque dependeria de ele
estar de pé, misturaria dado de teste com o de desenvolvimento, e não rodaria
igual na integração contínua.

## Alternativa recusada: suíte ampla de ponta a ponta

Cobrir todos os casos de uso por navegador custaria tempo de execução, banco
semeado e manutenção constante, para cobrir de novo o que as camadas 1 e 2 já
cobrem melhor e mais rápido.
