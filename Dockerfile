FROM maven:3.6.3-jdk-11-slim AS build
COPY src .
COPY pom.xml .
ADD temp/ /app/temp/
ADD execution/ /app/execution/
COPY NetBench.jar .
# RUN mvn clean compile assembly:single
ENTRYPOINT ["/app/execution/automated_execution_hybrid.sh"]