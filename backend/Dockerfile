# syntax=docker/dockerfile:1
# A imagem da api, em dois estágios (ADR-0037).

# Estágio 1: compila com o Maven do projeto. Os testes ficam com a integração
# contínua, que tem o Docker que o Testcontainers pede.
FROM eclipse-temurin:25.0.5_8-jdk-resolute AS construcao
WORKDIR /construcao
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# O cache guarda o Maven e as dependências entre um build e outro, fora da imagem
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -Dmaven.test.skip=true

# Estágio 2: só o Java de execução e o .jar
FROM eclipse-temurin:25.0.5_8-jre-resolute
# O repositório de origem, nos metadados da imagem
LABEL org.opencontainers.image.source=https://github.com/cainavieira/Mbarete
WORKDIR /app
COPY --from=construcao /construcao/target/api-*.jar api.jar
# O usuário comum que a imagem base já traz (ubuntu), e não o root
USER 1000
EXPOSE 8080
# Sem argumento, a api; com migrar ou convidar, o modo (convencoes-backend.md)
ENTRYPOINT ["java", "-jar", "api.jar"]
