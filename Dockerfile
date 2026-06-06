FROM gradle:8-jdk23-alpine AS builder
WORKDIR /app

# Сначала копируем только файлы сборки — чтобы зависимости кэшировались отдельным слоем
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# Качаем зависимости отдельно — этот слой будет закэширован Docker'ом
# при повторных сборках если build.gradle не менялся
RUN gradle dependencies --no-daemon || true

# Теперь копируем исходники и собираем
COPY src/ src/
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
