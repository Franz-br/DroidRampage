package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gemeinsame Logik aller Gegner: Lebenspunkte, Treffer-Blitzen, Betäubung,
 * Berührungsschaden und "Draufspringen" (Stomp).
 * Das eigentliche Verhalten liefern die Unterklassen in behave().
 */
public abstract class EnemyComponent extends Component {

    private static final double STOMP_BOUNCE = 620;

    protected final EnemyKind kind;
    protected final ImageView sprite;
    protected final double minX, maxX; // Bewegungsgrenzen (linke Kante des Entities)

    protected int hp;
    protected int facing = -1;
    protected double stun;
    private double flash;
    private boolean dead;
    private final ColorAdjust flashEffect = new ColorAdjust();

    protected EnemyComponent(EnemyKind kind, ImageView sprite, double minX, double maxX) {
        this.kind = kind;
        this.sprite = sprite;
        this.minX = minX;
        this.maxX = maxX;
        this.hp = kind.getMaxHp();
        sprite.setEffect(flashEffect);
    }

    @Override
    public final void onUpdate(double tpf) {
        if (flash > 0) {
            flash -= tpf;
            flashEffect.setBrightness(flash > 0 ? 0.75 : 0);
        }

        // Gegner weit außerhalb des Bildschirms schlafen (keine Schüsse aus dem Off)
        if (!Combat.onScreen(entity.getX(), 120)) return;

        Entity player = Combat.player();
        if (stun > 0) {
            stun -= tpf;
            sprite.setTranslateX(ThreadLocalRandom.current().nextDouble(-2, 2));
            if (ThreadLocalRandom.current().nextDouble() < tpf * 6) {
                Point2D c = Combat.center(entity);
                Effects.sparks(c.getX(), c.getY(), Color.web("#7fd8ff"), 3);
            }
        } else {
            sprite.setTranslateX(0);
            behave(tpf, player);
        }
        sprite.setScaleX(facing);

        if (player != null && !dead) checkContact(player);
    }

    /** Verhalten der jeweiligen Gegnerart. player kann null sein. */
    protected abstract void behave(double tpf, Entity player);

    private void checkContact(Entity player) {
        if (!player.isColliding(entity)) return;
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        PhysicsComponent pp = player.getComponent(PhysicsComponent.class);

        // Von oben draufgesprungen -> Gegner nimmt Schaden, Spieler federt ab
        if (pp.getVelocityY() > 80 && player.getBottomY() < entity.getY() + 20) {
            damage(1, 0.3, 0);
            pc.bounce(STOMP_BOUNCE);
            return;
        }
        if (stun <= 0) {
            double dir = Math.signum(Combat.center(player).getX() - Combat.center(entity).getX());
            pc.damage(1, kind.getKillMessage(), dir == 0 ? 1 : dir);
        }
    }

    /**
     * Schaden zufügen.
     * @param stunTime Sekunden, die der Gegner betäubt ist (macht dann auch keinen Berührungsschaden)
     * @param knockX   Rückstoß in Pixel (wird in den Bewegungsgrenzen gehalten)
     */
    public void damage(int amount, double stunTime, double knockX) {
        if (dead) return;
        hp -= amount;
        flash = 0.12;
        stun = Math.max(stun, stunTime);
        if (knockX != 0) {
            entity.setX(Math.max(minX, Math.min(maxX, entity.getX() + knockX)));
        }
        Point2D c = Combat.center(entity);
        Effects.sparks(c.getX(), c.getY(), Color.web("#ffd27a"), 6);
        if (hp <= 0) die();
    }

    private void die() {
        dead = true;
        Point2D c = Combat.center(entity);
        Effects.explosion(c.getX(), c.getY(), 60);
        Effects.floatingText(c.getX(), c.getY() - 30, "+" + kind.getScoreValue(), Color.web("#ffd24a"));
        FXGL.inc("bonus", kind.getScoreValue());
        FXGL.inc("kills", 1);
        entity.removeFromWorld();
    }

    public boolean isDead() {
        return dead;
    }

    protected void setFrame(int index) {
        sprite.setViewport(new Rectangle2D(index * kind.getFrameW(), 0, kind.getFrameW(), kind.getFrameH()));
    }

    protected Point2D playerCenter(Entity player) {
        return Combat.center(player);
    }

    protected Point2D myCenter() {
        return Combat.center(entity);
    }

    protected PlayerComponent pc(Entity player) {
        return player.getComponent(PlayerComponent.class);
    }
}
