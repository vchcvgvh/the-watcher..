package com.thewatcher.gen;

/** Where realm generation puts its blocks: the real world in-game, or an array in tests and previews. */
public interface BlockSink {
    void set(int x, int y, int z, B block);

    B get(int x, int y, int z);

    default boolean isAir(int x, int y, int z) {
        return get(x, y, z) == B.AIR;
    }

    /** Called before building at (x, z) so the chunk there already has its decoration (in-game only). */
    default void prepare(int x, int z) {
    }
}
