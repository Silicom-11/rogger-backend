# ---------------------------------------------------------------------------
# Imagen del back-end de Rogger Express.
#
# Se construye en dos etapas: la primera compila con Maven y la segunda queda
# solo con el JAR y el runtime de Java, de modo que la imagen final no arrastra
# el compilador ni el repositorio de dependencias.
# ---------------------------------------------------------------------------

FROM maven:3.9-eclipse-temurin-17 AS construccion
WORKDIR /build

# Las dependencias se resuelven primero y en su propia capa: mientras el pom no
# cambie, una recompilacion no vuelve a descargarlas.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# La aplicacion no corre como root.
RUN addgroup -S rogger && adduser -S rogger -G rogger
COPY --from=construccion /build/target/*.jar app.jar
RUN chown rogger:rogger app.jar
USER rogger

# El proveedor de nube indica el puerto por la variable PORT.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT} -jar app.jar"]
