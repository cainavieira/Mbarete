# ADR-0034 Isolamento entre usuários na aplicação

- **Data:** 02-10-2026
- **Situação:** aceito

## Contexto

A RN-50 diz que cada usuário acessa exclusivamente os próprios dados (RF-05,
RNF-11). As tabelas `registro` e `fase` guardam os dados de todos os usuários
juntos, separados pela coluna `usuario_id`, e a fase é endereçada por
identificador sequencial compartilhado. Uma consulta que esqueça o
`usuario_id` alcança dado alheio.

O Postgres consegue impor esse isolamento sozinho, por segurança em nível de
linha, e o `convencoes-backend.md` manda restrição que o banco consegue impor
ir para o banco também (ADR-0033).

## Decisão

Na aplicação, e só nela. Toda consulta a `registro` e `fase` recebe o
`usuario_id` da sessão, e nenhuma rota consulta essas tabelas sem ele. Cada
rota tem teste de que um usuário não alcança o dado de outro.

## Consequências

O controle de acesso fica numa camada só, a mesma das regras, e é verificado
por teste de rota contra Postgres real (ADR-0020).

Recurso de outro usuário e recurso inexistente dão o mesmo resultado vazio, e
por isso a mesma resposta, 404 (`endpoints.md`).

A regra de pôr no banco também continua valendo para integridade de dado:
chave única, faixa, sobreposição. Controle de acesso não entra nela.

## Alternativa recusada: segurança em nível de linha do Postgres

O banco recusaria sozinho a consulta que esquecesse o filtro. Recusada pelo
custo, para proteger o que o teste por rota já pega: identidade informada ao
banco em toda transação, uma conta de banco a mais porque superusuário ignora
a política, as tabelas do login fora dela porque são consultadas antes de se
saber quem é a pessoa, e mais uma camada onde depurar quando um teste falha.
