# Endpoints

Contrato da API. Escrito por último de propósito: ele depende dos casos de uso
e das decisões de stack, e não o contrário.

Este documento é a **intenção**. A verdade executável é a especificação OpenAPI
que o FastAPI publica a partir do código (ADR-0001), e é dela que saem os tipos
do frontend (ADR-0027). Quando os dois divergirem, o OpenAPI está certo e este
documento está datado.

## Convenções gerais

- Prefixo `/api/v1`. Toda rota abaixo é relativa a ele.
- Autenticação por cookie de sessão (ADR-0009) em todas as rotas, exceto as de
  `auth/` e `saude`.
- Toda resposta de erro usa o formato único de `convencoes-backend.md`, com
  `codigo`, `campo`, `mensagem` e `regra`.
- Datas em `AAAA-MM-DD`, números com ponto decimal (RN-62, RN-63).
- O registro é endereçado pela data, não por identificador. A fase é endereçada
  por identificador sequencial. O motivo de cada um está abaixo.

## Lista

| Método | Caminho | Faz | Requisito |
|:--|:--|:--|:--|
| `GET` | `/auth/login` | redireciona para o Google | RF-01 |
| `GET` | `/auth/callback` | recebe o código, cria sessão, redireciona para a aplicação | RF-01, RF-03 |
| `POST` | `/auth/logout` | apaga a sessão e expira o cookie | RF-04 |
| `GET` | `/conta` | nome, e-mail e foto vindos do Google | UC-12 |
| `POST` | `/conta/exclusao` | apaga a conta e todos os dados | RF-06 |
| `GET` | `/conta/exportar` | devolve o histórico completo em CSV | RF-43 |
| `GET` | `/registros` | lista por intervalo de datas | RF-14 |
| `POST` | `/registros` | cria registro | RF-07, RF-08, RF-09 |
| `PATCH` | `/registros/{data}` | edita peso, cintura ou a própria data | RF-11 |
| `DELETE` | `/registros/{data}` | apaga registro | RF-12 |
| `GET` | `/fases` | lista todas, com a corrente marcada | RF-23 |
| `POST` | `/fases` | cria fase e encerra a corrente | RF-15, RF-19 |
| `PATCH` | `/fases/{id}` | edita tipo, nível, taxa alvo, peso alvo, início | RF-20 |
| `DELETE` | `/fases/{id}` | apaga fase sem registro | RF-22 |
| `GET` | `/acompanhamento` | tudo da tela principal, numa resposta | UC-08 |
| `POST` | `/importacao/previsualizacao` | valida o arquivo e relata, sem gravar | RF-37, RF-38 |
| `POST` | `/importacao` | grava o arquivo inteiro ou nada | RF-40 |
| `GET` | `/saude` | responde se o serviço está no ar | RNF-09 |

## As decisões que não se leem na lista

### O registro é identificado pela data, a fase por identificador sequencial

O registro tem chave natural: um por usuário por data (RN-01). Como cada usuário
só alcança os próprios dados (RN-50), a data identifica sem ambiguidade, e usá-la
dispensa expor identificador interno.

A fase não tem chave natural. A data de início pareceria servir, e não serve: a
primeira fase da conta **não tem** data de início, porque cobre todo o passado
(RN-64). Por isso ela é endereçada pelo próprio identificador, sequencial.

Sequencial e não opaco, apesar do argumento de contagem que o ADR-0012 usa para
o registro. A diferença é de peso: o sistema é fechado por convite (RN-48),
quantas fases um usuário já teve não é informação sensível, e `uuid` cobraria
índice maior e URL ilegível sem comprar nada aqui.

### Editar a data de um registro é mover o recurso

`PATCH /registros/{data}` aceita `peso`, `cintura` e também `data`. Alterar a
`data` move o registro para outro dia, e se aquele dia já tiver registro a
resposta é a mesma do conflito descrito abaixo. Não há caso especial: mover para
data ocupada e criar em data ocupada são o mesmo problema.

Mudando a data, o registro passa a pertencer à fase daquela data, porque a fase
é derivada e não gravada (RN-23).

### A confirmação de substituição acontece por conflito

`POST /registros` sem o campo `substituir`, numa data que já tem registro,
responde **409** com:

```json
{
  "erro": {
    "codigo": "REGISTRO_JA_EXISTE",
    "campo": "data",
    "mensagem": "Ja existe registro em 07/09/2026, com 82,4 kg.",
    "regra": "RN-01"
  }
}
```

O frontend abre o diálogo com esse valor, e o segundo envio repete o POST com
`"substituir": true`.

A confirmação vai no **corpo** e nunca na URL, porque parâmetro de URL entra no
log de acesso do servidor web (RNF-17), e ali não deve haver dado que descreva
ação do usuário.

Recusadas duas alternativas: consultar a data antes de abrir o formulário, que
gasta uma ida a mais em todo registro e tem condição de corrida entre a consulta
e a gravação; e substituir direto avisando depois, que contraria a RN-01.

### A exclusão de conta é um POST, não um DELETE

A confirmação exige que o usuário digite o próprio e-mail (UC-12), e esse valor
precisa ir no corpo. `DELETE` com corpo tem semântica indefinida na
especificação HTTP e servidor intermediário pode descartá-lo, e a URL está
descartada pelo mesmo motivo do item anterior.

### A lista de registros exige intervalo, e tem padrão

`GET /registros?de=AAAA-MM-DD&ate=AAAA-MM-DD`. Sem parâmetro, devolve os
últimos 90 dias.

Sem paginação por número de página: ninguém pensa "página 3 dos meus pesos",
pensa "março". Paginação é o desenho errado para série temporal.

### O acompanhamento é uma resposta só

`GET /acompanhamento` devolve, de uma vez: peso semanal e a contagem de
registros da janela, as três variações com o intervalo de datas de cada uma
(RN-47), a fase corrente inteira, o veredito de ritmo com a taxa alvo já
convertida em quilogramas da semana (RN-35), o progresso até o peso alvo, e o
último valor de cintura com as duas variações dela (RN-12).

O motivo é consistência, não desempenho. Tudo ali deriva do mesmo peso semanal
(RN-17). Em seis endpoints separados, seis respostas chegam em momentos
diferentes, e a tela pode exibir um veredito calculado com um peso semanal
diferente do que aparece ao lado. Ninguém percebe, e está errado.

Uma resposta significa uma chave de cache e uma invalidação depois de gravar
(ADR-0023).

Onde a janela não tem os 4 registros mínimos, o peso semanal vem nulo e o
veredito vem com estado de dados insuficientes, em vez de o campo desaparecer
(RN-16, RN-39). Campo ausente e campo nulo são coisas diferentes para quem
consome.

### A importação são duas chamadas sem estado entre elas

A primeira recebe o arquivo e devolve o relatório: linhas válidas, linhas
recusadas com número de linha e motivo, e datas existentes que serão
substituídas (RN-58). Nada é gravado.

A segunda **reenvia o mesmo arquivo** junto com a confirmação. O servidor não
guarda nada entre as duas chamadas.

Isso evita armazenamento temporário com expiração e limpeza, e garante que o
que se grava é exatamente o que foi confirmado. O navegador já tem o arquivo em
memória, então o reenvio não custa nada ao usuário.

### O convite não tem endpoint

Convidar é ação do AT-02, que não tem interface no sistema (`atores.md`). É
comando de linha executado no servidor (RF-44, ADR-0011).

## O que não tem endpoint, e por quê

| O quê | Por quê |
|:--|:--|
| Convite | ação do operador, por comando de linha |
| Migração de esquema | comando no deploy (ADR-0013) |
| Peso semanal isolado | vem em `/acompanhamento`, e separado permitiria tela inconsistente |
| Fase corrente isolada | idem |
| Qualquer rota que emita sessão sem autenticar | recusado no ADR-0022 |

## Códigos de status

| Código | Quando |
|:--|:--|
| 200 | leitura bem-sucedida |
| 201 | registro ou fase criados |
| 204 | exclusão bem-sucedida |
| 400 | valor recusado por regra (faixa, data futura, alvo incoerente) |
| 401 | sem sessão válida |
| 403 | e-mail não consta na lista de convidados |
| 409 | conflito de data em registro, ou fase em estado que impede a operação |
| 422 | corpo malformado, antes de chegar a regra de negócio |
