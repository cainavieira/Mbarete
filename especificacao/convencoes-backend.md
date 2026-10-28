# Convenções do backend

Regras de escrita, não de negócio. O que vale como negócio está em
`regras-de-negocio.md`, e o porquê de cada escolha de ferramenta está nos ADR.

## Idioma dos identificadores

Domínio em português, vocabulário técnico em inglês.

| O que | Como | Exemplos |
|:--|:--|:--|
| Tabela | português, singular, snake_case | `registro`, `fase`, `usuario`, `token_renovacao`, `convidado` |
| Coluna | português, snake_case | `peso`, `cintura`, `data`, `tipo`, `nivel`, `taxa_alvo`, `peso_alvo`, `data_inicio` |
| Classe | assunto em português, papel em inglês | `Registro`, `FaseService`, `RegistroController`, `RegistroResponse` |
| Método de regra | português, camelCase | `calcularPesoSemanal`, `avaliarRitmo`, `calcularProgresso` |
| Vocabulário técnico | inglês | `controller`, `service`, `repository`, `mapper`, `config` |

O motivo: os termos do domínio não têm tradução limpa, e traduzir obriga a
manter um glossário mental entre a regra escrita e o código. `cutting` e
`bulking` ficam como estão, porque é assim que se fala.

Sem acento em identificador (`token_renovacao`, `nivel`). O Java aceitaria, mas
o banco, o terminal e a busca no código ficam mais simples sem. Acento só em
texto: mensagem, comentário, Javadoc.

## Nomeação

- **Tabela**: singular, snake_case, nome da entidade. A tabela descreve o tipo
  de uma linha, e a linha é um registro.
- **Coluna**: snake_case, sem repetir o nome da tabela. `registro.peso`, nunca
  `registro.registro_peso`.
- **Chave estrangeira**: `<entidade>_id`. `usuario_id`.
- **Campo e coluna**: o campo Java em camelCase, a coluna em snake_case.
  `dataInicio` no código, `data_inicio` no banco.
- **Classe de regra**: nomeada pelo assunto, em `regras/`. `regras/PesoSemanal.java`.
- Método que pode não ter resposta não avisa isso no nome. O retorno `Optional`
  avisa.

## Tipagem

O compilador confere tipo em todo o código. O que ele não confere é nulo, e
por isso as regras abaixo, com o NullAway no pacote de regras (ADR-0042).

- Peso e cintura são `BigDecimal`, nunca `double` ou `float` (ADR-0017).
- `BigDecimal` construído de texto, de inteiro ou por `BigDecimal.valueOf`,
  nunca por `new BigDecimal(double)`.
- `BigDecimal` se compara com `compareTo`, nunca com `equals`: o `equals`
  considera as casas, e 82.4 e 82.40 dão diferentes.
- O arredondamento do veredito (RN-37) é `RoundingMode.HALF_UP`, que leva o
  meio para longe do zero, aplicado só ao resultado, nunca no meio da conta.
- Data é `LocalDate`, nunca `LocalDateTime` nem `Instant` onde a regra fala de
  data (ADR-0018).
- Ausência no retorno é `Optional`, como o peso semanal que não existe pela
  RN-16. Nunca valor sentinela como `-1`, `0` ou string vazia.
- `Optional` só no retorno: nunca em campo, parâmetro ou coleção.
- DTO é `record`.

## Formato da resposta de erro

Um formato só, para todos os erros: os de regra, os de validação do Bean
Validation e os que o Spring gera sozinho, como corpo malformado. Um tratador
global, em `exception/`, converte toda exceção nesse formato.

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
| `TAXA_ALVO_AUSENTE` | cutting ou bulking sem taxa alvo | RN-29 |
| `TAXA_ALVO_INVALIDA` | taxa alvo zero ou negativa; o sinal vem do tipo | RN-29 |
| `FASE_SOBREPOSTA` | a fase invade outra num caso que a RN-19 recusa | RN-19 |
| `FASE_NAO_CORRENTE` | encerramento pedido para fase que não é a corrente | RN-27 |
| `DATA_FIM_FUTURA` | data de fim no futuro, ao criar, editar ou encerrar fase | RN-67 |
| `DATA_FIM_ANTES_DO_INICIO` | data de fim anterior à de início | RN-18 |
| `NAO_CONVIDADO` | e-mail fora da lista de convidados, ou não verificado pelo Google; vai como `motivo` no redirecionamento do callback, não como corpo de erro | RN-48 |
| `LOGIN_NAO_CONCLUIDO` | retorno do login cancelado no Google ou com verificação falha; vai como `motivo` no redirecionamento do callback, não como corpo de erro | RN-49 |
| `NAO_AUTENTICADO` | sem token de acesso válido, inclusive o de conta já apagada; na renovação, sem token de renovação válido; responde 401 | ADR-0041 |
| `NAO_ENCONTRADO` | registro ou fase inexistente, ou de outro usuário, sem distinguir os dois; também rota que não existe, método que a rota não aceita e data ou identificador malformado no caminho | RN-50 |
| `CSV_COLUNA_AUSENTE` | falta coluna obrigatória | RN-53 |
| `CSV_SEPARADOR_DESCONHECIDO` | separador não reconhecido | RN-54 |
| `CSV_LINHA_INVALIDA` | linha recusada na pré-visualização | RN-60 |
| `CORPO_MALFORMADO` | corpo que não vira o DTO (JSON quebrado ou valor sem o tipo do campo), anotação do Bean Validation sem código, parâmetro de consulta com tipo errado, ou outro pedido que o Spring recusa sozinho; responde 422 | sem regra |
| `ERRO_INTERNO` | erro inesperado; a resposta e o log não levam a mensagem da exceção, que pode trazer peso ou cintura; responde 500 | RNF-13 |

`REGISTRO_JA_EXISTE` é o único da lista que não é recusada final: ele alimenta a
confirmação de substituição do UC-02. O mecanismo exato (resposta de conflito
ou verificação prévia) é decidido em `endpoints.md`.

## Onde mora o quê

**Validação de forma e de campo isolado** fica no DTO, por Bean Validation:
tipo, obrigatoriedade, casas decimais, faixa de peso e de cintura, taxa alvo
maior que zero.

**Validação que depende de outro dado** fica em `regras/`: a proibição de
sobreposição de fase (RN-19) precisa das outras fases do usuário, e a validação
de um campo não conhece o resto do sistema.

**Transação** é aberta e fechada no service, com `@Transactional`. O controller
não abre transação, e a função de regra nunca recebe o repositório (ADR-0019).

**Cálculo** nunca fica no controller. O controller recebe, chama o service,
devolve.

**Consulta ao banco** só acontece dentro do service (ADR-0036).

**Conversão entre entidade e DTO** fica em `mapper/`, escrita à mão (ADR-0036).

**Restrição de integridade que o banco consegue impor** vai para o banco
**também**, e não em vez de: a verificação na aplicação existe para devolver
mensagem que o usuário entende, e a restrição no banco existe para pegar o
caminho que alguém esqueceu de validar (ADR-0033).

**Isolamento entre usuários** fica na aplicação, e só nela (ADR-0034): toda
consulta a `registro` e `fase` recebe o `usuario_id` do usuário autenticado, e
nenhuma rota consulta essas tabelas sem ele. Cada rota tem teste de que um
usuário não alcança o dado de outro.

## Configuração

| O quê | Valor | Por quê |
|:--|:--|:--|
| `spring.jpa.hibernate.ddl-auto` | `validate` | o esquema pertence às migrações (ADR-0036) |
| `spring.jpa.open-in-view` | `false` | consulta só dentro do service (ADR-0036) |
| `spring.flyway.enabled` | `false` na api; ligado no modo `migrar` e nos testes | a api nunca migra ao subir (ADR-0040) |
| springdoc e Swagger UI | só no perfil de desenvolvimento | os tipos do frontend são gerados lá (ADR-0027) |
| nível de log | só erro | RNF-16 |
| estilo do Spotless | google-java-format | estilo pronto, sem discussão (ADR-0042) |

Senha, chave e token chegam por variável de ambiente, preenchida pelo Secret
do Kubernetes em produção. Nunca por arquivo versionado (ADR-0037).

O mesmo `.jar` tem três modos:

- sem argumento, é a api;
- `migrar` aplica as migrações e termina: é o que o initContainer e o ensaio
  rodam (ADR-0040);
- `convidar <e-mail>` registra o convite e termina (RF-44, ADR-0011).

## Antipadrões proibidos

1. `catch (Exception e)` largo que não relança. É o defeito do script de
   origem deste projeto: ele engolia arquivo inexistente, imprimia, e reportava
   resultado vazio em vez de falha. Capturar exceção específica, ou deixar subir
   até o tratador global.
2. `double` ou `float` para peso ou cintura (ADR-0017).
3. `LocalDateTime` ou `Instant` onde a regra fala de data (ADR-0018).
4. Importar `org.springframework` ou `jakarta.persistence` dentro de `regras/`
   (ADR-0019). O ArchUnit verifica.
5. Cálculo de agregação dentro do controller.
6. Injeção por campo, com `@Autowired` no atributo. A dependência entra pelo
   construtor, que mostra o que a classe usa e permite testar sem o Spring.
7. `System.out.println` ou `printStackTrace` para diagnóstico. O log existe e é
   só nível erro (RNF-16).
8. Valor sentinela para representar ausência.
9. Editar migração já aplicada em produção. O banco de produção já executou
   aquele arquivo; alterá-lo faz o histórico do Flyway mentir sobre o estado
   real, e o Flyway se recusa a rodar. Correção entra como migração nova.
10. Consulta dentro de laço. No volume da RNF-25 não dói, e o hábito dói depois.
    No JPA ela aparece sem laço visível, como N+1: uma consulta para a lista e
    mais uma por item ao ler uma relação preguiçosa (ADR-0036).
11. Peso ou cintura em linha de log (RNF-13), inclusive pelo caminho
    indireto: mensagem de exceção que carrega o corpo da requisição, erro do
    Bean Validation com o valor recebido, erro de restrição do Postgres que
    repete a linha recusada, ou objeto inteiro registrado.
12. Devolver entidade no controller. A rota devolve DTO (ADR-0036).

## Migração de esquema

- Toda migração é um arquivo SQL novo, escrito à mão, em
  `src/main/resources/db/migration/`, com nome
  `V<ano>_<mes>_<dia>_<hora>__<acao>_<tabela>.sql`, por exemplo
  `V2026_10_05_1402__cria_tabelas.sql`. Zero à esquerda, para a ordem dos nomes
  ser a ordem em que rodam.
- O arquivo não tem `BEGIN` nem `COMMIT`: o Flyway abre a transação, e a
  anotação no histórico entra dentro dela.
- Não há reversão: o plano de recuperação é o backup (ADR-0040). Desfazer uma
  migração em produção é restaurar o backup e voltar a api para a etiqueta
  anterior no mesmo passo: com a imagem nova, o initContainer aplicaria a
  migração de novo no banco restaurado. Em desenvolvimento, desfazer é recriar
  o banco.
- A primeira migração habilita a extensão `btree_gist`, exigida pela restrição
  de exclusão que impede sobreposição de fase (ADR-0033).
- O ensaio do deploy restaura o backup e aplica as migrações antes da produção
  (RNF-08, ADR-0040). Migração que falha no ensaio não chega ao banco de
  produção.
