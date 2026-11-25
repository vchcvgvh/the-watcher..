package com.thewatcher.gen;

/**
 * The places it takes you. Each is a flat world (data/thewatcher/dimension/*.json) that {@link RealmGen}
 * builds on, chunk by chunk, as you explore.
 */
public enum Realm {
    /** Endless yellowed office corridors under humming, flickering lights. A red room with a door. */
    HALLWAYS("hallways", 11, 4, 7, 90, 50,
            new B[]{B.BEDROCK, B.STONE, B.YELLOW_TERRACOTTA, B.AIR, B.WHITE_TERRACOTTA, B.STONE, B.BEDROCK},
            new int[]{1, 9, 1, 4, 1, 3, 1}),
    /** A dead black forest in fog and falling ash. Follow the soul lanterns to the archway. */
    HOLLOW("hollow", 64, 14, 6, 90, 50,
            new B[]{B.BEDROCK, B.DEEPSLATE, B.DIRT, B.COARSE_DIRT},
            new int[]{1, 58, 4, 1}),
    /** Living red caves: walls of meat, ribcage tunnels, teeth, pools of blood. Follow the heartbeat to the mouth. */
    FLESH("flesh", 11, 12, 5, 70, 40,
            new B[]{B.BEDROCK, B.RED_TERRACOTTA, B.NETHER_WART_BLOCK, B.AIR, B.NETHER_WART_BLOCK, B.RED_TERRACOTTA, B.BEDROCK},
            new int[]{1, 9, 1, 12, 2, 2, 1}),
    /** An endless, misty, knee-deep black lake: drowned trees, sunken ruins, lanterns on posts. A bell tolls. */
    STILL_WATER("still_water", 63, 10, 6, 90, 50,
            new B[]{B.BEDROCK, B.STONE, B.MUD, B.WATER},
            new int[]{1, 54, 8, 1});

    public final String id;
    /** Y the player stands at. */
    public final int standY;
    /** How much headroom to clear on arrival. */
    public final int clearHeight;
    /** Minutes until you "wake up" if you never find the way out. */
    public final int minutes;
    /** Exit distance: min + random(range). */
    public final int exitMin, exitRange;
    /** The flat base layers, bottom up (must match the dimension json). */
    public final B[] layers;
    public final int[] layerHeights;

    Realm(String id, int standY, int clearHeight, int minutes, int exitMin, int exitRange, B[] layers, int[] layerHeights) {
        this.id = id;
        this.standY = standY;
        this.clearHeight = clearHeight;
        this.minutes = minutes;
        this.exitMin = exitMin;
        this.exitRange = exitRange;
        this.layers = layers;
        this.layerHeights = layerHeights;
    }

    /** Y of the exit marker the player steps onto (Still Water's is on a little island). */
    public int exitY() {
        return this == STILL_WATER ? standY + 1 : standY;
    }

    public static Realm byId(String id) {
        for (Realm r : values()) if (r.id.equals(id)) return r;
        return null;
    }
}
