package at.htl.droidrampage;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.texture.Texture;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;

public class PlayerComponent extends Component {
    private final double speed = 5.0;

    @Override
    public void onAdded() {
        // Set player texture as view
        // If you keep the image in src/main/resources/Textures/R2D2.png, load it from classpath:
        Image img = new Image(getClass().getResourceAsStream("/Textures/R2D2.png"));
        Texture texture = new Texture(img);
        texture.setFitWidth(25);
        texture.setFitHeight(40);
        entity.getViewComponent().addChild(texture);


        // Register input actions for movement
        FXGL.getInput().addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                entity.translateX(speed);
                FXGL.inc("pixelsMoved", (int) speed);
            }
        }, KeyCode.D);

        FXGL.getInput().addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                entity.translateX(-speed);
                FXGL.inc("pixelsMoved", (int) -speed);
            }
        }, KeyCode.A);

        FXGL.getInput().addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                entity.translateY(-speed);
                FXGL.inc("pixelsMoved", (int) speed);
            }
        }, KeyCode.W);

        FXGL.getInput().addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                entity.translateY(speed);
                FXGL.inc("pixelsMoved", (int) -speed);
            }
        }, KeyCode.S);
    }
}
