package at.htl.droidrampage;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private static final int VIEWPORT_W = 120 * 16; // 1920
    private static final int VIEWPORT_H = 68 * 16;  // 1088

    // Parallax speeds (fraction of camera scroll speed)
    private static final double SPEED_LAYER1 = 0.05; // sky + far towers — barely moves
    private static final double SPEED_LAYER2 = 0.25; // mid buildings

    private ImageView bgLayer1a, bgLayer1b; // sky (seamless double)
    private ImageView bgLayer2a, bgLayer2b; // mid buildings (seamless double)
    private ImageView bgLayer3;             // ground glow (fixed)

    private double lastCameraX = 0;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(VIEWPORT_W);
        settings.setHeight(VIEWPORT_H);
        settings.setFullScreenFromStart(true);
        settings.setTitle("Droid Rampage");
        settings.setVersion("0.5");
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
    }

    private void initParallaxBackground() {
        Image imgSky = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer1_sky.png"));
        Image imgMid = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer2_mid.png"));
        Image imgGnd = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer3_ground.png"));

        // Two copies per layer for seamless horizontal looping
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

        // Second copy starts one viewport-width to the right
        bgLayer1b.setTranslateX(VIEWPORT_W);
        bgLayer2b.setTranslateX(VIEWPORT_W);

        Pane bgPane = new Pane(bgLayer1a, bgLayer1b, bgLayer2a, bgLayer2b, bgLayer3);
        bgPane.setMouseTransparent(true);

        // Insert behind everything else
        getGameScene().getRoot().getChildren().add(0, bgPane);
    }

    @Override
    protected void onUpdate(double tpf) {
        double cameraX = getGameScene().getViewport().getX();
        double delta   = cameraX - lastCameraX;
        lastCameraX    = cameraX;

        scrollLayer(bgLayer1a, bgLayer1b, cameraX * SPEED_LAYER1);
        scrollLayer(bgLayer2a, bgLayer2b, cameraX * SPEED_LAYER2);
        // Layer 3 (ground glow) stays fixed in screen space — no scroll needed
    }

    /**
     * Positions the two copies of a layer so they tile seamlessly.
     * offset = how many pixels this layer has scrolled from origin.
     */
    private void scrollLayer(ImageView a, ImageView b, double offset) {
        double mod = offset % VIEWPORT_W;
        a.setTranslateX(-mod);
        b.setTranslateX(VIEWPORT_W - mod);
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 980);

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
        creditText.setFill(Color.CYAN); // visible on dark cyberpunk background
        creditText.setLayoutX(20);
        creditText.setLayoutY(50);

        getWorldProperties().intProperty("credit").addListener((_, _, newValue) ->
                creditText.setText("Score: " + newValue)
        );

        getGameScene().addUINode(creditText);
    }

    public static void main(String[] args) {
        launch(args);
    }
}