package at.htl.droidrampage;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

/**
 * Erzeugt die Grafiken für die prozedural generierten Plattformen.
 * Alles wird mit JavaFX-Formen gezeichnet, damit beliebige Breiten sauber aussehen
 * (keine gestreckten Texturen) und der Neon-Stil der Tilemaps aufgegriffen wird.
 */
public final class PlatformVisuals {

    public static final Color NEON_CYAN    = Color.web("#5ff4ff");
    public static final Color NEON_MAGENTA = Color.web("#ff4fd8");
    public static final Color NEON_ORANGE  = Color.web("#ffa13d");
    public static final Color NEON_GREEN   = Color.web("#5dff8f");
    public static final Color NEON_RED     = Color.web("#ff3048");

    private static final Color METAL_TOP = Color.web("#2f3a5a");
    private static final Color METAL_BOTTOM = Color.web("#121729");
    private static final Color EDGE = Color.web("#070912");

    private PlatformVisuals() {}

    // ─────────────────────────── Basis-Bauteile ───────────────────────────

    private static Rectangle body(double w, double h, Color top, Color bottom) {
        Rectangle r = new Rectangle(w, h);
        r.setArcWidth(8);
        r.setArcHeight(8);
        r.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        r.setStroke(EDGE);
        r.setStrokeWidth(1.5);
        return r;
    }

    private static Rectangle lightStrip(double x, double y, double w, double h, Color accent) {
        Rectangle strip = new Rectangle(x, y, w, h);
        strip.setArcWidth(h);
        strip.setArcHeight(h);
        strip.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.45, accent), new Stop(1, accent.darker())));
        DropShadow glow = new DropShadow(14, accent);
        glow.setSpread(0.35);
        strip.setEffect(glow);
        return strip;
    }

    private static void rivets(Group g, double w, double y) {
        for (double x = 10; x < w - 6; x += 26) {
            Circle c = new Circle(x, y, 1.8, Color.web("#56638a"));
            g.getChildren().add(c);
        }
    }

    /** Dezente Unterseite, damit Plattformen nicht "flach" wirken. */
    private static Rectangle underShadow(double w, double h) {
        Rectangle s = new Rectangle(4, h, w - 8, 6);
        s.setFill(Color.rgb(0, 0, 0, 0.35));
        s.setEffect(new GaussianBlur(4));
        return s;
    }

    // ─────────────────────────── Plattform-Typen ───────────────────────────

    /** Normale, feste Plattform (cyan). */
    public static Node solid(double w, double h) {
        Group g = new Group(underShadow(w, h), body(w, h, METAL_TOP, METAL_BOTTOM));
        g.getChildren().add(lightStrip(4, 2, w - 8, 4, NEON_CYAN));
        rivets(g, w, h - 6);
        for (double x = 48; x < w - 20; x += 48) {
            g.getChildren().add(seam(x, 8, h - 3));
        }
        return g;
    }

    /** Bewegliche Plattform (magenta) mit Pfeilen und Düsen. */
    public static Node moving(double w, double h, boolean vertical) {
        Group g = new Group(underShadow(w, h));

        // Düsen-Flammen unter den Enden
        for (double x : new double[]{10, w - 22}) {
            Rectangle flame = new Rectangle(x, h - 2, 12, 10);
            flame.setArcWidth(10);
            flame.setArcHeight(10);
            flame.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#fff3c4")), new Stop(0.5, NEON_ORANGE), new Stop(1, Color.TRANSPARENT)));
            flame.setEffect(new DropShadow(10, NEON_ORANGE));
            flame.setBlendMode(BlendMode.ADD);
            g.getChildren().add(flame);
        }

        g.getChildren().add(body(w, h, Color.web("#3a2c58"), Color.web("#160f27")));
        g.getChildren().add(lightStrip(4, 2, w - 8, 4, NEON_MAGENTA));

        // Richtungs-Pfeile
        double cx = w / 2, cy = h / 2 + 3;
        for (int i = -1; i <= 1; i += 2) {
            // vertikal: ^ oben und v unten  ·  horizontal: < links und > rechts
            Polyline arrow = vertical
                    ? new Polyline(cx - 6, cy + i * 2, cx, cy + i * 8, cx + 6, cy + i * 2)
                    : new Polyline(cx + i * 6, cy - 6, cx + i * 12, cy, cx + i * 6, cy + 6);
            arrow.setStroke(NEON_MAGENTA);
            arrow.setStrokeWidth(2.2);
            arrow.setEffect(new DropShadow(6, NEON_MAGENTA));
            g.getChildren().add(arrow);
        }
        rivets(g, w, h - 6);
        return g;
    }

    /** Bröckelnde Plattform (orange) mit Rissen. */
    public static Node crumble(double w, double h) {
        Group g = new Group(underShadow(w, h), body(w, h, Color.web("#4a3426"), Color.web("#1d140e")));
        g.getChildren().add(lightStrip(4, 2, w - 8, 3, NEON_ORANGE));

        // Segment-Fugen
        for (double x = 24; x < w - 8; x += 24) {
            g.getChildren().add(seam(x, 6, h - 2));
        }
        // Risse
        Polyline crack1 = new Polyline(w * 0.22, 6, w * 0.30, h * 0.45, w * 0.24, h * 0.7, w * 0.33, h - 2);
        Polyline crack2 = new Polyline(w * 0.68, 6, w * 0.62, h * 0.4, w * 0.72, h * 0.62, w * 0.66, h - 2);
        for (Polyline c : new Polyline[]{crack1, crack2}) {
            c.setStroke(Color.web("#0b0705"));
            c.setStrokeWidth(1.6);
            g.getChildren().add(c);
        }
        return g;
    }

    /** Sprungplattform (grün) mit Feder. */
    public static Node bouncePad(double w, double h) {
        Group g = new Group();

        Rectangle base = new Rectangle(0, h - 6, w, 6);
        base.setFill(Color.web("#1a2233"));
        base.setStroke(EDGE);

        // Feder (Zickzack)
        Polyline spring = new Polyline();
        int coils = 5;
        for (int i = 0; i <= coils * 2; i++) {
            double x = w / 2 + (i % 2 == 0 ? -w * 0.28 : w * 0.28);
            double y = h - 6 - (h - 12) * i / (coils * 2.0);
            spring.getPoints().addAll(x, y);
        }
        spring.setStroke(Color.web("#9aa6c8"));
        spring.setStrokeWidth(2);

        Rectangle pad = new Rectangle(0, 0, w, 7);
        pad.setArcWidth(7);
        pad.setArcHeight(7);
        pad.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#e7ffe9")), new Stop(0.4, NEON_GREEN), new Stop(1, Color.web("#1d7a3c"))));
        DropShadow glow = new DropShadow(18, NEON_GREEN);
        glow.setSpread(0.3);
        pad.setEffect(glow);

        // Pfeile nach oben
        Group arrows = new Group();
        for (int i = 0; i < 2; i++) {
            Polyline a = new Polyline(w / 2 - 8, -10 - i * 9, w / 2, -18 - i * 9, w / 2 + 8, -10 - i * 9);
            a.setStroke(NEON_GREEN);
            a.setStrokeWidth(2.5);
            a.setOpacity(0.85 - i * 0.35);
            arrows.getChildren().add(a);
        }
        arrows.setEffect(new DropShadow(8, NEON_GREEN));

        g.getChildren().addAll(base, spring, pad, arrows);
        return g;
    }

    /** Boden-Block wie in den Tilemaps (blaues Metall). */
    public static Node ground(double w, double h) {
        Group g = new Group();
        Rectangle r = new Rectangle(w, h);
        r.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2b3a5c")), new Stop(0.25, Color.web("#1c2640")), new Stop(1, Color.web("#0b0f1c"))));
        g.getChildren().add(r);
        g.getChildren().add(lightStrip(0, 0, w, 3, Color.web("#7fb8ff")));
        for (double x = 32; x < w; x += 64) {
            g.getChildren().add(seam(x, 6, h));
        }
        for (double x = 16; x < w - 8; x += 64) {
            Rectangle vent = new Rectangle(x, 16, 28, 4);
            vent.setFill(Color.web("#0a0e1a"));
            g.getChildren().add(vent);
        }
        return g;
    }

    /**
     * Laser-Tor: Emitter oben und unten, Strahl dazwischen.
     * Strahl und Warn-Linie werden separat zurückgegeben, damit das LaserComponent sie umschalten kann.
     */
    public static LaserView laser(double h) {
        double w = 28;
        Group g = new Group();

        Rectangle beamOuter = new Rectangle(w / 2 - 7, 18, 14, h - 36);
        beamOuter.setFill(NEON_RED.deriveColor(0, 1, 1, 0.35));
        beamOuter.setEffect(new GaussianBlur(8));
        beamOuter.setBlendMode(BlendMode.ADD);

        Rectangle beamCore = new Rectangle(w / 2 - 2, 18, 4, h - 36);
        beamCore.setFill(Color.web("#ffe3e6"));
        DropShadow coreGlow = new DropShadow(16, NEON_RED);
        coreGlow.setSpread(0.5);
        beamCore.setEffect(coreGlow);

        Group beam = new Group(beamOuter, beamCore);

        Rectangle warn = new Rectangle(w / 2 - 1, 18, 2, h - 36);
        warn.setFill(NEON_RED.deriveColor(0, 1, 1, 0.5));
        warn.getStrokeDashArray().addAll(6.0, 8.0);
        warn.setVisible(false);

        g.getChildren().addAll(beam, warn, emitter(w, 0, true), emitter(w, h - 20, false));
        return new LaserView(g, beam, warn);
    }

    private static Node emitter(double w, double y, boolean top) {
        Group e = new Group();
        Rectangle box = new Rectangle(0, y, w, 20);
        box.setArcWidth(6);
        box.setArcHeight(6);
        box.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#1c2236")), new Stop(0.5, Color.web("#3c4766")), new Stop(1, Color.web("#151a2a"))));
        box.setStroke(EDGE);
        Polygon nozzle = top
                ? new Polygon(w / 2 - 6, y + 20, w / 2 + 6, y + 20, w / 2 + 3, y + 24, w / 2 - 3, y + 24)
                : new Polygon(w / 2 - 6, y, w / 2 + 6, y, w / 2 + 3, y - 4, w / 2 - 3, y - 4);
        nozzle.setFill(Color.web("#5a6488"));
        Circle led = new Circle(w / 2, y + 10, 3, NEON_RED);
        led.setEffect(new DropShadow(8, NEON_RED));
        e.getChildren().addAll(box, nozzle, led);
        return e;
    }

    // ─────────────────────────── Objekte aus den Tiled-Maps ───────────────────────────

    public static final Color NEON_VIOLET = Color.web("#a77bff");

    /** Metallkiste (für kleine "Boden"-Blöcke in den Tiled-Maps). */
    public static Node crate(double w, double h) {
        Group g = new Group(underShadow(w, h), body(w, h, Color.web("#2a3350"), Color.web("#111628")));
        Rectangle inner = new Rectangle(6, 6, w - 12, h - 12);
        inner.setFill(Color.TRANSPARENT);
        inner.setStroke(Color.web("#3d4a72"));
        inner.setStrokeWidth(2);
        Line d1 = new Line(8, 8, w - 8, h - 8);
        Line d2 = new Line(w - 8, 8, 8, h - 8);
        for (Line l : new Line[]{d1, d2}) {
            l.setStroke(Color.web("#2c3656"));
            l.setStrokeWidth(3);
        }
        Rectangle lamp = new Rectangle(w / 2 - 10, h / 2 - 3, 20, 6);
        lamp.setArcWidth(6);
        lamp.setArcHeight(6);
        lamp.setFill(Color.web("#ffd24a"));
        lamp.setEffect(new DropShadow(10, Color.web("#ffb000")));
        g.getChildren().addAll(inner, d1, d2, lightStrip(4, 2, w - 8, 3, NEON_CYAN), lamp);
        rivets(g, w, h - 6);
        return g;
    }

    /** Elektro-Stacheln (tödliche "Death"-Zonen). Stacheln stehen auf der Unterkante. */
    public static Node spikes(double w, double h) {
        Group g = new Group();
        double sh = Math.min(h, 22);
        Rectangle base = new Rectangle(0, h - 6, w, 6);
        base.setFill(Color.web("#2a1420"));
        base.setStroke(EDGE);
        Group teeth = new Group();
        int n = Math.max(2, (int) (w / 14));
        double tw = w / n;
        for (int i = 0; i < n; i++) {
            Polygon t = new Polygon(i * tw + 1, h - 5, i * tw + tw / 2, h - sh, (i + 1) * tw - 1, h - 5);
            t.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#5a1a28")), new Stop(0.7, NEON_RED), new Stop(1, Color.web("#ffe0a0"))));
            teeth.getChildren().add(t);
        }
        DropShadow glow = new DropShadow(12, NEON_RED);
        glow.setSpread(0.2);
        teeth.setEffect(glow);
        g.getChildren().addAll(teeth, base);
        return g;
    }

    /** Holo-Säule (Deko-Objekt "Item1"). */
    public static Node holoPillar(double w, double h) {
        Group g = new Group();
        Rectangle beam = new Rectangle(w * 0.2, 10, w * 0.6, h - 26);
        beam.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.4, NEON_CYAN.deriveColor(0, 1, 1, 0.35)),
                new Stop(1, NEON_CYAN.deriveColor(0, 1, 1, 0.6))));
        beam.setEffect(new GaussianBlur(3));
        for (double y = 18; y < h - 20; y += 8) {
            Line scan = new Line(w * 0.22, y, w * 0.78, y);
            scan.setStroke(NEON_CYAN.deriveColor(0, 1, 1, 0.25));
            g.getChildren().add(scan);
        }
        Polygon gem = new Polygon(w / 2, 6, w / 2 + 12, 24, w / 2, 42, w / 2 - 12, 24);
        gem.setFill(NEON_CYAN.deriveColor(0, 1, 1, 0.75));
        gem.setEffect(new DropShadow(20, NEON_CYAN));
        Rectangle base = new Rectangle(0, h - 18, w, 18);
        base.setArcWidth(6);
        base.setArcHeight(6);
        base.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, METAL_TOP), new Stop(1, METAL_BOTTOM)));
        base.setStroke(EDGE);
        g.getChildren().addAll(0, java.util.List.of(beam));
        g.getChildren().addAll(gem, base, lightStrip(4, h - 17, w - 8, 3, NEON_CYAN));
        return g;
    }

    // ─────────────────────────── Weitere Plattform-Typen ───────────────────────────

    /** Phasen-Plattform (violett) – verschwindet periodisch. */
    public static Node phase(double w, double h) {
        Group g = new Group(underShadow(w, h), body(w, h, Color.web("#30264f"), Color.web("#130e24")));
        g.getChildren().add(lightStrip(4, 2, w - 8, 4, NEON_VIOLET));
        for (double x = 12; x < w - 10; x += 18) {
            Polygon hex = new Polygon(x, h / 2 + 3, x + 4, h / 2 - 2, x + 10, h / 2 - 2, x + 14, h / 2 + 3,
                    x + 10, h / 2 + 8, x + 4, h / 2 + 8);
            hex.setFill(Color.TRANSPARENT);
            hex.setStroke(NEON_VIOLET.deriveColor(0, 1, 1, 0.55));
            g.getChildren().add(hex);
        }
        return g;
    }

    /** Umriss, der angezeigt wird, während eine Phasen-Plattform weg ist. */
    public static Node phaseGhost(double w, double h) {
        Rectangle r = new Rectangle(w, h);
        r.setArcWidth(8);
        r.setArcHeight(8);
        r.setFill(NEON_VIOLET.deriveColor(0, 1, 1, 0.08));
        r.setStroke(NEON_VIOLET.deriveColor(0, 1, 1, 0.6));
        r.getStrokeDashArray().addAll(8.0, 6.0);
        r.setStrokeWidth(1.5);
        return r;
    }

    /** Förderband. Gibt Wurzel und das animierte Pfeil-Band zurück. */
    public static ConveyorView conveyor(double w, double h, boolean right) {
        Group g = new Group(body(w, h, Color.web("#2b3048"), Color.web("#0f1220")));
        Rectangle belt = new Rectangle(4, 3, w - 8, 12);
        belt.setFill(Color.web("#141826"));
        Group arrows = new Group();
        Color c = Color.web("#ffd24a");
        for (double x = -40; x < w + 40; x += 28) {
            Polyline a = right
                    ? new Polyline(x, 5, x + 6, 9, x, 13)
                    : new Polyline(x + 6, 5, x, 9, x + 6, 13);
            a.setStroke(c);
            a.setStrokeWidth(2.2);
            arrows.getChildren().add(a);
        }
        arrows.setEffect(new DropShadow(6, c));
        Group clipHolder = new Group(arrows);
        clipHolder.setClip(new Rectangle(4, 3, w - 8, 12));
        for (double x = 14; x < w; x += 40) {
            Circle wheel = new Circle(x, h - 14, 6, Color.web("#3a4466"));
            wheel.setStroke(EDGE);
            g.getChildren().add(wheel);
        }
        g.getChildren().addAll(belt, clipHolder);
        return new ConveyorView(g, arrows);
    }

    /** Kreissäge (Gefahr) – das zurückgegebene Node wird vom SawComponent gedreht. */
    public static Node sawBlade(double r) {
        Group g = new Group();
        Polygon teeth = new Polygon();
        int n = 14;
        for (int i = 0; i < n * 2; i++) {
            double a = Math.PI * i / n;
            double rr = i % 2 == 0 ? r : r * 0.78;
            teeth.getPoints().addAll(r + Math.cos(a) * rr, r + Math.sin(a) * rr);
        }
        teeth.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#e6ecff")), new Stop(1, Color.web("#7a86a8"))));
        teeth.setStroke(EDGE);
        Circle hub = new Circle(r, r, r * 0.35, Color.web("#2a3048"));
        Circle led = new Circle(r, r, r * 0.14, NEON_RED);
        led.setEffect(new DropShadow(8, NEON_RED));
        g.getChildren().addAll(teeth, hub, led);
        g.setEffect(new DropShadow(10, Color.rgb(255, 60, 60, 0.6)));
        return g;
    }

    /** Schiene, auf der eine Säge fährt. */
    public static Node sawRail(double w) {
        Group g = new Group();
        Rectangle rail = new Rectangle(0, 0, w, 6);
        rail.setArcWidth(6);
        rail.setArcHeight(6);
        rail.setFill(Color.web("#3a4466"));
        rail.setStroke(EDGE);
        g.getChildren().add(rail);
        for (double x : new double[]{0, w - 8}) {
            Rectangle post = new Rectangle(x, 0, 8, 40);
            post.setFill(Color.web("#262d45"));
            post.setStroke(EDGE);
            g.getChildren().add(post);
        }
        return g;
    }

    public record ConveyorView(Group root, Node arrows) {}

    private static Line seam(double x, double y0, double y1) {
        Line l = new Line(x, y0, x, y1);
        l.setStroke(Color.rgb(0, 0, 0, 0.45));
        l.setStrokeWidth(1.2);
        return l;
    }

    /** Bündelt die Laser-Knoten, damit das LaserComponent Strahl und Warnung umschalten kann. */
    public record LaserView(Group root, Node beam, Node warning) {}
}
