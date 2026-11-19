package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;

/** Sprungplattform: Landet der Spieler darauf, wird er hoch in die Luft geschleudert. */
public class BouncePadComponent extends Component {

    public static final double BOUNCE_FORCE = 1150;
    private static final double COOLDOWN = 0.35;

    private final Node view;
    private double cooldown = 0;
    private double squash = 0; // 0..1, Feder-Animation

    public BouncePadComponent(Node view) {
        this.view = view;
    }

    @Override
    public void onUpdate(double tpf) {
        cooldown = Math.max(0, cooldown - tpf);
        squash = Math.max(0, squash - tpf * 4);
        view.setScaleY(1 - 0.35 * squash);
        view.setTranslateY(entity.getBoundingBoxComponent().getHeight() * 0.35 * squash / 2);

        if (cooldown > 0) return;

        for (Entity p : FXGL.getGameWorld().getEntitiesByType(EntityType.Player)) {
            if (PlayerComponent.isStandingOn(p, entity)) {
                PlayerComponent pc = p.getComponent(PlayerComponent.class);
                if (pc != null) pc.bounce(BOUNCE_FORCE);
                cooldown = COOLDOWN;
                squash = 1;
            }
        }
    }
}
