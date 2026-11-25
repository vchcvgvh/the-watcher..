package com.thewatcher.horror;

import com.thewatcher.TheWatcher;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player "dread", saved with the world. Dread rises every second you play
 * (faster in the dark and underground) and decides how bold the Watcher gets.
 */
public class DreadState extends PersistentState {
    /** Dread (in seconds-ish) needed to reach stage 1..4. */
    public static final int[] STAGE_THRESHOLDS = {0, 3 * 60, 10 * 60, 22 * 60, 40 * 60};

    private static final Type<DreadState> TYPE = new Type<>(DreadState::new, DreadState::fromNbt, null);

    private final Map<UUID, Integer> dread = new HashMap<>();

    public static DreadState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, TheWatcher.MOD_ID + "_dread");
    }

    public int getDread(UUID player) {
        return dread.getOrDefault(player, 0);
    }

    public void setDread(UUID player, int value) {
        dread.put(player, Math.max(0, value));
        markDirty();
    }

    public void addDread(UUID player, int amount) {
        setDread(player, getDread(player) + amount);
    }

    public int getStage(UUID player) {
        return stageFor(getDread(player));
    }

    public static int stageFor(int dread) {
        int stage = 0;
        for (int i = 1; i < STAGE_THRESHOLDS.length; i++) {
            if (dread >= STAGE_THRESHOLDS[i]) stage = i;
        }
        return stage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound players = new NbtCompound();
        dread.forEach((id, value) -> players.putInt(id.toString(), value));
        nbt.put("dread", players);
        return nbt;
    }

    private static DreadState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        DreadState state = new DreadState();
        NbtCompound players = nbt.getCompound("dread");
        for (String key : players.getKeys()) {
            try {
                state.dread.put(UUID.fromString(key), players.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return state;
    }
}
