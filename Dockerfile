FROM ghcr.io/navikt/baseimages/temurin:21

COPY app/target/app.jar /app/app.jar
COPY export-nais-secrets.sh /init-scripts/10-export-nais-secrets.sh

ENV JAVA_OPTS="-Xmx5000m \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"