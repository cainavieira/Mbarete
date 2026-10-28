# Endpoints

Contrato da API. Escrito por último de propósito: ele depende dos casos de uso
e das decisões de stack, e não o contrário.

Este documento é a **intenção**. A verdade executável é a especificação OpenAPI
que o springdoc publica a partir do código (ADR-0035), e é dela que saem os tipos
do frontend (ADR-0027). Quando os dois divergirem, o OpenAPI está certo e este
documento está datado.

## Convenções gerais

- Prefixo `/api/v1`. Toda rota abaixo é relativa a ele.
- Autenticação pelo token de acesso em cookie (ADR-0041) em todas as rotas,
  exceto as de `auth/` e `saude`.
- Toda resposta de erro usa o formato único de `convencoes-backend.md`, com
  `codigo`, `campo`, `mensagem` e `regra`. A exceção é o retorno do login, que
  volta como redirecionamento, explicado abaixo.
- Datas em `AAAA-MM-DD`, números com ponto decimal (RN-62, RN-63).
- O dia corrente é o do dispositivo (RN-04) e chega no cabeçalho `Data-Local`,
  em `AAAA-MM-DD`, em toda requisição. O motivo está abaixo.
- O registro é endereçado pela data, não por identificador. A fase é endereçada
  por identificador sequencial. O motivo de cada um está abaixo.

## Lista

| Método | Caminho | Faz | Requisito |
|:--|:--|:--|:--|
| `GET` | `/auth/login` | redireciona para o Google | RF-01 |
| `GET` | `/auth/callback` | recebe o código, emite os tokens e redireciona para a aplicação; em qualquer falha, inclusive a recusa por convite, redireciona para a tela de entrada com o motivo | RF-01, RF-02, RF-03 |
| `POST` | `/auth/renovacao` | troca o token de renovação por um token de acesso novo, e empurra a validade da renovação | RNF-22 |
| `POST` | `/auth/logout` | apaga o token de renovação e expira os dois cookies | RF-04 |
| `GET` | `/conta` | nome, e-mail e foto vindos do Google | UC-12 |
| `POST` | `/conta/exclusao` | apaga a conta e todos os dados | RF-06 |
| `GET` | `/conta/exportar` | devolve o histórico completo em CSV | RF-43 |
| `GET` | `/registros` | lista por intervalo de datas | RF-14 |
| `POST` | `/registros` | cria registro | RF-07, RF-08, RF-09 |
| `PATCH` | `/registros/{data}` | edita peso, cintura ou a própria data | RF-11 |
| `DELETE` | `/registros/{data}` | apaga registro | RF-12 |
| `GET` | `/fases` | lista todas, marcando a corrente quando existir, com quantos registros cada uma tem | RF-23, RF-22 |
| `POST` | `/fases` | cria fase, com data de fim opcional; sem ela, encerra a fase em aberto que começou antes da nova | RF-15, RF-19 |
| `PATCH` | `/fases/{id}` | edita tipo, nível, taxa alvo, peso alvo, início e fim; mudar o início move o fim no futuro da fase anterior | RF-20, RF-19 |
| `POST` | `/fases/{id}/encerramento` | encerra a fase corrente | RF-45 |
| `DELETE` | `/fases/{id}` | apaga a fase; os registros permanecem sem fase; o fim no futuro da fase anterior vai para a véspera da próxima, ou some | RF-22, RF-19 |
| `GET` | `/niveis` | a taxa alvo padrão de cada nível, para cutting e bulking | RF-16 |
| `GET` | `/acompanhamento` | tudo da tela principal, numa resposta | UC-08 |
| `POST` | `/importacao/previsualizacao` | valida o arquivo e relata, sem gravar | RF-37, RF-38 |
| `POST` | `/importacao` | grava o arquivo inteiro ou nada | RF-40 |
| `GET` | `/saude` | responde se a api está no ar; o script de deploy confere por ela | ADR-0037 |

## As decisões que não se leem na lista

### O registro é identificado pela data, a fase por identificador sequencial

O registro tem chave natural: um por usuário por data (RN-01). Como cada usuário
só alcança os próprios dados (RN-50), a data identifica sem ambiguidade, e usá-la
dispensa expor identificador interno.

A fase não tem chave natural utilizável. A data de início pareceria servir, e
não serve: ela é editável (RN-24), e identificador que muda quando o usuário
corrige uma data quebra toda referência já guardada. Por isso a fase é
endereçada pelo próprio identificador, sequencial.

Sequencial e não opaco, apesar do argumento de contagem que o ADR-0036 usa para
o registro. A diferença é de peso: o sistema é fechado por convite (RN-48),
quantas fases um usuário já teve não é informação sensível, e `uuid` cobraria
índice maior e URL ilegível sem comprar nada aqui.

### Dado de outro usuário responde 404

Toda consulta carrega o usuário autenticado (ADR-0034), então pedir a fase de
outra pessoa e pedir uma fase que não existe dão o mesmo resultado: nada. A
resposta é a mesma, 404, com `NAO_ENCONTRADO`.

Responder 403 exigiria ler a linha alheia para saber que ela existe, que é
justamente a consulta sem filtro que o isolamento proíbe. Esconder a
existência não é o motivo: o identificador sequencial já deixa estimar quantas
fases existem, e isso foi aceito acima.

### O dia corrente chega em cabeçalho

A RN-04 manda usar o dia do dispositivo, não o do servidor. O frontend põe a
data local no cabeçalho `Data-Local`, em `AAAA-MM-DD`, em toda requisição, a
partir de um lugar só (`convencoes-frontend.md`).

Cabeçalho, e não parâmetro de URL nem campo de corpo: URL entra no log de
acesso (RNF-17), leitura não tem corpo, e um mecanismo único no cliente faz
com que nenhuma rota nova nasça sem a data.

A rota que depende de hoje (data futura no registro e na importação, data de
fim de fase, fase corrente, janela do acompanhamento e da lista de registros)
recusa com 400 e `DATA_LOCAL_INVALIDA` quando o cabeçalho falta ou vem
malformado. Cair para a data do servidor seria o mesmo defeito com outra
cara: passa em todo teste, porque servidor e máquina de teste estão no mesmo
fuso, e aparece em viagem ou na virada do dia.

A data também é recusada quando está a mais de um dia da data do servidor em
UTC. Os fusos vão de 12 horas antes a 14 horas depois do UTC, então relógio
certo nunca passa de um dia de distância; além disso é relógio errado, e
aceitar deixaria registrar peso no futuro pela porta do dispositivo (RN-05).

### Editar a data de um registro é mover o recurso

`PATCH /registros/{data}` aceita `peso`, `cintura` e também `data`. Alterar a
`data` move o registro para outro dia, e se aquele dia já tiver registro a
resposta é a mesma do conflito descrito abaixo. Não há caso especial: mover para
data ocupada e criar em data ocupada são o mesmo problema.

O `PATCH` aceita o campo `substituir` com a mesma semântica do `POST`: sem ele,
data ocupada responde 409; com `"substituir": true`, a substituição acontece.
Sem isso o conflito não teria saída neste caminho, e o frontend ficaria
repetindo a mesma requisição a cada confirmação.

A consequência surpreende, e por isso está escrita: no `POST` a substituição
apaga um registro, e no `PATCH` ela apaga um e move o outro, então a contagem
total de registros do usuário cai em um (UC-03).

Mudando a data, o registro passa a pertencer à fase daquela data, ou a nenhuma,
porque a fase é derivada e não gravada (RN-20, RN-23).

### A confirmação de substituição acontece por conflito

`POST /registros` sem o campo `substituir`, numa data que já tem registro,
responde **409** com:

```json
{
  "erro": {
    "codigo": "REGISTRO_JA_EXISTE",
    "campo": "data",
    "mensagem": "Já existe registro em 07/09/2026, com 82,4 kg.",
    "regra": "RN-01"
  }
}
```

Quando o registro gravado tem cintura, a mensagem a traz também: "Já existe
registro em 07/09/2026, com 82,4 kg e 91,0 cm de cintura.", porque a
substituição troca o registro inteiro e a cintura de antes não fica. O
frontend abre o diálogo com essa mensagem, e o segundo envio repete o POST com
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
(RN-47), a fase corrente inteira, o veredito de ritmo com a taxa real e o
intervalo dela (RN-36) e a taxa alvo já convertida em quilogramas da semana
(RN-35), o progresso até o peso alvo, o
último valor de cintura com as duas variações dela (RN-12), e a série do
gráfico.

O motivo é consistência, não desempenho. Tudo ali deriva do mesmo peso semanal
(RN-17). Em seis endpoints separados, seis respostas chegam em momentos
diferentes, e a tela pode exibir um veredito calculado com um peso semanal
diferente do que aparece ao lado. Ninguém percebe, e está errado.

Pelo mesmo motivo, os números do veredito vêm já arredondados como a tela os
mostra, os mesmos que o veredito comparou (RN-37): o peso semanal e os limites
da faixa com uma casa, a taxa real e a taxa alvo em quilogramas com duas. Se a
tela arredondasse por conta própria, poderia escrever o peso igual à borda ao
lado de um veredito de fora.

Uma resposta significa uma chave de cache e uma invalidação depois de gravar
(ADR-0023).

A série do gráfico é uma entrada por dia, dos últimos 120 dias até hoje, com
a data, o peso (nulo onde não houve registro), o peso semanal (nulo onde a
janela não teve os 4 registros) e o identificador da fase que cobre o dia
(nulo onde não há fase). Se o primeiro registro da conta é mais recente que
isso, a série começa nele (RF-49). Os botões de período recortam a série na
tela, sem nova chamada.

Uma entrada por dia, inclusive o dia sem dado, porque a linha só se
interrompe onde existe ponto nulo: dia omitido faria o gráfico ligar os
vizinhos e desenhar uma trajetória que não aconteceu (RN-09, ADR-0029). E a
fase vem em cada ponto, em vez de vir do `GET /fases`, para a tela continuar
montada por uma chamada só.

Onde a janela não tem os 4 registros mínimos, o peso semanal vem nulo e o
veredito vem com estado de dados insuficientes, em vez de o campo desaparecer
(RN-16, RN-39). Sem peso semanal hoje, as três variações também vêm nulas, e
cada uma vem nula quando nenhum dia antes de hoje tem peso semanal desde o
ponto de partida dela (RN-47). Campo ausente e campo nulo são coisas
diferentes para quem consome.

Em cutting e bulking, enquanto não se completam os 14 dias da RN-39, o
veredito vem também com a data em que eles se completam, e a tela diz a partir
de quando o veredito aparece sem conhecer os 14 dias (ADR-0025). Quando o que
falta é um dos dois pesos semanais comparados, a data vem nula, porque não há
como prever quando ele vai existir.

O progresso segue o mesmo princípio, com dois estados além do número, que a
tela trata de forma diferente: dados insuficientes, quando a base da RN-68 não
existe (UC-08 5c), e não exibido, quando o peso alvo está do lado errado da
base (UC-08 5d). A faixa de tolerância da manutenção depende da mesma base:
sem ela, o veredito da manutenção também vem com dados insuficientes. Com
ela, a faixa vem com os dois limites já calculados em quilogramas, e a tela
desenha e escreve esses números sem conhecer o 1% da RN-33 (ADR-0025).

**A fase corrente pode não existir** (RN-27), e nesse caso ela vem nula, e vêm
nulos com ela o veredito, a taxa alvo, o progresso e a faixa de tolerância.
Peso semanal, variações e cintura continuam preenchidos, porque nenhum dos três
depende de fase (RN-66). Isso não é estado de erro: é o que acontece em conta
nova, depois de encerrar uma fase, e em todo o passado importado.

Fase futura cadastrada não aparece aqui, apenas em `/fases` (RN-69).

### A lista de fases traz a contagem de registros

O UC-07 informa, antes de apagar, quantos registros perdem o rótulo. A
contagem vem em cada fase do `GET /fases`, e não da tela contando o
`GET /registros` sobre o intervalo: saber que registro é de qual fase é a
RN-23, e a regra fica numa casa só (ADR-0025).

### A taxa alvo trafega sem sinal nas rotas de fase

Em `POST /fases`, `PATCH /fases/{id}`, `GET /fases` e `GET /niveis`, a taxa
alvo vai e volta como a pessoa a digita e edita, sem sinal, e o backend aplica
o sinal do tipo (RN-29). O sinal tem uma casa só, e o menos esquecido não tem
por onde chegar. No acompanhamento ela vem com sinal, porque ali fica ao lado
da taxa real, que tem sinal (RN-37).

### A importação são duas chamadas sem estado entre elas

A primeira recebe o arquivo e devolve o relatório: linhas válidas, linhas
recusadas com número de linha e motivo, e datas existentes que serão
substituídas, cada uma com o registro gravado nela (RN-58). Nada é gravado.

A segunda **reenvia o mesmo arquivo** junto com a confirmação. O servidor não
guarda nada entre as duas chamadas.

Isso evita armazenamento temporário com expiração e limpeza, e garante que o
que se grava é exatamente o que foi confirmado. O navegador já tem o arquivo em
memória, então o reenvio não custa nada ao usuário.

### O convite não tem endpoint

Convidar é ação do AT-02, que não tem interface no sistema (`atores.md`). É
comando de linha executado no servidor (RF-44, ADR-0011).

### O retorno do login volta como redirecionamento

O `GET /auth/callback` não é chamado pela aplicação: o navegador chega nele
vindo do Google, numa navegação de página inteira, e um erro em JSON ali
apareceria cru na tela. Por isso o retorno sempre termina em
redirecionamento: no caminho feliz, para a aplicação; em qualquer falha, para
a tela de entrada com o motivo na URL, e a tela explica.

| Motivo | Quando | Caso de uso |
|:--|:--|:--|
| `NAO_CONVIDADO` | o e-mail não está na lista de convidados, ou o Google não o deu como verificado | UC-01 1a |
| `LOGIN_NAO_CONCLUIDO` | a pessoa cancelou no Google, ou a verificação do retorno falhou (`state`, troca do código, `id_token`) | UC-01 1d |

O `NAO_CONVIDADO` não leva o e-mail e só diz que o acesso é por convite, sem
revelar se aquele e-mail existe no sistema (RNF-23).

### A renovação tem rota própria

O token de renovação só viaja para as rotas de `auth/`: o cookie dele vale
para `/api/v1/auth`, e nunca chega ao resto da api. A renovação o troca por um
token de acesso novo, e o logout o usa para achar a linha no banco e apagá-la.
Quando uma chamada volta 401, o cliente HTTP do frontend chama a renovação uma
vez e repete a chamada. Se a renovação também voltar 401, a pessoa vai para a
tela de entrada.

## O que não tem endpoint, e por quê

| O quê | Por quê |
|:--|:--|
| Convite | ação do operador, por comando de linha |
| Migração de esquema | initContainer no deploy (ADR-0040) |
| Peso semanal isolado | vem em `/acompanhamento`, e separado permitiria tela inconsistente |
| Fase corrente isolada | idem |
| Qualquer rota que emita token sem autenticar | recusado no ADR-0022 |

## Códigos de status

| Código | Quando |
|:--|:--|
| 200 | leitura bem-sucedida |
| 201 | registro ou fase criados |
| 204 | exclusão bem-sucedida |
| 400 | valor recusado por regra (faixa, data futura no registro, data de fim futura ou anterior ao início, data local ausente ou inválida, arquivo de importação sem as colunas obrigatórias ou com separador desconhecido, e-mail digitado na exclusão de conta que não é o da conta) |
| 401 | sem token de acesso válido; na renovação, sem token de renovação válido |
| 404 | registro ou fase que não existe, ou que é de outro usuário, sem distinguir os dois; também rota que não existe e método que a rota não aceita |
| 409 | conflito de data em registro, sobreposição de fase que a RN-19 recusa, ou encerramento de fase que não é a corrente |
| 422 | corpo malformado, antes de chegar a regra de negócio |
| 500 | erro inesperado, sem a mensagem da exceção (RNF-13) |
