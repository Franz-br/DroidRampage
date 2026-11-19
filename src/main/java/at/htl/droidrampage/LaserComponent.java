package at.htl.droidrampage;

import com.almasb.fxgl.entity.component.Component;

/**
 * Blinkendes Laser-Tor. Zyklus: AUS (offTime) -> WARNUNG (letzte 0.4s von AUS) -> AN (onTime).
 * Nur im Zustand AN ist der Laser tödlich (geprüft in GameApp.onUpdate).
 */
public class LaserComponent extends Component {

    private static final double WARNING_TIME = 0.4;

    private final PlatformVisuals.LaserView view;
    private final double onTime;
    private final double offTime;
    private double time;

    public LaserComponent(PlatformVisuals.LaserView view, double onTime, double offTime, double startOffset) {
        this.view = view;
        this.onTime = onTime;
        this.offTime = offTime;
        this.time = startOffset;
    }

    @Override
    public void onUpdate(double tpf) {
        time += tpf;
        boolean on = isLethal();
        double inCycle = time % (onTime + offTime);
        boolean warning = !on && inCycle > offTime - WARNING_TIME;

        view.beam().setVisible(on);
        view.warning().setVisible(warning);
        if (on) {
            view.beam().setOpacity(0.85 + 0.15 * Math.sin(time * 40)); // leichtes Flimmern
        }
    }

    /** true, wenn der Strahl gerade aktiv ist. */
    public boolean isLethal() {
        double inCycle = time % (onTime + offTime);
        return inCycle >= offTime;
    }
}
