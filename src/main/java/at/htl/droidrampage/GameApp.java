package at.htl.droidrampage;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {



    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(120 * 16);
        settings.setHeight(68 * 16);
        settings.setFullScreenFromStart(true);
        settings.setTitle("Droid Rampage");
        settings.setVersion("0.5");

    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("pixelsMoved", 0);
        vars.put("score", 0);
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new PlatformFactory());
        setLevelFromMap("TileStart.tmx"); //.tmx benutzen, nicht .tmj
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 980);

        onCollisionBegin(EntityType.Player, EntityType.Coin, (player, coin) -> {
            coin.removeFromWorld();

            int points = 0;
            if (coin.getProperties().exists("spawnName")) {
                String spawnName = coin.getProperties().getString("spawnName");
                if (spawnName.contains("Credit1")) points = 10;
                else if (spawnName.contains("Credit2")) points = 50;
                else if (spawnName.contains("Credit3")) points = 100;
            }

            inc("score", points);
        });
    }

    @Override
    protected void initUI() {
        Text scoreText = new Text();
        scoreText.setFont(Font.font("Arial", 32));
        scoreText.setStyle("-fx-fill: white; -fx-font-weight: bold;");

        // Listener für Score-Änderungen
        getWorldProperties().intProperty("score").addListener((obs, old, newValue) -> {
            scoreText.setText("Score: " + newValue);
        });

        scoreText.setText("Score: 0");
        getGameScene().addUINode(scoreText);
    }



    static void main(String[] args) {
        launch(args);
    }
}