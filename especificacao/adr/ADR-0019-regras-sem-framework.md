# ADR-0019 Regras de agregação não importam Spring nem JPA

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

A RNF-27 exige teste automatizado das regras de agregação (peso semanal, taxa
real, veredito de ritmo, progresso), porque são a parte do sistema onde o erro
passa despercebido: o número sai errado e continua parecendo plausível.

Foi levantada a possibilidade de adotar Clean Architecture completa, com
entidades, casos de uso, adaptadores e infraestrutura.

## Decisão

Uma regra só, em vez de camadas: o módulo que contém as regras de agregação não
importa Spring nem JPA. Ele recebe listas de dados e devolve valores.

Da mesma família, duas regras concretas de responsabilidade: o controller não
calcula nada, ele chama quem calcula; e a função de regra recebe dados, não o
repositório.

## Consequências

O teste das regras roda sem banco e sem servidor, em milissegundos, que é o que
torna a RNF-27 barata em vez de cerimonial (ADR-0020).

O módulo de regras é o único lugar com o NullAway (ADR-0042), o que é possível
justamente porque ele não toca em tipo de biblioteca de terceiro.

## Alternativa recusada: camadas completas de Clean Architecture

Entidades, casos de uso, adaptadores e infraestrutura com inversão de
dependência em cada fronteira custam muita indireção para cinco telas. O núcleo
da ideia que vale aqui é a regra acima, e ela se escreve numa linha.

## Sobre SOLID

Dos cinco princípios, dois têm efeito neste sistema e estão escritos acima como
regras concretas. Os outros três (substituição de Liskov, segregação de
interface, aberto e fechado) pressupõem hierarquia de herança que este sistema
não tem. Declarar adesão a SOLID sem nomear o que ela implica não muda uma linha
de código, e por isso não está declarado.
