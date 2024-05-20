FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

COPY . /workspace/app
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew clean build
WORKDIR /workspace/app/build/dependency
RUN jar -xf ../libs/*-SNAPSHOT.jar

FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/build/dependency

RUN addgroup java && adduser -SG java java
USER java

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java", "-cp", "app:app/lib/*", "pl.edu.pg.rsww.tripreservations.TripReservationsApplicationKt"]

HEALTHCHECK CMD exit 0
