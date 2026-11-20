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
        // Nur ganze Pixel und kein Pulsieren per Skalierung – sonst zeichnet JavaFX den Chip verschwommen
        view.setTranslateY(PixelArt.snap(Math.sin(time * 3) * 4));
    }
}
