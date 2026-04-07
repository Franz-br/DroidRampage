package at.htl.droidrampage;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.level.Level;
import com.almasb.fxgl.entity.level.tiled.TMXLevelLoader;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private static final int VIEWPORT_W = 120 * 16; // 1920
    private static final int VIEWPORT_H = 68 * 16;  // 1088

    // TileStart is exactly one map wide: 120 tiles × 16 pixels = 1920 pixels
    private static final double TILESTART_WORLD_WIDTH = 120 * 16.0; // 1920 -- Fehleranfällig

    private static final double SPEED_LAYER1 = 0.05;
    private static final double SPEED_LAYER2 = 0.25;

    private ImageView bgLayer1a, bgLayer1b;
    private ImageView bgLayer2a, bgLayer2b;
    private ImageView bgLayer3;

    private static final double CAM_ACCEL     = 15.0;
    private static final double CAM_MAX_SPEED = 150.0;
    private double  camSpeed         = 0;
    private boolean camPanning       = false;
    private double  cameraPanTargetX = -1;

    private boolean tile1Spawned = false;
    // Trigger well before camera reaches Tile1 so tiles are ready
    private static final double TILE1_SPAWN_TRIGGER_X = TILESTART_WORLD_WIDTH - VIEWPORT_W;

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(VIEWPORT_W);
        settings.setHeight(VIEWPORT_H);
        settings.setFullScreenFromStart(true);
        settings.setTitle("Droid Rampage");
        settings.setVersion("0.8");
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("pixelsMoved", 0);
        vars.put("credit", 0);
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new PlatformFactory());
        setLevelFromMap("TileStart.tmx");
        initParallaxBackground();

        player = spawn("player", new SpawnData(144, 780)
                .put("width", 40)
                .put("height", 64));

        runOnce(this::startCameraPan, Duration.seconds(2));
    }

    private void startCameraPan() {
        double startX    = getGameScene().getViewport().getX();
        // Pan across TileStart + all of Tile1
        cameraPanTargetX = TILESTART_WORLD_WIDTH + VIEWPORT_W * 3;
        camSpeed         = 0;
        camPanning       = true;
    }

    /**
     * Load Tile1's tile layer visuals and physics objects into the existing world
     * at an X offset equal to TileStart's world width — no world reset.
     */
    private void spawnTile1() {
        if (tile1Spawned) return;
        tile1Spawned = true;

        double offsetX = TILESTART_WORLD_WIDTH;

        // ── 1. Load the Tile1 level with TMX loader ────────────────────────────
        try {
            Level tile1Level = getAssetLoader().loadLevel("Tile1.tmx", new TMXLevelLoader());

            // ── 2. Process all entities from the level ─────────────────────────
            for (Entity e : tile1Level.getEntities()) {
                // Tile layer entities (no physics) - add as visuals
                if (!e.hasComponent(com.almasb.fxgl.physics.PhysicsComponent.class)) {
                    // Position the entity at the offset
                    e.setPosition(e.getPosition().add(offsetX, 0));
                    getGameWorld().addEntity(e);
                }
                // Physics entities (platforms, obstacles) - also add with offset
                else if (e.hasComponent(com.almasb.fxgl.physics.PhysicsComponent.class)) {
                    // Add offset to the entity position
                    e.setPosition(e.getPosition().add(offsetX, 0));
                    getGameWorld().addEntity(e);
                }
            }
        } catch (Exception e) {
            System.err.println("[TILE1] Failed to load level: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[TILE1] Spawned Tile1 at offsetX=" + offsetX);
    }

    private void spawnBox(String type, double x, double y, int w, int h) {
        spawn(type, new SpawnData(x, y).put("width", w).put("height", h));
    }

    private void initParallaxBackground() {
        Image imgSky = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer1_sky.png"));
        Image imgMid = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer2_mid.png"));
        Image imgGnd = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer3_ground.png"));

        bgLayer1a = new ImageView(imgSky);
        bgLayer1b = new ImageView(imgSky);
        bgLayer2a = new ImageView(imgMid);
        bgLayer2b = new ImageView(imgMid);
        bgLayer3  = new ImageView(imgGnd);

        for (ImageView iv : new ImageView[]{bgLayer1a, bgLayer1b, bgLayer2a, bgLayer2b, bgLayer3}) {
            iv.setFitWidth(VIEWPORT_W);
            iv.setFitHeight(VIEWPORT_H);
            iv.setPreserveRatio(false);
        }

        bgLayer1b.setTranslateX(VIEWPORT_W);
        bgLayer2b.setTranslateX(VIEWPORT_W);

        Pane bgPane = new Pane(bgLayer1a, bgLayer1b, bgLayer2a, bgLayer2b, bgLayer3);
        bgPane.setMouseTransparent(true);
        getGameScene().getRoot().getChildren().add(0, bgPane);
    }

    @Override
    protected void onUpdate(double tpf) {
        // ── Camera pan ────────────────────────────────────────────────────────
        if (camPanning) {
            camSpeed = Math.min(camSpeed + CAM_ACCEL * tpf, CAM_MAX_SPEED);
            double currentX = getGameScene().getViewport().getX();
            double newX     = currentX + camSpeed * tpf;
            if (newX >= cameraPanTargetX) {
                newX       = cameraPanTargetX;
                camPanning = false;
            }
            getGameScene().getViewport().setX(newX);
        }

        // ── Spawn Tile1 ahead of camera ───────────────────────────────────────
        double cameraX = getGameScene().getViewport().getX();
        if (!tile1Spawned && cameraX >= TILE1_SPAWN_TRIGGER_X) {
            spawnTile1();
        }

        // ── Parallax ──────────────────────────────────────────────────────────
        scrollLayer(bgLayer1a, bgLayer1b, cameraX * SPEED_LAYER1);
        scrollLayer(bgLayer2a, bgLayer2b, cameraX * SPEED_LAYER2);
    }

    private void scrollLayer(ImageView a, ImageView b, double offset) {
        double mod = offset % VIEWPORT_W;
        a.setTranslateX(-mod);
        b.setTranslateX(VIEWPORT_W - mod);
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 980);
        registerPlayerInputs();

        onCollisionBegin(EntityType.Player, EntityType.Coin, (_, coin) -> {
            coin.removeFromWorld();
            int credits = 0;
            if (coin.getProperties().exists("spawnName")) {
                String spawnName = coin.getProperties().getString("spawnName");
                if      (spawnName.contains("Credit1")) credits += 5;
                else if (spawnName.contains("Credit2")) credits += 50;
                else if (spawnName.contains("Credit3")) credits += 75;
            }
            inc("credit", credits);
        });
    }

    @Override
    protected void initUI() {
        Text creditText = new Text("Credits: 0");
        creditText.setFont(Font.font("Arial", 40));
        creditText.setFill(Color.CYAN);
        creditText.setLayoutX(20);
        creditText.setLayoutY(50);
        getWorldProperties().intProperty("credit").addListener((_, _, newValue) ->
                creditText.setText("Score: " + newValue));
        getGameScene().addUINode(creditText);
    }

    private void registerPlayerInputs() {
        getInput().addAction(new com.almasb.fxgl.input.UserAction("Move Right") {
            @Override protected void onAction() {
                if (player != null) {
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(250.0);
                    player.setScaleX(1);
                }
            }
            @Override protected void onActionEnd() {
                if (player != null)
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(0);
            }
        }, javafx.scene.input.KeyCode.D);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Move Left") {
            @Override protected void onAction() {
                if (player != null) {
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(-250.0);
                    player.setScaleX(-1);
                }
            }
            @Override protected void onActionEnd() {
                if (player != null)
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(0);
            }
        }, javafx.scene.input.KeyCode.A);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Jump") {
            @Override protected void onActionBegin() {
                if (player != null) {
                    var c = player.getComponent(PlayerComponent.class);
                    if (c != null) c.jump();
                }
            }
        }, javafx.scene.input.KeyCode.SPACE);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Toggle Cheat Mode") {
            @Override protected void onActionBegin() {
                if (player != null) {
                    var c = player.getComponent(PlayerComponent.class);
                    if (c != null) c.toggleCheatMode();
                }
            }
        }, javafx.scene.input.KeyCode.I);
    }

    public static void main(String[] args) {
        launch(args);
    }
}