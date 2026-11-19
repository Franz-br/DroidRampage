package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Geschosse von Spieler und Gegnern: Blaster-Bolzen (gerade Flugbahn)
 * und Bomben (Wurfparabel, explodieren bei Kontakt oder nach Ablauf der Zündschnur).
 */
public class ProjectileComponent extends Component {

    public enum Owner { PLAYER, ENEMY }

    public static final double BOMB_GRAVITY = 1100;

    private final Owner owner;
    private double vx, vy;
    private final double gravity;
    private double life;
    private final int damage;
    private final double radius;     // > 0 = explodiert
    private final String killMessage;
    private final Node spin;         // dreht sich (Bombe) oder null

    private ProjectileComponent(Owner owner, double vx, double vy, double gravity, double life,
                                int damage, double radius, String killMessage, Node spin) {
        this.owner = owner;
        this.vx = vx;
        this.vy = vy;
        this.gravity = gravity;
        this.life = life;
        this.damage = damage;
        this.radius = radius;
        this.killMessage = killMessage;
        this.spin = spin;
    }

    // ─────────────────────────── Erzeugen ───────────────────────────

    /** Blaster-Bolzen. Position = Mittelpunkt. */
    public static Entity spawnBolt(double x, double y, double vx, double vy, Owner owner, int damage,
                                   double life, String killMessage) {
        Color color = owner == Owner.PLAYER ? Color.web("#ffd24a") : Color.web("#ff3048");
        Rectangle bolt = new Rectangle(-15, -3, 30, 6);
        bolt.setArcWidth(6);
        bolt.setArcHeight(6);
        bolt.setFill(Color.WHITE);
        DropShadow glow = new DropShadow(14, color);
        glow.setSpread(0.6);
        bolt.setEffect(glow);
        bolt.setRotate(Math.toDegrees(Math.atan2(vy, vx)));
        Group view = new Group(bolt);

        return FXGL.entityBuilder()
                .at(x, y)
                .type(EntityType.Projectile)
                .view(view)
                .bbox(new HitBox(new Point2D(-6, -6), BoundingShape.box(12, 12)))
                .zIndex(150)
                .with(new ProjectileComponent(owner, vx, vy, 0, life, damage, 0, killMessage, null))
                .buildAndAttach();
    }

    /** Bombe mit Zündschnur. Position = Mittelpunkt. */
    public static Entity spawnBomb(double x, double y, double vx, double vy, Owner owner, double radius,
                                   int damage, String killMessage) {
        Circle body = new Circle(0, 0, 9);
        body.setFill(new RadialGradient(0, 0, -0.3, -0.3, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6a7090")), new Stop(1, Color.web("#151826"))));
        body.setStroke(Color.web("#06070d"));
        Circle led = new Circle(0, -1, 2.5, owner == Owner.PLAYER ? Color.web("#ffa13d") : Color.web("#ff3048"));
        led.setEffect(new DropShadow(8, (Color) led.getFill()));
        Line fuse = new Line(0, -9, 4, -14);
        fuse.setStroke(Color.web("#c9b07a"));
        fuse.setStrokeWidth(2);
        Circle spark = new Circle(4, -15, 2.5, Color.web("#fff3a0"));
        spark.setEffect(new DropShadow(10, Color.web("#ffb000")));
        Group view = new Group(body, fuse, spark, led);

        return FXGL.entityBuilder()
                .at(x, y)
                .type(EntityType.Projectile)
                .view(view)
                .bbox(new HitBox(new Point2D(-9, -9), BoundingShape.box(18, 18)))
                .zIndex(150)
                .with(new ProjectileComponent(owner, vx, vy, BOMB_GRAVITY, 1.8, damage, radius, killMessage, view))
                .buildAndAttach();
    }

    // ─────────────────────────── Ablauf ───────────────────────────

    @Override
    public void onUpdate(double tpf) {
        vy += gravity * tpf;
        entity.translate(vx * tpf, vy * tpf);
        life -= tpf;
        if (spin != null) spin.setRotate(spin.getRotate() + vx * tpf * 1.2);

        double cx = entity.getX(), cy = entity.getY();

        if (owner == Owner.PLAYER) {
            for (Entity enemy : Combat.enemies()) {
                if (entity.isColliding(enemy)) {
                    if (radius > 0) {
                        explode();
                    } else {
                        EnemyComponent ec = Combat.enemyOf(enemy);
                        if (ec != null) ec.damage(damage, 0.12, Math.signum(vx) * 8);
                        Effects.sparks(cx, cy, Color.web("#ffd24a"), 6);
                        entity.removeFromWorld();
                    }
                    return;
                }
            }
        } else {
            Entity player = Combat.player();
            if (player != null && entity.isColliding(player)) {
                if (radius > 0) {
                    explode();
                } else {
                    player.getComponent(PlayerComponent.class).damage(damage, killMessage, Math.signum(vx));
                    Effects.sparks(cx, cy, Color.web("#ff3048"), 6);
                    entity.removeFromWorld();
                }
                return;
            }
        }

        if (gravity > 0 && Combat.hitsTerrain(cx, cy + 9)) {
            explode();
            return;
        }

        if (life <= 0 || cy > 1400) {
            if (radius > 0) explode();
            else entity.removeFromWorld();
        }
    }

    private void explode() {
        double cx = entity.getX(), cy = entity.getY();
        Effects.explosion(cx, cy, radius);

        if (owner == Owner.PLAYER) {
            for (Entity enemy : Combat.enemies()) {
                Point2D c = Combat.center(enemy);
                if (c.distance(cx, cy) < radius + 24) {
                    EnemyComponent ec = Combat.enemyOf(enemy);
                    if (ec != null) ec.damage(damage, 0.5, Math.signum(c.getX() - cx) * 30);
                }
            }
        } else {
            Entity player = Combat.player();
            if (player != null && Combat.center(player).distance(cx, cy) < radius + 10) {
                double dir = Math.signum(Combat.center(player).getX() - cx);
                player.getComponent(PlayerComponent.class).damage(damage, killMessage, dir == 0 ? 1 : dir);
            }
        }
        entity.removeFromWorld();
    }

    public Owner getOwner() {
        return owner;
    }
}
