package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.scene.input.KeyCode;

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

        FXGL.getInput().addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                physics.setVelocityX(getSpeed());
                entity.setScaleX(1);
            }

            @Override
            protected void onActionEnd() {
                physics.setVelocityX(0);
            }
        }, KeyCode.D);

        FXGL.getInput().addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                physics.setVelocityX(-getSpeed());
                entity.setScaleX(-1);
            }

            @Override
            protected void onActionEnd() {
                physics.setVelocityX(0);
            }
        }, KeyCode.A);

        FXGL.getInput().addAction(new UserAction("Jump") {
            @Override
            protected void onActionBegin() {
                if (cheatMode || jumpsRemaining > 0) {
                    physics.setVelocityY(-JUMP_FORCE);
                    jumpsRemaining--;
                }
            }
        }, KeyCode.SPACE);

        FXGL.getInput().addAction(new UserAction("Toggle Cheat Mode") {
            @Override
            protected void onActionBegin() {
                cheatMode = !cheatMode;
                FXGL.getNotificationService().pushNotification(
                        cheatMode ? "CHEAT MODE: ON" : "CHEAT MODE: OFF"
                );
            }
        }, KeyCode.I);
    }

    @Override
    public void onUpdate(double tpf) {
        if (physics.isOnGround()) {
            jumpsRemaining = cheatMode ? Integer.MAX_VALUE : MAX_JUMPS;
        }
    }

    private double getSpeed() {
        return cheatMode ? CHEAT_SPEED : NORMAL_SPEED;
    }
}
