package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

/** Kleine Helfer für Kampf-Abfragen (Gegner suchen, Gelände treffen, Kamera-Bereich). */
public final class Combat {

    /** Alles, woran Bomben explodieren. */
    private static final EntityType[] TERRAIN = {
            EntityType.Ground, EntityType.Platform, EntityType.MovingPlatform,
            EntityType.CrumblePlatform, EntityType.BouncePad, EntityType.Conveyor
    };

    private Combat() {}

    public static Entity player() {
        List<Entity> list = FXGL.getGameWorld().getEntitiesByType(EntityType.Player);
        return list.isEmpty() ? null : list.get(0);
    }

    public static PlayerComponent playerComponent() {
        Entity p = player();
        return p == null ? null : p.getComponent(PlayerComponent.class);
    }

    /** Kopie der Gegner-Liste (Gegner können während der Iteration sterben). */
    public static List<Entity> enemies() {
        return new ArrayList<>(FXGL.getGameWorld().getEntitiesByType(EntityType.Enemy));
    }

    /** Liefert das EnemyComponent (Unterklasse) eines Gegners – FXGL sucht Komponenten nach exakter Klasse. */
    public static EnemyComponent enemyOf(Entity e) {
        for (var c : e.getComponents()) {
            if (c instanceof EnemyComponent ec) return ec;
        }
        return null;
    }

    public static Point2D center(Entity e) {
        var b = e.getBoundingBoxComponent();
        return new Point2D((b.getMinXWorld() + b.getMaxXWorld()) / 2, (b.getMinYWorld() + b.getMaxYWorld()) / 2);
    }

    public static boolean hitsTerrain(double x, double y) {
        for (Entity e : FXGL.getGameWorld().getEntitiesByType(TERRAIN)) {
            var b = e.getBoundingBoxComponent();
            if (x >= b.getMinXWorld() && x <= b.getMaxXWorld() && y >= b.getMinYWorld() && y <= b.getMaxYWorld()) {
                return true;
            }
        }
        return false;
    }

    /** true, wenn x (Welt) gerade auf dem Bildschirm ist (mit Rand). */
    public static boolean onScreen(double x, double margin) {
        double camX = FXGL.getGameScene().getViewport().getX();
        return x > camX - margin && x < camX + FXGL.getAppWidth() + margin;
    }
}
