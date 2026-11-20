package at.htl.droidrampage;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Spielbare Droiden. Jeder Skin hat ein eigenes Spritesheet
 * (4 Frames à 40x64: Idle, Walk A, Walk B, Jump) und leicht unterschiedliche Werte.
 *
 * Die Sheets werden mit tools/generate_sprites.py erzeugt.
 */
public enum DroidSkin {

    ASTRO("ASTRO", "Astromech · R2 style", "Balanced all-rounder",
            "TASER: chain-zaps 3 foes, stuns", "F  TASER  (chain-zaps up to 3 enemies and stuns them)",
            "droid_astro.png", 400, 720, Color.web("#4da3ff")),

    GRUMP("GRUMP", "Cranky astromech · Chopper style", "Fast, but jumps lower",
            "CLAW + BOMBS: close & explosive", "F  CLAW  (also blocks shots)     G  BOMB",
            "droid_grumpy.png", 470, 700, Color.web("#ff8a2a")),

    PROTO("PROTO", "Protocol droid · C-3PO style", "Slow walker, huge jumps",
            "BLASTER: long-range rapid fire", "F  BLASTER  (long-range shots)",
            "droid_protocol.png", 365, 790, Color.web("#ffd24a"));

    public static final int FRAME_W = 40;
    public static final int FRAME_H = 64;
    public static final int FRAME_IDLE = 0;
    public static final int FRAME_WALK_A = 1;
    public static final int FRAME_WALK_B = 2;
    public static final int FRAME_JUMP = 3;

    // Grenzwerte für die Statistik-Balken im Menü
    private static final double SPEED_MIN = 300, SPEED_MAX = 500;
    private static final double JUMP_MIN = 600, JUMP_MAX = 820;

    private final String displayName;
    private final String inspiredBy;
    private final String perk;
    private final String attack;
    private final String attackHint;
    private final String sheetFile;
    private final double speed;
    private final double jumpForce;
    private final Color accent;
    private Image rawSheet; // lazy geladen (JavaFX muss bereits laufen)
    private Image sheet;

    DroidSkin(String displayName, String inspiredBy, String perk, String attack, String attackHint, String sheetFile,
              double speed, double jumpForce, Color accent) {
        this.displayName = displayName;
        this.inspiredBy = inspiredBy;
        this.perk = perk;
        this.attack = attack;
        this.attackHint = attackHint;
        this.sheetFile = sheetFile;
        this.speed = speed;
        this.jumpForce = jumpForce;
        this.accent = accent;
    }

    /** Sprite-Sheet in Originalgröße (40×64 pro Frame). */
    public Image getRawSheet() {
        if (rawSheet == null) {
            rawSheet = new Image(DroidSkin.class.getResourceAsStream("/assets/textures/" + sheetFile));
        }
        return rawSheet;
    }

    /** Sprite-Sheet, {@link PixelArt#FACTOR}-fach vergrößert – Frames mit {@link PixelArt#frame} auswählen. */
    public Image getSheet() {
        if (sheet == null) {
            sheet = PixelArt.sharp(getRawSheet());
        }
        return sheet;
    }

    public String getDisplayName() { return displayName; }
    public String getInspiredBy()  { return inspiredBy; }
    public String getPerk()        { return perk; }
    public String getAttack()      { return attack; }
    public String getAttackHint()  { return attackHint; }
    public double getSpeed()       { return speed; }
    public double getJumpForce()   { return jumpForce; }
    public Color getAccent()       { return accent; }

    public double speedRatio() { return clamp01((speed - SPEED_MIN) / (SPEED_MAX - SPEED_MIN)); }
    public double jumpRatio()  { return clamp01((jumpForce - JUMP_MIN) / (JUMP_MAX - JUMP_MIN)); }

    public DroidSkin next()     { return values()[(ordinal() + 1) % values().length]; }
    public DroidSkin previous() { return values()[(ordinal() + values().length - 1) % values().length]; }

    public static DroidSkin byName(String name) {
        for (DroidSkin s : values()) {
            if (s.name().equalsIgnoreCase(name) || s.displayName.equalsIgnoreCase(name)) return s;
        }
        return ASTRO;
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}
