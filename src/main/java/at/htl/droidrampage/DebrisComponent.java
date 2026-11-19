package at.htl.droidrampage;

import com.almasb.fxgl.entity.component.Component;

/** Rein optisches Trümmerteil: fällt, dreht sich leicht und blendet aus. */
public class DebrisComponent extends Component {

    private double velocityY = 60;
    private double life = 1.2;

    @Override
    public void onUpdate(double tpf) {
        velocityY += 1400 * tpf;
        entity.translateY(velocityY * tpf);
        entity.setRotation(entity.getRotation() + 25 * tpf);
        life -= tpf;
        entity.setOpacity(Math.max(0, life / 1.2));
        if (life <= 0) {
            entity.removeFromWorld();
        }
    }
}
