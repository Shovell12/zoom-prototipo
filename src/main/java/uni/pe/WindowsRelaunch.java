package uni.pe;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class WindowsRelaunch {

    private WindowsRelaunch() {}

    static boolean necesario() {
        return System.getProperty("os.name", "").toLowerCase().contains("win")
                && System.getProperty("app.relaunched") == null;
    }

    /**
     * Relanza el JAR actual añadiendo los flags que bridj necesita bajo JPMS (Java 9+).
     * Retorna true si el relanzamiento fue exitoso; el llamador debe salir en ese caso.
     */
    static boolean ejecutar(String[] args) {
        try {
            URI uri = WindowsRelaunch.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File jar = new File(uri);
            if (!jar.getName().endsWith(".jar")) return false; // entorno IDE, clases sueltas

            String javaExe = ProcessHandle.current().info().command().orElse("java");

            List<String> cmd = new ArrayList<>(List.of(
                javaExe,
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/sun.misc=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens", "java.base/java.nio=ALL-UNNAMED",
                "-Dapp.relaunched=true",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dsun.java2d.uiScale=true",
                "-jar", jar.getAbsolutePath()
            ));
            cmd.addAll(Arrays.asList(args));

            new ProcessBuilder(cmd).inheritIO().start().waitFor();
            return true;
        } catch (Exception e) {
            return false; // si falla el relanzamiento, continúa sin los flags
        }
    }
}
