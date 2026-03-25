package at.htl.droidrampage;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
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

    // Parallax speeds
    private static final double SPEED_LAYER1 = 0.05;
    private static final double SPEED_LAYER2 = 0.25;

    private ImageView bgLayer1a, bgLayer1b;
    private ImageView bgLayer2a, bgLayer2b;
    private ImageView bgLayer3;

    // Camera pan — acceleration based
    private static final double CAM_ACCEL     = 30.0;  // pixels/s² ramp-up
    private static final double CAM_MAX_SPEED = 200.0; // pixels/s cruise speed
    private double  camSpeed         = 0;
    private boolean camPanning       = false;
    private double  cameraPanTargetX = -1;

    // Level tracking
    private boolean nextLevelLoaded = false;

    // Trigger loading Tile1 near the END of the pan (not at the same point)
    // TileStart is 3 viewports wide; trigger at 2.5 so tiles aren't off-screen yet
    private static final double NEXT_LEVEL_TRIGGER_X = VIEWPORT_W * 2.5;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(VIEWPORT_W);
        settings.setHeight(VIEWPORT_H);
        settings.setFullScreenFromStart(true);
        settings.setTitle("Droid Rampage");
        settings.setVersion("0.6");
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("pixelsMoved", 0);
        vars.put("credit", 0);
    }

    // Player reference to keep it alive across level transitions
    private com.almasb.fxgl.entity.Entity player;

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new PlatformFactory());
        setLevelFromMap("TileStart.tmx");

        initParallaxBackground();

        // Create the player AFTER loading the level so it doesn't get deleted
        var playerSpawnData = new com.almasb.fxgl.entity.SpawnData(144, 780)
                .put("width", 40)
                .put("height", 64);
        player = spawn("player", playerSpawnData);


        runOnce(this::startCameraPan, Duration.seconds(2));
    }

    private void startCameraPan() {
        double startX    = getGameScene().getViewport().getX();
        cameraPanTargetX = startX + VIEWPORT_W * 3;
        camSpeed         = 0;
        camPanning       = true;
    }

    private void loadNextLevel() {
        if (nextLevelLoaded) return;
        nextLevelLoaded = true;

        // Only load if player still exists
        if (player == null) return;

        // Save player position
        double playerX = player.getX();
        double playerY = player.getY();

        // Load the new level (this removes all entities, including player)
        setLevelFromMap("Tile1.tmx");

        // Recreate the player at the saved position
        var playerSpawnData = new com.almasb.fxgl.entity.SpawnData(playerX, playerY)
                .put("width", 40)
                .put("height", 64);
        player = spawn("player", playerSpawnData);

        // Safety check
        if (player == null) {
            System.err.println("ERROR: Failed to spawn player in Tile1!");
            return;
        }

        // Continue camera pan - extend target by 1 more viewport width
        // (Tile1 is same size as TileStart, so add 1920 more)
        double currentCameraX = getGameScene().getViewport().getX();
        cameraPanTargetX = currentCameraX + VIEWPORT_W;
        camPanning = true;
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
        // ── Camera pan with acceleration ──────────────────────────────────────
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

        // ── Trigger Tile1 load partway through the first pan ──────────────────
        double cameraX = getGameScene().getViewport().getX();
        if (!nextLevelLoaded && cameraX >= NEXT_LEVEL_TRIGGER_X) {
            loadNextLevel();
        }

        // ── Parallax scroll ───────────────────────────────────────────────────
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

        // Register player input after physics is initialized
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
                creditText.setText("Score: " + newValue)
        );

        getGameScene().addUINode(creditText);
    }

     private void registerPlayerInputs() {
        getInput().addAction(new com.almasb.fxgl.input.UserAction("Move Right") {
            @Override
            protected void onAction() {
                if (player != null) {
                    var physics = player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                    physics.setVelocityX(250.0);
                    player.setScaleX(1);
                }
            }
            @Override
            protected void onActionEnd() {
                if (player != null) {
                    var physics = player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                    physics.setVelocityX(0);
                }
            }
        }, javafx.scene.input.KeyCode.D);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Move Left") {
            @Override
            protected void onAction() {
                if (player != null) {
                    var physics = player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                    physics.setVelocityX(-250.0);
                    player.setScaleX(-1);
                }
            }
            @Override
            protected void onActionEnd() {
                if (player != null) {
                    var physics = player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class);
                    physics.setVelocityX(0);
                }
            }
        }, javafx.scene.input.KeyCode.A);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Jump") {
            @Override
            protected void onActionBegin() {
                if (player != null) {
                    var component = player.getComponent(PlayerComponent.class);
                    if (component != null) component.jump();
                }
            }
        }, javafx.scene.input.KeyCode.SPACE);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Toggle Cheat Mode") {
            @Override
            protected void onActionBegin() {
                if (player != null) {
                    var component = player.getComponent(PlayerComponent.class);
                    if (component != null) component.toggleCheatMode();
                }
            }
        }, javafx.scene.input.KeyCode.I);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

