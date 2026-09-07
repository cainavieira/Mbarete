# Instrução de construção

Documento de partida para quem for **implementar** este projeto. A
especificação já existe inteira em `especificacao/` e não precisa ser
redescoberta: 65 regras de negócio, 44 requisitos funcionais, 13 casos de uso,
29 requisitos não funcionais e 31 ADR, todos com identificador estável.

Este documento não repete nada disso. Ele diz **o que fazer, em que ordem, e
como saber que cada etapa acabou**.

---

## 1. Antes de escrever qualquer linha

Leia nesta ordem. É a ordem de dependência, não de importância.

1. `especificacao/regras-de-negocio.md`, inteiro. É o documento mais denso e
   tudo aponta para ele. Preste atenção especial à seção **Decisões recusadas**
   no fim: ela existe para você não propor de novo o que já foi descartado.
2. `especificacao/atores.md`. São três, e um deles não tem interface.
3. `especificacao/requisitos-funcionais.md`. Lista curta, leitura rápida.
4. `especificacao/casos-de-uso.md`. Os **fluxos alternativos** são o valor: eles
   dizem o que acontece quando dá errado, e é ali que a maior parte do trabalho
   real mora.
5. `especificacao/requisitos-nao-funcionais.md`. Presta atenção na assimetria: não
   há compromisso de disponibilidade, e o backup é o requisito mais duro do
   documento.
6. `especificacao/arquitetura.md`. Duas vistas C4 mais a regra de dependência.
7. `especificacao/convencoes-backend.md` e `convencoes-frontend.md`. Idioma dos
   identificadores, formato de erro, antipadrões proibidos.
8. `especificacao/endpoints.md`. O contrato.
9. `especificacao/fluxo-de-trabalho.md`. Ramo, commit, definição de pronto.
10. Os ADR, conforme a etapa. Não precisa ler os 31 de uma vez; cada etapa abaixo
    diz quais importam.

**A regra que resume tudo:** quando você for escrever uma linha e não souber qual
comportamento é o certo, a resposta está num documento com identificador. Se não
estiver, é decisão nova, e decisão nova é do interessado, não sua.

---

## 2. Como trabalhar com o interessado

Não é preferência de estilo, é o que já foi combinado e verificado nesta
conversa.

**Mostre o texto antes de gravar.** Conteúdo de arquivo novo, mensagem de
commit, qualquer coisa que vai para o disco: mostra, espera o sim, grava. Vale
mesmo quando a ação parece óbvia.

**Commit pode ser executado depois de ele aprovar a mensagem. Push nunca.** Push
é sempre dele, mesmo quando parecer o passo seguinte natural.

**Não busque na web sem ele pedir.** Se falta um fato, diga que não sabe e
pergunte se ele quer que você procure. Buscar por conta própria já reabriu
decisão fechada nesta conversa três vezes.

**Ele está aprendendo prática de indústria.** Explique o porquê, não só o quê.
Quando ele pergunta se uma prática dele é padrão, vá ver a fonte (código do SDK
instalado, precedente no próprio projeto) antes de responder. "Faz sentido" sem
verificação é opinião, não revisão.

**Rode antes de opinar sobre código.** Bug raramente aparece só de ler. Esta
conversa achou um sombreamento de variável no script original rodando o trecho,
que a leitura tinha deixado passar.

**Sem travessão.** Nem em conversa, nem em documento, nem em commit. Use vírgula,
dois pontos, parênteses ou duas frases.

**Decisão dele não se reabre.** Depois de apresentar prós e contras uma vez, se
ele confirmar, o assunto passa a ser "como fazer isso bem", não "por que não
fazer assim".

---

## 3. O que já está decidido e não se rediscute

Se você sentir vontade de propor qualquer coisa desta lista, o ADR
correspondente já explica por que não.

| Vontade | Já recusado em |
|:--|:--|
| SQLite, para simplificar backup ou teste | ADR-0004, ADR-0020 |
| SQLModel, para não duplicar modelo | ADR-0012 |
| JWT, para não consultar o banco a cada requisição | ADR-0009 |
| `fastapi-users`, para não escrever autenticação | ADR-0010 |
| `uv` ou `poetry`, porque `pip` é antigo | ADR-0014 |
| Camadas de Clean Architecture | ADR-0019 |
| Modo framework do React Router | ADR-0024 |
| Validar faixas também no frontend | ADR-0025 |
| Escrever tipos da API à mão no frontend | ADR-0027 |
| Deploy automático pela integração contínua | ADR-0031 |
| Endpoint de teste que emite sessão | ADR-0022 |
| Banco gerenciado na nuvem | ADR-0005 |
| Métricas com Prometheus e Grafana | recusado com os RNF-16 a RNF-18 |
| Streak de dias registrando | recusado nas Decisões recusadas |
| IMC, altura, projeção de data | idem |

---

## 4. Armadilhas conhecidas

Cada uma destas já mordeu alguém, e a maioria não gera erro na hora.

**`Decimal` construído de `float`.** `Decimal("82.4")` vale 82,4.
`Decimal(82.4)` vale 82,400000000000005684341886080801486968994140625. Sempre de
texto ou de inteiro.

**`CHECK` do Postgres não aceita `current_date`.** A restrição precisa ser
imutável. Isso significa que a RN-05 (data futura recusada) **não pode** virar
restrição de banco: ela vive só na aplicação. Não tente.

**A tabela `fase` não tem coluna `data_fim`.** Alguém vai querer adicionar. O
fim de uma fase é sempre a véspera do início da seguinte (RN-18 emendada), e a
fronteira é um valor único justamente para que estado inválido não exista
(RN-24). Adicionar `data_fim` recria a classe de erro que a decisão eliminou.

**Autogenerate do Alembic e renomeação.** Ele vê coluna que desapareceu e outra
que apareceu, e gera remoção mais criação, o que apaga dado. Renomeação se
corrige à mão para `ALTER TABLE ... RENAME COLUMN`. Todo rascunho é revisado.

**`SameSite=Strict` quebra o login.** O retorno do Google é navegação vinda de
outro site, e `Strict` faz o cookie não ser enviado nesse retorno. É `Lax`
(ADR-0009).

**Sem `try_files` no nginx, recarregar a página dá 404.** A rota `/fases` existe
só no navegador (ADR-0007, ADR-0024).

**No React Router 8 o pacote é `react-router`, não `react-router-dom`.** O
`react-router-dom` parou na linha 7 e não está marcado como obsoleto no npm,
então instalá-lo não gera aviso nenhum. Você só descobre quando o import falha.

**Terminal Windows em `cp1252`.** Emoji em `print` levanta
`UnicodeEncodeError`. O ambiente de desenvolvimento aqui é Windows.

**Caminho relativo em script.** Script que abre `comprovante.csv` funciona
rodando de uma pasta e falha de outra. Use caminho derivado do arquivo, e
reproduza o comando exato que ele vai rodar, não o mais fácil de testar.

**Chave lida no código e ausente no `.env`.** Mantenha `.env.example`
sincronizado, e faça a leitura de variável obrigatória falhar alto na subida, em
vez de devolver `None` que quebra três camadas adiante.

**`except Exception` largo.** Foi o defeito do script de origem: engolia arquivo
inexistente e reportava resultado vazio em vez de falha. Está proibido nas
convenções.

---

## 5. Ordem de construção

Onze etapas. Cada uma tem objetivo, o que criar, como verificar, e o tipo de
commit. Não pule a ordem: as etapas 1 e 2 são a fundação e as duas mais
testáveis do projeto.

### Etapa 0: ambiente

**Objetivo:** conseguir rodar linter, verificador de tipo e teste, com zero teste
existente.

Criar: `backend/.venv` por `python -m venv`, `requirements.txt` e
`requirements-dev.txt` (ADR-0014), `pyproject.toml` com `ruff` e `mypy`
configurados (ADR-0015, ADR-0016), `compose.dev.yml` com **apenas** o Postgres,
`.env.example`, e a estrutura de pasta que o `arquitetura.md` fixa.

Verificar: `ruff check .` roda sem erro de configuração, `mypy app` roda,
`pytest` roda dizendo zero teste coletado, e `docker compose -f compose.dev.yml
up -d` sobe o Postgres.

Commit: `chore`.

### Etapa 1: esquema e primeira migração

**Objetivo:** o banco existe, com as restrições que as regras permitem expressar.

Cinco tabelas. Nomes no singular, colunas em português, sem acento em
identificador (`convencoes-backend.md`).

**`usuario`**: `id` serial, `email` único, `nome`, `foto_url` nulo, `criado_em`.

**`convidado`**: `email` como chave primária, `convidado_em`. É a lista da RN-48.

**`sessao`**: `id` do tipo `uuid` com valor gerado pelo banco (este **é** opaco
de propósito: é o valor que vai no cookie e precisa ser impossível de adivinhar),
`usuario_id`, `criado_em`, `expira_em`. A renovação da RNF-22 atualiza
`expira_em`.

**`fase`**: `id` serial, `usuario_id`, `tipo`, `data_inicio` **nulo permitido**,
`nivel` nulo, `taxa_alvo_valor` nulo, `taxa_alvo_unidade` nulo, `peso_alvo`
nulo, `criado_em`. Sem `data_fim`.

**`registro`**: `id` serial (interno, nunca exposto), `usuario_id`, `data`,
`peso` como `numeric(4,1)`, `cintura` como `numeric(4,1)` nulo, `criado_em`.

Restrições que **vão** para o banco:

| Restrição | Regra |
|:--|:--|
| `registro` único por (`usuario_id`, `data`) | RN-01 |
| `registro.peso` entre 20,0 e 400,0 | RN-03 |
| `registro.cintura` nulo ou entre 40,0 e 200,0 | RN-11 |
| `fase.tipo` em cutting, bulking, manutencao | RN-18 |
| `fase.nivel` em conservador, moderado, agressivo, personalizado | RN-28 |
| `fase.taxa_alvo_unidade` em percentual_semana, kg_semana | RN-29 |
| `fase` único por (`usuario_id`, `data_inicio`) | RN-19 |
| índice único parcial em `fase(usuario_id)` onde `data_inicio` é nulo | RN-64 |
| `fase`: manutenção tem alvo, nível e taxa nulos; cutting e bulking têm os três preenchidos | RN-33, RN-41 |
| `fase.peso_alvo` nulo ou entre 20,0 e 400,0 | RN-03 |

O índice único parcial é o que garante **no máximo uma** fase de início aberto
por usuário, que é a RN-64 expressa em SQL em vez de em código.

Restrições que **não** vão para o banco, e por quê:

- Data futura recusada (RN-05): `CHECK` precisa ser imutável.
- Coerência do peso alvo com o peso corrente (RN-65): depende de outra tabela.
- Fase derivada pela data (RN-23): é consulta, não restrição.

Verificar: `alembic upgrade head` cria tudo, `alembic downgrade base` desfaz
tudo, e uma tentativa manual de inserir dois registros na mesma data para o mesmo
usuário é recusada pelo banco.

Commit: `feat`.

### Etapa 2: camada de regras, com os testes

**Objetivo:** todo cálculo do sistema existe, testado, sem banco e sem servidor.

Esta é a etapa mais importante e a que deve consumir mais tempo. Leia o ADR-0019
e o ADR-0020 antes.

Módulo `app/regras/`, que **não importa `fastapi` nem `sqlalchemy`**. As funções
recebem listas e devolvem valores.

Funções, com a regra que cada uma implementa:

| Função | Regras |
|:--|:--|
| `calcular_peso_semanal` | RN-15, RN-16 |
| `calcular_variacao` | RN-46, RN-47 |
| `calcular_taxa_real` | RN-36 |
| `converter_taxa_alvo` | RN-31, RN-32, RN-33, RN-34, RN-35 |
| `avaliar_ritmo` | RN-37, RN-38, RN-39 |
| `calcular_progresso` | RN-42, RN-43, RN-44 |
| `fase_da_data` | RN-22, RN-23, RN-64 |
| `resumo_cintura` | RN-12 |

Os casos de teste que **precisam** existir, porque são as fronteiras onde a
regra decide:

- peso semanal com 3 registros na janela devolve vazio; com 4 devolve média
  (RN-16)
- peso semanal ignora registro fora dos 7 dias de calendário, e a janela é de
  calendário e não dos últimos 7 registros (RN-15)
- variação devolve o intervalo de datas **realmente usado**, não o pedido, quando
  há buraco (RN-47)
- taxa real devolve vazio quando falta o peso semanal de 7 dias atrás (RN-36)
- taxa alvo percentual incide sobre o peso semanal **corrente**, não sobre o do
  início da fase (RN-34)
- veredito com 13 dias de fase, ou com 14 dias e 7 registros, devolve dados
  insuficientes; com 14 dias e 8 registros devolve veredito (RN-39)
- veredito no limite exato da tolerância de 20%: alvo de -0,50 com real de -0,40
  está no ritmo, com -0,39 não está (RN-38)
- progresso trava em 100% ao atingir e em 0% na direção contrária (RN-44)
- progresso em bulking tem a direção invertida em relação a cutting (RN-42)
- `fase_da_data` para data anterior a toda `data_inicio` devolve a fase de início
  nulo (RN-64)
- `fase_da_data` na fronteira: o dia igual à `data_inicio` pertence à fase nova, e
  a anterior termina na véspera (RN-22)

Mais o **teste de arquitetura**: lê os arquivos de `app/regras/` e falha se
`fastapi` ou `sqlalchemy` aparecerem entre os imports (`arquitetura.md`).

Verificar: `pytest` verde sem Postgres rodando, `mypy --strict app/regras`
limpo.

Commit: `feat` mais `test`, ou um só se preferir.

### Etapa 3: autenticação e sessão

**Objetivo:** entrar e sair funcionando de verdade, com conta Google real.

Leia ADR-0008, ADR-0009, ADR-0010 e o UC-01 inteiro, com os três fluxos
alternativos.

Criar: as três rotas de `auth/`, a verificação do e-mail contra a tabela
`convidado` (RN-48), a criação de conta no primeiro acesso **junto com a fase de
manutenção de início nulo** (RN-21, RN-64), e o cookie com as três marcas.

**Esta etapa depende de configuração externa que o interessado precisa fazer**:
criar o projeto no console do Google, registrar o URI de redirecionamento, e
cadastrar o próprio e-mail na lista de usuários de teste. Peça isso antes de
começar, e peça os escopos limitados a `openid`, e-mail e perfil (RNF-21), porque
é isso que evita a expiração de 7 dias do refresh token.

O comando de linha de convite (RF-44) entra aqui, porque sem ele ninguém entra.

Verificar: login real funciona localmente, logout apaga a linha da tabela e o
cookie, e um e-mail fora da tabela `convidado` recebe recusa que não revela se
aquele e-mail existe (RNF-23).

Commit: `feat`.

### Etapa 4: rotas de registro

**Objetivo:** registrar, editar e apagar peso, com todos os fluxos alternativos
do UC-02, UC-03 e UC-04.

Atenção ao conflito de data: o POST sem `substituir` numa data ocupada responde
409 com `REGISTRO_JA_EXISTE` e o valor gravado, e a confirmação vem no corpo do
segundo POST (`endpoints.md`). O mesmo vale para `PATCH` que **move** o registro
para uma data ocupada.

Verificar: testes de rota contra Postgres real, cada um em transação revertida
(ADR-0020). Cobrir os fluxos alternativos, não só o caminho sem erro.

Commit: `feat`.

### Etapa 5: rotas de fase

**Objetivo:** criar, editar e apagar fase mantendo os invariantes.

Os invariantes que não podem quebrar em nenhum caminho: uma fase corrente por
usuário, nenhuma sobreposição, nenhuma data com registro fora de fase, e a
fronteira movendo as duas fases ao mesmo tempo (RN-19, RN-20, RN-24).

Leia o UC-05, UC-06 e UC-07 com os alternativos. O 2b do UC-05 (início caindo
dentro de fase encerrada) é recusa, não divisão.

Verificar: teste de rota para cada recusa, com o código de erro certo do
catálogo.

Commit: `feat`.

### Etapa 6: acompanhamento

**Objetivo:** a resposta única do `/acompanhamento`, montada chamando a camada de
regras da etapa 2.

Aqui a rota não calcula nada. Se você sentir vontade de escrever aritmética
dentro dela, a função está faltando em `app/regras/`.

Atenção: onde a janela não tem os 4 registros, o peso semanal vem **nulo** e o
veredito vem com estado de dados insuficientes. O campo não desaparece
(`endpoints.md`).

Commit: `feat`.

### Etapa 7: importação e exportação

**Objetivo:** as duas chamadas de importação e a exportação em CSV.

Leia o UC-10 inteiro. A pré-visualização não grava nada, a confirmação reenvia o
arquivo, e a gravação é tudo ou nada. Toda regra de validação de registro vale
para linha importada (RN-60).

A exportação sai no formato que a importação aceita, e conta vazia devolve
arquivo só com cabeçalho (UC-11).

Commit: `feat`.

### Etapa 8: frontend, base

**Objetivo:** aplicação subindo, autenticada, com uma tela vazia e o caminho de
dados funcionando.

Criar: projeto Vite com TypeScript, Tailwind, shadcn/ui, React Router 8 em modo
declarativo, TanStack Query, o proxy do Vite repassando `/api` ao uvicorn
(ADR-0031), a geração dos tipos a partir do OpenAPI (ADR-0027), e o módulo
`formato/` com número e data (RN-62, RN-63).

O `formato/` vem **antes** das telas de propósito. Se vier depois, formatação
nasce espalhada em cinco componentes e divergiu antes do fim do mês.

Verificar: login funciona ponta a ponta pelo navegador, e `tsc` limpo com
`strict`.

Commit: `feat`.

### Etapa 9: telas

**Objetivo:** os casos de uso visíveis.

Ordem sugerida, da que ensina mais sobre o resto para a que depende de tudo:
registro do peso, acompanhamento, fases, importação, conta.

A tela de registro carrega a meta de interação da RNF-26: uma tela e uma
confirmação, sem passo intermediário. É a tela que decide se o produto é usado.

Teste com Vitest apenas nos componentes que **decidem** (ADR-0020): número
contra dados insuficientes, barra travando em 0 e 100, vírgula na entrada e na
exibição.

Acessibilidade: os quatro itens verificáveis do `convencoes-frontend.md`, e o
gráfico não sendo a única forma de ler o dado.

Commit: `feat` por tela.

### Etapa 10: produção

**Objetivo:** rodar no servidor doméstico, com backup funcionando.

Criar: `compose.yml` de produção com nginx, api, db e o conector do túnel; a
imagem do frontend em dois estágios; o `try_files` no nginx; o script de deploy
que para no primeiro erro; o `pg_dump` agendado com as três cópias e a retenção
da RNF-07; e o interruptor de homem morto da RNF-09.

O teste de fumaça do Playwright (ADR-0021, ADR-0022) entra aqui, com a sessão
inserida direto no banco.

A integração contínua entra aqui (ADR-0030), com ruff, mypy, pytest, Vitest e o
teste de fumaça.

**Verificar a restauração de verdade**, não a existência do arquivo de backup: 
subir o dump num banco vazio e conferir que os dados voltaram. É a RNF-08, e é o
passo que todo mundo pula.

Commit: `build` e `ci`.

---

## 6. O que depende do mundo real e não de código

Peça ao interessado, na etapa em que faz falta:

| O quê | Quando | Para quê |
|:--|:--|:--|
| Projeto no console do Google, com URI de redirecionamento e lista de usuários de teste | etapa 3 | sem isso, ninguém entra |
| Repositório remoto criado | antes da etapa 10 | integração contínua e cópia externa do código |
| Domínio `.com.br` registrado e apontado para a Cloudflare | etapa 10 | o Cloudflare Tunnel exige site na Cloudflare |
| Conta de armazenamento externo para o backup | etapa 10 | a cópia fora de casa da RNF-05 |
| Serviço de interruptor de homem morto | etapa 10 | RNF-09 |

Uma decisão ainda aberta quando este documento foi escrito: **o remoto vai para
GitHub ou GitLab.** O ADR-0030 diz GitHub, e essa escolha foi escrita a partir
de uma recomendação que nunca recebeu confirmação explícita. Confirme com o
interessado antes da etapa 10, e se a resposta for GitLab, o ADR-0030 é superado
por um ADR novo, não editado.

---

## 7. Como saber que acabou

A definição de pronto de cada tarefa está no `fluxo-de-trabalho.md`, com seis
linhas. Para o projeto inteiro, o critério é mais curto:

Uma pessoa convidada consegue entrar com a conta Google dela, registrar o peso de
hoje em uma tela e uma confirmação, ver o peso semanal com a linha interrompida
onde faltou dado, declarar uma fase de cutting com nível moderado, e receber a
resposta de se está no ritmo, tudo isso rodando no servidor doméstico atrás do
túnel, com o backup de ontem restaurável.

Se qualquer parte dessa frase não for verdade, não acabou.
