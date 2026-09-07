# ADR-0007 nginx serve o estático e faz proxy reverso

- **Data:** 07-09-2026
- **Situação:** aceito

## Contexto

O build do Vite produz arquivos estáticos, não um programa: em produção não
existe processo de frontend rodando. A API é um processo separado. Falta
decidir quem atende o navegador.

## Decisão

nginx recebe toda requisição. Caminhos sob `/api` são repassados ao FastAPI,
que escuta apenas na rede interna. Todo outro caminho recebe arquivo estático,
com `try_files` caindo em `index.html` para que as rotas do lado do cliente
(ADR-0024) funcionem ao recarregar a página.

## Consequências

Frontend e API compartilham a mesma origem. Isso elimina de uma vez a
configuração de origem cruzada, a necessidade de marcar o cookie de sessão como
utilizável entre sites (ADR-0009), e a duplicação do URI de redirecionamento no
console do Google.

O FastAPI nunca é alcançável direto da internet.

Em desenvolvimento o proxy do Vite reproduz a mesma origem única, então não há
configuração de CORS em nenhum dos dois ambientes (ADR-0031).

## Alternativa recusada: FastAPI servindo o estático

Funciona e é mais simples de montar. Recusada por pôr o Python a entregar
arquivo estático, que é desperdício de processo e complica controle de cache.

## Alternativa recusada: frontend em CDN separada

Cloudflare Pages ou equivalente daria distribuição global aos arquivos. O ganho
é de dezenas de milissegundos para um público de poucas pessoas no Brasil, e o
custo é reintroduzir duas origens, com CORS, cookie entre sites, dois lugares
de deploy e duas entradas no Google. Além disso, o Cloudflare Tunnel já coloca
a borda da Cloudflare na frente da aplicação, com cache do estático.
