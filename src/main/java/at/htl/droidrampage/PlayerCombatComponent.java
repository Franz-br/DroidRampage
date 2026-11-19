package at.htl.droidrampage;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Angriffe der Droiden – jeder Droide kämpft anders:
 *  ASTRO: Taser – Kurzstrecken-Blitz, springt auf bis zu 3 Gegner über und betäubt sie
 *  GRUMP: Klaue (Nahkampf, wehrt Gegner-Schüsse ab) + Bombe (Spezial-Taste)
 *  PROTO: Blaster – schneller Fernkampf-Schuss
 */
public class PlayerCombatComponent extends Component {

    // ASTRO – Taser
    private static final double TASER_RANGE = 210;
    private static final double TASER_CHAIN = 180;
    private static final int TASER_TARGETS = 3;
    private static final double TASER_COOLDOWN = 0.55;

    // GRUMP – Klaue + Bombe
    private static final double CLAW_RANGE = 90;
    private static final double CLAW_COOLDOWN = 0.32;
    private static final double BOMB_COOLDOWN = 1.1;

    // PROTO – Blaster
    private static final double BLASTER_SPEED = 1400;
    private static final double BLASTER_COOLDOWN = 0.26;

    private final DroidSkin skin;
    private double cooldown, specialCooldown;

    public PlayerCombatComponent(DroidSkin skin) {
        this.skin = skin;
    }

    @Override
    public void onUpdate(double tpf) {
        cooldown = Math.max(0, cooldown - tpf);
        specialCooldown = Math.max(0, specialCooldown - tpf);
    }

    private int facing() {
        return entity.getComponent(PlayerComponent.class).getFacing();
    }

    private Point2D hand() {
        return Combat.center(entity).add(facing() * 16, -4);
    }

    /** Haupt-Angriff (F / J). */
    public void attack() {
        if (cooldown > 0) return;
        switch (skin) {
            case ASTRO: taser(); cooldown = TASER_COOLDOWN; break;
            case GRUMP: claw(); cooldown = CLAW_COOLDOWN; break;
            case PROTO: blaster(); cooldown = BLASTER_COOLDOWN; break;
        }
    }

    /** Spezial-Angriff (G / K) – nur GRUMP hat einen: Bombe werfen. */
    public void special() {
        if (skin != DroidSkin.GRUMP || specialCooldown > 0) return;
        int f = facing();
        Point2D h = hand().add(0, -10);
        double pvx = entity.getComponent(PhysicsComponent.class).getVelocityX();
        // Flugweite ca. 300 px auf gleicher Höhe – trifft Gegner auf mittlerer Distanz
        ProjectileComponent.spawnBomb(h.getX(), h.getY(), f * 370 + pvx * 0.3, -440,
                ProjectileComponent.Owner.PLAYER, 130, 3, null);
        specialCooldown = BOMB_COOLDOWN;
    }

    // ─────────────────────────── Angriffe ───────────────────────────

    private void taser() {
        int f = facing();
        Point2D origin = hand();
        Color arc = Color.web("#7fd8ff");

        // Erstes Ziel: nächster Gegner vor dem Droiden
        List<Entity> candidates = new ArrayList<>();
        for (Entity e : Combat.enemies()) {
            Point2D c = Combat.center(e);
            double ahead = (c.getX() - origin.getX()) * f;
            if (ahead > -20 && ahead < TASER_RANGE && Math.abs(c.getY() - origin.getY()) < 100) {
                candidates.add(e);
            }
        }
        if (candidates.isEmpty()) {
            Effects.lightning(origin.getX(), origin.getY(), origin.getX() + f * 140, origin.getY() + 10, arc);
            return;
        }
        candidates.sort(Comparator.comparingDouble(e -> Combat.center(e).distance(origin)));

        // Kettenblitz: vom letzten Ziel zum nächstgelegenen noch nicht getroffenen Gegner
        List<Entity> hit = new ArrayList<>();
        Entity current = candidates.get(0);
        Point2D from = origin;
        while (current != null && hit.size() < TASER_TARGETS) {
            hit.add(current);
            Point2D to = Combat.center(current);
            Effects.lightning(from.getX(), from.getY(), to.getX(), to.getY(), arc);
            from = to;

            Entity next = null;
            double best = TASER_CHAIN;
            for (Entity e : Combat.enemies()) {
                if (hit.contains(e)) continue;
                double d = Combat.center(e).distance(from);
                if (d < best) {
                    best = d;
                    next = e;
                }
            }
            current = next;
        }
        for (Entity e : hit) {
            EnemyComponent ec = Combat.enemyOf(e);
            if (ec != null) ec.damage(2, 1.4, f * 6);
        }
    }

    private void claw() {
        int f = facing();
        Point2D c = Combat.center(entity);
        Effects.slash(c.getX() + f * 34, c.getY() + 4, f, Color.web("#ff8a2a"));

        for (Entity e : Combat.enemies()) {
            Point2D ec = Combat.center(e);
            double ahead = (ec.getX() - c.getX()) * f;
            if (ahead > -15 && ahead < CLAW_RANGE + 20 && Math.abs(ec.getY() - c.getY()) < 60) {
                EnemyComponent comp = Combat.enemyOf(e);
                if (comp != null) comp.damage(2, 0.25, f * 40);
            }
        }
        // Gegnerische Geschosse in Reichweite abwehren
        for (Entity p : new ArrayList<>(com.almasb.fxgl.dsl.FXGL.getGameWorld().getEntitiesByType(EntityType.Projectile))) {
            var pc = p.getComponent(ProjectileComponent.class);
            if (pc.getOwner() != ProjectileComponent.Owner.ENEMY) continue;
            double ahead = (p.getX() - c.getX()) * f;
            if (ahead > -15 && ahead < CLAW_RANGE + 15 && Math.abs(p.getY() - c.getY()) < 60) {
                Effects.sparks(p.getX(), p.getY(), Color.web("#ffb04a"), 8);
                p.removeFromWorld();
            }
        }
    }

    private void blaster() {
        int f = facing();
        Point2D h = hand().add(f * 6, -2);
        Effects.muzzle(h.getX(), h.getY(), Color.web("#ffd24a"));
        ProjectileComponent.spawnBolt(h.getX(), h.getY(), f * BLASTER_SPEED, 0,
                ProjectileComponent.Owner.PLAYER, 1, 1.0, null);
    }

    public double getCooldown() {
        return cooldown;
    }
}
