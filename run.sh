#!/bin/bash
JAR="target/zoom-prototipo-1.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$JAR" ]; then
    echo "No se encontró el JAR. Ejecuta primero: mvn package"
    exit 1
fi

java \
  -Dfile.encoding=UTF-8 \
  -Dstdout.encoding=UTF-8 \
  -Dsun.java2d.uiScale=true \
  -jar "$JAR" "$@"
