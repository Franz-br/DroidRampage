package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

public class PlayerComponent extends Component {
    private static final double NORMAL_SPEED = 250.0;
    private static final double CHEAT_SPEED = 600.0;
    private static final double JUMP_FORCE = 700.0;
    private static final int MAX_JUMPS = 1;

    private PhysicsComponent physics;
    private boolean cheatMode = false;
    private int jumpsRemaining = MAX_JUMPS;

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        if (physics != null && physics.isOnGround()) {
            jumpsRemaining = cheatMode ? Integer.MAX_VALUE : MAX_JUMPS;
        }
    }

    public void jump() {
        if (physics != null && (cheatMode || jumpsRemaining > 0)) {
            physics.setVelocityY(-JUMP_FORCE);
            jumpsRemaining--;
        }
    }

    public void toggleCheatMode() {
        cheatMode = !cheatMode;
        FXGL.getNotificationService().pushNotification(
                cheatMode ? "CHEAT MODE: ON" : "CHEAT MODE: OFF"
        );
    }

    public double getSpeed() {
        return cheatMode ? CHEAT_SPEED : NORMAL_SPEED;
    }
}

