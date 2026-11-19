package at.htl.droidrampage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Speichert die besten Runs dauerhaft in ~/.droidrampage/highscores.txt
 * Format pro Zeile: score;distanz;credits;skin;datum
 */
public class HighScoreManager {

    public record Entry(int score, int distance, int credits, String skin, String date) {
        String serialize() {
            return score + ";" + distance + ";" + credits + ";" + skin + ";" + date;
        }

        static Entry parse(String line) {
            String[] p = line.split(";");
            if (p.length < 5) return null;
            try {
                return new Entry(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()),
                        Integer.parseInt(p[2].trim()), p[3].trim(), p[4].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private static final int MAX_ENTRIES = 10;

    private final Path file;
    private final List<Entry> entries = new ArrayList<>();

    public HighScoreManager() {
        this(Path.of(System.getProperty("user.home"), ".droidrampage", "highscores.txt"));
    }

    public HighScoreManager(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        entries.clear();
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Entry e = Entry.parse(line);
                if (e != null) entries.add(e);
            }
            sortAndTrim();
        } catch (IOException e) {
            System.err.println("[HIGHSCORE] Konnte Highscores nicht laden: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            List<String> lines = new ArrayList<>();
            for (Entry e : entries) lines.add(e.serialize());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[HIGHSCORE] Konnte Highscores nicht speichern: " + e.getMessage());
        }
    }

    private void sortAndTrim() {
        entries.sort(Comparator.comparingInt(Entry::score).reversed());
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
    }

    /**
     * Trägt einen Run ein.
     * @return Platzierung (1 = neuer Rekord) oder -1, wenn der Run nicht in die Top 10 kommt
     */
    public int submit(int score, int distance, int credits, DroidSkin skin) {
        if (score <= 0) return -1;
        Entry entry = new Entry(score, distance, credits, skin.getDisplayName(), LocalDate.now().toString());
        entries.add(entry);
        sortAndTrim();
        int index = entries.indexOf(entry);
        save();
        return index >= 0 ? index + 1 : -1;
    }

    public List<Entry> top(int n) {
        return List.copyOf(entries.subList(0, Math.min(n, entries.size())));
    }

    public int best() {
        return entries.isEmpty() ? 0 : entries.get(0).score();
    }
}
