#!/usr/bin/env sh
# Lightweight Gradle launcher for this Adaptive Quantum SOR workspace.
#
# A full Gradle wrapper JAR is intentionally not checked in during the scaffold
# task. This script delegates to the Gradle installation available in the
# developer environment while preserving the standard `./gradlew` command shape
# required by the task prompts and README.
exec gradle "$@"
