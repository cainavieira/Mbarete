# ADR-0028 Tailwind com shadcn/ui

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O sistema tem poucas telas, e precisa de alguns componentes com comportamento
não trivial: diálogo de confirmação de substituição (UC-02, fluxo 4a), diálogo
de exclusão de conta (UC-12), formulários, tabela de registros, e a tela de
pré-visualização da importação.

Diálogo acessível feito à mão (foco preso dentro dele, fechar por Escape,
retorno do foco ao fechar) é mais difícil do que parece.

## Decisão

Tailwind para estilo, shadcn/ui para os componentes.

## Consequências

O shadcn/ui **não é uma dependência instalada**: é uma coleção de arquivos de
componente que um comando copia para dentro do repositório. Os componentes
passam a ser código próprio, editável direto, sem versão de biblioteca para
brigar.

Por baixo ele combina Radix UI, que dá o comportamento e a acessibilidade, com
Tailwind, que dá a aparência.

Contrapartida do modelo de cópia: correção feita no projeto de origem não chega
sozinha. Atualizar é recopiar o componente quando houver motivo.

O repositório cresce com código que não foi escrito aqui mas é mantido aqui.

## Alternativa recusada: CSS Modules

É a opção que ensina mais CSS, e seria a escolha se o aprendizado tivesse
prioridade sobre o produto. Não tem, pela prioridade declarada do projeto.

## Alternativa recusada: biblioteca de componentes pronta (MUI, Chakra)

Traria um sistema de design inteiro para poucas telas, com o efeito colateral de
brigar com ele quando o gráfico (ADR-0029) e a barra de progresso não couberem
no molde.
