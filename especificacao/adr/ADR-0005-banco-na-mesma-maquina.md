# ADR-0005 Banco na mesma máquina do backend

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

Com Postgres escolhido (ADR-0004), sobra decidir onde ele roda: junto do
backend, ou como serviço gerenciado na nuvem.

## Decisão

Postgres roda no mesmo servidor do backend, como serviço do Compose, alcançável
apenas pela rede interna (RNF-20).

## Consequências

A consulta não atravessa a internet: a diferença é menos de 1 milissegundo
contra algo entre 20 e 80 por consulta.

O backup passa a ser trabalho nosso: dump agendado, retenção definida e
restauração testada a cada migração (RNF-05 a RNF-09). Serviço gerenciado faria
isso sozinho, e é a única vantagem real que se está recusando.

O dado de saúde dos usuários fica em hardware próprio, sem terceiro no caminho.

## Alternativa recusada: Neon

O plano gratuito desliga o banco depois de 5 minutos sem uso e não permite
desativar esse comportamento, então a primeira requisição do dia espera o banco
acordar.

## Alternativa recusada: Supabase

O plano gratuito pausa o projeto depois de 7 dias de baixa atividade, e
despausar é manual. Um sistema de acompanhamento de peso entre poucos amigos
passa uma semana quieto com facilidade.

## Alternativa recusada: Render

O Postgres gratuito é apagado 30 dias depois de criado, com os dados. Isso
colide de frente com a RNF-04.
