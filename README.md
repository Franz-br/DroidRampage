# Droid Rampage

A fast-paced 2D side-scrolling game set in a minimal neon cyberpunk city.  
Built with **FXGL**, focused on clean visuals, increasing difficulty, and skill-based gameplay.

---

## ▶️ Play it

**Ready-to-play files are in the [`target/`](target/) folder** – no IDE, Maven or build needed,
they start the game instantly:

- [`target/DroidRampage.jar`](target/DroidRampage.jar) – double-click it if Java 17+ is installed
- [`target/DroidRampage.zip`](target/DroidRampage.zip) – jar + start scripts, works even without Java

**Without installing anything:** get `DroidRampage.zip`, unzip it and double-click
`Start-Windows.bat` (Windows) or `Start-Mac.command` (macOS), or run `./start-linux.sh` (Linux).
If Java 17+ is missing, the starter downloads a private Java runtime (~50 MB) into a
`java-runtime` folder next to it – nothing is installed system-wide.

**With Java 17+ installed:** just double-click `DroidRampage.jar` (or `java -jar DroidRampage.jar`).

**From source:** `./mvnw javafx:run` (needs a JDK 17+).

### Building the zip to share

```bash
python3 tools/build_release.py
```

This creates `target/DroidRampage.jar` (one jar for Windows, macOS Intel + Apple Silicon and Linux)
and `target/DroidRampage.zip` (jar + start scripts from `launcher/` + `HOW-TO-PLAY.txt`).
The script merges JavaFX's Intel and Apple-Silicon Mac libraries into universal binaries,
because both use the same file names and only one could otherwise fit into the jar.

---

## 🎮 Gameplay

- Move through an **endless generated world**
- Collect credits while avoiding failure
- The game gets progressively faster over time

### 🕹️ Controls

| Key | Action |
|-----|--------|
| A / ←   | Move Left (menu: previous droid) |
| D / →   | Move Right (menu: next droid) |
| Space / W / ↑ | Jump |
| F / J | Attack (depends on droid) |
| G / K | Throw bomb (GRUMP only) |
| Enter / Space | Start run (menu) |
| R / Enter | Restart after death |
| M   | Back to main menu after death |
| F11 | Toggle fullscreen (the game starts in fullscreen) |

### 🤖 Droids

Pick your droid in the start menu – each one plays a bit differently:

| Droid | Inspired by | Speed | Jump | Attack |
|-------|-------------|-------|------|--------|
| ASTRO | R2 style astromech | 400 | 720 | **Taser** – short range, chain-zaps up to 3 enemies and stuns them |
| GRUMP | Chopper style astromech | 470 | 700 | **Claw** (melee, also blocks enemy shots) + **Bombs** (G) |
| PROTO | C-3PO style protocol droid | 365 | 790 | **Blaster** – long-range rapid fire |

### 👾 Enemies

Each enemy type mirrors one droid's fighting style:

| Enemy | Mirrors | Behaviour | HP | Points |
|-------|---------|-----------|----|--------|
| Zapper drone | ASTRO | Hovers towards you, charges up and zaps at close range | 2 | 40 |
| Brute | GRUMP | Patrols, charges, smashes with its claw and lobs bombs | 4 | 70 |
| Marksman | PROTO | Aims with a red laser sight, then fires – dodge when the sight locks | 2 | 60 |

- You have **3 HP** – enemy hits and saws cost 1 HP (short invulnerability afterwards)
- Falling into the void, lasers, energy spikes and the camera still kill instantly
- Jump on an enemy to **stomp** it

Sprites are generated with `python3 tools/generate_sprites.py` (4 frames: idle, walk ×2, jump).

### 🏆 High Scores

- Score = collected credits + distance in meters + points for destroyed enemies
- The top 10 runs are saved in `~/.droidrampage/highscores.txt` and shown in the start menu

---

## ⚡ Core Mechanics

### 🌍 Endless World
- Tiles are generated infinitely
- No fixed level → every run is different
- Hand-built Tiled maps alternate with **procedurally generated segments** that get harder over time:

| Element | Behaviour |
|---------|-----------|
| 🟦 Solid platform | Normal neon platform |
| 🟪 Moving platform | Elevators (up/down) and ferries (left/right) – they carry you along |
| 🟧 Crumbling platform | Shakes and falls shortly after you step on it |
| 🟩 Bounce pad | Launches you up to high bonus platforms |
| 🟥 Laser gate | Blinks on and off – time it or jump over the low ones |
| 🟪 Phase platform | Disappears and reappears in a rhythm (flickers before vanishing) |
| 🟨 Conveyor belt | Pushes you forwards or backwards |
| ⚙️ Saw | Rolls back and forth on a rail – jump over it |
| 🔺 Energy spikes | Instant death (also used in the Tiled maps) |

The hand-built Tiled maps use the same visuals as the generated segments: only their objects
(`plattform`, `Boden`, `Death`, `Item1`) are drawn, the old tile layer is hidden.

### 🚀 Increasing Difficulty
- Camera speed increases over time
- Forces faster reaction and better movement control

### 🪙 Credit System

Collect credits to increase your score:

| Type   | Value |
|--------|------|
| 🟤 Bronze | 5   |
| ⚪ Silver | 50  |
| 🟡 Gold   | 75  |

- Credits are placed in the world
- Require movement skill and timing to collect efficiently

---

## 🎨 Visual Style

- Clean **pixel-art (32x32)** assets
- Minimal **neon cyberpunk background**
- Designed to:
  - Highlight gameplay elements
  - Avoid visual clutter
  - Maintain readability at high speed

---

## 🧠 Design Philosophy

> Skill-based endless gameplay.

- No artificial limits → only player skill matters
- Increasing speed creates natural difficulty scaling
- Risk vs reward (harder paths = better credits)

---

## 🧱 Tech Stack

- Java
- FXGL (JavaFX Game Library)
- JavaFX Rendering

---

## 🗂️ Project Structure
