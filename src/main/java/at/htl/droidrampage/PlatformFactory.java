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
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.HashMap;
import java.util.Map;

public class PlatformFactory implements EntityFactory {

    /** Credit-Chips, bereits auf Anzeigegröße verkleinert (Schlüssel: Datei@Breite). */
    private static final Map<String, Image> CHIP_CACHE = new HashMap<>();

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

    /** Liest einen optionalen Zahlenwert aus den SpawnData (mit Standardwert). */
    private static double num(SpawnData data, String key, double def) {
        Object val = data.getData().get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : def;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OBJEKTE AUS DEN TILED-MAPS
    // Die Kachel-Ebene der Maps wird nicht mehr angezeigt (siehe GameApp.loadTmxSegment) –
    // stattdessen bekommen die Objekte dieselben Grafiken wie die generierten Segmente.
    // ═══════════════════════════════════════════════════════════════════════════

    private static final double FLOOR_TOP = 1030; // alles, was hier anfängt, ist Boden

    @Spawns("plattform")
    public Entity newPlatform(SpawnData data) {
        return staticBlock(data, EntityType.Platform, PlatformVisuals.solid(dim(data, "width"), dim(data, "height")));
    }

    @Spawns("Boden")
    public Entity newBoden(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");
        // Breite Bodenstreifen ganz unten = Boden, kleine Blöcke = Kisten
        boolean floor = data.getY() >= FLOOR_TOP && w >= 150;
        Node view = floor ? PlatformVisuals.ground(w, h) : PlatformVisuals.crate(w, h);
        return staticBlock(data, EntityType.Ground, view);
    }

    @Spawns("Item1")
    public Entity newItem1(SpawnData data) {
        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Decoration)
                .view(PlatformVisuals.holoPillar(dim(data, "width"), dim(data, "height")))
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
                .view(PlatformVisuals.spikes(w, h))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Death")
                .build();
    }

    @Spawns("Credit1")
    public Entity newCredit1(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        Node view = creditView("credit1.png", w, h, Color.web("#ff9a52"));
        Group holder = new Group(view);
        return snapped(holder, FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(holder)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Credit1")
                .with(new FloatComponent(view))
                .build());
    }

    @Spawns("Credit2")
    public Entity newCredit2(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        Node view = creditView("credit2.png", w, h, Color.web("#d8e6ff"));
        Group holder = new Group(view);
        return snapped(holder, FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(holder)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Credit2")
                .with(new FloatComponent(view))
                .build());
    }

    @Spawns("Credit3")
    public Entity newCredit3(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");

        Node view = creditView("credit3.png", w, h, Color.web("#ffd34a"));
        Group holder = new Group(view);
        return snapped(holder, FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Coin)
                .view(holder)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .collidable()
                .with("spawnName", "Credit3")
                .with(new FloatComponent(view))
                .build());
    }

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");
        Object skinValue = data.getData().get("skin");
        DroidSkin skin = skinValue instanceof DroidSkin ? (DroidSkin) skinValue : DroidSkin.ASTRO;

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().friction(0.0f));
        physics.addGroundSensor(new HitBox(
                new Point2D(6, h - 4),
                BoundingShape.box(w - 12, 4)
        ));

        // Spritesheet (4 Frames) – der sichtbare Ausschnitt wird vom PlayerComponent animiert
        ImageView sprite = new ImageView(skin.getSheet());
        sprite.setViewport(PixelArt.frame(0, DroidSkin.FRAME_W, DroidSkin.FRAME_H));
        sprite.setFitWidth(w);   // Sheet liegt vergrößert vor (PixelArt.FACTOR) → wird scharf auf Spielgröße gebracht
        sprite.setFitHeight(h);
        sprite.setSmooth(false);
        DropShadow rim = new DropShadow(10, skin.getAccent().deriveColor(0, 1, 1, 0.55));
        sprite.setEffect(rim);

        Group holder = new Group(sprite);
        return snapped(holder, FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Player)
                .view(holder)
                .bbox(new HitBox("body", new Point2D(4, 0), BoundingShape.box(w - 8, h - 4)))
                .with(physics)
                .with(new PlayerComponent(skin, sprite))
                .with(new PlayerCombatComponent(skin))
                .collidable()
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROZEDURALE PLATTFORMEN (werden vom SegmentGenerator gespawnt)
    // ═══════════════════════════════════════════════════════════════════════════

    private static Entity staticBlock(SpawnData data, EntityType type, Node view) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);
        return FXGL.entityBuilder()
                .from(data)
                .type(type)
                .view(view)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(dim(data, "width"), dim(data, "height"))))
                .with(physics)
                .build();
    }

    @Spawns("groundBlock")
    public Entity newGroundBlock(SpawnData data) {
        return staticBlock(data, EntityType.Ground, PlatformVisuals.ground(dim(data, "width"), dim(data, "height")));
    }

    @Spawns("solidPlatform")
    public Entity newSolidPlatform(SpawnData data) {
        return staticBlock(data, EntityType.Platform, PlatformVisuals.solid(dim(data, "width"), dim(data, "height")));
    }

    @Spawns("crumblePlatform")
    public Entity newCrumblePlatform(SpawnData data) {
        Node view = PlatformVisuals.crumble(dim(data, "width"), dim(data, "height"));
        Entity e = staticBlock(data, EntityType.CrumblePlatform, view);
        e.addComponent(new CrumbleComponent(view));
        return e;
    }

    @Spawns("bouncePad")
    public Entity newBouncePad(SpawnData data) {
        Node view = PlatformVisuals.bouncePad(dim(data, "width"), dim(data, "height"));
        Entity e = staticBlock(data, EntityType.BouncePad, view);
        e.addComponent(new BouncePadComponent(view));
        return e;
    }

    @Spawns("movingPlatform")
    public Entity newMovingPlatform(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");
        double ampX = num(data, "ampX", 0);
        double ampY = num(data, "ampY", 0);

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.KINEMATIC);

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.MovingPlatform)
                .view(PlatformVisuals.moving(w, h, ampY > ampX))
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .with(physics)
                .with(new MovingPlatformComponent(ampX, ampY, num(data, "period", 3), num(data, "phase", 0)))
                .build();
    }

    @Spawns("laser")
    public Entity newLaser(SpawnData data) {
        int h = dim(data, "height");
        PlatformVisuals.LaserView view = PlatformVisuals.laser(h);

        // Hitbox nur um den Strahl (nicht um die Emitter)
        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Hazard)
                .view(view.root())
                .bbox(new HitBox(new Point2D(9, 18), BoundingShape.box(10, h - 36)))
                .with(new LaserComponent(view,
                        num(data, "onTime", 1.0), num(data, "offTime", 1.4), num(data, "offset", 0)))
                .build();
    }

    @Spawns("conveyor")
    public Entity newConveyor(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");
        double speed = num(data, "speed", 150);
        PlatformVisuals.ConveyorView view = PlatformVisuals.conveyor(w, h, speed > 0);
        Entity e = staticBlock(data, EntityType.Conveyor, view.root());
        e.addComponent(new ConveyorComponent(speed, view.arrows()));
        return e;
    }

    @Spawns("phasePlatform")
    public Entity newPhasePlatform(SpawnData data) {
        int w = dim(data, "width");
        int h = dim(data, "height");
        Node view = PlatformVisuals.phase(w, h);

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.KINEMATIC);

        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Platform)
                .view(view)
                .bbox(new HitBox(new Point2D(0, 0), BoundingShape.box(w, h)))
                .with(physics)
                .with(new PhaseComponent(view, num(data, "onTime", 1.8), num(data, "offTime", 1.1), num(data, "offset", 0)))
                .build();
    }

    @Spawns("sawRail")
    public Entity newSawRail(SpawnData data) {
        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Decoration)
                .view(PlatformVisuals.sawRail(dim(data, "width")))
                .build();
    }

    @Spawns("saw")
    public Entity newSaw(SpawnData data) {
        double r = num(data, "radius", 26);
        Node blade = PlatformVisuals.sawBlade(r);
        return FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Hazard)
                .view(blade)
                .bbox(new HitBox(new Point2D(r * 0.25, r * 0.25), BoundingShape.box(r * 1.5, r * 1.5)))
                .zIndex(5)
                .with(new SawComponent(blade, num(data, "amplitude", 160), num(data, "period", 2.6), num(data, "phase", 0)))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GEGNER  (SpawnData: x/y = linke obere Ecke, minX/maxX = Bewegungsgrenzen)
    // ═══════════════════════════════════════════════════════════════════════════

    private static ImageView enemySprite(EnemyKind kind) {
        ImageView sprite = new ImageView(kind.getSheet());
        sprite.setViewport(PixelArt.frame(0, kind.getFrameW(), kind.getFrameH()));
        sprite.setFitWidth(kind.getFrameW());
        sprite.setFitHeight(kind.getFrameH());
        sprite.setSmooth(false);
        return sprite;
    }

    private static Entity enemy(SpawnData data, EnemyKind kind, Node view, HitBox box, EnemyComponent behaviour) {
        Group holder = new Group(view);
        return snapped(holder, FXGL.entityBuilder()
                .from(data)
                .type(EntityType.Enemy)
                .view(holder)
                .bbox(box)
                .zIndex(10)
                .with(behaviour)
                .build());
    }

    /** Hält die Grafik der Entity auf ganzen Pixeln (sonst verschwimmt sie bei Kommazahl-Positionen). */
    private static Entity snapped(Node holder, Entity entity) {
        PixelArt.snapToPixels(entity, holder);
        return entity;
    }

    @Spawns("enemyZapper")
    public Entity newZapper(SpawnData data) {
        ImageView sprite = enemySprite(EnemyKind.ZAPPER);
        return enemy(data, EnemyKind.ZAPPER, sprite,
                new HitBox(new Point2D(8, 5), BoundingShape.box(32, 28)),
                new ZapperComponent(sprite, num(data, "minX", data.getX()), num(data, "maxX", data.getX())));
    }

    @Spawns("enemyBrute")
    public Entity newBrute(SpawnData data) {
        ImageView sprite = enemySprite(EnemyKind.BRUTE);
        return enemy(data, EnemyKind.BRUTE, sprite,
                new HitBox(new Point2D(9, 6), BoundingShape.box(38, 53)),
                new BruteComponent(sprite, num(data, "minX", data.getX()), num(data, "maxX", data.getX())));
    }

    @Spawns("enemySniper")
    public Entity newSniper(SpawnData data) {
        ImageView sprite = enemySprite(EnemyKind.SNIPER);
        Line aim = new Line();
        aim.setStroke(Color.web("#ff3048"));
        aim.setEffect(new DropShadow(6, Color.web("#ff3048")));
        aim.setVisible(false);
        return enemy(data, EnemyKind.SNIPER, new Group(aim, sprite),
                new HitBox(new Point2D(10, 2), BoundingShape.box(20, 60)),
                new SniperComponent(sprite, aim, num(data, "minX", data.getX()), num(data, "maxX", data.getX())));
    }

    /** Credit-Grafik mit farbigem Leuchten (schwebt per FloatComponent). */
    private static Node creditView(String texture, int w, int h, Color glowColor) {
        // Zugeschnittener Chip (siehe tools/generate_sprites.py), etwas breiter als die Hitbox, vertikal zentriert
        String chipFile = texture.replace(".png", "_chip.png");
        // Einmal beim Laden hochwertig auf FACTOR × Anzeigegröße verkleinern → JavaFX muss danach kaum noch skalieren
        int shownWidth = (int) Math.round(w * 1.25);
        Image img = CHIP_CACHE.computeIfAbsent(chipFile + "@" + shownWidth, key -> new Image(
                PlatformFactory.class.getResourceAsStream("/assets/textures/" + chipFile),
                shownWidth * PixelArt.FACTOR, 0, true, true));
        double shownHeight = Math.round(img.getHeight() / PixelArt.FACTOR);
        ImageView chip = new ImageView(img);
        chip.setFitWidth(shownWidth);
        chip.setFitHeight(shownHeight);
        chip.setLayoutX(Math.round((w - shownWidth) / 2.0));
        chip.setLayoutY(Math.round((h - shownHeight) / 2));

        DropShadow glow = new DropShadow(18, glowColor);
        glow.setSpread(0.25);
        chip.setEffect(glow);
        return new Group(chip);
    }
}
