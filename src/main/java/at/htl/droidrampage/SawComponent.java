package at.htl.droidrampage;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/** Kreissäge, die auf einer Schiene hin und her fährt. Treffer kosten 1 HP (geprüft in GameApp). */
public class SawComponent extends Component {

    public static final String KILL_MESSAGE = "Shredded by a saw";

    private final Node blade;
    private final double amplitude, period;
    private Point2D origin;
    private double time;

    public SawComponent(Node blade, double amplitude, double period, double phase) {
        this.blade = blade;
        this.amplitude = amplitude;
        this.period = period;
        this.time = phase;
    }

    @Override
    public void onUpdate(double tpf) {
        if (origin == null) origin = entity.getPosition();
        time += tpf;
        double x = origin.getX() + amplitude * Math.sin(2 * Math.PI * time / period);
        double vx = amplitude * 2 * Math.PI / period * Math.cos(2 * Math.PI * time / period);
        entity.setX(x);
        blade.setRotate(blade.getRotate() + (vx >= 0 ? 1 : -1) * 720 * tpf);
    }
}
