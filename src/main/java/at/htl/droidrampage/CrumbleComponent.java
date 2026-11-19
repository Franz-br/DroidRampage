package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Bröckelnde Plattform: Sobald der Spieler darauf steht, wackelt sie kurz
 * und fällt dann als Trümmerteil nach unten (die Kollision verschwindet).
 */
public class CrumbleComponent extends Component {

    private static final double SHAKE_TIME = 0.45;

    private final Node view;
    private boolean triggered = false;
    private double timer = SHAKE_TIME;

    public CrumbleComponent(Node view) {
        this.view = view;
    }

    @Override
    public void onUpdate(double tpf) {
        if (!triggered) {
            for (Entity p : FXGL.getGameWorld().getEntitiesByType(EntityType.Player)) {
                if (PlayerComponent.isStandingOn(p, entity)) {
                    triggered = true;
                    break;
                }
            }
            return;
        }

        timer -= tpf;
        var rnd = ThreadLocalRandom.current();
        view.setTranslateX(rnd.nextDouble(-2.5, 2.5));
        view.setTranslateY(rnd.nextDouble(-1, 1.5));

        if (timer <= 0) {
            // Trümmer (nur Grafik) fallen lassen, eigentliche Plattform entfernen
            FXGL.entityBuilder()
                    .at(entity.getPosition())
                    .view(PlatformVisuals.crumble(entity.getWidth(), entity.getBoundingBoxComponent().getHeight()))
                    .with(new DebrisComponent())
                    .buildAndAttach();
            entity.removeFromWorld();
        }
    }
}
