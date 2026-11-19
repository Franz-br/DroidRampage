package at.htl.droidrampage;

import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * Marksman (Gegenstück zu PROTO): steht still, zielt mit einem roten Laservisier
 * auf den Spieler und feuert dann einen Blaster-Schuss auf die zuletzt anvisierte Stelle.
 * In den letzten 0.25 s vor dem Schuss friert das Visier ein -> ausweichen ist möglich.
 */
public class SniperComponent extends EnemyComponent {

    private static final double RANGE = 950;
    private static final double AIM_TIME = 1.0;
    private static final double LOCK_TIME = 0.25;
    private static final double BOLT_SPEED = 760;

    private enum State { IDLE, AIM, COOLDOWN }

    private final Line aimLine;
    private State state = State.IDLE;
    private double timer;
    private Point2D locked;

    public SniperComponent(ImageView sprite, Line aimLine, double minX, double maxX) {
        super(EnemyKind.SNIPER, sprite, minX, maxX);
        this.aimLine = aimLine;
    }

    /** Mündung in lokalen Koordinaten (Sprite ist 40 breit, nach rechts gezeichnet). */
    private Point2D muzzleLocal() {
        return new Point2D(facing > 0 ? 39 : 1, 25);
    }

    @Override
    protected void behave(double tpf, Entity player) {
        Point2D me = myCenter();
        Point2D p = player != null ? playerCenter(player) : null;
        if (p != null && state != State.AIM) facing = p.getX() >= me.getX() ? 1 : -1;

        switch (state) {
            case IDLE:
                setFrame(0);
                aimLine.setVisible(false);
                if (p != null && Math.abs(p.getX() - me.getX()) < RANGE && Math.abs(p.getY() - me.getY()) < 520
                        && Combat.onScreen(entity.getX(), -20)) {
                    state = State.AIM;
                    timer = AIM_TIME;
                    locked = p;
                }
                break;
            case AIM: {
                setFrame(1);
                timer -= tpf;
                if (p != null && timer > LOCK_TIME) locked = p;
                Point2D m = muzzleLocal();
                aimLine.setVisible(true);
                aimLine.setStartX(m.getX());
                aimLine.setStartY(m.getY());
                aimLine.setEndX(locked.getX() - entity.getX());
                aimLine.setEndY(locked.getY() - entity.getY());
                aimLine.setOpacity(timer > LOCK_TIME ? 0.35 + 0.2 * Math.sin(timer * 30) : 0.95);
                aimLine.setStrokeWidth(timer > LOCK_TIME ? 1.2 : 2.5);
                if (timer <= 0) {
                    fire();
                    state = State.COOLDOWN;
                    timer = 2.1;
                }
                break;
            }
            case COOLDOWN:
                aimLine.setVisible(false);
                setFrame(timer > 1.9 ? 2 : 0);
                timer -= tpf;
                if (timer <= 0) state = State.IDLE;
                break;
        }
    }

    private void fire() {
        aimLine.setVisible(false);
        Point2D from = entity.getPosition().add(muzzleLocal());
        Point2D dir = locked.subtract(from);
        if (dir.magnitude() < 1) dir = new Point2D(facing, 0);
        dir = dir.normalize();
        Effects.muzzle(from.getX(), from.getY(), Color.web("#ff5a6a"));
        ProjectileComponent.spawnBolt(from.getX(), from.getY(), dir.getX() * BOLT_SPEED, dir.getY() * BOLT_SPEED,
                ProjectileComponent.Owner.ENEMY, 1, 1.8, kind.getKillMessage());
    }
}
