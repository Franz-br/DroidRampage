package at.htl.droidrampage;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;

/**
 * Bewegt eine KINEMATIC-Plattform sinusförmig um ihre Startposition.
 * Die Geschwindigkeit wird jedes Frame aus der Soll-Position berechnet,
 * dadurch driftet die Plattform auch nach langer Zeit nicht weg.
 */
public class MovingPlatformComponent extends Component {

    private final double amplitudeX;
    private final double amplitudeY;
    private final double period;
    private final double phase;

    private PhysicsComponent physics;
    private Point2D origin;
    private double time;

    public MovingPlatformComponent(double amplitudeX, double amplitudeY, double period, double phase) {
        this.amplitudeX = amplitudeX;
        this.amplitudeY = amplitudeY;
        this.period = period;
        this.phase = phase;
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        if (tpf <= 0 || physics == null) return;
        if (origin == null) {
            origin = entity.getPosition(); // erst hier: Position ist nach dem Spawnen final
        }

        time += tpf;
        double angle = 2 * Math.PI * time / period + phase;
        double targetX = origin.getX() + amplitudeX * Math.sin(angle);
        double targetY = origin.getY() + amplitudeY * Math.sin(angle);

        physics.setLinearVelocity((targetX - entity.getX()) / tpf, (targetY - entity.getY()) / tpf);
    }
}
