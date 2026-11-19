package at.htl.droidrampage;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;

/** Förderband: schiebt den Spieler (siehe PlayerComponent.carryVelocityX) und animiert die Pfeile. */
public class ConveyorComponent extends Component {

    private static final double ARROW_SPACING = 28;

    private final double speed;
    private final Node arrows;
    private double offset;

    public ConveyorComponent(double speed, Node arrows) {
        this.speed = speed;
        this.arrows = arrows;
    }

    @Override
    public void onUpdate(double tpf) {
        offset = (offset + speed * tpf) % ARROW_SPACING;
        arrows.setTranslateX(offset);
    }

    public double getSpeed() {
        return speed;
    }
}
