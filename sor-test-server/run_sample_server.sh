#!/usr/bin/env sh
set -eu
JAVA_OPTS="${JAVA_OPTS:-"-XX:+UseZGC -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch -Xms8g -Xmx8g"}"
exec java $JAVA_OPTS -cp "lib/*" com.nitroj.sor.testserver.SimulatorServerApplication "$@"
