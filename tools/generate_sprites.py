#!/usr/bin/env python3
"""
Generiert die Droid-Spritesheets für Droid Rampage.

Jedes Sheet hat 4 Frames à 40x64 Pixel (nebeneinander, 160x64):
  Frame 0 = Idle, Frame 1 = Walk A, Frame 2 = Walk B, Frame 3 = Jump

Die Sprites werden in 4-facher Auflösung mit Schattierung gezeichnet und
dann herunterskaliert -> glatte, detailreiche Texturen statt verwaschener
hochskalierter Pixel.

Aufruf:  python3 tools/generate_sprites.py
"""
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops

S = 4                    # Supersampling-Faktor
FW, FH = 40, 64          # Frame-Größe im Spiel
FRAMES = 4
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/textures"


# ───────────────────────── Hilfsfunktionen ─────────────────────────

def mix(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def cyl_layer(w, h, base, highlight=0.35, lo=0.55, hi=1.25):
    """Farb-Ebene mit Zylinder-Schattierung (horizontal)."""
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for x in range(w):
        t = x / max(1, w - 1)
        # Lichtkegel bei 'highlight', dunkle Ränder
        d = abs(t - highlight)
        f = hi - (hi - lo) * min(1.0, (d / 0.65) ** 1.4)
        col = shade(base, f) + (255,)
        for y in range(h):
            px[x, y] = col
    return img


class Canvas:
    """Zeichenfläche in Spielkoordinaten (40x64), intern S-fach aufgelöst."""

    def __init__(self, fw=FW, fh=FH):
        self.fw, self.fh = fw, fh
        self.img = Image.new("RGBA", (fw * S, fh * S), (0, 0, 0, 0))

    def _mask(self, fn):
        m = Image.new("L", self.img.size, 0)
        fn(ImageDraw.Draw(m))
        return m

    @staticmethod
    def r(box):
        return [v * S for v in box]

    def part(self, shape, box, base, style="cyl", highlight=0.35, outline=None):
        """Zeichnet eine schattierte Form. shape: rect|ellipse|chord|poly|rrect"""
        if shape == "rect":
            m = self._mask(lambda d: d.rectangle(self.r(box), fill=255))
        elif shape == "rrect":
            m = self._mask(lambda d: d.rounded_rectangle(self.r(box[:4]), radius=box[4] * S, fill=255))
        elif shape == "ellipse":
            m = self._mask(lambda d: d.ellipse(self.r(box), fill=255))
        elif shape == "dome":   # obere Hälfte einer Ellipse
            x0, y0, x1, y1 = box
            m = self._mask(lambda d: d.pieslice(self.r([x0, y0, x1, y1 + (y1 - y0)]), 180, 360, fill=255))
        elif shape == "poly":
            m = self._mask(lambda d: d.polygon([(x * S, y * S) for x, y in box], fill=255))
        else:
            raise ValueError(shape)

        if style == "cyl":
            layer = cyl_layer(self.img.width, self.img.height, base, highlight)
            # Zylinder relativ zur Form-Breite ausrichten
            bb = m.getbbox()
            if bb:
                w = bb[2] - bb[0]
                layer = Image.new("RGBA", self.img.size)
                layer.paste(cyl_layer(w, self.img.height, base, highlight), (bb[0], 0))
        else:
            layer = Image.new("RGBA", self.img.size, tuple(base[:3]) + (255,))
        self.img.paste(layer, (0, 0), m)
        if outline:
            edge = m.filter(ImageFilter.MaxFilter(5))
            edge = ImageChops.subtract(edge, m)
            self.img.paste(Image.new("RGBA", self.img.size, outline + (255,)), (0, 0), edge)

    def flat(self, shape, box, color, width=0):
        d = ImageDraw.Draw(self.img)
        if shape == "rect":
            d.rectangle(self.r(box), fill=color)
        elif shape == "ellipse":
            d.ellipse(self.r(box), fill=color)
        elif shape == "line":
            d.line(self.r(box), fill=color, width=max(1, int(width * S)))
        elif shape == "poly":
            d.polygon([(x * S, y * S) for x, y in box], fill=color)

    def glow(self, cx, cy, radius, color):
        """Weicher Leucht-Punkt (z.B. Augen, LEDs)."""
        g = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(g)
        d.ellipse(self.r([cx - radius * 2.2, cy - radius * 2.2, cx + radius * 2.2, cy + radius * 2.2]),
                  fill=color + (110,))
        g = g.filter(ImageFilter.GaussianBlur(radius * S * 0.9))
        self.img.alpha_composite(g)
        d = ImageDraw.Draw(self.img)
        d.ellipse(self.r([cx - radius, cy - radius, cx + radius, cy + radius]), fill=color + (255,))
        d.ellipse(self.r([cx - radius * 0.4 - 0.3, cy - radius * 0.4 - 0.3,
                          cx + radius * 0.1 - 0.3, cy + radius * 0.1 - 0.3]), fill=(255, 255, 255, 230))

    def finish(self):
        """Dunkle Kontur + Downsampling auf 40x64."""
        a = self.img.getchannel("A")
        edge = a.filter(ImageFilter.MaxFilter(7))
        outline = Image.new("RGBA", self.img.size, (12, 10, 24, 255))
        outline.putalpha(edge)
        outline.alpha_composite(self.img)
        return outline.resize((self.fw, self.fh), Image.LANCZOS)


# ───────────────────────── Astro-Droid (R2-inspiriert) ─────────────────────────

WHITE = (232, 236, 245)
SILVER = (190, 198, 214)
BLUE = (40, 92, 220)
DARK = (28, 30, 44)


def astro(frame):
    c = Canvas()
    bob = {0: 0, 1: -0.6, 2: 0.6, 3: -1}[frame]
    tilt = {0: 0, 1: 0, 2: 0, 3: 0}[frame]
    # Seiten-Beine (hinten = dunkler)
    lf = {0: 0, 1: -1.5, 2: 1.5, 3: -2.5}[frame]
    c.part("rrect", [27, 23 + bob, 35, 56 + lf * 0.3, 2], shade(WHITE, 0.75), highlight=0.3)
    c.part("poly", [(25, 56 + lf * 0.3), (37, 56 + lf * 0.3), (39, 62 + min(0, lf)), (23, 62 + min(0, lf))],
           shade(SILVER, 0.7), "flat")
    # Mittel-Fuß
    c.part("rect", [16, 50 + bob, 24, 58 + bob], shade(SILVER, 0.8))
    c.part("poly", [(14, 58 + bob), (26, 58 + bob), (27, 62), (13, 62)], shade(SILVER, 0.65), "flat")
    # Körper
    c.part("rrect", [7, 22 + bob, 31, 52 + bob, 2], WHITE, highlight=0.35)
    # blaue Paneele
    c.part("rect", [15, 26 + bob, 23, 44 + bob], BLUE, highlight=0.4)
    c.flat("rect", [16.5, 28 + bob, 21.5, 30 + bob], (180, 200, 255, 255))
    c.flat("rect", [16.5, 33 + bob, 21.5, 34 + bob], (20, 40, 110, 255))
    c.flat("rect", [16.5, 37 + bob, 21.5, 38 + bob], (20, 40, 110, 255))
    c.part("rect", [9, 26 + bob, 13, 36 + bob], BLUE, highlight=0.4)
    c.part("rect", [25, 26 + bob, 29, 31 + bob], BLUE, highlight=0.4)
    c.flat("rect", [9, 46 + bob, 29, 47.5 + bob], (150, 160, 180, 255))
    c.glow(27, 40 + bob, 0.9, (255, 70, 60) if frame != 3 else (80, 255, 120))
    # Vorderes Bein
    lf2 = -lf
    c.part("rrect", [3, 23 + bob, 11, 56 + lf2 * 0.3, 2], WHITE, highlight=0.3)
    c.part("rect", [5, 28 + bob, 9, 46 + bob], BLUE, highlight=0.5)
    c.part("poly", [(1, 56 + lf2 * 0.3), (13, 56 + lf2 * 0.3), (15, 62 + min(0, lf2)), (-1, 62 + min(0, lf2))],
           SILVER, "flat")
    # Kuppel
    c.part("dome", [6, 4 + bob, 32, 22 + bob], SILVER, highlight=0.32)
    c.flat("rect", [6, 21 + bob, 32, 23 + bob], (120, 128, 150, 255))
    c.part("rect", [10, 14 + bob, 15, 18 + bob], BLUE, highlight=0.4)
    c.part("rect", [22, 14 + bob, 28, 18 + bob], BLUE, highlight=0.4)
    c.part("rect", [16, 8 + bob, 21, 11 + bob], BLUE, highlight=0.4)
    # Auge
    c.flat("ellipse", [23, 8 + bob, 29, 13.5 + bob], DARK + (255,))
    c.glow(26.2, 10.7 + bob, 1.4, (255, 60, 60))
    c.glow(13, 11 + bob, 0.7, (90, 200, 255))
    return c.finish()


# ───────────────────────── Grumpy-Droid (Chopper-inspiriert) ─────────────────────────

ORANGE = (236, 120, 36)
GREY = (150, 152, 160)
BONE = (220, 214, 196)
RUST = (130, 70, 30)


def grumpy(frame):
    c = Canvas()
    bob = {0: 0, 1: -0.8, 2: 0.4, 3: -1.5}[frame]
    lf = {0: 0, 1: -2, 2: 2, 3: -3}[frame]
    arm = {0: 0, 1: 2, 2: -2, 3: -5}[frame]
    # Antennen
    c.flat("line", [16, 14 + bob, 13, 3 + bob], (70, 70, 80, 255), 1)
    c.flat("line", [21, 14 + bob, 24, 1 + bob + (arm * 0.2)], (70, 70, 80, 255), 1)
    c.glow(13, 3 + bob, 0.9, (255, 210, 60))
    c.glow(24, 1.5 + bob + arm * 0.2, 0.9, (255, 80, 50))
    # Hinteres Bein
    c.part("rect", [24, 42 + bob, 30, 57 + lf * 0.3], shade(GREY, 0.7))
    c.part("poly", [(22, 57 + lf * 0.3), (32, 57 + lf * 0.3), (33, 62 + min(0, lf)), (21, 62 + min(0, lf))],
           shade(GREY, 0.55), "flat")
    # Körper (Fass, unten schmaler)
    c.part("poly", [(8, 24 + bob), (32, 24 + bob), (30, 50 + bob), (10, 50 + bob)], BONE, highlight=0.35)
    # Bänder
    c.part("rect", [8, 26 + bob, 32, 30 + bob], ORANGE, highlight=0.35)
    c.part("rect", [9.5, 41 + bob, 30.5, 44 + bob], ORANGE, highlight=0.35)
    c.part("rect", [14, 32 + bob, 24, 39 + bob], GREY, highlight=0.4)
    c.flat("rect", [15.5, 33.5 + bob, 22.5, 34.5 + bob], (60, 60, 70, 255))
    c.flat("rect", [15.5, 36 + bob, 22.5, 37 + bob], (60, 60, 70, 255))
    # Rost / Gebrauchsspuren
    for (x, y) in [(11, 33), (27, 36), (12, 46), (26, 47), (29, 31)]:
        c.flat("ellipse", [x - 0.7, y + bob - 0.5, x + 0.7, y + bob + 0.5], RUST + (200,))
    # Arm mit Greifer
    ax = 31
    c.flat("line", [ax, 34 + bob, ax + 5, 36 + bob + arm], (90, 90, 100, 255), 1.6)
    c.flat("line", [ax + 5, 36 + bob + arm, ax + 8, 33 + bob + arm], (90, 90, 100, 255), 1.2)
    c.flat("line", [ax + 5, 36 + bob + arm, ax + 8, 38 + bob + arm], (90, 90, 100, 255), 1.2)
    # Vorderes Bein
    lf2 = -lf
    c.part("rect", [9, 42 + bob, 15, 57 + lf2 * 0.3], GREY)
    c.part("rect", [9, 46 + bob, 15, 49 + bob], ORANGE)
    c.part("poly", [(7, 57 + lf2 * 0.3), (17, 57 + lf2 * 0.3), (18, 62 + min(0, lf2)), (6, 62 + min(0, lf2))],
           shade(GREY, 0.8), "flat")
    # Kuppel (Kopf)
    c.part("dome", [8, 11 + bob, 32, 24 + bob], ORANGE, highlight=0.33)
    c.part("rect", [8, 22 + bob, 32, 24.5 + bob], GREY)
    c.flat("rect", [9, 17 + bob, 31, 18 + bob], (150, 70, 20, 255))
    # Grimmiges Auge
    c.flat("ellipse", [21, 13 + bob, 29, 21 + bob], (40, 40, 48, 255))
    c.flat("ellipse", [22, 14 + bob, 28, 20 + bob], (15, 15, 20, 255))
    c.glow(25.5, 17 + bob, 1.4, (255, 50, 40))
    # "Augenbraue" (grimmig)
    c.flat("line", [20, 12.5 + bob, 29, 14.5 + bob], (60, 40, 30, 255), 1.1)
    return c.finish()


# ───────────────────────── Protocol-Droid (C-3PO-inspiriert) ─────────────────────────

GOLD = (226, 178, 62)
GOLD_D = (150, 105, 30)
WIRE = (90, 60, 40)


def protocol(frame):
    c = Canvas()
    bob = {0: 0, 1: -0.6, 2: -0.6, 3: -1}[frame]
    # Schrittstellung: (hinteres Bein dx, vorderes Bein dx)
    step = {0: (0, 0), 1: (-3, 3), 2: (3, -3), 3: (-2, 4)}[frame]
    swing = {0: 0, 1: 3, 2: -3, 3: -6}[frame]

    def leg(dx, back):
        base = shade(GOLD, 0.72) if back else GOLD
        hip_x = 20 + (-1.5 if back else 1.5)
        knee = (hip_x + dx * 0.5, 50 + bob * 0.5)
        foot = (hip_x + dx, 60)
        c.flat("line", [hip_x, 39 + bob, knee[0], knee[1]], WIRE + (255,), 2.8)
        c.part("poly", [(hip_x - 2.4, 38 + bob), (hip_x + 2.4, 38 + bob),
                        (knee[0] + 2, knee[1]), (knee[0] - 2, knee[1])], base, highlight=0.35)
        c.part("poly", [(knee[0] - 2, knee[1] + 1), (knee[0] + 2, knee[1] + 1),
                        (foot[0] + 1.8, foot[1] - 1), (foot[0] - 1.8, foot[1] - 1)], base, highlight=0.35)
        c.flat("ellipse", [knee[0] - 2, knee[1] - 1.2, knee[0] + 2, knee[1] + 1.4], shade(base, 0.7) + (255,))
        c.part("poly", [(foot[0] - 3, foot[1] - 1.5), (foot[0] + 4, foot[1] - 1.5),
                        (foot[0] + 4.5, 63), (foot[0] - 3, 63)], shade(base, 0.85), "flat")

    def arm(sw, back):
        base = shade(GOLD, 0.7) if back else GOLD
        sx = 20 + (-6.5 if back else 6.5)
        elbow = (sx + sw * 0.4, 31 + bob)
        hand = (sx + sw, 38 + bob)
        c.flat("line", [sx, 21 + bob, elbow[0], elbow[1]], base + (255,), 2.6)
        c.flat("line", [elbow[0], elbow[1], hand[0], hand[1]], base + (255,), 2.2)
        c.flat("ellipse", [hand[0] - 1.4, hand[1] - 1, hand[0] + 1.4, hand[1] + 1.8], shade(base, 0.8) + (255,))

    leg(step[0], True)
    arm(-swing, True)
    # Becken
    c.part("rrect", [15, 34 + bob, 25, 40 + bob, 1.5], GOLD_D, highlight=0.4)
    # Bauch mit Kabeln
    c.part("rect", [16, 29 + bob, 24, 34 + bob], (60, 45, 35))
    for x in (17.5, 19.5, 21.5):
        c.flat("line", [x, 29 + bob, x + 0.6, 34 + bob], (190, 70, 40, 255) if x != 19.5 else (70, 120, 200, 255), 0.6)
    # Torso
    c.part("poly", [(13, 18 + bob), (27, 18 + bob), (26, 29 + bob), (14, 29 + bob)], GOLD, highlight=0.35)
    c.flat("ellipse", [17, 21 + bob, 23, 26 + bob], shade(GOLD, 0.8) + (255,))
    c.flat("ellipse", [18.5, 22.5 + bob, 21.5, 24.8 + bob], (110, 80, 30, 255))
    c.flat("line", [14, 19 + bob, 26, 19 + bob], (255, 230, 150, 255), 0.6)
    # Hals
    c.part("rect", [18.5, 15 + bob, 21.5, 18.5 + bob], (70, 55, 40))
    # Kopf
    c.part("ellipse", [13.5, 2 + bob, 26.5, 16 + bob], GOLD, highlight=0.3)
    c.part("rect", [15, 11 + bob, 25, 15.5 + bob], shade(GOLD, 0.9), highlight=0.3)
    c.flat("rect", [16.5, 12.5 + bob, 23.5, 13.5 + bob], (90, 65, 25, 255))   # Mund
    # Augen
    c.flat("ellipse", [16.2, 6 + bob, 19.8, 9.6 + bob], (60, 40, 20, 255))
    c.flat("ellipse", [21.2, 6 + bob, 24.8, 9.6 + bob], (60, 40, 20, 255))
    c.glow(18, 7.8 + bob, 1.2, (255, 200, 80))
    c.glow(23, 7.8 + bob, 1.2, (255, 200, 80))
    leg(step[1], False)
    arm(swing, False)
    return c.finish()



# ───────────────────────── Gegner ─────────────────────────
# Jeder Gegner spiegelt den Angriffsstil eines Droiden:
#   Zapper  (48x40, 3 Frames: Schweben A, Schweben B, Aufladen)   -> Nahkampf-Elektro wie ASTRO
#   Brute   (56x60, 4 Frames: Gehen A, Gehen B, Ausholen, Schlag) -> Nahkampf/Bomben wie GRUMP
#   Sniper  (40x64, 3 Frames: Ruhe, Zielen, Schuss)               -> Fernkampf wie PROTO

GUNMETAL = (92, 96, 114)
ENEMY_RED = (255, 52, 64)
CRIMSON = (128, 38, 40)
HAZARD = (240, 196, 40)
VIOLET = (78, 64, 104)


def zapper(frame):
    c = Canvas(48, 40)
    bob = {0: 0, 1: 1.2, 2: 0}[frame]
    # Rotor-Scheiben
    for x0 in (2, 37):
        c.flat("ellipse", [x0, 14 + bob, x0 + 9, 18 + bob], (150, 170, 210, 120))
    c.flat("line", [9, 16 + bob, 13, 17 + bob], (60, 64, 80, 255), 1.2)
    c.flat("line", [35, 17 + bob, 39, 16 + bob], (60, 64, 80, 255), 1.2)
    # Elektroden
    for (x1, x2) in ((19, 15), (29, 33)):
        c.flat("line", [x1, 29 + bob, x2, 37 + bob], (110, 115, 130, 255), 1.4)
    # Düse
    c.glow(24, 33 + bob, 1.3 if frame == 1 else 1.0, (255, 140, 40))
    # Körper
    c.part("ellipse", [10, 5 + bob, 38, 32 + bob], GUNMETAL, highlight=0.35)
    c.part("rect", [10.5, 17 + bob, 37.5, 21 + bob], CRIMSON, highlight=0.4)
    c.flat("poly", [(21, 6 + bob), (27, 6 + bob), (24, 1 + bob)], (70, 72, 88, 255))
    # Auge
    c.flat("ellipse", [25, 9 + bob, 36, 20 + bob], (20, 20, 28, 255))
    c.glow(30.5, 14.5 + bob, 3.0 if frame == 2 else 2.2, ENEMY_RED)
    if frame == 2:
        for (x, y) in ((15, 37), (33, 37)):
            c.glow(x, y, 1.4, (120, 200, 255))
        c.flat("line", [15, 37, 20, 35, 24, 38, 28, 35, 33, 37], (170, 230, 255, 255), 0.8)
    return c.finish()


def brute(frame):
    c = Canvas(56, 60)
    bob = {0: 0, 1: 0.8, 2: -0.6, 3: 0.6}[frame]
    lf = {0: -2, 1: 2, 2: 0, 3: 1}[frame]
    # Auspuff
    c.part("rect", [5, 8 + bob, 9, 22 + bob], (70, 70, 78))
    c.flat("ellipse", [3, 2 + bob, 10, 8 + bob], (90, 90, 100, 140))
    # Beine
    c.part("rect", [13 + lf, 42 + bob, 22 + lf, 56], shade(GUNMETAL, 0.8))
    c.part("rect", [32 - lf, 42 + bob, 41 - lf, 56], GUNMETAL)
    c.part("rect", [10 + lf, 54, 24 + lf, 59], (40, 40, 48), "flat")
    c.part("rect", [30 - lf, 54, 44 - lf, 59], (40, 40, 48), "flat")
    # Rumpf
    c.part("poly", [(8, 14 + bob), (46, 14 + bob), (44, 45 + bob), (10, 45 + bob)], CRIMSON, highlight=0.35)
    # Warnstreifen
    for i in range(6):
        x = 11 + i * 6
        c.flat("poly", [(x, 30 + bob), (x + 3, 30 + bob), (x + 6, 35 + bob), (x + 3, 35 + bob)], HAZARD + (255,))
    c.flat("rect", [10, 29 + bob, 44, 29.8 + bob], (30, 20, 20, 255))
    c.flat("rect", [10, 35.2 + bob, 44, 36 + bob], (30, 20, 20, 255))
    for (x, y) in ((13, 18), (41, 18), (13, 41), (41, 41)):
        c.flat("ellipse", [x - 1, y + bob - 1, x + 1, y + bob + 1], (60, 30, 30, 255))
    # Kopf
    c.part("dome", [18, 3 + bob, 38, 15 + bob], shade(GUNMETAL, 0.9), highlight=0.3)
    c.flat("rect", [23, 9 + bob, 36, 12 + bob], (20, 16, 18, 255))
    c.glow(31, 10.5 + bob, 1.3, (255, 150, 30))
    c.glow(26, 10.5 + bob, 0.9, (255, 150, 30))
    # Klauen-Arm
    shoulder = (44, 21 + bob)
    hand = {0: (49, 40), 1: (48, 41), 2: (50, 4), 3: (55, 32)}[frame]
    c.flat("line", [shoulder[0], shoulder[1], hand[0], hand[1]], (70, 72, 84, 255), 4.5)
    c.part("ellipse", [shoulder[0] - 4, shoulder[1] - 4, shoulder[0] + 4, shoulder[1] + 4], GUNMETAL)
    hx, hy = hand
    c.flat("poly", [(hx - 4, hy - 3), (hx + 3, hy - 4), (hx + 1, hy + 4), (hx - 3, hy + 3)], (180, 60, 40, 255))
    c.flat("line", [hx + 1, hy - 3, hx + 4, hy - 6], (200, 200, 210, 255), 1.4)
    c.flat("line", [hx + 1, hy + 3, hx + 4, hy + 6], (200, 200, 210, 255), 1.4)
    return c.finish()


def sniper(frame):
    c = Canvas(40, 64)
    # Beine
    c.part("poly", [(15, 38), (19, 38), (17, 62), (13, 62)], shade(VIOLET, 0.8))
    c.part("poly", [(21, 38), (25, 38), (27, 62), (23, 62)], VIOLET)
    c.part("rect", [11, 60, 18, 63], (34, 30, 44), "flat")
    c.part("rect", [22, 60, 30, 63], (34, 30, 44), "flat")
    # Umhang / Rumpf
    c.part("poly", [(12, 16), (28, 16), (30, 40), (10, 40)], (52, 44, 72), highlight=0.3)
    c.part("poly", [(14, 16), (26, 16), (25, 30), (15, 30)], VIOLET, highlight=0.35)
    c.flat("rect", [15, 31, 25, 33], (30, 26, 40, 255))
    # Kopf (lang, mit Zielfernrohr-Auge)
    c.part("rrect", [16, 1, 26, 15, 3], (64, 58, 84), highlight=0.3)
    c.flat("rect", [21, 5, 28, 9], (20, 18, 26, 255))
    c.glow(26, 7, 1.6 if frame else 1.2, ENEMY_RED)
    # Gewehr
    if frame == 0:
        c.flat("line", [14, 34, 30, 22], (40, 40, 50, 255), 2.4)
        c.flat("line", [24, 26, 34, 18], (120, 120, 140, 255), 1.0)
    else:
        c.flat("line", [16, 27, 39.5, 25], (40, 40, 50, 255), 2.6)
        c.flat("rect", [22, 22.5, 28, 24.5], (90, 90, 110, 255))
        c.glow(27, 23.5, 0.7, ENEMY_RED)
    # Arm
    c.flat("line", [24, 18, 22, 27], (70, 62, 92, 255), 2.2)
    if frame == 2:
        c.glow(38, 25, 2.4, (255, 220, 120))
    return c.finish()


def sheet(fn, name, fw=FW, fh=FH, frames=FRAMES):
    out = Image.new("RGBA", (fw * frames, fh), (0, 0, 0, 0))
    for f in range(frames):
        out.paste(fn(f), (f * fw, 0))
    out.save(OUT / name)
    print("✓", OUT / name)


def crop_credits():
    """
    Die Original-Credits sind 1024x1024 mit viel transparentem Rand -> im Spiel winzig.
    Hier wird der Chip ausgeschnitten und auf 128 px Breite verkleinert (scharf im Spiel).
    """
    for n in ("credit1", "credit2", "credit3"):
        im = Image.open(OUT / f"{n}.png").convert("RGBA")
        solid = im.getchannel("A").point(lambda v: 255 if v > 40 else 0)
        chip = im.crop(solid.getbbox())
        h = round(chip.height * 128 / chip.width)
        chip.resize((128, h), Image.LANCZOS).save(OUT / f"{n}_chip.png")
        print("✓", OUT / f"{n}_chip.png")


if __name__ == "__main__":
    sheet(astro, "droid_astro.png")
    sheet(grumpy, "droid_grumpy.png")
    sheet(protocol, "droid_protocol.png")
    sheet(zapper, "enemy_zapper.png", 48, 40, 3)
    sheet(brute, "enemy_brute.png", 56, 60, 4)
    sheet(sniper, "enemy_sniper.png", 40, 64, 3)
    crop_credits()
