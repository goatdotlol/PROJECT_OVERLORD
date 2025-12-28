# Project OVERLORD v3.0 - Strategic Algorithm Implementation

Based on `pro_research.txt` and user requirements.

## 1. Core System Fixes (Immediate)
- [x] **InteractionControl**: Fix `Vec3d`/`BlockHitResult` imports.
- [x] **GoalEngine**: Fix accessor methods (`pitch` vs `getPitch`).
- [x] **HumanoidControl**: Fix "Camera Lock" bug (Free look when idle).
- [ ] **Build Verification**: Ensure `./gradlew clean build` passes.

## 2. Advanced Systems
### HUD & Awareness
- [ ] **HUD Renderer**: Create `BotHud.java` using `InGameHud`.
    - Display: Goal, Task, Status.
- [ ] **Subtitles/Hearing**: Connect to `SoundSystem` or `ClientPlayNetworkHandler` to parse packet sounds (e.g., `ENTITY_GENERIC_SPLASH` for lava, `ENTITY_IRON_GOLEM_HURT`).

### Pathfinding Overhaul
- [ ] **Baritone-Lite**: Improve `AStarPathfinder`.
    - Better heuristics for complex terrain.
    - "Parkour" nodes (jump-sprinting).
- [ ] **Visual Debug**: Draw path lines using `WorldRenderer`.

## 3. Game Loop Overhaul (State Machine)
### Phase I: Resource Acquisition
- [ ] **Pirate Option**: Check biome/seed. If Ocean < 200 blocks -> Pirate Start.
- [ ] **Resource Gathering**:
    - **Wood**: Mine -> Collect Drops -> Craft Planks/Sticks.
    - **Stone**: Scan for exposed blocks. Fallback: Dig shaft.
    - **Tools**: Wood Pick -> Stone -> Stone Pick -> Iron.
- [ ] **Village/Food**:
    - Deep Scan (0FPS allowable) for Bells, Haybales.

### Phase II: Iron Golem Protocol
- [ ] **Detection**: Find Golem.
- [ ] **Engagement**:
    - Calculate Tower Height (`ceil(3 + deltaY)`).
    - Tower Up (4 blocks default).
    - Crit Attack.
    - Loot Iron.

## 4. Nether & Beyond (Future)
- [ ] **Portal Casting**: Upside-Down L or Magma Ravine logic.
- [ ] **Piglin Bartering**: 1x1x2 Trap logic.
- [ ] **Stronghold**: Triangulation (4,4 method).
- [ ] **Dragon**: One-Cycle Bed logic.

## 5. Constraints
- Human-like inputs only (No packet edits).
- No instant snaps.
