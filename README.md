# Project OVERLORD - Minecraft 1.16.1 Speedrun Bot

Pure vanilla Fabric mod for automated Minecraft speedrunning. Built from scratch without external libraries like Baritone.

## Build in GitHub Codespaces

```bash
# Install Java 17
sdk install java 17.0.9-tem
sdk use java 17.0.9-tem

# Build the mod
./gradlew build -x downloadAssets

# The output JAR will be in:
# build/libs/ghost-runner-1.0.0.jar
```

## Features
- Custom A* pathfinding
- World scanning (villages, shipwrecks, ores)
- Natural input simulation
- Dynamic strategy fallback ("7 Sexy Iron")
- In-game config GUI (Right Shift)

## Installation
Copy the JAR from `build/libs/` to your `.minecraft/mods/` folder.
