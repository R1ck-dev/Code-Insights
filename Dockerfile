# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# pom.xml antes do src: enquanto as dependencias nao mudam, esta camada vem do cache
# e o build so recompila o codigo.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime ----
# Alpine em vez da base Debian: ~180MB contra ~270MB, o que encurta o deploy no tier free.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sem privilegios: nada aqui precisa de root em tempo de execucao.
RUN addgroup -S codeinsights && adduser -S codeinsights -G codeinsights
COPY --from=build --chown=codeinsights:codeinsights /app/target/*.jar app.jar
USER codeinsights

EXPOSE 8080

# Ajuste de memoria para container pequeno (512MB no free tier do Render):
#  - MaxRAMPercentage=65 deixa ~180MB fora do heap para metaspace, stacks de thread e
#    memoria nativa. Os 75% anteriores sufocavam o metaspace do Spring e levavam a OOM
#    do container em vez de OutOfMemoryError do Java (mais dificil de diagnosticar).
#  - TieredStopAtLevel=1 limita o JIT ao compilador C1: menos CPU e memoria de compilacao,
#    ao custo de throughput de pico — troca certa para uma aplicacao de baixo trafego.
#  - O tamanho de stack fica no default de proposito: o motor de metricas percorre a AST
#    recursivamente (AnalisadorDeRecursao, AvaliadorDeCusto) e um -Xss reduzido quebraria
#    a analise de codigos mais aninhados.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
