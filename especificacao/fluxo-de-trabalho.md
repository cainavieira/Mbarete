# Fluxo de trabalho

Como o trabalho entra no repositório. O projeto tem uma pessoa mais assistência,
e este documento é calibrado para isso: nada aqui existe para coordenar equipe.

## Ramos

`main` mais um ramo curto por tarefa, integrado rápido.

Nome do ramo: o mesmo tipo do commit, mais o assunto em poucas palavras.
`feat/registro-diario`, `fix/janela-com-poucos-pontos`,
`docs/convencoes-frontend`.

**Git Flow foi recusado**, e a recusa vale registro porque muita gente chama
qualquer uso de ramo de "git flow". O modelo original, de 2010, tem `develop`,
`release/*` e `hotfix/*`, e existe para produto com versões numeradas e várias
mantidas em paralelo. Aqui existe uma versão: a que está no ar (ADR-0031).

Ramo curto significa dias, não semanas. Ramo que vive um mês acumula conflito
com `main` e vira um segundo projeto.

## Commit

Formato:

```
<tipo>: <o que muda, no imperativo, ate ~50 caracteres>

<corpo, quebrado em 72 colunas>
```

Tipos: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style`, `build`,
`ci`, `chore`, `revert`.

Os quatro que se confundem: `style` é formatação sem mudança de comportamento e
não tem relação com design visual; `build` mexe no que produz o artefato e `ci`
mexe em quem dispara; `perf` é mudança que só existe pela velocidade; `revert`
desfaz um commit anterior.

**O corpo existe para o porquê, não para o como.** O diff já mostra o como.
Quem abre o commit em seis meses quer o que não está no diff: qual problema
existia, que alternativa foi recusada, que preço se aceitou pagar. Mudança
mecânica pode não ter corpo nenhum.

Dois testes antes de escrever a frase do corpo:

1. **Sobrevive a um rename?** Se cita nome de função, assinatura ou caminho de
   código, vira mentira no dia em que alguém renomeia. A intenção não envelhece;
   o identificador sim.
2. **O diff já mostra isso?** Se sim, corta.

Mensagem de commit **sem acento**: ela vai para terminal, log e ferramenta de
terceiro, onde codificação errada aparece como lixo.

## Antes de integrar

Roda local e roda na integração contínua (ADR-0030), e os dois têm que passar:

- `ruff` para lint e formatação
- `mypy`, estrito no módulo de regras
- `pytest`, as duas camadas de backend
- `vitest`, os componentes que decidem
- o teste de fumaça do Playwright

Não há revisão de segunda pessoa. A integração contínua é o revisor, e é por
isso que ela não é opcional: ferramenta de qualidade que roda quando alguém
lembra deixa de rodar em duas semanas.

Quando o código é escrito por assistência, a revisão é do interessado. Quando é
escrito pelo interessado, a assistência aponta o defeito e explica o mecanismo,
sem reescrever o arquivo sem pedido.

### Artefatos da integração contínua

Rastro de teste e relatório de build sobem como artefato **apenas quando o job
falha**, com retenção curta.

O motivo é cota e retenção. Artefato divide a mesma cota que pacotes, 500 MB no
plano gratuito, e um rastro do Playwright tem alguns megabytes porque carrega
imagem e vídeo. A documentação do GitHub afirma isenção de cota para **pacote**
público, e não afirma o mesmo para **artefato**, então não se conta com isenção.
Somado a isso, desde 1º de outubro de 2026 a retenção padrão de execuções passou
a 90 dias.

Execução que passou não gera pergunta, então o rastro dela não serve e ocupa o
espaço do rastro que serviria. O que serve é o de falha: linha do tempo, captura
de tela de cada passo, estado do DOM e requisições de rede. É o que permite
depurar teste que falha na integração contínua e passa na máquina de quem
escreveu, que é a situação mais difícil de investigar sem ele.

## Definição de pronto

Uma tarefa está pronta quando as seis linhas abaixo são verdade.

1. O comportamento corresponde ao requisito, citado pelo identificador (`RF-07`,
   `UC-02`).
2. Regra nova ou alterada está escrita em `regras-de-negocio.md`, com número
   novo, nunca reaproveitando número aposentado.
3. Existe teste na camada certa (ADR-0020): regra pura sem banco, rota contra
   Postgres, ou nenhum se for tela que apenas desenha.
4. `ruff`, `mypy` e os testes passam local e na integração contínua.
5. Se houve decisão com alternativa recusada, existe ADR novo. Se a decisão
   substitui uma anterior, o ADR novo diz qual supera, e o antigo não é editado.
6. Se houve migração de esquema, a restauração do backup foi testada (RNF-08).

## Quando cada documento muda

| Aconteceu | Muda |
|:--|:--|
| Regra de negócio nova ou alterada | `regras-de-negocio.md`, e o requisito que a cita |
| Comportamento novo visível ao usuário | `requisitos-funcionais.md` e `casos-de-uso.md` |
| Decisão de tecnologia com alternativa recusada | ADR novo em `especificacao/adr/` |
| Rota nova ou alterada | `endpoints.md` |
| Contêiner novo, ou caminho da requisição mudou | `arquitetura.md` |
| Antipadrão descoberto na prática | a convenção do lado correspondente |

Documento datado que uma descoberta posterior contradiz **não estava errado**:
registrava o que se sabia. O teste, antes de reescrever, é se a informação nova
muda o que deve ser feito ou apenas o que se acreditava. Só a primeira justifica
mexer. ADR nunca é reescrito: é superado.

A **única** edição permitida num ADR superado é a linha de situação, que passa a
apontar o ADR que o substituiu. Sem isso, quem abrir o antigo sozinho não
descobre que ele morreu, e pode agir em cima de decisão que não vale mais. O
texto do raciocínio continua intacto, que é o que a regra protege.

## `especificacao/` é versionada

Deliberado, e vale dizer em voz alta porque há o hábito de manter pasta de
documentação fora do versionamento. Aqui a especificação é o artefato mais
valioso do projeto: ela existe antes do código e é o que impede a discussão de
recomeçar. Ela entra em commit como qualquer arquivo.

O que **não** entra: `.env`, o `.venv/`, `node_modules/`, e a ferramenta local
em `.claude/`.

## Deploy

Manual, por script que para no primeiro erro (ADR-0031). A ordem importa:

1. `git pull` no servidor
2. `alembic upgrade head`
3. testar a restauração do backup, se houve migração (RNF-08)
4. `docker compose up -d --build`

Push para o remoto é sempre ação do interessado, nunca automática.
