package at.htl.droidrampage;

import javafx.scene.image.Image;

/**
 * Die drei Gegner-Typen. Jeder spiegelt den Kampfstil eines spielbaren Droiden:
 *  ZAPPER  ↔ ASTRO  (Elektro-Nahkampf)
 *  BRUTE   ↔ GRUMP  (Nahkampf + Bomben)
 *  SNIPER  ↔ PROTO  (Fernkampf-Blaster)
 */
public enum EnemyKind {

    ZAPPER("Zapper drone", "enemy_zapper.png", 48, 40, 2, 40, "Zapped by a Zapper drone"),
    BRUTE("Brute", "enemy_brute.png", 56, 60, 4, 70, "Smashed by a Brute"),
    SNIPER("Marksman", "enemy_sniper.png", 40, 64, 2, 60, "Sniped by a Marksman");

    private final String displayName;
    private final String sheetFile;
    private final int frameW, frameH;
    private final int maxHp;
    private final int scoreValue;
    private final String killMessage;
    private Image sheet;

    EnemyKind(String displayName, String sheetFile, int frameW, int frameH, int maxHp, int scoreValue, String killMessage) {
        this.displayName = displayName;
        this.sheetFile = sheetFile;
        this.frameW = frameW;
        this.frameH = frameH;
        this.maxHp = maxHp;
        this.scoreValue = scoreValue;
        this.killMessage = killMessage;
    }

    public Image getSheet() {
        if (sheet == null) {
            sheet = new Image(EnemyKind.class.getResourceAsStream("/assets/textures/" + sheetFile));
        }
        return sheet;
    }

    public String getDisplayName() { return displayName; }
    public int getFrameW()         { return frameW; }
    public int getFrameH()         { return frameH; }
    public int getMaxHp()          { return maxHp; }
    public int getScoreValue()     { return scoreValue; }
    public String getKillMessage() { return killMessage; }
}
