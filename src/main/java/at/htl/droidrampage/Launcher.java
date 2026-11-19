package at.htl.droidrampage;

/**
 * Einstiegspunkt für die ausführbare .jar-Datei.
 *
 * Java startet eine Klasse, die von javafx.application.Application erbt, nicht direkt
 * aus einem "fat jar" ("JavaFX runtime components are missing"). Diese Klasse erbt
 * von nichts und ruft nur GameApp.main() auf – damit funktioniert {@code java -jar}.
 */
public class Launcher {
    public static void main(String[] args) {
        GameApp.main(args);
    }
}
