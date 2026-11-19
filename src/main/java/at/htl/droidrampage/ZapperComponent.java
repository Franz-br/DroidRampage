package at.htl.droidrampage;

import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * Zapper-Drohne (Gegenstück zu ASTRO): schwebt herum, nähert sich dem Spieler,
 * lädt kurz auf (Elektroden funkeln) und entlädt dann einen Elektroschock auf kurze Distanz.
 */
public class ZapperComponent extends EnemyComponent {

    private static final double NOTICE_RANGE = 340;
    private static final double CHARGE_RANGE = 165;
    private static final double ZAP_RANGE = 200;
    private static final double LEASH = 280;
    private static final double SPEED = 120;

    private enum State { PATROL, CHARGE, COOLDOWN }

    private State state = State.PATROL;
    private Point2D anchor;
    private double time, timer;

    public ZapperComponent(ImageView sprite, double minX, double maxX) {
        super(EnemyKind.ZAPPER, sprite, minX, maxX);
    }

    @Override
    protected void behave(double tpf, Entity player) {
        if (anchor == null) anchor = entity.getPosition();
        time += tpf;

        Point2D me = myCenter();
        Point2D target = anchor.add(Math.sin(time * 0.9) * 80, Math.sin(time * 2.3) * 12);
        double dist = Double.MAX_VALUE;

        if (player != null) {
            Point2D p = playerCenter(player);
            dist = p.distance(me);
            facing = p.getX() >= me.getX() ? 1 : -1;
            if (dist < NOTICE_RANGE && state != State.CHARGE) {
                // etwa auf Kopfhöhe neben den Spieler schweben (so treffen auch Blaster-Schüsse),
                // aber an der "Leine" bleiben
                Point2D want = p.add(-facing * 70, -12).subtract(entity.getWidth() / 2, 20);
                Point2D off = want.subtract(anchor);
                if (off.magnitude() > LEASH) off = off.normalize().multiply(LEASH);
                target = anchor.add(off);
            }
        }

        switch (state) {
            case PATROL:
            case COOLDOWN: {
                Point2D delta = target.subtract(entity.getPosition());
                double step = SPEED * tpf;
                entity.translate(delta.magnitude() <= step ? delta : delta.normalize().multiply(step));
                setFrame(((int) (time / 0.15)) % 2);
                if (state == State.COOLDOWN) {
                    timer -= tpf;
                    if (timer <= 0) state = State.PATROL;
                } else if (dist < CHARGE_RANGE) {
                    state = State.CHARGE;
                    timer = 0.65;
                }
                break;
            }
            case CHARGE: {
                setFrame(2);
                timer -= tpf;
                if (timer <= 0) {
                    discharge(player, dist);
                    state = State.COOLDOWN;
                    timer = 1.6;
                }
                break;
            }
        }
    }

    private void discharge(Entity player, double dist) {
        Point2D me = myCenter().add(0, 14);
        Color arc = Color.web("#7fd8ff");
        if (player != null && dist < ZAP_RANGE) {
            Point2D p = playerCenter(player);
            Effects.lightning(me.getX(), me.getY(), p.getX(), p.getY(), arc);
            pc(player).damage(1, kind.getKillMessage(), facing);
        } else {
            Effects.lightning(me.getX(), me.getY(), me.getX() + facing * 90, me.getY() + 70, arc);
        }
    }
}
