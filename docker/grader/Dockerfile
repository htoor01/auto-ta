# syntax=docker/dockerfile:1
# Image AutoTA runs each submission in: a JDK plus the JUnit console launcher, nothing else.
# Build: docker build -t autota-grader:latest docker/grader
FROM eclipse-temurin:25-jdk-noble

# Standalone launcher bundles the JUnit Jupiter API the instructor tests compile against.
# The checksum pins the exact jar, so a tampered or changed download fails the build.
ADD --chmod=644 \
    --checksum=sha256:3ba0d6150af79214a1411f9ea2fbef864eef68b68c89a17f672c0b89bff9d3a2 \
    https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.0.3/junit-platform-console-standalone-6.0.3.jar \
    /opt/junit-platform-console-standalone.jar
ENV JUNIT_JAR=/opt/junit-platform-console-standalone.jar

COPY --chmod=755 grade.sh /usr/local/bin/grade.sh

# Student code never runs as root ("nobody").
USER 65534:65534
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/grade.sh"]
