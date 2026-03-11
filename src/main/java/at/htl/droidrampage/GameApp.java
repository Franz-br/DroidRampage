package at.htl.droidrampage;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import javafx.scene.text.Text;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(120 * 16);
        settings.setHeight(68 * 16);
        settings.setFullScreenFromStart(true);
        settings.setTitle("Droid Rampage");
        settings.setVersion("0.3");

    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("pixelsMoved", 0);
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new PlatformFactory());
        setLevelFromMap("TileStart.tmx"); //.tmx benutzen, nicht .tmj
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 980);
    }

    @Override
    protected void initUI() {

    }



    static void main(String[] args) {
        launch(args);
    }
}