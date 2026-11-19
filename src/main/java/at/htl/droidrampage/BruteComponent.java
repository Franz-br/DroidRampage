package at.htl.droidrampage;

import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * Brute (Gegenstück zu GRUMP): patrouilliert auf seiner Plattform, stürmt auf den Spieler zu,
 * schlägt mit der Klaue zu und wirft aus mittlerer Distanz Bomben.
 */
public class BruteComponent extends EnemyComponent {

    private static final double PATROL_SPEED = 60;
    private static final double CHASE_SPEED = 145;
    private static final double CHASE_RANGE = 420;
    private static final double MELEE_RANGE = 72;
    private static final double BOMB_MIN = 220, BOMB_MAX = 620;

    private enum State { WALK, WINDUP, SMASH }

    private State state = State.WALK;
    private int patrolDir = -1;
    private double timer, walkTime, bombCooldown = 1.5;

    public BruteComponent(ImageView sprite, double minX, double maxX) {
        super(EnemyKind.BRUTE, sprite, minX, maxX);
    }

    @Override
    protected void behave(double tpf, Entity player) {
        bombCooldown -= tpf;

        double dx = 0, dy = Double.MAX_VALUE;
        if (player != null) {
            Point2D p = playerCenter(player), me = myCenter();
            dx = p.getX() - me.getX();
            dy = p.getY() - me.getY();
        }
        boolean sameLevel = Math.abs(dy) < 110;

        switch (state) {
            case WALK: {
                double speed;
                int dir;
                if (player != null && sameLevel && Math.abs(dx) < CHASE_RANGE) {
                    dir = dx >= 0 ? 1 : -1;
                    speed = CHASE_SPEED;
                } else {
                    dir = patrolDir;
                    speed = PATROL_SPEED;
                }
                facing = dir;
                double nx = entity.getX() + dir * speed * tpf;
                if (nx <= minX || nx >= maxX) {
                    nx = Math.max(minX, Math.min(maxX, nx));
                    patrolDir = -patrolDir;
                }
                entity.setX(nx);

                walkTime += tpf;
                setFrame(((int) (walkTime / 0.2)) % 2);

                if (player != null && sameLevel && Math.abs(dx) < MELEE_RANGE) {
                    facing = dx >= 0 ? 1 : -1;
                    state = State.WINDUP;
                    timer = 0.4;
                } else if (player != null && bombCooldown <= 0 && Math.abs(dx) > BOMB_MIN
                        && Math.abs(dx) < BOMB_MAX && Math.abs(dy) < 320) {
                    throwBomb(player);
                    bombCooldown = 3.2;
                }
                break;
            }
            case WINDUP:
                setFrame(2);
                timer -= tpf;
                if (timer <= 0) {
                    smash(player);
                    state = State.SMASH;
                    timer = 0.35;
                }
                break;
            case SMASH:
                setFrame(3);
                timer -= tpf;
                if (timer <= 0) state = State.WALK;
                break;
        }
    }

    private void smash(Entity player) {
        Point2D me = myCenter();
        Effects.slash(me.getX() + facing * 30, me.getY() + 4, facing, Color.web("#ff6a3a"));
        if (player == null) return;
        Point2D p = playerCenter(player);
        double ahead = (p.getX() - me.getX()) * facing;
        if (ahead > -10 && ahead < 95 && Math.abs(p.getY() - me.getY()) < 70) {
            pc(player).damage(1, kind.getKillMessage(), facing);
        }
    }

    private void throwBomb(Entity player) {
        Point2D from = myCenter().add(facing * 26, -24);
        Point2D to = playerCenter(player);
        double flight = 0.95;
        double g = ProjectileComponent.BOMB_GRAVITY;
        double vx = (to.getX() - from.getX()) / flight;
        double vy = (to.getY() - from.getY() - 0.5 * g * flight * flight) / flight;
        setFrame(2);
        ProjectileComponent.spawnBomb(from.getX(), from.getY(), vx, vy, ProjectileComponent.Owner.ENEMY,
                85, 1, kind.getKillMessage());
    }
}
