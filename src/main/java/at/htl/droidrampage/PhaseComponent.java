package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 * Phasen-Plattform: ist abwechselnd da und weg. Kurz vor dem Verschwinden flackert sie.
 * Während sie weg ist, wird der KINEMATIC-Körper weit nach unten verschoben und
 * an der Originalposition ein gestrichelter Umriss angezeigt.
 */
public class PhaseComponent extends Component {

    private static final double WARNING_TIME = 0.5;
    private static final double HIDDEN_OFFSET = 6000;

    private final Node view;
    private final double onTime, offTime;
    private double time;
    private boolean solid = true;

    private PhysicsComponent physics;
    private Point2D origin;
    private Entity ghost;

    public PhaseComponent(Node view, double onTime, double offTime, double startOffset) {
        this.view = view;
        this.onTime = onTime;
        this.offTime = offTime;
        this.time = startOffset;
    }

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
    }

    @Override
    public void onUpdate(double tpf) {
        if (origin == null) {
            origin = entity.getPosition();
            ghost = FXGL.entityBuilder()
                    .at(origin)
                    .type(EntityType.Decoration)
                    .view(PlatformVisuals.phaseGhost(entity.getWidth(), entity.getBoundingBoxComponent().getHeight()))
                    .buildAndAttach();
            ghost.setVisible(false);
        }

        time += tpf;
        double inCycle = time % (onTime + offTime);
        boolean shouldBeSolid = inCycle < onTime;

        if (shouldBeSolid != solid) {
            solid = shouldBeSolid;
            physics.overwritePosition(solid ? origin : origin.add(0, HIDDEN_OFFSET));
            ghost.setVisible(!solid);
        }
        if (solid) {
            boolean warning = inCycle > onTime - WARNING_TIME;
            view.setOpacity(warning && ((int) (inCycle * 16)) % 2 == 0 ? 0.35 : 1.0);
        }
    }

    @Override
    public void onRemoved() {
        if (ghost != null && ghost.isActive()) ghost.removeFromWorld();
    }
}
