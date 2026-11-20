package at.htl.droidrampage;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Startbildschirm: Titel, Droiden-Auswahl (Skins) und Highscore-Liste.
 * Die Steuerung (A/D, Enter) passiert in GameApp, hier wird nur angezeigt.
 */
public class StartMenu {

    public static final String FONT = "Monospaced";
    // Ganzzahlig: Die Sheets werden vorab per Nearest Neighbor vergrößert (PixelArt) → scharfe Pixel
    private static final int SPRITE_SCALE = 3;

    private final StackPane root;
    private final List<VBox> cards = new ArrayList<>();
    private final List<ImageView> sprites = new ArrayList<>();
    private final Text title;
    private DroidSkin selected;
    private double time;

    public StartMenu(double width, double height, DroidSkin selected, List<HighScoreManager.Entry> scores) {
        this.selected = selected;

        title = new Text("DROID RAMPAGE");
        title.setFont(Font.font(FONT, FontWeight.BOLD, 112));
        title.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5ff4ff")), new Stop(1, Color.web("#ff4fd8"))));
        DropShadow titleGlow = new DropShadow(36, Color.web("#5ff4ff"));
        titleGlow.setSpread(0.15);
        title.setEffect(titleGlow);

        Text subtitle = text("Endless neon runner  ·  choose your droid", 28, Color.web("#b9c2e6"));

        HBox cardRow = new HBox(28);
        cardRow.setAlignment(Pos.CENTER);
        for (DroidSkin skin : DroidSkin.values()) {
            VBox card = buildCard(skin);
            cards.add(card);
            cardRow.getChildren().add(card);
        }

        HBox center = new HBox(56, cardRow, buildScoreBoard(scores));
        center.setAlignment(Pos.CENTER);

        Text hint = text("◀ A / D ▶  choose droid        ENTER / SPACE  start", 26, Color.web("#e8ecff"));
        Text controls = text("In game:  A / D move   ·   SPACE / W jump   ·   F attack   ·   G bomb (GRUMP)   ·   jump on enemies to stomp", 22,
                Color.web("#8c95b8"));

        VBox layout = new VBox(26, title, subtitle, center, hint, controls);
        layout.setAlignment(Pos.CENTER);
        VBox.setMargin(center, new Insets(24, 0, 24, 0));

        Rectangle dim = new Rectangle(width, height);
        dim.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(6, 4, 20, 0.88)), new Stop(1, Color.rgb(12, 4, 30, 0.78))));

        root = new StackPane(dim, layout);
        root.setPrefSize(width, height);

        refreshSelection();
    }

    private VBox buildCard(DroidSkin skin) {
        ImageView sprite = new ImageView(PixelArt.scaleNearest(skin.getRawSheet(), SPRITE_SCALE * PixelArt.FACTOR));
        sprite.setViewport(frame(DroidSkin.FRAME_IDLE));
        sprite.setFitWidth(DroidSkin.FRAME_W * SPRITE_SCALE);
        sprite.setFitHeight(DroidSkin.FRAME_H * SPRITE_SCALE);
        sprites.add(sprite);

        StackPane spriteBox = new StackPane(sprite);
        spriteBox.setPrefHeight(DroidSkin.FRAME_H * SPRITE_SCALE + 10);

        Text name = text(skin.getDisplayName(), 40, skin.getAccent());
        name.setFont(Font.font(FONT, FontWeight.BOLD, 40));
        Text inspired = text(skin.getInspiredBy(), 17, Color.web("#a3abcc"));
        Text perk = text(skin.getPerk(), 19, Color.web("#e8ecff"));
        Text attack = text(skin.getAttack(), 17, skin.getAccent().brighter());

        VBox card = new VBox(10, spriteBox, name, inspired,
                statBar("SPEED", skin.speedRatio(), skin.getAccent()),
                statBar("JUMP ", skin.jumpRatio(), skin.getAccent()),
                perk, attack);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22, 18, 22, 18));
        card.setPrefWidth(320);
        return card;
    }

    private HBox statBar(String label, double ratio, Color accent) {
        Rectangle bg = new Rectangle(150, 10, Color.web("#23283f"));
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        Rectangle fill = new Rectangle(Math.max(12, 150 * ratio), 10, accent);
        fill.setArcWidth(10);
        fill.setArcHeight(10);
        fill.setEffect(new DropShadow(8, accent));
        StackPane bar = new StackPane(bg, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        HBox row = new HBox(12, text(label, 18, Color.web("#c9d0ee")), bar);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private VBox buildScoreBoard(List<HighScoreManager.Entry> scores) {
        Text header = text("HIGH SCORES", 40, Color.web("#ffd24a"));
        header.setFont(Font.font(FONT, FontWeight.BOLD, 40));
        header.setEffect(new DropShadow(18, Color.web("#ffb000")));

        GridPane grid = new GridPane();
        grid.setHgap(26);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        if (scores.isEmpty()) {
            grid.add(text("No runs yet –", 24, Color.web("#a3abcc")), 0, 0);
            grid.add(text("be the first!", 24, Color.web("#a3abcc")), 0, 1);
        } else {
            String[] heads = {"#", "SCORE", "DROID", "DIST", "DATE"};
            for (int c = 0; c < heads.length; c++) {
                grid.add(text(heads[c], 18, Color.web("#7f88aa")), c, 0);
            }
            for (int i = 0; i < scores.size(); i++) {
                HighScoreManager.Entry e = scores.get(i);
                Color c = i == 0 ? Color.web("#ffd24a") : i == 1 ? Color.web("#dfe6ff") : i == 2
                        ? Color.web("#ff9a52") : Color.web("#c9d0ee");
                grid.add(text((i + 1) + ".", 24, c), 0, i + 1);
                grid.add(text(String.valueOf(e.score()), 24, c), 1, i + 1);
                grid.add(text(e.skin(), 24, DroidSkin.byName(e.skin()).getAccent()), 2, i + 1);
                grid.add(text(e.distance() + "m", 20, Color.web("#a3abcc")), 3, i + 1);
                grid.add(text(e.date(), 18, Color.web("#7f88aa")), 4, i + 1);
            }
        }

        VBox box = new VBox(22, header, grid);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(28, 34, 28, 34));
        box.setMinWidth(520);
        box.setStyle("-fx-background-color: rgba(14,16,36,0.88);"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: #ffd24a55; -fx-border-width: 2; -fx-border-radius: 18;");
        return box;
    }

    // ─────────────────────────── Auswahl ───────────────────────────

    public void next() {
        selected = selected.next();
        refreshSelection();
    }

    public void previous() {
        selected = selected.previous();
        refreshSelection();
    }

    public DroidSkin getSelected() {
        return selected;
    }

    private void refreshSelection() {
        for (DroidSkin skin : DroidSkin.values()) {
            VBox card = cards.get(skin.ordinal());
            boolean active = skin == selected;
            String accentHex = toHex(skin.getAccent());
            card.setStyle("-fx-background-color: " + (active ? "rgba(26,30,62,0.95)" : "rgba(14,16,36,0.80)") + ";"
                    + "-fx-background-radius: 18;"
                    + "-fx-border-radius: 18;"
                    + "-fx-border-width: " + (active ? 3 : 2) + ";"
                    + "-fx-border-color: " + (active ? accentHex : "#2a2f4a") + ";");
            card.setEffect(active ? new DropShadow(34, skin.getAccent()) : null);
            card.setOpacity(active ? 1.0 : 0.6);
            // Ausgewählte Karte wird angehoben statt vergrößert (Skalieren würde die Pixel-Grafik verwischen)
            card.setTranslateY(active ? -14 : 0);
            if (!active) {
                sprites.get(skin.ordinal()).setViewport(frame(DroidSkin.FRAME_IDLE));
                sprites.get(skin.ordinal()).setTranslateY(0);
            }
        }
        time = 0;
    }

    /** Animiert den ausgewählten Droiden (läuft auf der Stelle, springt ab und zu). */
    public void update(double tpf) {
        time += tpf;
        ImageView sprite = sprites.get(selected.ordinal());
        double cycle = time % 3.0;
        int f;
        if (cycle > 2.4) {
            f = DroidSkin.FRAME_JUMP;
            sprite.setTranslateY(PixelArt.snap(-Math.sin((cycle - 2.4) / 0.6 * Math.PI) * 26));
        } else {
            f = ((int) (time / 0.14)) % 2 == 0 ? DroidSkin.FRAME_WALK_A : DroidSkin.FRAME_WALK_B;
            sprite.setTranslateY(0);
        }
        sprite.setViewport(frame(f));
        title.setOpacity(0.9 + 0.1 * Math.sin(time * 2.2));
    }

    public Node getNode() {
        return root;
    }

    // ─────────────────────────── Helfer ───────────────────────────

    private static Rectangle2D frame(int index) {
        int s = SPRITE_SCALE * PixelArt.FACTOR;
        return new Rectangle2D(index * DroidSkin.FRAME_W * s, 0, DroidSkin.FRAME_W * s, DroidSkin.FRAME_H * s);
    }

    static Text text(String s, double size, Color color) {
        Text t = new Text(s);
        t.setFont(Font.font(FONT, FontWeight.NORMAL, size));
        t.setFill(color);
        return t;
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }
}
