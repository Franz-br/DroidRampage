package at.htl.droidrampage;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.almasb.fxgl.dsl.FXGL.spawn;

/**
 * Erzeugt zufällige Level-Segmente (1920 px breit) zusätzlich zu den Tiled-Maps
 * und verteilt Gegner – sowohl in generierten Segmenten als auch in den Tiled-Maps.
 *
 * Ein Segment besteht aus Boden-Stücken, zwischen denen "Features" liegen:
 * Treppen, Aufzüge, Fähren, Sprungfedern, Laser-Tore, Bröckel-Brücken, Phasen-Plattformen,
 * Förderbänder, Sägen, Türme, Außenposten und Elektroböden.
 * Alle Abstände sind so gewählt, dass sie mit jedem Droiden schaffbar sind.
 */
public class SegmentGenerator {

    public static final double SEGMENT_WIDTH = 1920;
    private static final double FLOOR_Y = 1038;
    private static final double FLOOR_H = 50;
    private static final int PLATFORM_H = 28;
    private static final double END_MARGIN = 160; // Boden am Segment-Ende (sicherer Übergang)

    private enum Feature {
        STEPS(540, 3), ELEVATOR(340, 2), FERRY(600, 2), BOUNCE(600, 2), LASER(460, 2), BRIDGE(540, 2),
        PHASE(560, 2), CONVEYOR(560, 2), SAW(560, 2), TOWER(620, 2), OUTPOST(600, 2), ZAP_FLOOR(560, 2);

        final double maxWidth;
        final int weight;

        Feature(double maxWidth, int weight) {
            this.maxWidth = maxWidth;
            this.weight = weight;
        }
    }

    private double ox;          // Welt-X des Segmentanfangs
    private double difficulty;  // 0 (leicht) .. 1 (schwer)
    private Feature lastFeature;

    /**
     * Baut ein komplettes Segment ab Welt-Position offsetX.
     * @param difficulty 0..1 – steigert Bröckel-Anteil, Laser-Dauer, Plattform-Tempo und Gegner-Anzahl
     */
    public void generate(double offsetX, double difficulty) {
        this.ox = offsetX;
        this.difficulty = clamp01(difficulty);

        double x = ground(0, range(180, 340));

        while (true) {
            Feature f = pickFeature();
            if (x + f.maxWidth + END_MARGIN > SEGMENT_WIDTH) break;

            x = build(f, x);
            lastFeature = f;

            double rest = SEGMENT_WIDTH - x;
            double run = Math.min(range(110, 260), rest);
            double runStart = x;
            x = ground(x, run);
            if (chance(0.35)) credit(1, x - run / 2, FLOOR_Y - 70);
            if (run >= 220 && chance(enemyChance() * 0.6)) brute(runStart + 10, x - 10, FLOOR_Y);
        }

        if (x < SEGMENT_WIDTH) {
            ground(x, SEGMENT_WIDTH - x);
        }
    }

    private Feature pickFeature() {
        List<Feature> pool = new ArrayList<>();
        for (Feature f : Feature.values()) {
            if (f == lastFeature) continue;
            for (int i = 0; i < f.weight; i++) pool.add(f);
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private double build(Feature f, double x) {
        switch (f) {
            case STEPS:     return steps(x);
            case ELEVATOR:  return elevator(x);
            case FERRY:     return ferry(x);
            case BOUNCE:    return bounce(x);
            case LASER:     return laser(x);
            case BRIDGE:    return bridge(x);
            case PHASE:     return phase(x);
            case CONVEYOR:  return conveyor(x);
            case SAW:       return saw(x);
            case TOWER:     return tower(x);
            case OUTPOST:   return outpost(x);
            default:        return zapFloor(x);
        }
    }

    private double enemyChance() {
        return 0.25 + 0.5 * difficulty;
    }

    // ─────────────────────────── Features ───────────────────────────

    /** Abgrund mit 2–3 Plattformen in Bogenform, optional eine hohe Bonus-Plattform. */
    private double steps(double x) {
        double gap = range(380, 520);
        int n = gap > 450 ? 3 : 2;
        double crumbleChance = 0.2 + 0.4 * difficulty;
        double w = 120;

        for (int i = 0; i < n; i++) {
            double px = x + gap * (i + 1) / (n + 1) - w / 2;
            boolean middle = n == 3 && i == 1;
            double top = (middle ? 880 : 940) + range(-20, 20);
            boolean crumble = chance(crumbleChance);
            platform(crumble ? "crumblePlatform" : "solidPlatform", px, top, w);
            credit(crumble ? 2 : 1, px + w / 2 - 24, top - 70);
        }

        if (chance(0.5)) {
            double bw = 130;
            double bx = x + gap / 2 - bw / 2;
            double top = 720;
            platform("solidPlatform", bx, top, bw);
            if (chance(enemyChance())) sniper(bx + bw / 2 - 20, top);
            else credit(3, bx + bw / 2 - 24, top - 70);
        } else if (chance(enemyChance())) {
            zapper(x + gap / 2, 760);
        }
        return x + gap;
    }

    /** Abgrund mit einem senkrecht fahrenden Aufzug. */
    private double elevator(double x) {
        double gap = 340;
        double w = 128;
        double px = x + (gap - w) / 2;
        double period = 3.4 - 0.9 * difficulty;
        moving(px, 880, w, 0, 120, period);
        credit(2, px + w / 2 - 24, 640);
        return x + gap;
    }

    /** Breiter Abgrund mit einer waagrecht pendelnden Fähre. */
    private double ferry(double x) {
        double gap = range(480, 600);
        double w = 144;
        double left = x + 30, right = x + gap - 30 - w;
        double period = 3.8 - 1.0 * difficulty;
        moving((left + right) / 2, 970, w, (right - left) / 2, 0, period);
        credit(1, x + gap * 0.3, 890);
        credit(1, x + gap * 0.7 - 48, 890);
        if (chance(enemyChance())) zapper(x + gap / 2, 720);
        else credit(2, x + gap / 2 - 24, 800);
        return x + gap;
    }

    /** Sprungfeder auf dem Boden, die zu einer hoch hängenden Bonus-Plattform führt. */
    private double bounce(double x) {
        double len = 600;
        ground(x, len);

        double padW = 72, padH = 22;
        spawn("bouncePad", new SpawnData(ox + x + 120, FLOOR_Y - padH)
                .put("width", (int) padW).put("height", (int) padH));

        double pw = 200;
        double top = 600;
        platform(chance(0.3 * difficulty) ? "crumblePlatform" : "solidPlatform", x + 380, top, pw);
        credit(2, x + 250, 520);
        credit(3, x + 380 + pw / 2 - 24, top - 70);
        if (chance(enemyChance() * 0.7)) sniper(x + 380 + pw - 50, top);
        return x + len;
    }

    /** Boden mit 1–2 blinkenden Laser-Toren (hohe Tore timen, niedrige überspringen). */
    private double laser(double x) {
        double len = 460;
        ground(x, len);

        int gates = (difficulty > 0.3 || chance(0.5)) ? 2 : 1;
        double[] gateX = gates == 2 ? new double[]{140, 330} : new double[]{220};
        for (int i = 0; i < gates; i++) {
            boolean tall = chance(0.5 + 0.3 * difficulty);
            double top = tall ? 600 : 860;
            spawn("laser", new SpawnData(ox + x + gateX[i], top)
                    .put("width", 28)
                    .put("height", (int) (FLOOR_Y - top))
                    .put("onTime", 0.9 + 0.5 * difficulty)
                    .put("offTime", 1.6 - 0.4 * difficulty)
                    .put("offset", range(0, 2.5)));
        }
        credit(2, x + (gates == 2 ? 235 : 330), FLOOR_Y - 70);
        return x + len;
    }

    /** Abgrund mit einer Brücke aus Bröckel-Plattformen – nicht stehen bleiben! */
    private double bridge(double x) {
        double gap = range(420, 540);
        double w = 88, spacing = 24;
        int count = (int) ((gap - 40) / (w + spacing));
        double start = x + (gap - (count * w + (count - 1) * spacing)) / 2;

        for (int i = 0; i < count; i++) {
            double px = start + i * (w + spacing);
            platform("crumblePlatform", px, 1000, w);
            if (i % 2 == 1) credit(1, px + w / 2 - 24, 930);
        }
        if (chance(enemyChance() * 0.6)) zapper(x + gap / 2, 800);
        return x + gap;
    }

    /** Abgrund mit Phasen-Plattformen, die abwechselnd verschwinden. */
    private double phase(double x) {
        double gap = range(420, 540);
        int n = 3;
        double w = 110;
        double onTime = 1.9 - 0.4 * difficulty, offTime = 1.1;
        double cycle = onTime + offTime;
        for (int i = 0; i < n; i++) {
            double px = x + gap * (i + 1) / (n + 1) - w / 2;
            double top = 960 - (i == 1 ? 60 : 0);
            spawn("phasePlatform", new SpawnData(ox + px, top)
                    .put("width", (int) w).put("height", PLATFORM_H)
                    .put("onTime", onTime).put("offTime", offTime)
                    .put("offset", cycle * i / n)); // versetzt -> immer mindestens eine sichtbar
            credit(i == 1 ? 2 : 1, px + w / 2 - 24, top - 70);
        }
        return x + gap;
    }

    /** Förderband am Boden (mal mit, mal gegen die Laufrichtung) – oft mit Laser oder Brute. */
    private double conveyor(double x) {
        double len = range(440, 560);
        double speed = (chance(0.5) ? -1 : 1) * (130 + 80 * difficulty);
        spawn("conveyor", new SpawnData(ox + x, FLOOR_Y)
                .put("width", (int) len).put("height", (int) FLOOR_H).put("speed", speed));
        credit(1, x + len * 0.3, FLOOR_Y - 70);
        credit(1, x + len * 0.6, FLOOR_Y - 70);
        if (chance(0.4)) {
            spawn("laser", new SpawnData(ox + x + len / 2, 860)
                    .put("width", 28).put("height", (int) (FLOOR_Y - 860))
                    .put("onTime", 1.0).put("offTime", 1.4).put("offset", range(0, 2.4)));
        } else if (chance(enemyChance())) {
            brute(x + 20, x + len - 20, FLOOR_Y);
        }
        return x + len;
    }

    /** Boden mit einer Kreissäge auf einer Schiene – drüberspringen! */
    private double saw(double x) {
        double len = 560;
        ground(x, len);
        double r = 26;
        double railY = FLOOR_Y - 40;
        double railW = len - 120;
        spawn("sawRail", new SpawnData(ox + x + 60, railY).put("width", (int) railW));
        spawn("saw", new SpawnData(ox + x + len / 2 - r, railY - r + 3)
                .put("radius", r).put("amplitude", railW / 2 - r)
                .put("period", 2.8 - 0.8 * difficulty).put("phase", range(0, 3)));
        platform("solidPlatform", x + len / 2 - 70, 820, 140);
        credit(2, x + len / 2 - 24, 750);
        return x + len;
    }

    /** Treppe aus drei Plattformen nach oben, oben wartet Beute (und manchmal eine Drohne). */
    private double tower(double x) {
        double len = 620;
        ground(x, len);
        double w = 130;
        double[][] steps = {{70, 910}, {240, 790}, {410, 670}};
        for (double[] s : steps) {
            platform(chance(0.25 * difficulty) ? "crumblePlatform" : "solidPlatform", x + s[0], s[1], w);
        }
        credit(1, x + 240 + w / 2 - 24, 720);
        credit(3, x + 410 + w / 2 - 24, 600);
        if (chance(enemyChance())) zapper(x + 460, 520);
        if (chance(enemyChance() * 0.6)) brute(x + 10, x + len - 10, FLOOR_Y);
        return x + len;
    }

    /** Außenposten: erhöhte Plattform mit Scharfschütze, darunter ein Brute. */
    private double outpost(double x) {
        double len = 600;
        ground(x, len);
        platform("solidPlatform", x + 60, 910, 110);
        double pw = 260, top = 780;
        platform("solidPlatform", x + 220, top, pw);
        credit(2, x + 220 + pw / 2 - 24, top - 70);
        credit(1, x + 90, 840);
        if (chance(0.4 + enemyChance())) sniper(x + 220 + pw - 60, top);
        if (chance(enemyChance())) brute(x + 160, x + len - 10, FLOOR_Y);
        return x + len;
    }

    /** Elektroboden in der Mitte – über die Plattform oder mit einem weiten Sprung. */
    private double zapFloor(double x) {
        double len = 560;
        ground(x, len);
        double zw = range(180, 240);
        double zx = x + (len - zw) / 2;
        spawn("Death", new SpawnData(ox + zx, FLOOR_Y - 18).put("width", (int) zw).put("height", 18));
        platform("solidPlatform", zx + zw / 2 - 70, 900, 140);
        credit(2, zx + zw / 2 - 24, 830);
        if (chance(enemyChance() * 0.6)) zapper(zx + zw / 2, 700);
        return x + len;
    }

    // ─────────────────────────── Gegner in Tiled-Maps ───────────────────────────

    /**
     * Verteilt Gegner auf den Plattformen/Böden einer frisch geladenen Tiled-Map.
     * Die ersten 300 px bleiben frei, damit der Spieler nicht direkt beim Betreten getroffen wird.
     */
    public void populateTmx(List<Entity> entities, double offsetX, double difficulty) {
        this.ox = 0; // Positionen der Entities sind bereits Welt-Koordinaten
        this.difficulty = clamp01(difficulty);

        List<Entity> floors = new ArrayList<>(), platforms = new ArrayList<>();
        for (Entity e : entities) {
            if (e.getX() < offsetX + 300) continue;
            if (e.isType(EntityType.Ground) && e.getWidth() >= 220) floors.add(e);
            else if (e.isType(EntityType.Platform) && e.getWidth() >= 110 && e.getY() > 300) platforms.add(e);
        }

        int count = 1 + (difficulty > 0.35 ? 1 : 0) + (difficulty > 0.75 ? 1 : 0);
        if (chance(0.25)) count--;
        for (int i = 0; i < count; i++) {
            int kind = ThreadLocalRandom.current().nextInt(3);
            if (kind == 0 && !floors.isEmpty()) {
                Entity f = floors.remove(ThreadLocalRandom.current().nextInt(floors.size()));
                double l = Math.max(f.getX(), offsetX + 300);
                brute(l + 10, f.getX() + f.getWidth() - 10, f.getY());
            } else if (kind == 1 && !platforms.isEmpty()) {
                Entity p = platforms.remove(ThreadLocalRandom.current().nextInt(platforms.size()));
                sniper(p.getX() + p.getWidth() / 2 - 20, p.getY());
            } else {
                zapper(offsetX + range(500, 1700), range(560, 820));
            }
        }
    }

    // ─────────────────────────── Spawn-Helfer ───────────────────────────

    private double ground(double x, double len) {
        if (len > 1) {
            spawn("groundBlock", new SpawnData(ox + x, FLOOR_Y)
                    .put("width", (int) Math.ceil(len)).put("height", (int) FLOOR_H));
        }
        return x + len;
    }

    private void platform(String type, double x, double top, double w) {
        spawn(type, new SpawnData(ox + x, top).put("width", (int) w).put("height", PLATFORM_H));
    }

    private void moving(double x, double top, double w, double ampX, double ampY, double period) {
        spawn("movingPlatform", new SpawnData(ox + x, top)
                .put("width", (int) w).put("height", PLATFORM_H)
                .put("ampX", ampX).put("ampY", ampY)
                .put("period", period).put("phase", range(0, Math.PI * 2)));
    }

    private void credit(int tier, double x, double y) {
        spawn("Credit" + tier, new SpawnData(ox + x, y).put("width", 48).put("height", 48));
    }

    /** Brute patrouilliert zwischen left und right (Welt-/Segment-Koordinaten) auf Höhe surfaceTop. */
    private void brute(double left, double right, double surfaceTop) {
        EnemyKind k = EnemyKind.BRUTE;
        if (right - left < k.getFrameW() + 60) return;
        double minX = ox + left, maxX = ox + right - k.getFrameW();
        spawn("enemyBrute", new SpawnData(range(minX, maxX), surfaceTop - k.getFrameH() + 1)
                .put("minX", minX).put("maxX", maxX));
    }

    private void sniper(double x, double surfaceTop) {
        EnemyKind k = EnemyKind.SNIPER;
        spawn("enemySniper", new SpawnData(ox + x, surfaceTop - k.getFrameH() + 1)
                .put("minX", ox + x).put("maxX", ox + x));
    }

    private void zapper(double centerX, double y) {
        EnemyKind k = EnemyKind.ZAPPER;
        double x = ox + centerX - k.getFrameW() / 2.0;
        spawn("enemyZapper", new SpawnData(x, y).put("minX", x - 300).put("maxX", x + 300));
    }

    private static double range(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private static boolean chance(double p) {
        return ThreadLocalRandom.current().nextDouble() < p;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
