# syntax=docker/dockerfile:1
# A imagem do frontend, em dois estágios (ADR-0037): o build do Vite, e o nginx
# só com os arquivos gerados (ADR-0038).

# Estágio 1: o build, com a mesma major do Node da integração contínua
FROM node:24.22.3-trixie-slim AS construcao
WORKDIR /construcao
# As dependências antes do código: enquanto o package-lock.json não muda, o
# Docker reaproveita esta camada
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

# Estágio 2: o nginx, sem Node
FROM nginxinc/nginx-unprivileged:1.30.6-alpine
LABEL org.opencontainers.image.source=https://github.com/cainavieira/Mbarete
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=construcao /construcao/dist /usr/share/nginx/html
