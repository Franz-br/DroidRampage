package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;

public class PlatformFactory implements EntityFactory {

    @Spawns("plattform")
    public Entity newPlatform(SpawnData data) {
        return FXGL.entityBuilder()
                .from(data)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(
                        data.<Integer>get("width"), data.<Integer>get("height"))))
                .with(new PhysicsComponent())
                .build();
    }
}
