# Arquitetura

Duas vistas do modelo C4, contexto e contêiner, mais a regra de dependência
interna. Os níveis 3 e 4 do C4 não estão aqui, e o motivo está no fim.

## Nível 1: contexto

Quem usa o sistema e com que sistema externo ele conversa.

```mermaid
flowchart LR
    U["Usuario (AT-01)<br/>registra o proprio peso"]
    O["Operador (AT-02)<br/>mantem o sistema no ar"]
    S["Acompanhamento de peso<br/>ESTE SISTEMA"]
    G["Google (AT-03)<br/>provedor de identidade"]

    U -->|"registra peso, cria fase,<br/>acompanha o ciclo"| S
    O -->|"convida por comando de linha,<br/>aplica migracao, restaura backup"| S
    S -->|"delega autenticacao"| G
    U -->|"consente o login"| G
```

O AT-02 aparece com seta para o sistema mesmo sem ter interface: ele age pelo
terminal do servidor e pelo console do Google, não por tela (`atores.md`).

Não há nenhum outro sistema externo. Não existe serviço de e-mail, nem passarela
de pagamento, nem integração com balança ou aplicativo de terceiro.

## Nível 2: contêineres

O que roda onde, e por onde a requisição passa.

```mermaid
flowchart TB
    SPA["SPA React<br/>arquivos estaticos no navegador"]
    CFE["Rede da Cloudflare<br/>TLS e borda"]
    G["Google<br/>OpenID Connect"]
    BK["Copias de backup<br/>disco lento e nuvem externa"]

    subgraph host["Servidor domestico (Proxmox, LXC)"]
        CF["cloudflared<br/>conector do tunel"]
        NG["nginx<br/>estatico e proxy reverso"]
        API["api<br/>FastAPI sobre Uvicorn"]
        DB["db<br/>PostgreSQL"]
    end

    SPA -->|"HTTPS"| CFE
    CFE -->|"conexao de saida,<br/>nenhuma porta aberta"| CF
    CF --> NG
    NG -->|"/api/*"| API
    NG -->|"outros caminhos:<br/>entrega o build do Vite"| SPA
    API -->|"SQL pela rede interna"| DB
    API -->|"troca codigo por token,<br/>verifica id_token"| G
    DB -->|"dump diario"| BK
```

| Contêiner | Responsabilidade | Decisão |
|:--|:--|:--|
| SPA React | telas, estado de tela, validação de forma | ADR-0002, ADR-0025 |
| cloudflared | publica o serviço sem abrir porta na casa | ADR-0006 |
| nginx | entrega o estático e repassa `/api` | ADR-0007 |
| api | regras de negócio, contrato de API, sessão | ADR-0001 |
| db | dado, restrições de integridade | ADR-0004, ADR-0005 |

Três propriedades que essa vista fixa, e que valem mais que o desenho:

**Origem única.** O navegador conhece um endereço só. Isso apaga a configuração
de origem cruzada, deixa o cookie de sessão funcionar sem marca especial, e faz
existir um único URI de redirecionamento no Google (ADR-0007, ADR-0009).

**O banco não é alcançável de fora.** Ele existe apenas na rede interna do
Compose, e nenhum caminho do diagrama chega nele sem passar pela api (RNF-20).

**Nenhuma porta aberta na casa.** A seta da Cloudflare para o `cloudflared`
aponta no sentido do desenho, mas a conexão é estabelecida de dentro para fora
(RNF-19).

## A regra de dependência interna

Em vez das camadas completas de Clean Architecture, uma regra só (ADR-0019):

> O módulo de regras de agregação não importa FastAPI nem SQLAlchemy.

Traduzida em estrutura de pasta do backend:

```
app/
  regras/     nao importa nada alem da biblioteca padrao e Decimal
  modelos/    SQLAlchemy: as tabelas
  esquemas/   Pydantic: entrada e saida de cada rota
  rotas/      FastAPI: recebe, chama regras, devolve esquema
  infra/      sessao de banco, configuracao, autenticacao
```

A direção é sempre a mesma: **todos podem importar `regras/`, e `regras/` não
importa ninguém.** Ela recebe listas de dados e devolve valores.

Duas regras concretas que vêm da mesma família:

- A rota não calcula nada. Ela chama quem calcula.
- A função de regra recebe dados, não a sessão do banco.

**Como isso se verifica em vez de se prometer:** um teste que lê os arquivos de
`regras/` e confere que `fastapi` e `sqlalchemy` não aparecem entre os imports.
É teste de arquitetura, roda em milissegundos, e falha no dia em que alguém
tomar o atalho. Sem ele, a regra é comentário.

O retorno concreto: os testes de peso semanal, taxa real, veredito e progresso
rodam sem banco e sem servidor, que é o que torna a RNF-27 barata em vez de
cerimonial (ADR-0020).

## Os níveis 3 e 4 do C4 não estão aqui

O C4 tem quatro níveis: contexto, contêiner, componente e código. Os dois
primeiros estão acima. Os dois últimos foram recusados, com motivo.

**Componente** desenharia as peças dentro da api. A estrutura de pasta acima já
diz isso em cinco linhas, e um diagrama de componente fica desatualizado no
primeiro refactor, porque descreve exatamente o que mais muda.

**Código** desenharia classes e funções. Isso é o código, e o código é a única
descrição dele que não pode divergir dele.

A pergunta que decide se um diagrama vale: ele mostra algo que não se descobre
lendo a pasta? Contexto e contêiner mostram (o túnel de saída, a origem única, o
banco isolado, o que é externo). Componente e código não.
