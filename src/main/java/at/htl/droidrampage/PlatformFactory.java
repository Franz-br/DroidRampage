package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.geometry.Point2D;

public class PlatformFactory implements EntityFactory {

    /**
     * TMX object dimensions arrive as Double from the TMX loader,
     * but manual SpawnData.put("width", 40) stores them as Integer.
     * This helper handles both safely via the Number supertype.
     */
    private static int dim(SpawnData data, String key) {
        Object val = data.getData().get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    @Spawns("plattform")
    public Entity newPlatform(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return FXGL.entityBuilder()
                .from(data)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(dim(data, "width"), dim(data, "height"))))
                .with(physics)
                .build();
    }

    @Spawns("Boden")
    public Entity newBoden(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);

        return FXGL.entityBuilder()
                .from(data)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(dim(data, "width"), dim(data, "height"))))
                .with(physics)
                .build();
    }

    @Spawns("Item1")
    public Entity newItem1(SpawnData data) {
        return FXGL.entityBuilder()
                .from(data)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(dim(data, "width"), dim(data, "height"))))
                .build();
    }

    @Spawns("Death")
    public Entity newDeath(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Death")
                .build();
    }

    @Spawns("Credit1")
    public Entity newCredit1(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(FXGL.getAssetLoader().loadTexture("credit1.png", w, h))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Credit1")
                .build();
    }

    @Spawns("Credit2")
    public Entity newCredit2(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(FXGL.getAssetLoader().loadTexture("credit2.png", w, h))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Credit2")
                .build();
    }

    @Spawns("Credit3")
    public Entity newCredit3(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(FXGL.getAssetLoader().loadTexture("credit3.png", w, h))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Credit3")
                .build();
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0.0f));
        physics.addGroundSensor(new HitBox(
                new Point2D(6, h - 4),
                BoundingShape.box(w - 12, 4)
        ));

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Player)
                .view(FXGL.getAssetLoader().loadTexture("r2d2.png", w, h))
                .bbox(new HitBox("body", new Point2D(4, 0), BoundingShape.box(w - 8, h - 4)))
                .with(physics)
                .with(new PlayerComponent())
                .collidable()
                .build();
    }
}