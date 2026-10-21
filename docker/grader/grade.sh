#!/bin/sh
# Runs inside the grader container. /work/submission and /work/tests are read-only mounts;
# /reports is the only writable mount and receives the JUnit XML report.
# Exit codes: 0 all tests passed, 1 some failed, 2 no tests found (JUnit launcher), 3 compile error.
set -u

CLASSES=/tmp/classes
mkdir -p "$CLASSES"

find /work/submission /work/tests -name '*.java' > /tmp/sources.txt
if ! javac -nowarn -encoding UTF-8 -d "$CLASSES" -cp "$JUNIT_JAR" @/tmp/sources.txt; then
    exit 3
fi

# Per-test timeout so one infinite loop costs that test, not the whole grade. The container's
# wall-clock timeout (enforced by AutoTA) is the backstop if the JVM itself hangs.
exec java -XX:-UsePerfData -Xmx256m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -jar "$JUNIT_JAR" execute \
    --disable-banner \
    --include-engine=junit-jupiter \
    --fail-if-no-tests \
    --details=tree \
    --class-path "$CLASSES" \
    --scan-class-path \
    --config=junit.jupiter.execution.timeout.default=2s \
    --config=junit.jupiter.execution.timeout.thread.mode.default=SEPARATE_THREAD \
    --reports-dir /reports
