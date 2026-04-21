package at.htl.droidrampage;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.level.Level;
import com.almasb.fxgl.entity.level.tiled.TMXLevelLoader;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private static final int VIEWPORT_W = 120 * 16; // 1920
    private static final int VIEWPORT_H = 68 * 16;  // 1088

    // TileStart is exactly one map wide: 120 tiles × 16 pixels = 1920 pixels
    private static final double TILESTART_WORLD_WIDTH = 120 * 16.0; // 1920 -- Fehleranfällig

    private static final double SPEED_LAYER1 = 0.05;
    private static final double SPEED_LAYER2 = 0.25;

    private static final String[] TILE_VARIANTS = {"Tile1.tmx", "Tile2.tmx"};
    private static final double TILE_SEGMENT_WIDTH = TILESTART_WORLD_WIDTH;

    private ImageView bgLayer1a, bgLayer1b;
    private ImageView bgLayer2a, bgLayer2b;
    private ImageView bgLayer3;

    private static final double AUTO_SCROLL_START_SPEED = 15.0;
    private static final double AUTO_SCROLL_ACCEL_PER_SEC = 10.0;
    private static final double AUTO_SCROLL_MAX_SPEED = 240.0;
    private boolean camPanning       = false;
    private double  cameraPanTargetX = -1;
    private double currentAutoScrollSpeed = AUTO_SCROLL_START_SPEED;

    private double nextTileSpawnX = TILESTART_WORLD_WIDTH;
    private String lastSpawnedTile = null;

    private static final boolean START_FULLSCREEN = false; // true = fullscreen, false = maximized window

    private Entity player;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(VIEWPORT_W);
        settings.setHeight(VIEWPORT_H);

        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(START_FULLSCREEN);

        // useful for maximized-window mode
        settings.setManualResizeEnabled(true);

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
        if (!START_FULLSCREEN) {
            Platform.runLater(() -> getPrimaryStage().setMaximized(true)); // Stage access must happen on JavaFX thread
        }

        getGameWorld().addEntityFactory(new PlatformFactory());
        setLevelFromMap("TileStart.tmx");
        initParallaxBackground();

        player = spawn("player", new SpawnData(144, 780)
                .put("width", 40)
                .put("height", 64));

        runOnce(this::startCameraPan, Duration.seconds(2));
    }

    private void startCameraPan() {
        // Pan across TileStart + all of Tile1
        cameraPanTargetX = TILESTART_WORLD_WIDTH + VIEWPORT_W * 3;
        camPanning       = true;
    }

    /**
     * Load the next tile segment into the existing world at the current spawn X.
     * The next tile is chosen randomly, but never equal to the previously spawned one.
     */
    private boolean spawnTiles() {
        double offsetX = nextTileSpawnX;

        String tileName = chooseNextTileVariant();

        // ── 1. Load a random tile segment with TMX loader ─────────────────────
        try {
            Level tileLevel = getAssetLoader().loadLevel(tileName, new TMXLevelLoader());

            // ── 2. Process all entities from the level ─────────────────────────
            for (Entity e : tileLevel.getEntities()) {
                e.setPosition(e.getPosition().add(offsetX, 0));
                getGameWorld().addEntity(e);
            }

            lastSpawnedTile = tileName;
            nextTileSpawnX += TILE_SEGMENT_WIDTH;
            System.out.println("[TILE] Spawned " + tileName + " at offsetX=" + offsetX);
            return true;
        } catch (Exception e) {
            System.err.println("[TILE] Failed to load level '" + tileName + "': " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String chooseNextTileVariant() {
        int variantCount = tileVariantCount();

        if (variantCount < 2) {
            return TILE_VARIANTS[0];
        }

        if (lastSpawnedTile == null) {
            return TILE_VARIANTS[ThreadLocalRandom.current().nextInt(variantCount)];
        }

        int lastIndex = indexOfTileVariant(lastSpawnedTile);
        if (lastIndex < 0) {
            return TILE_VARIANTS[ThreadLocalRandom.current().nextInt(variantCount)];
        }

        int index = ThreadLocalRandom.current().nextInt(variantCount - 1);
        if (index >= lastIndex) {
            index++;
        }

        return TILE_VARIANTS[index];
    }

    private int tileVariantCount() {
        return TILE_VARIANTS.length;
    }

    private int indexOfTileVariant(String tileName) {
        for (int i = 0; i < TILE_VARIANTS.length; i++) {
            if (TILE_VARIANTS[i].equals(tileName)) {
                return i;
            }
        }
        return -1;
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
        currentAutoScrollSpeed = Math.min(
                currentAutoScrollSpeed + AUTO_SCROLL_ACCEL_PER_SEC * tpf,
                AUTO_SCROLL_MAX_SPEED
        );

        // ── Camera pan ────────────────────────────────────────────────────────
        if (camPanning) {
            double currentX = getGameScene().getViewport().getX();
            double newX     = currentX + currentAutoScrollSpeed * tpf;
            if (newX >= cameraPanTargetX) {
                newX       = cameraPanTargetX;
                camPanning = false;
            }
            getGameScene().getViewport().setX(newX);
        } else {
            double currentX = getGameScene().getViewport().getX();
            getGameScene().getViewport().setX(currentX + currentAutoScrollSpeed * tpf);
        }

        // ── Spawn Tile1 ahead of camera ───────────────────────────────────────
        double cameraX = getGameScene().getViewport().getX();
        while (cameraX + VIEWPORT_W >= nextTileSpawnX) {
            if (!spawnTiles()) {
                break;
            }
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