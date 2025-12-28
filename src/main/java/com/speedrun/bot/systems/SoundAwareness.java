package com.speedrun.bot.systems;

import com.speedrun.bot.utils.DebugLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvents;
import java.util.ArrayList;
import java.util.List;

/**
 * SoundAwareness - Listens for critical audio cues.
 * "Hearing" the environment.
 */
public class SoundAwareness {

    private static final List<String> recentSounds = new ArrayList<>();

    public static void onSound(SoundInstance sound) {
        // Filter for useful sounds
        String id = sound.getId().toString();

        if (id.contains("lava") && id.contains("pop")) {
            DebugLogger.log("Heard Lava Pop at " + sound.getX() + ", " + sound.getZ());
            // Could trigger Magma Ravine logic here
        }

        if (id.contains("step"))
            return; // Ignore steps for now

        if (recentSounds.size() > 5)
            recentSounds.remove(0);
        recentSounds.add(id);
    }

    public static List<String> getRecentSounds() {
        return recentSounds;
    }
}
