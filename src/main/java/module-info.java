module at.htl.droidrampage {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.almasb.fxgl.entity;
    requires com.almasb.fxgl.all;


    opens at.htl.droidrampage to javafx.fxml;
    exports at.htl.droidrampage;
}