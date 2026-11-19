package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class PlayerComponent extends Component {
    private static final double CHEAT_SPEED = 800.0;
    private static final int MAX_JUMPS = 1;
    private static final double WALK_FRAME_TIME = 0.12;

    public static final int MAX_HP = 3;
    private static final double INVULNERABLE_TIME = 1.1;
    private static final double KNOCKBACK_TIME = 0.22;
    private static final double KNOCKBACK_SPEED = 340;

    private final DroidSkin skin;
    private final ImageView sprite;

    private PhysicsComponent physics;
    private boolean cheatMode = false;
    private int jumpsRemaining = MAX_JUMPS;

    // Horizontale Bewegung: -1 = links, 0 = stehen, 1 = rechts (wird von GameApp gesetzt)
    private int moveDirection = 0;
    private int facing = 1;
    private double walkTimer = 0;
    private boolean walkFrameA = true;

    // Lebenspunkte (Gegner-Treffer kosten 1 HP; Abgrund, Kamera, Laser & Stacheln töten sofort)
    private int hp = MAX_HP;
    private double invulnerable = 0;
    private double knockTimer = 0;
    private double knockVelocity = 0;
    private String deathMessage = "You were destroyed";

    public PlayerComponent(DroidSkin skin, ImageView sprite) {
        this.skin = skin;
        this.sprite = sprite;
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        if (physics == null) return;

        boolean onGround = physics.isOnGround();
        if (onGround) {
            jumpsRemaining = cheatMode ? Integer.MAX_VALUE : MAX_JUMPS;
        }

        // Bewegung + "Mitfahren" auf beweglichen Plattformen / Förderbändern
        if (knockTimer > 0) {
            knockTimer -= tpf;
            physics.setVelocityX(knockVelocity);
        } else {
            double carry = onGround ? carryVelocityX() : 0;
            physics.setVelocityX(moveDirection * getSpeed() + carry);
        }

        // Unverwundbarkeit nach Treffer -> Sprite blinkt
        if (invulnerable > 0) {
            invulnerable -= tpf;
            sprite.setOpacity(invulnerable > 0 && ((int) (invulnerable * 14)) % 2 == 0 ? 0.3 : 1.0);
        }

        updateAnimation(tpf, onGround);
    }

    /** Wenn der Spieler auf einer beweglichen Plattform oder einem Förderband steht, übernimmt er deren Tempo. */
    private double carryVelocityX() {
        for (Entity p : FXGL.getGameWorld().getEntitiesByType(EntityType.MovingPlatform)) {
            if (isStandingOn(entity, p)) {
                return p.getComponent(PhysicsComponent.class).getVelocityX();
            }
        }
        for (Entity c : FXGL.getGameWorld().getEntitiesByType(EntityType.Conveyor)) {
            if (isStandingOn(entity, c)) {
                return c.getComponent(ConveyorComponent.class).getSpeed();
            }
        }
        return 0;
    }

    private void updateAnimation(double tpf, boolean onGround) {
        if (sprite == null) return;

        if (moveDirection != 0) {
            facing = moveDirection;
        }
        sprite.setScaleX(facing); // Blickrichtung (Spiegeln um die Bildmitte)

        int frame;
        if (!onGround) {
            frame = DroidSkin.FRAME_JUMP;
        } else if (moveDirection != 0) {
            walkTimer += tpf;
            if (walkTimer >= WALK_FRAME_TIME) {
                walkTimer = 0;
                walkFrameA = !walkFrameA;
            }
            frame = walkFrameA ? DroidSkin.FRAME_WALK_A : DroidSkin.FRAME_WALK_B;
        } else {
            frame = DroidSkin.FRAME_IDLE;
        }
        sprite.setViewport(new Rectangle2D(frame * DroidSkin.FRAME_W, 0, DroidSkin.FRAME_W, DroidSkin.FRAME_H));
    }

    /**
     * Prüft geometrisch, ob 'player' auf der Oberseite von 'platform' steht.
     * Wird auch von Bröckel- und Sprungplattformen verwendet.
     */
    public static boolean isStandingOn(Entity player, Entity platform) {
        double feet = player.getBottomY();
        double top = platform.getY();
        if (feet < top - 6 || feet > top + 8) return false;

        var bbox = player.getBoundingBoxComponent();
        double pl = bbox.getMinXWorld() + 2, pr = bbox.getMaxXWorld() - 2;
        double l = platform.getX(), r = platform.getX() + platform.getWidth();
        if (pr <= l || pl >= r) return false;

        PhysicsComponent pp = player.getComponent(PhysicsComponent.class);
        return pp != null && pp.getVelocityY() > -60; // nicht gerade im Aufsteigen
    }

    public void setMoveDirection(int dir) {
        this.moveDirection = Integer.signum(dir);
    }

    public void jump() {
        if (physics != null && (cheatMode || jumpsRemaining > 0)) {
            physics.setVelocityY(-skin.getJumpForce());
            jumpsRemaining--;
        }
    }

    /** Von der Sprungplattform (oder beim Draufspringen auf Gegner) aufgerufen: Schub nach oben. */
    public void bounce(double force) {
        if (physics != null) {
            physics.setVelocityY(-force);
            jumpsRemaining = cheatMode ? Integer.MAX_VALUE : MAX_JUMPS;
        }
    }

    /**
     * Treffer durch Gegner oder Säge.
     * @param direction Richtung des Rückstoßes (-1 links, 1 rechts)
     * @return true, wenn der Treffer gezählt hat
     */
    public boolean damage(int amount, String source, double direction) {
        if (invulnerable > 0 || cheatMode || hp <= 0) return false;
        hp = Math.max(0, hp - amount);
        deathMessage = source != null ? source : deathMessage;
        invulnerable = INVULNERABLE_TIME;
        knockTimer = KNOCKBACK_TIME;
        knockVelocity = (direction >= 0 ? 1 : -1) * KNOCKBACK_SPEED;
        if (physics != null) physics.setVelocityY(-380);

        Point2D c = Combat.center(entity);
        Effects.sparks(c.getX(), c.getY(), Color.web("#ff5a6a"), 10);
        FXGL.getGameScene().getViewport().shake(6, 0.2);
        return true;
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }

    public int getHp() {
        return hp;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public int getFacing() {
        return facing;
    }

    public void toggleCheatMode() {
        cheatMode = !cheatMode;
        FXGL.getNotificationService().pushNotification(
                cheatMode ? "CHEAT MODE: ON" : "CHEAT MODE: OFF"
        );
    }

    public double getSpeed() {
        return cheatMode ? CHEAT_SPEED : skin.getSpeed();
    }

    public DroidSkin getSkin() {
        return skin;
    }
}
