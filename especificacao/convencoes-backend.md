# Convenções do backend

Regras de escrita, não de negócio. O que vale como negócio está em
`regras-de-negocio.md`, e o porquê de cada escolha de ferramenta está nos ADR.

## Idioma dos identificadores

Domínio em português, vocabulário técnico em inglês.

| O que | Como | Exemplos |
|:--|:--|:--|
| Tabela e modelo | português, singular | `registro`, `fase`, `usuario`, `sessao`, `convidado` |
| Coluna | português | `peso`, `cintura`, `data`, `tipo`, `nivel`, `taxa_alvo`, `peso_alvo`, `data_inicio` |
| Função de regra | português | `calcular_peso_semanal`, `avaliar_ritmo`, `calcular_progresso` |
| Vocabulário de biblioteca | inglês | `router`, `session`, `client`, `settings`, `get_session` |

O motivo: os termos do domínio não têm tradução limpa, e traduzir obriga a
manter um glossário mental entre a regra escrita e o código. `cutting` e
`bulking` ficam como estão, porque é assim que se fala.

Sem acento em identificador (`sessao`, `nivel`), como manda o Python idiomático.
Acento só em texto: mensagem, comentário, docstring.

## Nomeação

- **Tabela**: singular, snake_case, nome da entidade. A tabela descreve o tipo
  de uma linha, e a linha é um registro.
- **Coluna**: snake_case, sem repetir o nome da tabela. `registro.peso`, nunca
  `registro.registro_peso`.
- **Chave estrangeira**: `<entidade>_id`. `usuario_id`.
- **Módulo**: snake_case, nomeado pelo assunto. `regras/peso_semanal.py`.
- Função que pode não ter resposta não avisa isso no nome. O tipo `| None` avisa.

## Tipagem

Toda função tem anotação de parâmetro e de retorno. Não é preferência: o `mypy`
do ADR-0016 depende disso, e no módulo de regras o modo estrito recusa função
sem anotação.

- Peso e cintura são `Decimal`, nunca `float` (ADR-0017).
- Data é `date`, nunca `datetime` (ADR-0018).
- Ausência é `| None` explícito. Nunca valor sentinela como `-1`, `0` ou string
  vazia.
- `Decimal` construído de texto ou inteiro, nunca de `float`.

## Formato da resposta de erro

Um formato só, para todos os erros, inclusive os de validação do Pydantic:

```json
{
  "erro": {
    "codigo": "PESO_FORA_DE_FAIXA",
    "campo": "peso",
    "mensagem": "O peso deve estar entre 20,0 e 400,0 kg.",
    "regra": "RN-03"
  }
}
```

- `codigo` existe para o frontend decidir comportamento sem depender do texto,
  que muda.
- `campo` permite destacar o input certo.
- `mensagem` é o que o usuário lê, e vem do backend porque a regra é do backend
  (ADR-0025).
- `regra` cita o documento que originou a recusa. Serve para depuração, não para
  exibir: um erro em produção passa a ser rastreável no
  `regras-de-negocio.md` em vez de caçado no código.

### Catálogo de códigos

Derivado das regras. Todo código novo entra aqui junto com a regra que o gera.

| Código | Quando | Regra |
|:--|:--|:--|
| `PESO_FORA_DE_FAIXA` | peso fora de 20,0 a 400,0 | RN-03 |
| `CINTURA_FORA_DE_FAIXA` | cintura fora de 40,0 a 200,0 | RN-11 |
| `DATA_FUTURA` | data do registro no futuro | RN-05 |
| `DATA_LOCAL_INVALIDA` | cabeçalho `Data-Local` ausente, malformado ou a mais de um dia da data do servidor | RN-04 |
| `REGISTRO_JA_EXISTE` | já há registro na data | RN-01 |
| `PESO_ALVO_AUSENTE` | cutting ou bulking sem peso alvo | RN-41 |
| `NIVEL_AUSENTE` | cutting ou bulking sem nível | RN-28 |
| `FASE_SOBREPOSTA` | a fase invade outra num caso que a RN-19 recusa | RN-19 |
| `FASE_NAO_CORRENTE` | encerramento pedido para fase que não é a corrente | RN-27 |
| `DATA_FIM_FUTURA` | data de fim no futuro, ao criar, editar ou encerrar fase | RN-67 |
| `DATA_FIM_ANTES_DO_INICIO` | data de fim anterior à de início | RN-18 |
| `NAO_CONVIDADO` | e-mail fora da lista de convidados | RN-48 |
| `NAO_ENCONTRADO` | registro ou fase inexistente, ou de outro usuário, sem distinguir os dois | RN-50 |
| `CSV_COLUNA_AUSENTE` | falta coluna obrigatória | RN-53 |
| `CSV_SEPARADOR_DESCONHECIDO` | separador não reconhecido | RN-54 |
| `CSV_LINHA_INVALIDA` | linha recusada na pré-visualização | RN-60 |

`REGISTRO_JA_EXISTE` é o único da lista que não é recusada final: ele alimenta a
confirmação de substituição do UC-02. O mecanismo exato (resposta de conflito
ou verificação prévia) é decidido em `endpoints.md`.

## Onde mora o quê

**Validação de forma e de campo isolado** fica no esquema Pydantic: tipo,
obrigatoriedade, casas decimais, faixa de peso e de cintura.

**Validação que depende de outro dado** fica em `regras/`: a proibição de
sobreposição de fase (RN-19) precisa das outras fases do usuário, e o esquema de
um campo não conhece o resto do sistema.

**Transação** é aberta e fechada na rota, por dependência do FastAPI. Função de
regra nunca recebe sessão de banco (ADR-0019).

**Cálculo** nunca fica na rota. A rota recebe, chama, devolve.

**Restrição de integridade que o banco consegue impor** vai para o banco
**também**, e não em vez de: a verificação na aplicação existe para devolver
mensagem que o usuário entende, e a restrição no banco existe para pegar o
caminho que alguém esqueceu de validar (ADR-0033).

**Isolamento entre usuários** fica na aplicação, e só nela (ADR-0034): toda
consulta a `registro` e `fase` recebe o `usuario_id` da sessão, e nenhuma rota
consulta essas tabelas sem ele. Cada rota tem teste de que um usuário não
alcança o dado de outro.

## Antipadrões proibidos

1. `except Exception` largo que não re-levanta. É o defeito do script de
   origem deste projeto: ele engolia arquivo inexistente, imprimia, e reportava
   resultado vazio em vez de falha. Capturar exceção específica, ou deixar subir.
2. `float` para peso ou cintura (ADR-0017).
3. `datetime` onde a regra fala de data (ADR-0018).
4. Importar `fastapi` ou `sqlalchemy` dentro de `regras/` (ADR-0019). Existe
   teste que verifica isso.
5. Cálculo de agregação dentro da função de rota.
6. Argumento padrão mutável (`def f(x=[])`).
7. `print` para diagnóstico. O log existe e é só nível erro (RNF-16).
8. Valor sentinela para representar ausência.
9. Editar migração já aplicada em produção. O banco de produção já executou
   aquele arquivo; alterá-lo faz o histórico do Alembic mentir sobre o estado
   real. Correção entra como migração nova.
10. Consulta dentro de laço. No volume da RNF-25 não dói, e o hábito dói depois.

## Migração de esquema

- Todo rascunho do autogenerate é revisado antes de aplicado.
- Renomeação de coluna é corrigida à mão: o autogenerate gera remoção mais
  criação, o que apagaria dado.
- A reversão é escrita, e não é plano de recuperação. O plano é o backup.
- A primeira migração habilita a extensão `btree_gist`, exigida pela restrição
  de exclusão que impede sobreposição de fase (ADR-0033).
- Depois de aplicar, testar a restauração (RNF-08).
