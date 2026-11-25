package com.thewatcher.horror;

import com.thewatcher.TheWatcher;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Which chunks of a horror dimension have already been built (maze walls, dead trees...). */
public class DecoratedChunks extends PersistentState {
    private static final Type<DecoratedChunks> TYPE = new Type<>(DecoratedChunks::new, DecoratedChunks::fromNbt, null);
    private final LongOpenHashSet done = new LongOpenHashSet();

    public static DecoratedChunks get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, TheWatcher.MOD_ID + "_decorated");
    }

    public boolean isDone(long chunk) { return done.contains(chunk); }

    public void mark(long chunk) {
        if (done.add(chunk)) markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putLongArray("chunks", done.toLongArray());
        return nbt;
    }

    private static DecoratedChunks fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        DecoratedChunks d = new DecoratedChunks();
        for (long l : nbt.getLongArray("chunks")) d.done.add(l);
        return d;
    }
}
