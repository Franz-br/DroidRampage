package at.htl.droidrampage;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * Einstiegspunkt für die ausführbare .jar-Datei.
 *
 * Java startet eine Klasse, die von javafx.application.Application erbt, nicht direkt
 * aus einem "fat jar" ("JavaFX runtime components are missing"). Diese Klasse erbt
 * von nichts und ruft nur GameApp.main() auf – damit funktioniert {@code java -jar}.
 *
 * Diese eine Klasse wird für Java 8 kompiliert (siehe pom.xml), der Rest für Java 17.
 * So kann sie auch auf alten Java-Versionen (auf Schul-PCs oft Java 8) starten und
 * eine verständliche Meldung zeigen, statt per Doppelklick einfach nichts zu tun.
 * Deshalb wird GameApp auch nur per Reflection aufgerufen.
 */
public class Launcher {

    private static final int MIN_JAVA = 17;

    public static void main(String[] args) {
        int version = javaMajorVersion();
        if (version < MIN_JAVA) {
            showError("Droid Rampage needs Java " + MIN_JAVA + " or newer.\n"
                    + "This PC has Java " + version + " (" + System.getProperty("java.home") + ").\n\n"
                    + "Easiest fix: double-click Start-Windows.bat (Windows) or Start-Mac.command (Mac)\n"
                    + "from the DroidRampage zip - it downloads a suitable Java automatically.\n\n"
                    + "Or install Java " + MIN_JAVA + "+ from https://adoptium.net");
            System.exit(1);
            return;
        }

        try {
            Class.forName("at.htl.droidrampage.GameApp")
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) args);
        } catch (Throwable t) {
            Throwable cause = t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t;
            File log = writeErrorLog(cause);
            showError("Droid Rampage could not start:\n" + cause + "\n\n"
                    + (log != null ? "Details were saved to:\n" + log.getAbsolutePath() : ""));
            System.exit(1);
        }
    }

    /** "1.8" → 8, "17" → 17, "21.0.2" → 21 */
    private static int javaMajorVersion() {
        String v = System.getProperty("java.specification.version", "0");
        try {
            if (v.startsWith("1.")) v = v.substring(2);
            int dot = v.indexOf('.');
            return Integer.parseInt(dot >= 0 ? v.substring(0, dot) : v);
        } catch (NumberFormatException e) {
            return MIN_JAVA; // unbekanntes Format → einfach versuchen
        }
    }

    /** Fehler sichtbar machen: Beim Doppelklick (javaw) gibt es keine Konsole. */
    private static void showError(String message) {
        System.err.println(message);
        try {
            JOptionPane.showMessageDialog(null, message, "Droid Rampage", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {
            // kein Bildschirm verfügbar → die Konsolenausgabe muss reichen
        }
    }

    private static File writeErrorLog(Throwable t) {
        try {
            File dir = new File(System.getProperty("user.home"), ".droidrampage");
            dir.mkdirs();
            File log = new File(dir, "error.log");
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            FileWriter out = new FileWriter(log);
            try {
                out.write("Java " + System.getProperty("java.version") + " (" + System.getProperty("java.home") + ")\n");
                out.write(System.getProperty("os.name") + " " + System.getProperty("os.arch") + "\n\n");
                out.write(trace.toString());
            } finally {
                out.close();
            }
            return log;
        } catch (Exception e) {
            return null;
        }
    }
}
