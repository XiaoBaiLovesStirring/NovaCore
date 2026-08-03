#!/bin/sh
SAVED="$(pwd)"
cd "$(dirname "$0")"
APP_HOME="$(pwd -P)"
cd "$SAVED"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
