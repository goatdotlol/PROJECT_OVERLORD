# 🏎️ THE ULTIMATE PROJECT OVERLORD TRANSFER LOG 🏎️

> **FOR THE NEXT ASSISTANT**: Read this document in its entirety before starting. It contains the project's soul, history, and technical blueprint.

---

## 🌌 1. THE VISION: WHAT IS PROJECT OVERLORD?
**PROJECT OVERLORD** is a high-performance, fully autonomous Minecraft speedrunning bot for version **1.16.1 (Fabric)**. 

### Core Philosophy: "The Ghost in the Machine"
- **Zero Libraries**: No Baritone, no Meteor, no external hacks. Everything is built from scratch using Pure Vanilla Minecraft APIs and Reflection.
- **Natural Simulation**: The bot must move, look, and interact like a human. This means smooth rotations, variable delays, and non-robotic pathing.
- **Silent Operation**: All debugging goes to `latestlog.txt` (via `DebugLogger`), not the game chat.
- **The End Goal**: A fully autonomous run from spawn to the Credits in under 15 minutes.

---

## 🛠️ 2. THE ENVIRONMENT & SETUP
- **Minecraft**: 1.16.1
- **Mod Loader**: Fabric (Loader 0.18.4)
- **Java Version**: 8 (Standard for 1.16.1)
- **Workspace**: `c:\Users\goat.lol\the_speed\`
- **Project Type**: Gradle-based Fabric project.

---

## 📜 3. THE HISTORY (SAGA OF THE OVERHAUL)

### Phase 1: The Prototype (v1.0)
Initially, we built a monolithic bot that tried to scan the world synchronously. 
- **Result**: Severe lag (0.1 FPS), "Can't keep up" server errors, and robotic snapping.
- **Failures**: The A* pathfinder was too simple; it couldn't jump over 1-block gaps or drop down ledges safely. It stood still when targets weren't in immediate range.

### Phase 2: The "Thinking" Overhaul (v2.0)
We used a **Gemini 3 Pro Thinking** prompt to redesign the entire core. We moved to a "Systems-based" architecture that separates Perception, Decision, and Action.
- **Breakthrough**: Implemented **Async Chunk Scanning**. Instead of checking millions of blocks at once, we time-slice the work.
- **Breakthrough**: **Natural Motion Controller**. Replaced snaps with damped PID-like rotations.
- **Breakthrough**: **Baritone-Lite Pathfinder**. A node-based A* that understands parkour, jumping, and safe dropping.

---

## 🧠 4. HOW IT WORKS: THE v2.0 ARCHITECTURE

The bot follows a strict **5-Stage Pipeline** every tick:

1.  **PERCEPTION (`AsyncChunkScanner`)**
    - Scans 3x3 chunks immediately.
### 1. The 5-Stage Execution Pipeline (Per Tick)
Operating in `BotMain.java`, the bot runs these stages every tick:
1. **PERCEPTION (`AsyncChunkScanner`)**: Time-slices chunk scanning. Now caches **Exposed Stone** for tool upgrades.
2. **DECISION (`GoalEngine`)**: A strict state machine (Wood -> **Stone** -> Tools -> Iron -> Lava).
3. **NAVIGATION (`PathingControl` + `AStarPathfinder`)**: Drives paths. **STOPS moving** if interacting (Priority System).
4. **ACTION (`InteractionControl` + `CraftingControl`)**: Handles block breaking and **auto-crafting** (Planks, Sticks, Tables, Picks).
5. **MOTION (`HumanoidControl`)**: Uses a **Priority System** (Interaction > Pathing) to prevent "Spinning".

---

## 📂 MODULE GUIDE (File List)

### 🧠 Core & Systems (v2.1)
- `com.speedrun.bot.BotMain`: Primary entry point. Toggle with **'R'**.
- `com.speedrun.bot.systems.AsyncChunkScanner`: Zero-lag perception. Finds Wood, Stone, Iron, Golems.
- `com.speedrun.bot.systems.GoalEngine`: THE BRAIN. Strict progression logic.
- `com.speedrun.bot.systems.CraftingControl`: **NEW**. Auto-crafts Planks, Sticks, Tables, Pickaxes (Wood/Stone).
- `com.speedrun.bot.systems.HumanoidControl`: **UPDATED**. Priority-based rotation lock (Fixes "Spinning").
- `com.speedrun.bot.systems.InteractionControl`: Precision breaking & Block Interaction (Right-Click).
- `com.speedrun.bot.systems.PathingControl`: Movement execution. Halts when interacting.
- `com.speedrun.bot.systems.AStarPathfinder`: Baritone-lite engine.

### Utilities (`src/main/java/com/speedrun/bot/utils/`)
- `InventoryScanner.java`: Current resource checks (Crucial for GoalEngine).
- `DebugLogger.java`: The only way we "talk" to the dev.

---

## 🏁 6. THE ROADMAP: WHAT'S NEXT?

### Phase 2.5: Refining Execution (CURRENT)
- [ ] **Inventory Clicking**: Finish the automated crafting logic (converting Logs to Planks to Pickaxes).
- [ ] **Stone Tools**: Transition to stone once at Y=11 or near exposed stone.

### Phase 3: The Search for Lava
- [ ] **Lava Triangulation**: Use the background scanner to find surface lava pools.
- [ ] **Bucket Control**: Automated water/lava scooping.

### Phase 4: The Nether & The End
- [ ] **Portal Build**: Automated water-bucket portal frame construction.
- [ ] **Fortress Nav**: Blaze gathering.
- [ ] **Stronghold**: Triangulation and blind-travel.

---

## 🛑 FINAL CRITICAL ADVICE FOR THE NEXT AI
- **DO NOT** revert to synchronous scanning. It will kill the FPS.
- **DO NOT** use chat messages. Use the `DebugLogger`.
- **STAY IN v2.0**. The `systems` package is where the magic happens.
- **THE BOT IS IN GHOST MODE**. It should look like a pro speedrunner, not a TAS.

---
**PROJECT OVERLORD IS READY. PROCEED TO SUB-10.**
