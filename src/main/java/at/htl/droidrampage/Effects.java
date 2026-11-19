package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleConsumer;

/**
 * Kurzlebige, rein optische Effekte (Blitze, Explosionen, Funken, Schadens-Zahlen).
 * Jeder Effekt ist ein eigenes Entity, das sich nach Ablauf seiner Lebenszeit selbst entfernt.
 */
public final class Effects {

    private Effects() {}

    /** Spielt eine Animation über 'life' Sekunden ab; anim bekommt den Fortschritt 0..1. */
    public static class EffectComponent extends Component {
        private final double life;
        private final DoubleConsumer anim;
        private double t;

        public EffectComponent(double life, DoubleConsumer anim) {
            this.life = life;
            this.anim = anim;
        }

        @Override
        public void onUpdate(double tpf) {
            t += tpf;
            double p = Math.min(1, t / life);
            if (anim != null) anim.accept(p);
            if (p >= 1) entity.removeFromWorld();
        }
    }

    private static Entity spawn(double x, double y, Node view, double life, DoubleConsumer anim) {
        return FXGL.entityBuilder()
                .at(x, y)
                .type(EntityType.Effect)
                .view(view)
                .zIndex(200)
                .with(new EffectComponent(life, anim))
                .buildAndAttach();
    }

    private static double rnd(double a, double b) {
        return ThreadLocalRandom.current().nextDouble(a, b);
    }

    // ─────────────────────────── Effekte ───────────────────────────

    /** Zackiger Blitz von (x1,y1) nach (x2,y2). */
    public static void lightning(double x1, double y1, double x2, double y2, Color color) {
        Polyline glow = new Polyline();
        Polyline core = new Polyline();
        rebuildBolt(glow, core, x2 - x1, y2 - y1);
        glow.setStroke(color.deriveColor(0, 1, 1, 0.55));
        glow.setStrokeWidth(7);
        glow.setEffect(new GaussianBlur(5));
        glow.setBlendMode(BlendMode.ADD);
        core.setStroke(Color.WHITE);
        core.setStrokeWidth(2);
        core.setEffect(new DropShadow(10, color));
        Group g = new Group(glow, core);
        spawn(x1, y1, g, 0.18, p -> {
            if (ThreadLocalRandom.current().nextDouble() < 0.5) rebuildBolt(glow, core, x2 - x1, y2 - y1);
            g.setOpacity(1 - p * p);
        });
    }

    private static void rebuildBolt(Polyline glow, Polyline core, double dx, double dy) {
        glow.getPoints().clear();
        core.getPoints().clear();
        int segments = 7;
        double len = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / len, ny = dx / len; // Normale
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double off = (i == 0 || i == segments) ? 0 : rnd(-14, 14);
            double px = dx * t + nx * off, py = dy * t + ny * off;
            glow.getPoints().addAll(px, py);
            core.getPoints().addAll(px, py);
        }
    }

    /** Explosion mit Druckwelle und Blitz. */
    public static void explosion(double cx, double cy, double radius) {
        Circle flash = new Circle(0, 0, radius * 0.6, Color.web("#fff3c4"));
        flash.setEffect(new GaussianBlur(12));
        flash.setBlendMode(BlendMode.ADD);
        Circle fire = new Circle(0, 0, radius * 0.5, Color.web("#ff8a2a"));
        fire.setEffect(new GaussianBlur(8));
        fire.setBlendMode(BlendMode.ADD);
        Circle ring = new Circle(0, 0, radius * 0.3);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#ffd24a"));
        ring.setStrokeWidth(5);
        ring.setEffect(new DropShadow(14, Color.web("#ff8a2a")));
        Group g = new Group(fire, flash, ring);
        spawn(cx, cy, g, 0.45, p -> {
            ring.setRadius(radius * (0.3 + 0.75 * p));
            ring.setOpacity(1 - p);
            fire.setRadius(radius * (0.5 + 0.4 * p));
            fire.setOpacity(1 - p);
            flash.setOpacity(Math.max(0, 1 - p * 3));
        });
        sparks(cx, cy, Color.web("#ffb04a"), 12);
    }

    /** Krallenhieb-Bogen vor dem Droiden. */
    public static void slash(double cx, double cy, int facing, Color color) {
        Arc arc = new Arc(0, 0, 52, 40, facing > 0 ? -70 : 110, 140);
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStroke(color);
        arc.setStrokeWidth(6);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setEffect(new DropShadow(14, color));
        spawn(cx, cy, arc, 0.16, p -> {
            arc.setOpacity(1 - p);
            arc.setStrokeWidth(6 * (1 - p) + 1);
            arc.setScaleX(1 + 0.3 * p);
            arc.setScaleY(1 + 0.3 * p);
        });
    }

    /** Mündungsfeuer / kleiner Lichtblitz. */
    public static void muzzle(double cx, double cy, Color color) {
        Circle c = new Circle(0, 0, 10, color);
        c.setEffect(new GaussianBlur(6));
        c.setBlendMode(BlendMode.ADD);
        spawn(cx, cy, c, 0.1, p -> {
            c.setRadius(10 + 8 * p);
            c.setOpacity(1 - p);
        });
    }

    /** Funken, die in alle Richtungen fliegen. */
    public static void sparks(double cx, double cy, Color color, int count) {
        Group g = new Group();
        double[] vx = new double[count], vy = new double[count];
        for (int i = 0; i < count; i++) {
            Circle s = new Circle(0, 0, rnd(1.5, 3), color);
            g.getChildren().add(s);
            double a = rnd(0, Math.PI * 2), sp = rnd(120, 340);
            vx[i] = Math.cos(a) * sp;
            vy[i] = Math.sin(a) * sp - 80;
        }
        g.setEffect(new DropShadow(8, color));
        double life = 0.4;
        spawn(cx, cy, g, life, p -> {
            double t = p * life;
            for (int i = 0; i < count; i++) {
                Node n = g.getChildren().get(i);
                n.setTranslateX(vx[i] * t);
                n.setTranslateY(vy[i] * t + 600 * t * t);
            }
            g.setOpacity(1 - p);
        });
    }

    /** Aufsteigender Text (z.B. "+40"). */
    public static void floatingText(double cx, double cy, String text, Color color) {
        Text t = new Text(text);
        t.setFont(Font.font(StartMenu.FONT, FontWeight.BOLD, 26));
        t.setFill(color);
        t.setEffect(new DropShadow(10, color));
        t.setTranslateX(-text.length() * 7.5);
        spawn(cx, cy, t, 0.9, p -> {
            t.setTranslateY(-50 * p);
            t.setOpacity(1 - p * p);
        });
    }
}
