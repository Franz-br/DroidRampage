package at.htl.droidrampage;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;

import java.util.concurrent.ThreadLocalRandom;

/** Lässt Credits sanft auf und ab schweben, damit sie im Level auffallen. */
public class FloatComponent extends Component {

    private final Node view;
    private double time = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);

    public FloatComponent(Node view) {
        this.view = view;
    }

    @Override
    public void onUpdate(double tpf) {
        time += tpf;
        view.setTranslateY(Math.sin(time * 3) * 4);
        double s = 1 + Math.sin(time * 6) * 0.04;
        view.setScaleX(s);
        view.setScaleY(s);
    }
}
