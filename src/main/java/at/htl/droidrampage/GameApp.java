package at.htl.droidrampage;

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.level.Level;
import com.almasb.fxgl.entity.level.tiled.TMXLevelLoader;
import com.almasb.fxgl.input.UserAction;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.almasb.fxgl.dsl.FXGL.*;

public class GameApp extends GameApplication {

    private static final int VIEWPORT_W = 120 * 16; // 1920
    // Genau Full-HD: Auf einem 1920×1080-Bildschirm wird dann 1:1 ohne Skalierung gezeichnet (scharfe Pixel).
    // Die Tiled-Maps sind 68 Tiles = 1088 px hoch – die untersten 8 px (Teil des Bodens) liegen außerhalb des Bildes.
    private static final int VIEWPORT_H = 1080;
    private static final int WORLD_H = 68 * 16;     // 1088

    // TileStart is exactly one map wide: 120 tiles × 16 pixels = 1920 pixels
    private static final double TILESTART_WORLD_WIDTH = 120 * 16.0; // 1920 -- Fehleranfällig

    private static final double SPEED_LAYER1 = 0.05;
    private static final double SPEED_LAYER2 = 0.25;



    // KONFIGURATION DER TILE-GENERIERUNG
    // ═══════════════════════════════════════════════════════════════════════════
    // Liste der verfügbaren Tile-Map-Varianten für prozedurale Generierung.
    // Das Spiel wählt zufällig aus diesen Dateien, um ein endloses Level zu erstellen.
    private static final String[] TILE_VARIANTS = {"Tile1.tmx", "Tile2.tmx", "Tile3.tmx", "Tile4.tmx", "Tile5.tmx", "Tile6.tmx", "TileStart.tmx"};

    // Breite jedes Tile-Segments in Pixeln (gleich wie TileStart.tmx: 120 Tiles × 16px = 1920px).
    // Dies ist die horizontale Distanz, die die Spielwelt voranschreitet, wenn ein neues Tile gespawnt wird.
    private static final double TILE_SEGMENT_WIDTH = TILESTART_WORLD_WIDTH;

    // Zusätzlich zu den Tiled-Maps gibt es zufällig generierte Segmente (SegmentGenerator)
    // mit beweglichen, bröckelnden und federnden Plattformen sowie Laser-Toren.
    private static final double PROCEDURAL_SEGMENT_CHANCE = 0.5;
    private static final String PROCEDURAL_SEGMENT = "<procedural>";
    private static final int SEGMENTS_UNTIL_MAX_DIFFICULTY = 15;


    // PARALLAX-HINTERGRUND-EBENEN
    // ═══════════════════════════════════════════════════════════════════════════
    // Dreilagiger Parallax-Hintergrund für Tiefeneffekt:
    // - bgLayer1a/1b (Himmel): Bewegt sich mit 5% der Kamerageschwindigkeit → erscheint am weitesten weg, am langsamsten
    // - bgLayer2a/2b (Mitte): Bewegt sich mit 25% der Kamerageschwindigkeit → erscheint im Mittelgrund
    // - bgLayer3 (Grund): Bewegt sich mit 100% der Kamerageschwindigkeit (direktes Scrollen)
    private ImageView bgLayer1a, bgLayer1b;
    private ImageView bgLayer2a, bgLayer2b;
    private ImageView bgLayer3;
    private Pane bgPane;

    // ═══════════════════════════════════════════════════════════════════════════
    // KAMERA- UND AUTO-SCROLL-KONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════
    // Auto-Scroll-System: Kamera startet bei 0 Pixel/Sekunde und beschleunigt bis zur maximalen Geschwindigkeit.
    // Dies erzeugt progressive Schwierigkeitssteigerung—früh im Spiel langsamer (Spieler kann reagieren), später schneller.
    private static final double AUTO_SCROLL_START_SPEED = 0.0;
    private static final double AUTO_SCROLL_ACCEL_PER_SEC = 5.0;
    private static final double AUTO_SCROLL_MAX_SPEED = 400.0;
    private static final double CAMERA_PAN_FALLBACK_SPEED = 0.0; // used during the initial camera pan if auto-scroll isn't active
    private static final double DEATH_FALL_BUFFER = 120.0;

    // Spieler-Start und Distanz-Berechnung (50 Pixel = 1 Meter)
    private static final double PLAYER_START_X = 144;
    private static final double PLAYER_START_Y = 780;
    private static final double PIXELS_PER_METER = 50.0;

    // Entities, die so weit links hinter der Kamera liegen, werden entfernt (Speicher/Performance)
    private static final double CLEANUP_DISTANCE = VIEWPORT_W * 2.0;
    private static final double CLEANUP_INTERVAL = 2.0;

    //Todesnachrichten
    private static final String DEATH_REASON_VOID = "You fell into the void";
    private static final String DEATH_REASON_CAMERA = "The camera overtook you";
    private static final String DEATH_REASON_GENERAL = "You were killed by the Environment";
    private static final String DEATH_REASON_LASER = "Fried by a laser gate";
    private static final String DEATH_REASON_SPIKES = "Impaled on energy spikes";

    /** Spielzustand: Startmenü → Spielen → Tot (→ Neustart oder zurück ins Menü). */
    private enum GameState { MENU, PLAYING, DEAD }

    private GameState state = GameState.MENU;
    private boolean skipMenuOnNextStart = false; // R = direkter Neustart ohne Menü
    private DroidSkin selectedSkin = DroidSkin.ASTRO;
    private final HighScoreManager highScores = new HighScoreManager();
    private final SegmentGenerator segmentGenerator = new SegmentGenerator();

    private boolean camPanning       = false;
    private double  cameraPanTargetX = -1;
    private double currentAutoScrollSpeed = AUTO_SCROLL_START_SPEED;
    private boolean autoScrollActive = false; // only accelerate when enabled (after first input + delay)
    private boolean firstInputReceived = false;
    private static final double AUTO_SCROLL_START_DELAY = 0.8; // seconds after first input to start accelerating
    private boolean isDead = false;

    private double nextTileSpawnX = TILESTART_WORLD_WIDTH;
    private String lastSpawnedTile = null;
    private int segmentsSpawned = 0;
    private double cleanupTimer = 0;
    private double furthestPlayerX = PLAYER_START_X;

    // Tastenzustand (A/← und D/→ werden zu einer Richtung zusammengefasst)
    private boolean leftKeyA, leftKeyArrow, rightKeyD, rightKeyArrow;

    // UI
    private StartMenu menu;
    private Node deathOverlay;
    private Node hud;
    private Text scoreText, statsText, bestText;
    private final List<javafx.scene.shape.Rectangle> hpCells = new ArrayList<>();
    private int lastShownScore = -1;
    private int lastShownHp = -1;
    private Text attackHint;

    private static final boolean START_FULLSCREEN = true; // true = Vollbild (F11 wechselt), false = maximiertes Fenster

    // Exakte Kameraposition; der Viewport selbst wird auf ganze Pixel gerundet, damit nichts "zwischen" Pixeln landet
    private double cameraXExact = 0;

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

        // Keine "logs/"- und "system/"-Ordner neben dem .jar anlegen: Auf Schul-PCs liegt das Spiel oft in
        // Ordnern ohne Schreibrechte. Die High Scores speichert der HighScoreManager im Benutzerordner.
        settings.setFileSystemWriteAllowed(false);
        settings.setApplicationMode(ApplicationMode.RELEASE);

        settings.setTitle("Droid Rampage");
        settings.setVersion("1.5");
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("pixelsMoved", 0);
        vars.put("credit", 0);
        vars.put("bonus", 0);   // Punkte für zerstörte Gegner
        vars.put("kills", 0);
    }

    @Override
    protected void initGame() {
        resetRunState();

        if (!START_FULLSCREEN) {
            Platform.runLater(() -> getPrimaryStage().setMaximized(true)); // Stage access must happen on JavaFX thread
        }
        // Wird das Vollbild verlassen (F11), soll das Fenster den Bildschirm trotzdem ausfüllen
        getSettings().getFullScreen().addListener((obs, was, now) -> {
            if (!now) Platform.runLater(() -> getPrimaryStage().setMaximized(true));
        });

        getGameWorld().addEntityFactory(new PlatformFactory());
        loadTmxSegment("TileStart.tmx", 0);
        initParallaxBackground();
        // Der Spieler wird erst in startRun() gespawnt (nach dem Startmenü)
    }

    /** Startet einen Run mit dem ausgewählten Droiden. */
    private void startRun() {
        hideMenu();
        state = GameState.PLAYING;
        if (hud != null) hud.setVisible(true);
        if (attackHint != null) {
            attackHint.setText(selectedSkin.getAttackHint());
            attackHint.setFill(selectedSkin.getAccent());
        }

        player = spawn("player", new SpawnData(PLAYER_START_X, PLAYER_START_Y)
                .put("width", 40)
                .put("height", 64)
                .put("skin", selectedSkin));
        furthestPlayerX = PLAYER_START_X;

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
     * 0. Mit PROCEDURAL_SEGMENT_CHANCE wird stattdessen ein zufälliges Segment generiert
     * 1. Versucht mehrere Varianten zufällig
     * 2. Vermeidet, dass das gleiche Tile zweimal hintereinander gespawnt wird
     * 3. Lädt das TMX-Level und versetzt alle Entities um offsetX
     * 4. Addiert TILE_SEGMENT_WIDTH zu nextTileSpawnX, damit das nächste Tile weiter rechts spawnt
     * 5. Gibt true zurück bei Erfolg, false wenn etwas schiefgeganen ist
     */
    private boolean spawnTiles() {
        double offsetX = nextTileSpawnX;

        // Das erste Segment nach dem Start ist immer eine handgebaute Map (sanfter Einstieg)
        if (segmentsSpawned > 0 && ThreadLocalRandom.current().nextDouble() < PROCEDURAL_SEGMENT_CHANCE) {
            segmentGenerator.generate(offsetX, difficulty());
            lastSpawnedTile = PROCEDURAL_SEGMENT;
            nextTileSpawnX += SegmentGenerator.SEGMENT_WIDTH;
            segmentsSpawned++;
            System.out.println("[TILE] Generated procedural segment at offsetX=" + offsetX);
            return true;
        }

        int variantCount = tileVariantCount();
        if (variantCount == 0) {
            System.err.println("[TILE] Keine Tile-Varianten konfiguriert");
            return false;
        }

        // Versuche mehrere Varianten, falls einige Maps fehlerhaft sind
        for (int attempt = 0; attempt < variantCount; attempt++) {
            String candidate = TILE_VARIANTS[ThreadLocalRandom.current().nextInt(variantCount)]; //Lokaler Thread-Safe-Zufallsgenerator, von außen nicht beinflussbar
            // Vermeide unmittelbare Wiederholung wenn möglich
            if (lastSpawnedTile != null && candidate.equals(lastSpawnedTile) && variantCount > 1) {
                continue;
            }

            try {
                List<Entity> added = loadTmxSegment(candidate, offsetX);

                // Gegner verteilen (nicht im allerersten Segment – sanfter Einstieg)
                if (segmentsSpawned > 0) {
                    segmentGenerator.populateTmx(added, offsetX, difficulty());
                }

                lastSpawnedTile = candidate;
                nextTileSpawnX += TILE_SEGMENT_WIDTH;
                segmentsSpawned++;
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

    private double difficulty() {
        return (double) segmentsSpawned / SEGMENTS_UNTIL_MAX_DIFFICULTY;
    }

    /**
     * Lädt eine Tiled-Map und fügt ihre Objekte um offsetX verschoben in die Welt ein.
     * Die Kachel-Ebene (alte Tileset-Grafik) wird bewusst weggelassen: Plattformen, Böden,
     * Kisten und Stacheln bekommen in der PlatformFactory dieselben Grafiken wie die
     * generierten Segmente – so sieht alles einheitlich aus.
     * Kachel-Ebenen erkennt man daran, dass sie keine Hitbox haben.
     */
    private List<Entity> loadTmxSegment(String mapName, double offsetX) {
        Level level = getAssetLoader().loadLevel(mapName, new TMXLevelLoader());
        List<Entity> added = new ArrayList<>();
        for (Entity e : level.getEntities()) {
            if (e.getBoundingBoxComponent().hitBoxesProperty().isEmpty()) {
                continue; // Kachel-Ebene
            }
            e.setPosition(e.getPosition().add(offsetX, 0));
            getGameWorld().addEntity(e);
            added.add(e);
        }
        return added;
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
        // Bereinige Tod-Bildschirm und Menü falls vorhanden
        clearDeathScreen();
        hideMenu();

        // Setze alle Spiel-Zustandsvariablen zurück
        state = GameState.MENU;
        isDead = false;
        camPanning = false;
        cameraPanTargetX = -1;
        currentAutoScrollSpeed = AUTO_SCROLL_START_SPEED;
        nextTileSpawnX = TILESTART_WORLD_WIDTH;
        lastSpawnedTile = null;
        segmentsSpawned = 0;
        cleanupTimer = 0;
        furthestPlayerX = PLAYER_START_X;
        lastShownScore = -1;
        lastShownHp = -1;

        // Entferne alten Spieler
        if (player != null && player.isActive()) {
            player.removeFromWorld();
        }

        player = null;

        // Setze Viewport/Kamera zurück (0,0), damit Spieler nicht sofort nach Restart stirbt
        try {
            if (getGameScene() != null && getGameScene().getViewport() != null) {
                cameraXExact = 0;
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


    //Hintergrund

    private static Image imgSky, imgMid, imgGnd;

    private static Image loadBackground(String file) {
        Image raw = new Image(GameApp.class.getResourceAsStream("/assets/textures/" + file));
        // Höchstens ×2: 3840×2160 passt noch sicher in jede Grafikkarten-Textur
        return PixelArt.scaleNearest(raw, Math.min(PixelArt.FACTOR, 2));
    }

    private void initParallaxBackground() {
        // Alten Hintergrund (vom letzten Run) entfernen, sonst stapeln sich die Ebenen bei jedem Neustart
        if (bgPane != null) {
            getGameScene().getContentRoot().getChildren().remove(bgPane);
        }

        // Lade die drei Hintergrund-Layer-Bilder
        // Einmal geladen und doppelt so groß vorbereitet (scharf auch auf 1440p-Bildschirmen, siehe PixelArt)
        if (imgSky == null) {
            imgSky = loadBackground("bg_layer1_sky.png");
            imgMid = loadBackground("bg_layer2_mid.png");
            imgGnd = loadBackground("bg_layer3_ground.png");
        }

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
            iv.setSmooth(false); // Bilder haben genau Viewport-Größe → keine Interpolation nötig, bleibt scharf
        }

        // Positioniere die zweiten Views rechts neben den ersten (für Schleifen-Effekt)
        bgLayer1b.setTranslateX(VIEWPORT_W);
        bgLayer2b.setTranslateX(VIEWPORT_W);

        // Kombiniere alle Layer in einer Pane und füge sie zum Hintergrund hinzu (Index 0 = ganz hinten)
        bgPane = new Pane(bgLayer1a, bgLayer1b, bgLayer2a, bgLayer2b, bgLayer3);
        bgPane.setMouseTransparent(true);
        // contentRoot wird von FXGL mitskaliert (z.B. ×1.333 auf 2560×1440) – root nicht, dort würde der Hintergrund nur eine Ecke füllen
        getGameScene().getContentRoot().getChildren().add(0, bgPane);
    }



    @Override
    protected void onUpdate(double tpf) {
        // Im Menü nur die Menü-Animation laufen lassen
        if (state == GameState.MENU) {
            if (menu != null) menu.update(tpf);
            return;
        }

        // Falls Spieler tot ist, aktualisiere nichts (keine Bewegung oder Spawning)
        if (isDead) {
            return;
        }

        // Tastenzustand an den Spieler weitergeben
        if (player != null && player.isActive()) {
            int dir = ((rightKeyD || rightKeyArrow) ? 1 : 0) - ((leftKeyA || leftKeyArrow) ? 1 : 0);
            player.getComponent(PlayerComponent.class).setMoveDirection(dir);
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
            double panSpeed = autoScrollActive ? currentAutoScrollSpeed : CAMERA_PAN_FALLBACK_SPEED;
            double newX     = cameraXExact + panSpeed * tpf;
            if (newX >= cameraPanTargetX) {
                newX       = cameraPanTargetX;
                camPanning = false;  // Pan beendet, wechsle zu normalem Auto-Scroll
            }
            cameraXExact = newX;
        } else {
            // Normaler Auto-Scroll: Bewege Kamera kontinuierlich mit currentAutoScrollSpeed
            cameraXExact += currentAutoScrollSpeed * tpf;
        }
        // Auf ganze Bildschirm-Pixel runden: Bei halben Pixeln würde JavaFX Texturen interpolieren → verschwommen
        PixelArt.setDeviceScale(currentDeviceScale());
        double cameraX = PixelArt.snap(cameraXExact);
        getGameScene().getViewport().setX(cameraX);

        // ═══════════════════════════════════════════════════════════════════════════
        // TOD-PRÜFUNG: Spieler fällt ins Void, wird von Kamera überholt oder vom Laser getroffen
        // ═══════════════════════════════════════════════════════════════════════════
        if (isPlayerBelowVoid() || isPlayerBehindCamera(cameraX)) {
            String reason = isPlayerBelowVoid() ? DEATH_REASON_VOID : DEATH_REASON_CAMERA;
            handlePlayerDeath(reason);
            return;
        }
        if (isPlayerHitByLaser()) {
            handlePlayerDeath(DEATH_REASON_LASER);
            return;
        }
        checkSawHits();
        PlayerComponent pc = player != null ? player.getComponent(PlayerComponent.class) : null;
        if (pc != null && pc.isDestroyed()) {
            handlePlayerDeath(pc.getDeathMessage());
            return;
        }
        if (pc != null && pc.getHp() != lastShownHp) {
            lastShownHp = pc.getHp();
            refreshHpBar(lastShownHp);
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

        updateScore();

        // Alte Entities weit hinter der Kamera aufräumen
        cleanupTimer += tpf;
        if (cleanupTimer >= CLEANUP_INTERVAL) {
            cleanupTimer = 0;
            removeEntitiesBehind(cameraX - CLEANUP_DISTANCE);
        }
    }

    /** Echte Bildschirm-Pixel pro Spiel-Pixel (FXGL-Skalierung × Windows/macOS-Anzeigeskalierung). */
    private double currentDeviceScale() {
        double scale = Math.min(getSettings().getActualWidth() / VIEWPORT_W,
                                getSettings().getActualHeight() / VIEWPORT_H);
        var window = getPrimaryStage();
        return window != null ? scale * window.getOutputScaleX() : scale;
    }

    private boolean isPlayerBelowVoid() {
        return player != null && player.getY() > WORLD_H + DEATH_FALL_BUFFER;
    }

    private boolean isPlayerBehindCamera(double cameraX) {
        return player != null && (player.getX() + player.getWidth()) < cameraX;
    }

    private boolean isPlayerHitByLaser() {
        if (player == null) return false;
        for (Entity hazard : getGameWorld().getEntitiesByType(EntityType.Hazard)) {
            LaserComponent laser = hazard.getComponentOptional(LaserComponent.class).orElse(null);
            if (laser != null && laser.isLethal() && player.isColliding(hazard)) {
                return true;
            }
        }
        return false;
    }

    private void checkSawHits() {
        if (player == null) return;
        for (Entity hazard : getGameWorld().getEntitiesByType(EntityType.Hazard)) {
            if (hazard.hasComponent(SawComponent.class) && player.isColliding(hazard)) {
                double dir = Math.signum(Combat.center(player).getX() - Combat.center(hazard).getX());
                player.getComponent(PlayerComponent.class).damage(1, SawComponent.KILL_MESSAGE, dir == 0 ? 1 : dir);
            }
        }
    }

    private void removeEntitiesBehind(double minX) {
        for (Entity e : getGameWorld().getEntitiesCopy()) {
            if (e != player && e.getX() < minX) {
                e.removeFromWorld();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SCORE: Credits + zurückgelegte Meter
    // ═══════════════════════════════════════════════════════════════════════════

    private int distanceMeters() {
        return (int) Math.max(0, (furthestPlayerX - PLAYER_START_X) / PIXELS_PER_METER);
    }

    private int currentScore() {
        return geti("credit") + distanceMeters() + geti("bonus");
    }

    private void updateScore() {
        if (player == null) return;
        furthestPlayerX = Math.max(furthestPlayerX, player.getX());
        int score = currentScore();
        if (score != lastShownScore) {
            lastShownScore = score;
            refreshHud();
        }
    }

    private void refreshHud() {
        if (scoreText == null) return;
        int score = currentScore();
        scoreText.setText("SCORE " + score);
        statsText.setText(distanceMeters() + " m   ·   " + geti("credit") + " credits   ·   " + geti("kills") + " kills");
        int best = Math.max(highScores.best(), score);
        bestText.setText("BEST " + best);
    }

    private void handlePlayerDeath(String reason) {
        if (isDead) {
            return;
        }

        isDead = true;
        state = GameState.DEAD;
        updateScore();

        if (player != null && player.isActive()) {
            player.removeFromWorld();
        }

        int score = currentScore();
        int previousBest = highScores.best();
        int rank = highScores.submit(score, distanceMeters(), geti("credit"), selectedSkin);
        showDeathScreen(reason, score, rank, previousBest);
    }

    private void showDeathScreen(String reason, int score, int rank, int previousBest) {
        clearDeathScreen();

        Text deathTitle = new Text("YOU DIED");
        deathTitle.setFont(Font.font(StartMenu.FONT, FontWeight.BOLD, 96));
        deathTitle.setFill(Color.web("#ff3048"));
        DropShadow glow = new DropShadow(36, Color.web("#ff3048"));
        glow.setSpread(0.2);
        deathTitle.setEffect(glow);

        Text deathReason = StartMenu.text(reason, 34, Color.WHITE);

        Text scoreLine = new Text("SCORE " + score);
        scoreLine.setFont(Font.font(StartMenu.FONT, FontWeight.BOLD, 64));
        scoreLine.setFill(Color.web("#5ff4ff"));
        scoreLine.setEffect(new DropShadow(24, Color.web("#5ff4ff")));

        Text details = StartMenu.text(distanceMeters() + " m   ·   " + geti("credit") + " credits   ·   "
                + geti("kills") + " kills   ·   " + selectedSkin.getDisplayName(), 28, Color.web("#c9d0ee"));

        Text rankLine;
        if (rank == 1) {
            rankLine = new Text("NEW HIGH SCORE!");
            rankLine.setFont(Font.font(StartMenu.FONT, FontWeight.BOLD, 44));
            rankLine.setFill(Color.web("#ffd24a"));
            rankLine.setEffect(new DropShadow(24, Color.web("#ffb000")));
            FadeTransition pulse = new FadeTransition(Duration.seconds(0.6), rankLine);
            pulse.setFromValue(1.0);
            pulse.setToValue(0.35);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        } else if (rank > 1) {
            rankLine = StartMenu.text("#" + rank + " on the leaderboard", 34, Color.web("#ffd24a"));
        } else {
            rankLine = StartMenu.text("Best: " + previousBest, 30, Color.web("#a3abcc"));
        }

        Text restartHint = StartMenu.text("R / ENTER  restart        M  main menu", 28, Color.web("#e8ecff"));

        VBox box = new VBox(18, deathTitle, deathReason, scoreLine, details, rankLine, restartHint);
        box.setAlignment(Pos.CENTER);
        VBox.setMargin(restartHint, new javafx.geometry.Insets(30, 0, 0, 0));

        Rectangle dim = new Rectangle(VIEWPORT_W, VIEWPORT_H, Color.rgb(4, 2, 14, 0.7));
        StackPane overlay = new StackPane(dim, box);
        overlay.setPrefSize(VIEWPORT_W, VIEWPORT_H);
        overlay.setMouseTransparent(true);

        deathOverlay = overlay;
        getGameScene().addUINode(deathOverlay);
    }

    /**
     * Scrolle zwei ImageViews für nahtlose Parallax-Schleife.
     * Parallax-Schleife: Nahtlos wiederholender Hintergrund,
     * mit verschiedenen Ebenen die unterschiedlich schnell sind
     *
     * Wie es funktioniert:
     * - offset ist gesamte Scroll-Distanz
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
        double mod = PixelArt.snap(offset % VIEWPORT_W); // ganze Bildschirm-Pixel → scharf
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

        onCollisionBegin(EntityType.Player, EntityType.Coin, (playerEntity, coin) -> {
            coin.removeFromWorld();
            if (coin.getProperties().exists("spawnName")) {
                String spawnName = coin.getProperties().getString("spawnName");
                if ("Death".equals(spawnName)) {
                    handlePlayerDeath(DEATH_REASON_SPIKES);
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
        buildHud();

        // Direkt weiterspielen (R) oder zuerst das Startmenü zeigen
        if (skipMenuOnNextStart) {
            skipMenuOnNextStart = false;
            startRun();
        } else {
            showMenu();
        }
    }

    private void buildHud() {
        if (hud != null) {
            getGameScene().removeUINode(hud);
        }

        scoreText = new Text("SCORE 0");
        scoreText.setFont(Font.font(StartMenu.FONT, FontWeight.BOLD, 44));
        scoreText.setFill(Color.web("#5ff4ff"));
        scoreText.setEffect(new DropShadow(16, Color.web("#5ff4ff")));
        scoreText.setLayoutX(24);
        scoreText.setLayoutY(56);

        statsText = StartMenu.text("0 m   ·   0 credits", 24, Color.web("#c9d0ee"));
        statsText.setLayoutX(26);
        statsText.setLayoutY(92);

        bestText = new Text("BEST " + highScores.best());
        bestText.setFont(Font.font(StartMenu.FONT, FontWeight.BOLD, 30));
        bestText.setFill(Color.web("#ffd24a"));
        bestText.setEffect(new DropShadow(12, Color.web("#ffb000")));
        bestText.setLayoutX(VIEWPORT_W - 300);
        bestText.setLayoutY(52);

        // Update UI when the 'credit' / 'bonus' world properties change
        getWorldProperties().intProperty("credit").addListener((obs, oldValue, newValue) -> refreshHud());
        getWorldProperties().intProperty("bonus").addListener((obs, oldValue, newValue) -> refreshHud());

        // Energiezellen = Lebenspunkte
        Text hpLabel = StartMenu.text("HP", 22, Color.web("#c9d0ee"));
        hpLabel.setLayoutX(26);
        hpLabel.setLayoutY(132);
        hpCells.clear();
        for (int i = 0; i < PlayerComponent.MAX_HP; i++) {
            javafx.scene.shape.Rectangle cell = new javafx.scene.shape.Rectangle(64 + i * 40, 114, 32, 20);
            cell.setArcWidth(6);
            cell.setArcHeight(6);
            cell.setStroke(Color.web("#0a0d18"));
            hpCells.add(cell);
        }
        refreshHpBar(PlayerComponent.MAX_HP);

        // Angriffs-Hinweis für den gewählten Droiden
        attackHint = StartMenu.text(selectedSkin.getAttackHint(), 22, selectedSkin.getAccent());
        attackHint.setLayoutX(26);
        attackHint.setLayoutY(170);
        attackHint.setEffect(new DropShadow(8, Color.BLACK));

        Pane hudPane = new Pane(scoreText, statsText, bestText, hpLabel, attackHint);
        hudPane.getChildren().addAll(hpCells);
        hudPane.setMouseTransparent(true);
        hudPane.setVisible(state != GameState.MENU);
        hud = hudPane;
        getGameScene().addUINode(hud);
    }

    private void refreshHpBar(int hp) {
        for (int i = 0; i < hpCells.size(); i++) {
            boolean full = i < hp;
            javafx.scene.shape.Rectangle cell = hpCells.get(i);
            cell.setFill(full ? Color.web("#5dff8f") : Color.web("#1d2236"));
            cell.setEffect(full ? new DropShadow(10, Color.web("#5dff8f")) : null);
        }
    }

    private void showMenu() {
        hideMenu();
        state = GameState.MENU;
        if (hud != null) hud.setVisible(false);
        menu = new StartMenu(VIEWPORT_W, VIEWPORT_H, selectedSkin, highScores.top(8));
        getGameScene().addUINode(menu.getNode());
    }

    private void hideMenu() {
        if (menu != null) {
            getGameScene().removeUINode(menu.getNode());
            selectedSkin = menu.getSelected();
            menu = null;
        }
    }

    /** Neustart: direkt weiterspielen oder zurück ins Menü. */
    private void restart(boolean skipMenu) {
        clearDeathScreen();
        skipMenuOnNextStart = skipMenu;
        resetRunState();
        getGameController().startNewGame();
    }

    private void registerPlayerInputs() {
        // Bewegung: A/D und Pfeiltasten. Im Menü wechseln sie den Droiden.
        getInput().addAction(movementAction("Move Right", true, false), KeyCode.D);
        getInput().addAction(movementAction("Move Right (Arrow)", true, true), KeyCode.RIGHT);
        getInput().addAction(movementAction("Move Left", false, false), KeyCode.A);
        getInput().addAction(movementAction("Move Left (Arrow)", false, true), KeyCode.LEFT);

        // Springen: SPACE startet im Menü zusätzlich das Spiel
        getInput().addAction(jumpAction("Jump", true), KeyCode.SPACE);
        // Zusätzliches Binding: W / ↑ sollen ebenfalls springen (separate Action-Namen verhindern Duplikate)
        getInput().addAction(jumpAction("Jump (W)", false), KeyCode.W);
        getInput().addAction(jumpAction("Jump (Up)", false), KeyCode.UP);

        // Angriffe: F / J = Haupt-Angriff, G / K = Spezial (GRUMP: Bombe)
        getInput().addAction(combatAction("Attack", false), KeyCode.F);
        getInput().addAction(combatAction("Attack (J)", false), KeyCode.J);
        getInput().addAction(combatAction("Special", true), KeyCode.G);
        getInput().addAction(combatAction("Special (K)", true), KeyCode.K);

        getInput().addAction(new UserAction("Start") {
            @Override protected void onActionBegin() {
                if (state == GameState.MENU) {
                    startRun();
                } else if (state == GameState.DEAD) {
                    restart(true);
                }
            }
        }, KeyCode.ENTER);

        getInput().addAction(new UserAction("Toggle Cheat Mode") {
            @Override protected void onActionBegin() {
                if (player != null && state == GameState.PLAYING) {
                    notifyFirstInput();
                    var c = player.getComponent(PlayerComponent.class);
                    if (c != null) c.toggleCheatMode();
                }
            }
        }, KeyCode.I);

        getInput().addAction(new UserAction("Restart") {
            @Override protected void onActionBegin() {
                if (state == GameState.DEAD) {
                    restart(true);
                }
            }
        }, KeyCode.R);

        getInput().addAction(new UserAction("Main Menu") {
            @Override protected void onActionBegin() {
                if (state == GameState.DEAD) {
                    restart(false);
                }
            }
        }, KeyCode.M);

        getInput().addAction(new UserAction("Toggle Fullscreen") {
            @Override protected void onActionBegin() {
                var fullScreen = getSettings().getFullScreen();
                fullScreen.set(!fullScreen.get());
            }
        }, KeyCode.F11);
    }

    private UserAction movementAction(String name, boolean right, boolean arrow) {
        return new UserAction(name) {
            @Override protected void onActionBegin() {
                if (state == GameState.MENU) {
                    if (menu != null) {
                        if (right) menu.next(); else menu.previous();
                    }
                    return;
                }
                setKey(right, arrow, true);
                if (state == GameState.PLAYING) notifyFirstInput();
            }

            @Override protected void onActionEnd() {
                setKey(right, arrow, false);
            }
        };
    }

    private UserAction combatAction(String name, boolean special) {
        return new UserAction(name) {
            @Override protected void onActionBegin() {
                if (state != GameState.PLAYING || player == null || !player.isActive()) return;
                notifyFirstInput();
                PlayerCombatComponent combat = player.getComponent(PlayerCombatComponent.class);
                if (special) combat.special(); else combat.attack();
            }
        };
    }

    private void setKey(boolean right, boolean arrow, boolean down) {
        if (right && arrow) rightKeyArrow = down;
        else if (right) rightKeyD = down;
        else if (arrow) leftKeyArrow = down;
        else leftKeyA = down;
    }

    private UserAction jumpAction(String name, boolean startsGame) {
        return new UserAction(name) {
            @Override protected void onActionBegin() {
                if (state == GameState.MENU) {
                    if (startsGame) startRun();
                    return;
                }
                if (player != null && state == GameState.PLAYING) {
                    notifyFirstInput();
                    var c = player.getComponent(PlayerComponent.class);
                    if (c != null) c.jump();
                }
            }
        };
    }

    private void clearDeathScreen() {
        if (deathOverlay != null) {
            getGameScene().removeUINode(deathOverlay);
            deathOverlay = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
