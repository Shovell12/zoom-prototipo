@echo off
setlocal

set JAR=target\zoom-prototipo-1.0-SNAPSHOT-jar-with-dependencies.jar

if not exist "%JAR%" (
    echo No se encontro el JAR. Ejecuta primero: mvn package
    pause
    exit /b 1
)

java ^
  --add-opens java.base/java.lang=ALL-UNNAMED ^
  --add-opens java.base/sun.misc=ALL-UNNAMED ^
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
  --add-opens java.base/java.nio=ALL-UNNAMED ^
  -Dfile.encoding=UTF-8 ^
  -Dstdout.encoding=UTF-8 ^
  -Dsun.java2d.uiScale=true ^
  -jar "%JAR%" %*
