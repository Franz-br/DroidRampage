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


    // ═══════════════════════════════════════════════════════════════════════════
    // KONFIGURATION DER TILE-GENERIERUNG
    // ═══════════════════════════════════════════════════════════════════════════
    // Liste der verfügbaren Tile-Map-Varianten für prozedurale Generierung.
    // Das Spiel wählt zufällig aus diesen Dateien, um ein endloses Level zu erstellen.
    // Maps werden von src/main/resources/assets/levels/ geladen.
    private static final String[] TILE_VARIANTS = {"Tile1.tmx", "Tile2.tmx", "Tile3.tmx", "Tile4.tmx", "Tile5.tmx", "Tile6.tmx"};

    // Breite jedes Tile-Segments in Pixeln (gleich wie TileStart.tmx: 120 Tiles × 16px = 1920px).
    // Dies ist die horizontale Distanz, die die Spielwelt voranschreitet, wenn ein neues Tile gespawnt wird.
    private static final double TILE_SEGMENT_WIDTH = TILESTART_WORLD_WIDTH;

    // ═══════════════════════════════════════════════════════════════════════════
    // PARALLAX-HINTERGRUND-EBENEN
    // ═══════════════════════════════════════════════════════════════════════════
    // Dreilagiger Parallax-Hintergrund für Tiefeneffekt:
    // - bgLayer1a/1b (Himmel): Bewegt sich mit 5% der Kamerageschwindigkeit → erscheint am weitesten weg, am langsamsten
    // - bgLayer2a/2b (Mitte): Bewegt sich mit 25% der Kamerageschwindigkeit → erscheint im Mittelgrund
    // - bgLayer3 (Grund): Bewegt sich mit 100% der Kamerageschwindigkeit (direktes Scrollen)
    // Jede Ebene nutzt zwei ImageViews (a/b), die nahtlos schleifen, wenn die Kamera nach rechts scrollt.
    // Wenn View 'a' vom Bildschirm verschwindet, übernimmt 'b' ihren Platz und wir wickeln 'a' zurück zum Start.
    private ImageView bgLayer1a, bgLayer1b;
    private ImageView bgLayer2a, bgLayer2b;
    private ImageView bgLayer3;

    // ═══════════════════════════════════════════════════════════════════════════
    // KAMERA- UND AUTO-SCROLL-KONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    // Auto-Scroll-System: Kamera startet bei 0 Pixel/Sekunde und beschleunigt bis zur maximalen Geschwindigkeit.
    // Dies erzeugt progressive Schwierigkeitssteigerung—früh im Spiel langsamer (Spieler kann reagieren), später schneller.
    private static final double AUTO_SCROLL_START_SPEED = 0.0;
    private static final double AUTO_SCROLL_ACCEL_PER_SEC = 5.0;
    private static final double AUTO_SCROLL_MAX_SPEED = 400.0;
    private static final double CAMERA_PAN_FALLBACK_SPEED = 120.0; // used during the initial camera pan if auto-scroll isn't active
    private static final double DEATH_FALL_BUFFER = 120.0;
    private static final String DEATH_REASON_VOID = "You fell into the void";
    private static final String DEATH_REASON_CAMERA = "The camera overtook you";
    private static final String DEATH_REASON_GENERAL = "You were killed by the Environment";

    private boolean camPanning       = false;
    private double  cameraPanTargetX = -1;
    private double currentAutoScrollSpeed = AUTO_SCROLL_START_SPEED;
    private boolean autoScrollActive = false; // only accelerate when enabled (after first input + delay)
    private boolean firstInputReceived = false;
    private static final double AUTO_SCROLL_START_DELAY = 0.8; // seconds after first input to start accelerating
    private boolean isDead = false;
    private Pane deathOverlay;

    private double nextTileSpawnX = TILESTART_WORLD_WIDTH;
    private String lastSpawnedTile = null;

    private static final boolean START_FULLSCREEN = false; // true = fullscreen, false = maximized window

    private Entity player;
    private boolean inputsRegistered = false;

    private void notifyFirstInput() {
        if (firstInputReceived) return;
        firstInputReceived = true;
        // Starte Auto-Scroll nach kurzer Verzögerung, um dem Spieler Zeit zum Reagieren zu geben
        runOnce(() -> autoScrollActive = true, Duration.seconds(AUTO_SCROLL_START_DELAY));
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(VIEWPORT_W);
        settings.setHeight(VIEWPORT_H);

        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(START_FULLSCREEN);

        // useful for maximized-window mode
        settings.setManualResizeEnabled(true);

        settings.setTitle("Droid Rampage");
        settings.setVersion("1.0");
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("pixelsMoved", 0);
        vars.put("credit", 0);
    }

    @Override
    protected void initGame() {
        resetRunState();

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
        // Intro-Camera-Pan: Verschiebe Kamera von 0 über TileStart + Tile1
        // Dies gibt dem Spieler Zeit, das Spiel zu beobachten, bevor Auto-Scroll richtig beginnt
        cameraPanTargetX = TILESTART_WORLD_WIDTH + VIEWPORT_W * 3;
        camPanning       = true;
    }

    /**
     * Laden des nächsten Tile-Segments in die bestehende Welt an der aktuellen Spawn-Position.
     * Das nächste Tile wird zufällig gewählt, ist aber nie gleich dem zuvor gespawnten.
     *
     * Ablauf:
     * 1. Versucht mehrere Varianten zufällig aus, falls einige Maps fehlerhaft sind
     * 2. Vermeidet, dass das gleiche Tile zweimal hintereinander gespawnt wird
     * 3. Lädt das TMX-Level und versetzt alle Entities um offsetX
     * 4. Addiert TILE_SEGMENT_WIDTH zu nextTileSpawnX, damit das nächste Tile weiter rechts spawnt
     * 5. Gibt true zurück bei Erfolg, false falls alle Varianten fehlgeschlagen sind
     */
    private boolean spawnTiles() {
        double offsetX = nextTileSpawnX;

        int variantCount = tileVariantCount();
        if (variantCount == 0) {
            System.err.println("[TILE] Keine Tile-Varianten konfiguriert");
            return false;
        }

        // Versuche mehrere Varianten, falls einige Maps fehlerhaft sind
        for (int attempt = 0; attempt < variantCount; attempt++) {
            String candidate = TILE_VARIANTS[ThreadLocalRandom.current().nextInt(variantCount)];
            // Vermeide unmittelbare Wiederholung wenn möglich
            if (lastSpawnedTile != null && candidate.equals(lastSpawnedTile) && variantCount > 1) {
                continue;
            }

            try {
                Level tileLevel = getAssetLoader().loadLevel(candidate, new TMXLevelLoader());

                // Verschiebe alle Entities dieses Levels um offsetX nach rechts
                for (Entity e : tileLevel.getEntities()) {
                    e.setPosition(e.getPosition().add(offsetX, 0));
                    getGameWorld().addEntity(e);
                }

                lastSpawnedTile = candidate;
                nextTileSpawnX += TILE_SEGMENT_WIDTH;
                System.out.println("[TILE] Spawned " + candidate + " at offsetX=" + offsetX);
                return true;
            } catch (Exception ex) {
                System.err.println("[TILE] Fehler beim Laden von '" + candidate + "': " + ex.getMessage());
                // Versuche nächste Variante
            }
        }

        System.err.println("[TILE] Alle Tile-Varianten konnten nicht geladen werden für Spawn bei x=" + offsetX);
        return false;
    }

    private String chooseNextTileVariant() {
        int variantCount = tileVariantCount();

        if (variantCount < 2) {
            return TILE_VARIANTS[0];
        }

        if (lastSpawnedTile == null) {
            return TILE_VARIANTS[ThreadLocalRandom.current().nextInt(variantCount)]; //Nur in einem Thread und kann nicht modifiziert werden
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



    private void resetRunState() {
        // Bereinige Tod-Bildschirm falls vorhanden
        clearDeathScreen();

        // Setze alle Spiel-Zustandsvariablen zurück
        isDead = false;
        camPanning = false;
        cameraPanTargetX = -1;
        currentAutoScrollSpeed = AUTO_SCROLL_START_SPEED;
        nextTileSpawnX = TILESTART_WORLD_WIDTH;
        lastSpawnedTile = null;

        // Entferne alten Spieler
        if (player != null && player.isActive()) {
            player.removeFromWorld();
        }

        player = null;

        // Setze Viewport/Kamera zurück (0,0), damit Spieler nicht sofort nach Restart stirbt
        try {
            if (getGameScene() != null && getGameScene().getViewport() != null) {
                getGameScene().getViewport().setX(0);
            }
        } catch (Exception ignore) {
            // defensiv: falls Szene nicht bereit ist, ignoriere
        }

        // Entferne alle verbleibenden Entities (Tiles, Münzen, Hazards) für sauberen Start
        try {
            if (getGameWorld() != null) {
                // Entferne eine Kopie um Concurrent Modification zu vermeiden
                for (Entity e : getGameWorld().getEntitiesCopy()) {
                    // Behalte nichts vom letzten Run
                    e.removeFromWorld();
                }
            }
        } catch (Exception ignore) {
            // defensiv
        }

        // Setze Parallax-Ebenen auf Anfangspositionen zurück
        try {
            if (bgLayer1a != null) bgLayer1a.setTranslateX(0);
            if (bgLayer1b != null) bgLayer1b.setTranslateX(VIEWPORT_W);
            if (bgLayer2a != null) bgLayer2a.setTranslateX(0);
            if (bgLayer2b != null) bgLayer2b.setTranslateX(VIEWPORT_W);
            if (bgLayer3  != null) bgLayer3.setTranslateX(0);
        } catch (Exception ignore) {
        }

        // Setze Input/Auto-Scroll-Trigger zurück
        firstInputReceived = false;
        autoScrollActive = false;
    }



    private void spawnBox(String type, double x, double y, int w, int h) {
        spawn(type, new SpawnData(x, y).put("width", w).put("height", h));
    }

    private void initParallaxBackground() {
        // Lade die drei Hintergrund-Layer-Bilder
        Image imgSky = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer1_sky.png"));
        Image imgMid = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer2_mid.png"));
        Image imgGnd = new Image(getClass().getResourceAsStream("/assets/textures/bg_layer3_ground.png"));

        // Erstelle je zwei ImageViews pro Layer für nahtlose Schleife
        bgLayer1a = new ImageView(imgSky);
        bgLayer1b = new ImageView(imgSky);
        bgLayer2a = new ImageView(imgMid);
        bgLayer2b = new ImageView(imgMid);
        bgLayer3  = new ImageView(imgGnd);

        // Stelle alle Layer auf Viewport-Größe und deaktiviere Seitenverhältnis-Erhaltung
        for (ImageView iv : new ImageView[]{bgLayer1a, bgLayer1b, bgLayer2a, bgLayer2b, bgLayer3}) {
            iv.setFitWidth(VIEWPORT_W);
            iv.setFitHeight(VIEWPORT_H);
            iv.setPreserveRatio(false);
        }

        // Positioniere die zweiten Views rechts neben den ersten (für Schleifen-Effekt)
        bgLayer1b.setTranslateX(VIEWPORT_W);
        bgLayer2b.setTranslateX(VIEWPORT_W);

        // Kombiniere alle Layer in einer Pane und füge sie zum Hintergrund hinzu (Index 0 = ganz hinten)
        Pane bgPane = new Pane(bgLayer1a, bgLayer1b, bgLayer2a, bgLayer2b, bgLayer3);
        bgPane.setMouseTransparent(true);
        getGameScene().getRoot().getChildren().add(0, bgPane);
    }

    @Override
    protected void onUpdate(double tpf) {
        // Falls Spieler tot ist, aktualisiere nichts (keine Bewegung oder Spawning)
        if (isDead) {
            return;
        }

        // Beschleunige Auto-Scroll wenn aktiv, bis zur maximalen Geschwindigkeit
        if (autoScrollActive) {
            currentAutoScrollSpeed = Math.min(
                    currentAutoScrollSpeed + AUTO_SCROLL_ACCEL_PER_SEC * tpf,
                    AUTO_SCROLL_MAX_SPEED
            );
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // KAMERA-VERSCHIEBUNG (Auto-Scroll oder Intro-Pan)
        // ═══════════════════════════════════════════════════════════════════════════
        if (camPanning) {
            // "Intro-Pan": Bewege Kamera sanft von 0 bis cameraPanTargetX
            double currentX = getGameScene().getViewport().getX();
            double panSpeed = autoScrollActive ? currentAutoScrollSpeed : CAMERA_PAN_FALLBACK_SPEED;
            double newX     = currentX + panSpeed * tpf;
            if (newX >= cameraPanTargetX) {
                newX       = cameraPanTargetX;
                camPanning = false;  // Pan beendet, wechsle zu normalem Auto-Scroll
            }
            getGameScene().getViewport().setX(newX);
        } else {
            // Normaler Auto-Scroll: Bewege Kamera kontinuierlich mit currentAutoScrollSpeed
            double currentX = getGameScene().getViewport().getX();
            getGameScene().getViewport().setX(currentX + currentAutoScrollSpeed * tpf);
        }

        double cameraX = getGameScene().getViewport().getX();

        // ═══════════════════════════════════════════════════════════════════════════
        // TOD-PRÜFUNG: Spieler fällt ins Void oder wird von Kamera überholt
        // ═══════════════════════════════════════════════════════════════════════════
        if (isPlayerBelowVoid() || isPlayerBehindCamera(cameraX)) {
            String reason = isPlayerBelowVoid() ? DEATH_REASON_VOID : DEATH_REASON_CAMERA;
            handlePlayerDeath(reason);
            return;
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // TILE-SPAWNING: Wenn Kamera nahe am nächsten Spawn-Punkt, spawne neues Tile
        // ═══════════════════════════════════════════════════════════════════════════
        while (cameraX + VIEWPORT_W >= nextTileSpawnX) {
            if (!spawnTiles()) {
                break;  // Falls spawning fehlschlägt, beende Loop
            }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // PARALLAX-EFFEKT: Scroll alle Hintergrund-Layer mit unterschiedlichen Geschwindigkeiten
        // ═══════════════════════════════════════════════════════════════════════════
        scrollLayer(bgLayer1a, bgLayer1b, cameraX * SPEED_LAYER1);  // 5% der Kamera-Position
        scrollLayer(bgLayer2a, bgLayer2b, cameraX * SPEED_LAYER2);  // 25% der Kamera-Position
    }

    private boolean isPlayerBelowVoid() {
        return player != null && player.getY() > VIEWPORT_H + DEATH_FALL_BUFFER;
    }

    private boolean isPlayerBehindCamera(double cameraX) {
        return player != null && (player.getX() + player.getWidth()) < cameraX;
    }

    private void handlePlayerDeath(String reason) {
        if (isDead) {
            return;
        }

        isDead = true;

        if (player != null && player.isActive()) {
            player.removeFromWorld();
        }

        showDeathScreen(reason);
    }

    private void showDeathScreen(String reason) {
        Text deathTitle = new Text("YOU DIED");
        deathTitle.setFont(Font.font("Arial", 84));
        deathTitle.setFill(Color.CRIMSON);
        deathTitle.setLayoutX(VIEWPORT_W / 2.0 - 210);
        deathTitle.setLayoutY(VIEWPORT_H / 2.0 - 40);

        Text deathReason = new Text(reason);
        deathReason.setFont(Font.font("Arial", 34));
        deathReason.setFill(Color.WHITE);
        deathReason.setLayoutX(VIEWPORT_W / 2.0 - 180);
        deathReason.setLayoutY(VIEWPORT_H / 2.0 + 10);

        Text restartHint = new Text("Press R to restart");
        restartHint.setFont(Font.font("Arial", 30));
        restartHint.setFill(Color.LIGHTGRAY);
        restartHint.setLayoutX(VIEWPORT_W / 2.0 - 155);
        restartHint.setLayoutY(VIEWPORT_H / 2.0 + 70);

        deathOverlay = new Pane(deathTitle, deathReason, restartHint);
        deathOverlay.setMouseTransparent(true);
        getGameScene().getRoot().getChildren().add(deathOverlay);
    }

    /**
     * Scrolle zwei ImageViews für nahtlose Parallax-Schleife.
     *
     * Wie es funktioniert:
     * - offset ist die gesamte Scroll-Distanz (z.B. cameraX * SPEED_LAYER = 5% oder 25% der Kamera-Position)
     * - mod ist der "Fraktional-Teil" der Scroll-Distanz: offset % VIEWPORT_W
     * - Das bedeutet: Wenn wir 2400px weit scrollten und Viewport ist 1920px breit,
     *   dann ist mod = 480px (wie weit a nach links verschoben ist)
     * - Verschiebe 'a' um -480px nach links (sodass 480px von der rechten Seite verschwindet)
     * - Verschiebe 'b' um (1920 - 480) = 1440px nach rechts (nächst zum Start von a)
     * - Wenn 'a' komplett nach links verschwindet, ist 'b' vollständig sichtbar, dann frisch neuer Cycle
     *
     * Ergebnis: Ständiges nahtloses Looping des Bildes ohne Lücken.
     */
    private void scrollLayer(ImageView a, ImageView b, double offset) {
        double mod = offset % VIEWPORT_W;
        a.setTranslateX(-mod);           // Verschiebe 'a' nach links
        b.setTranslateX(VIEWPORT_W - mod); // Positioniere 'b' direkt rechts neben 'a'
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 980);

        if (!inputsRegistered) {
            registerPlayerInputs();
            inputsRegistered = true;
        }

        onCollisionBegin(EntityType.Player, EntityType.Coin, (_, coin) -> {
            coin.removeFromWorld();
            if (coin.getProperties().exists("spawnName")) {
                String spawnName = coin.getProperties().getString("spawnName");
                if ("Death".equals(spawnName)) {
                    handlePlayerDeath(DEATH_REASON_GENERAL);
                    return;
                }
            }

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
                if (player != null && !isDead) {
                    notifyFirstInput();
                    var pc = player.getComponent(PlayerComponent.class);
                    double sp = pc != null ? pc.getSpeed() : 250.0;
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(sp);
                    player.setScaleX(1);
                }
            }
            @Override protected void onActionEnd() {
                if (player != null && player.isActive() && player.hasComponent(com.almasb.fxgl.physics.PhysicsComponent.class)) {
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(0);
                }
            }
        }, javafx.scene.input.KeyCode.D);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Move Left") {
            @Override protected void onAction() {
                if (player != null && !isDead) {
                    notifyFirstInput();
                    var pc = player.getComponent(PlayerComponent.class);
                    double sp = pc != null ? pc.getSpeed() : 250.0;
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(-sp);
                    player.setScaleX(-1);
                }
            }
            @Override protected void onActionEnd() {
                if (player != null && player.isActive() && player.hasComponent(com.almasb.fxgl.physics.PhysicsComponent.class)) {
                    player.getComponent(com.almasb.fxgl.physics.PhysicsComponent.class).setVelocityX(0);
                }
            }
        }, javafx.scene.input.KeyCode.A);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Jump") {
            @Override protected void onActionBegin() {
                if (player != null && !isDead) {
                    notifyFirstInput();
                    var c = player.getComponent(PlayerComponent.class);
                    if (c != null) c.jump();
                }
            }
        }, javafx.scene.input.KeyCode.SPACE);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Toggle Cheat Mode") {
            @Override protected void onActionBegin() {
                if (player != null && !isDead) {
                    notifyFirstInput();
                    var c = player.getComponent(PlayerComponent.class);
                    if (c != null) c.toggleCheatMode();
                }
            }
        }, javafx.scene.input.KeyCode.I);

        getInput().addAction(new com.almasb.fxgl.input.UserAction("Restart") {
            @Override protected void onActionBegin() {
                if (isDead) {
                    clearDeathScreen();
                    resetRunState();
                    getGameController().startNewGame();
                }
            }
        }, javafx.scene.input.KeyCode.R);
    }

    private void clearDeathScreen() {
        if (deathOverlay != null) {
            getGameScene().getRoot().getChildren().remove(deathOverlay);
            deathOverlay = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
