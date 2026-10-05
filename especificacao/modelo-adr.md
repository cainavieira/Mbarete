# Modelo de ADR

ADR registra uma decisão de arquitetura ou de stack que teve alternativa de
verdade. O formato parte do de Michael Nygard (título, situação, contexto,
decisão, consequências) e acrescenta o que este projeto aprendeu a precisar: a
data, a alternativa recusada com o motivo e quando a decisão merece ser
revista.

## O que vira ADR, e o que não vira

Vira ADR a escolha que alguém poderia razoavelmente ter feito diferente, e cujo
motivo, se perdido, faz a discussão recomeçar: framework, banco, onde o sistema
roda, como autentica, como migra.

Não vira ADR o detalhe de implementação: nome de propriedade de configuração,
biblioteca pequena, padrão de escrita de código. Isso vai para
`convencoes-backend.md` ou `convencoes-frontend.md`. O detalhe só entra no ADR
quando ele é a própria decisão, ou quando sem ele a decisão não se cumpre.

## Cabeçalho

    # ADR-NNNN Título que diz a decisão

    - **Data:** DD-MM-AAAA
    - **Situação:** aceito
    - **Supera:** ADR-NNNN

- O título diz a decisão, não o assunto: "Postgres como banco de dados", e não
  "Banco de dados".
- **Situação** é `aceito` ou `superado por ADR-NNNN`.
- **Supera** só existe quando o ADR substitui outro.

## Seções, nesta ordem

**Contexto.** O problema e o que pesa nele, citando a regra ou o requisito pelo
identificador. Número conferido vai com a data da conferência.

**Decisão.** O que se decidiu, em poucas frases, no presente.

**Consequências.** O que passa a valer, o que se ganha e o custo aceito. Custo
que ninguém escreveu volta como surpresa.

**Alternativa recusada: nome.** Uma seção por alternativa real: o que ela
oferecia e por que perdeu. Opção que ninguém considerou não entra.

**Quando rever**, quando houver. O fato que tornaria a decisão errada.

**Observação**, opcional, para o que não cabe nas outras.

## Regras

- O número nunca volta, nem o de ADR superado.
- ADR nunca é reescrito. Decisão que muda ganha ADR novo, que a supera, e no
  superado muda só a linha de situação. O raciocínio antigo fica intacto.
- Criar ADR atualiza as contagens do `README.md` e do `CLAUDE.md` no mesmo
  commit.
- O arquivo se chama `ADR-NNNN-assunto-curto.md` e fica em
  `especificacao/adr/`.
