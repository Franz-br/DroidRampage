package at.htl.droidrampage;

import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.Screen;

/**
 * Hilfen, damit Pixel-Grafiken scharf bleiben.
 *
 * JavaFX interpoliert Bilder (bilinear), sobald sie skaliert oder auf halbe Pixel verschoben werden –
 * auf vielen Systemen sogar trotz {@code ImageView.setSmooth(false)}. Deshalb:
 *  - Vergrößerungen macht {@link #scaleNearest} vorab selbst (jedes Pixel wird zu einem Block),
 *  - {@link #snapToPixels} hält Sprites auf ganzen Bildschirm-Pixeln, auch wenn die Physik Kommazahlen liefert.
 *
 * Auf größeren Bildschirmen (z.B. 2560×1440) wird das ganze Spiel von FXGL hochskaliert (dort ×1.333).
 * Damit es trotzdem scharf bleibt, liegen alle Sprites {@link #FACTOR}-fach vergrößert im Speicher
 * ("Sharp Bilinear"): JavaFX muss dann nur noch leicht verkleinern statt verschwommen zu vergrößern.
 */
public final class PixelArt {

    private PixelArt() {}

    /** Ganzzahlige Vorab-Vergrößerung aller Sprites (mind. 2, bei 4K-Bildschirmen mehr). */
    public static final int FACTOR = computeFactor();

    /** Wie viele echte Bildschirm-Pixel ein Spiel-Pixel gerade groß ist (1.0 bei 1920×1080, 1.333 bei 2560×1440). */
    private static double deviceScale = 1.0;

    private static int computeFactor() {
        try {
            Screen s = Screen.getPrimary();
            double scale = Math.min(s.getBounds().getWidth() * s.getOutputScaleX() / 1920.0,
                                    s.getBounds().getHeight() * s.getOutputScaleY() / 1080.0);
            return (int) Math.max(2, Math.min(4, Math.ceil(scale - 0.01)));
        } catch (Exception e) {
            return 2;
        }
    }

    public static void setDeviceScale(double scale) {
        if (scale > 0 && Double.isFinite(scale)) deviceScale = scale;
    }

    /** Rundet eine Spiel-Koordinate so, dass sie auf einem ganzen Bildschirm-Pixel landet. */
    public static double snap(double v) {
        return Math.rint(v * deviceScale) / deviceScale;
    }

    /** Sprite-Sheet in {@link #FACTOR}-facher Größe (für ImageViews mit fitWidth/fitHeight in Spielgröße). */
    public static Image sharp(Image src) {
        return scaleNearest(src, FACTOR);
    }

    /** Ausschnitt von Frame {@code index} in einem mit {@link #sharp} vergrößerten Sheet. */
    public static Rectangle2D frame(int index, int frameW, int frameH) {
        return new Rectangle2D(index * frameW * FACTOR, 0, frameW * FACTOR, frameH * FACTOR);
    }

    /** Vergrößert ein Bild um einen ganzzahligen Faktor ohne Weichzeichnen (Nearest Neighbor). */
    public static Image scaleNearest(Image src, int factor) {
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        PixelReader in = src.getPixelReader();
        WritableImage out = new WritableImage(w * factor, h * factor);
        PixelWriter px = out.getPixelWriter();
        int[] row = new int[w * factor];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = in.getArgb(x, y);
                for (int i = 0; i < factor; i++) row[x * factor + i] = argb;
            }
            for (int i = 0; i < factor; i++) {
                px.setPixels(0, y * factor + i, w * factor, 1,
                        javafx.scene.image.PixelFormat.getIntArgbInstance(), row, 0, w * factor);
            }
        }
        return out;
    }

    /**
     * Verschiebt {@code node} jeweils um den Nachkomma-Anteil der Entity-Position zurück,
     * sodass er immer auf ganzen Pixeln gezeichnet wird (die Kamera steht ebenfalls auf ganzen Pixeln).
     * {@code node} sollte ein eigener Container um das Sprite sein, weil hier seine translateX/Y gesetzt wird.
     */
    public static void snapToPixels(Entity entity, Node node) {
        Runnable snap = () -> {
            node.setTranslateX(snap(entity.getX()) - entity.getX());
            node.setTranslateY(snap(entity.getY()) - entity.getY());
        };
        entity.xProperty().addListener((obs, o, n) -> snap.run());
        entity.yProperty().addListener((obs, o, n) -> snap.run());
        snap.run();
    }
}
