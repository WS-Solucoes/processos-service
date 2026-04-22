FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY eFrotas/pom.xml eFrotas/pom.xml
COPY eRH-Service/pom.xml eRH-Service/pom.xml
COPY processos-service/pom.xml processos-service/pom.xml
COPY file-storage-service/pom.xml file-storage-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY service-discovery/pom.xml service-discovery/pom.xml
COPY compras-service/pom.xml compras-service/pom.xml
COPY licitacao-service/pom.xml licitacao-service/pom.xml
COPY contratos-service/pom.xml contratos-service/pom.xml
COPY egestao/pom.xml egestao/pom.xml

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -pl processos-service -am dependency:go-offline --batch-mode

COPY common/src common/src
COPY processos-service/src processos-service/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -pl processos-service -am clean package -DskipTests --batch-mode

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/processos-service/target/*-exec.jar app.jar

EXPOSE 9084

CMD ["java", "-jar", "app.jar"]
