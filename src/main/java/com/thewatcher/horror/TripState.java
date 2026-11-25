package com.thewatcher.horror;

import com.thewatcher.TheWatcher;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Who has been taken where, where they came from, and where their way out is. Saved with the world. */
public class TripState extends PersistentState {
    public static final class Trip {
        public String fromDim;
        public long fromPos;
        public float fromYaw;
        public String toDim;
        public long exitPos;
        public long startTime;
    }

    private static final Type<TripState> TYPE = new Type<>(TripState::new, TripState::fromNbt, null);
    private final Map<UUID, Trip> trips = new HashMap<>();

    public static TripState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, TheWatcher.MOD_ID + "_trips");
    }

    @Nullable
    public Trip get(UUID id) { return trips.get(id); }

    public void put(UUID id, Trip trip) {
        trips.put(id, trip);
        markDirty();
    }

    public void remove(UUID id) {
        if (trips.remove(id) != null) markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound all = new NbtCompound();
        trips.forEach((id, t) -> {
            NbtCompound c = new NbtCompound();
            c.putString("from", t.fromDim);
            c.putLong("fromPos", t.fromPos);
            c.putFloat("yaw", t.fromYaw);
            c.putString("to", t.toDim);
            c.putLong("exit", t.exitPos);
            c.putLong("start", t.startTime);
            all.put(id.toString(), c);
        });
        nbt.put("trips", all);
        return nbt;
    }

    private static TripState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        TripState s = new TripState();
        NbtCompound all = nbt.getCompound("trips");
        for (String key : all.getKeys()) {
            try {
                NbtCompound c = all.getCompound(key);
                Trip t = new Trip();
                t.fromDim = c.getString("from");
                t.fromPos = c.getLong("fromPos");
                t.fromYaw = c.getFloat("yaw");
                t.toDim = c.getString("to");
                t.exitPos = c.getLong("exit");
                t.startTime = c.getLong("start");
                s.trips.put(UUID.fromString(key), t);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return s;
    }
}
