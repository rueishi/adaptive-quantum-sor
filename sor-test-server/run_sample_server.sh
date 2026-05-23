#!/usr/bin/env sh
set -eu
JAVA_OPTS="${JAVA_OPTS:-"-XX:+UseZGC -XX:+AlwaysPreTouch -Xms8g -Xmx8g"}"
exec java $JAVA_OPTS -cp "lib/*" com.nitroj.sor.sim.server.SimulatorServerApplication "$@"
