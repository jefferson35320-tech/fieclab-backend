# Estágio de construção (Builder)
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copia o projeto para dentro do container
COPY . .

# Compila e gera o JAR
RUN mvn clean package -DskipTests


# Estágio de execução (Runtime)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copia o JAR gerado no estágio anterior
COPY --from=builder /app/target/*.jar app.jar

# Cria usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

USER spring:spring

# Expõe a porta da aplicação
EXPOSE 8080

# Executa a aplicação
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
