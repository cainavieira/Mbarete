# Acompanhamento de peso

Evolução do script original `backend/pytho.py` (le `comprovante.csv`, tira
media por blocos de 7 linhas) para uma aplicacao web: backend Python e
frontend React, registro diario de peso com acompanhamento diario e semanal.
Armazenamento por enquanto em CSV; banco de dados fica para uma fase
posterior.

## Estado atual (parcial, nao funcional ainda)

Nada roda de ponta a ponta ainda. O que existe e o scaffold e o contrato
entre as duas camadas, para o trabalho continuar depois sem redecidir forma.

**`backend/`**
- `pytho.py`: o script original, inalterado.
- `.venv/`: ambiente virtual criado com Python 3.14.6, ja com `fastapi`,
  `uvicorn[standard]`, `pydantic`, `pytest` e `httpx` instalados.

**`frontend/`**
- React 19.2, Vite 8.2, TypeScript 6.0.
Nenhum dos dois lados foi commitado: nao ha repositorio git na pasta ainda.

## O que falta para a versao funcional

**Backend**
- Camada de armazenamento em CSV (`app/storage.py` ou similar): ler/escrever
  `backend/data/comprovante.csv` com um registro por dia (o script original
  nao tinha essa restricao; a API vai impor).
- Regras de agregacao: serie diaria com media movel, fechamento por semana
  ISO (segunda a domingo), variacao 7/30 dias e total, progresso ate a meta, sequencia de dias seguidos
  registrando.
- Rotas FastAPI: CRUD de registros, perfil (altura, peso-meta),
  estatisticas, series diaria e semanal, importacao de CSV externo
  (`ImportRequest`/`ImportResult` ja modelam isso), health check.
- Testes com pytest + httpx cobrindo as regras de agregacao e as rotas.

**Frontend**
- Cliente HTTP para a API (`src/lib/`).
- Telas: formulario de registro diario, grafico de evolucao (diario e
  semanal), cartoes de estatisticas, tela de perfil/meta, import de CSV.
- Estado de carregamento/erro tratando o formato de erro que o backend vai
  devolver (`ErrorDetail`).

