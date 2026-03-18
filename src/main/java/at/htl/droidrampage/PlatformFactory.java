package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.collision.ContactID;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PlatformFactory implements EntityFactory {

    @Spawns("plattform")
    public Entity newPlatform(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return FXGL.entityBuilder()
                .from(data)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(
                        data.<Integer>get("width"), data.<Integer>get("height"))))
                .with(physics)
                .build();
    }

    @Spawns("Item1")
    public Entity newItem1(SpawnData data) {
        return FXGL.entityBuilder()
                .from(data)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(
                        data.<Integer>get("width"), data.<Integer>get("height"))))
                .build();
    }


    //32x32 groß machen
    @Spawns("Credit1")
    public Entity newCredit1(SpawnData data) {
        int w = data.<Integer>get("width");
        int h = data.<Integer>get("height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(FXGL.getAssetLoader().loadTexture("credit1.png", w, h))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()  // <-- replaces PhysicsComponent
                .with("spawnName", "Credit1")
                .build();
    }

    @Spawns("Credit2")
    public Entity newCredit2(SpawnData data) {
        int w = data.<Integer>get("width");
        int h = data.<Integer>get("height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(FXGL.getAssetLoader().loadTexture("credit2.png", w, h))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()  // <-- replaces PhysicsComponent
                .with("spawnName", "Credit1")
                .build();
    }

    @Spawns("Credit3")
    public Entity newCredit3(SpawnData data) {
        int w = data.<Integer>get("width");
        int h = data.<Integer>get("height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(FXGL.getAssetLoader().loadTexture("credit3.png", w, h))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()  // <-- replaces PhysicsComponent
                .with("spawnName", "Credit1")
                .build();
    }

    @Spawns("Boden")
    public Entity newBoden(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return FXGL.entityBuilder()
                .from(data)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(
                        data.<Integer>get("width"), data.<Integer>get("height"))))
                .with(physics)
                .build();
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0.0f));
        physics.addGroundSensor(new HitBox(
                new Point2D(6, data.<Integer>get("height") - 4),
                BoundingShape.box(data.<Integer>get("width") - 12, 4)
        ));

        int w = data.<Integer>get("width");
        int h = data.<Integer>get("height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Player)
                .view(FXGL.getAssetLoader().loadTexture("R2D2.png", w, h))
                .bbox(new HitBox("body", new Point2D(4, 0), BoundingShape.box(w - 8, h - 4)))
                .with(physics)
                .with(new PlayerComponent())
                .collidable()  // <-- add this
                .build();
    }
}