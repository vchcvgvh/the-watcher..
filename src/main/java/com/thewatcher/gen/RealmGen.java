package com.thewatcher.gen;

/**
 * Builds the realms. Pure Java: the same code runs in-game (through a world-backed {@link BlockSink}) and
 * in tests/previews (through an array), so what you see in a preview is exactly what generates.
 *
 * Everything is deterministic from world coordinates, so a chunk always decorates the same way.
 */
public final class RealmGen {
    private RealmGen() {}

    // ================================================================== randomness

    public static float hash(long a, long b, int salt) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static float noise(float x, float z, int seed) {
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z);
        float fx = x - xi, fz = z - zi;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = hash(xi, zi, seed), b = hash(xi + 1, zi, seed), c = hash(xi, zi + 1, seed), d = hash(xi + 1, zi + 1, seed);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    private static float fbm(float x, float z, float scale, int seed) {
        return noise(x / scale, z / scale, seed) * 0.65f + noise(x / scale * 2.3f, z / scale * 2.3f, seed + 9) * 0.35f;
    }

    // ================================================================== entry points

    public static void decorate(Realm realm, BlockSink w, int chunkX, int chunkZ) {
        switch (realm) {
            case HALLWAYS -> hallways(w, chunkX, chunkZ);
            case HOLLOW -> hollow(w, chunkX, chunkZ);
            case FLESH -> flesh(w, chunkX, chunkZ);
            case STILL_WATER -> stillWater(w, chunkX, chunkZ);
        }
    }

    /** Clear a safe spot to arrive at. */
    public static void prepareArrival(Realm realm, BlockSink w, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int i = 0; i < realm.clearHeight; i++) {
                    if (realm == Realm.STILL_WATER && i == 0) w.set(x + dx, y, z + dz, B.WATER);
                    else w.set(x + dx, y + i, z + dz, B.AIR);
                }
    }

    /** Build the way out (and, where the realm has one, the trail leading to it). */
    public static void buildExit(Realm realm, BlockSink w, int ex, int ez, int ax, int az) {
        w.prepare(ex, ez);
        switch (realm) {
            case HALLWAYS -> hallwaysExit(w, ex, ez);
            case HOLLOW -> {
                hollowExit(w, ex, ez);
                trail(w, ax, az, ex, ez, 13, (px, pz) -> {
                    for (int y = 0; y < 3; y++) w.set(px, Realm.HOLLOW.standY + y, pz, B.AIR);
                    w.set(px, Realm.HOLLOW.standY, pz, B.COBBLED_DEEPSLATE_WALL);
                    w.set(px, Realm.HOLLOW.standY + 1, pz, B.SOUL_LANTERN);
                });
            }
            case FLESH -> fleshExit(w, ex, ez);
            case STILL_WATER -> {
                waterExit(w, ex, ez);
                trail(w, ax, az, ex, ez, 12, (px, pz) -> {
                    int y = Realm.STILL_WATER.standY;
                    for (int i = 1; i < 5; i++) w.set(px, y + i, pz, B.AIR);
                    w.set(px, y, pz, B.SPRUCE_FENCE_WET);
                    w.set(px, y + 1, pz, B.SPRUCE_FENCE);
                    w.set(px, y + 2, pz, B.SPRUCE_FENCE);
                    w.set(px, y + 3, pz, B.LANTERN);
                });
            }
        }
    }

    private interface Post { void place(int x, int z); }

    private static void trail(BlockSink w, int ax, int az, int ex, int ez, int step, Post post) {
        double len = Math.hypot(ex - ax, ez - az);
        for (double d = 10; d < len - 6; d += step) {
            double t = d / len;
            int px = (int) Math.floor(ax + (ex - ax) * t + 0.5) + 2, pz = (int) Math.floor(az + (ez - az) * t + 0.5);
            w.prepare(px, pz);
            post.place(px, pz);
        }
    }

    // ================================================================== THE HALLWAYS

    private static final int HY = 11;

    private static void hallways(BlockSink w, int cx0, int cz0) {
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cx0 * 16 + lx, z = cz0 * 16 + lz;
                int gx = Math.floorMod(x, 8), gz = Math.floorMod(z, 8);
                long cx = Math.floorDiv(x, 8), cz = Math.floorDiv(z, 8);
                boolean solid = false;
                if (gx == 0 && gz == 0) solid = true;
                else if (gx == 0) solid = hash(cx, cz, 1) < 0.55f && !((gz == 3 || gz == 4) && hash(cx, cz, 2) < 0.6f);
                else if (gz == 0) solid = hash(cx, cz, 3) < 0.55f && !((gx == 3 || gx == 4) && hash(cx, cz, 4) < 0.6f);
                if (solid) {
                    for (int y = HY; y < HY + 4; y++) w.set(x, y, z, B.SMOOTH_SANDSTONE);
                    if (hash(x, z, 14) < 0.14f) w.set(x, HY, z, B.BROWN_TERRACOTTA);              // water damage at the base
                    if (gx + gz != 0 && hash(x, z, 12) < 0.035f) {                                // a hole punched through
                        w.set(x, HY + 1, z, B.AIR);
                        w.set(x, HY + 2, z, B.AIR);
                    }
                } else {
                    if (hash(x, z, 9) < 0.035f) w.set(x, HY, z, B.BROWN_CARPET);                  // damp stains
                    if (gx == 1 && gz == 1 && hash(cx, cz, 15) < 0.12f) w.set(x, HY + 3, z, B.COBWEB);
                }
                if (gx == 4 && gz == 4) {
                    w.set(x, HY + 4, z, hash(cx, cz, 5) < 0.72f ? B.SEA_LANTERN : B.REDSTONE_LAMP);
                }
                if (gx == 2 && gz == 6 && hash(cx, cz, 6) < 0.05f) w.set(x, HY - 1, z, B.BROWN_TERRACOTTA);
                if ((gx == 2 || gx == 3) && (gz == 5 || gz == 6) && hash(cx, cz, 7) < 0.12f) w.set(x, HY + 4, z, B.AIR); // missing tiles
                if (gx == 6 && gz == 1 && hash(cx, cz, 13) < 0.09f) w.set(x, HY + 4, z, B.IRON_BARS);                // vents
                if (gx == 4 && gz == 2 && hash(cx, cz, 10) < 0.045f) {                                                // a lone chair
                    B[] chairs = {B.CHAIR_N, B.CHAIR_E, B.CHAIR_S, B.CHAIR_W};
                    w.set(x, HY, z, chairs[(int) (hash(cx, cz, 11) * 4) % 4]);
                }
            }
        }
    }

    private static void hallwaysExit(BlockSink w, int ex, int ez) {
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                for (int y = 0; y < 4; y++) w.set(ex + dx, HY + y, ez + dz, B.AIR);
                w.set(ex + dx, HY - 1, ez + dz, B.RED_TERRACOTTA);
            }
        w.set(ex, HY - 1, ez, B.CRYING_OBSIDIAN);
        w.set(ex, HY + 4, ez, B.SHROOMLIGHT);
        w.set(ex, HY, ez, B.DOOR_LOWER);
        w.set(ex, HY + 1, ez, B.DOOR_UPPER);
    }

    // ================================================================== THE HOLLOW

    private static final int OY = 64;
    private static final int[][] HORIZ = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};

    private static void deadTree(BlockSink w, long a, long b, int t, int x, int z, int baseY, B log, B logX, B logZ,
                                 boolean lanterns, B hangLight, int saltBase) {
        int h = 7 + (int) (hash(a, b, saltBase + 31 + t) * 7);
        for (int y = 0; y < h; y++) w.set(x, baseY + y, z, log);
        int br = 2 + (int) (hash(a, b, saltBase + 41 + t) * 3);
        for (int k = 0; k < br; k++) {
            int[] d = HORIZ[(int) (hash(a, b, saltBase + 51 + t * 7 + k) * 4) % 4];
            int y0 = baseY + h / 2 + (int) (hash(a, b, saltBase + 61 + t * 7 + k) * (h / 2));
            int len = 2 + (int) (hash(a, b, saltBase + 71 + t * 7 + k) * 3);
            for (int i = 1; i <= len; i++) w.set(x + d[0] * i, y0 + i / 2, z + d[1] * i, d[0] != 0 ? logX : logZ);
            if (lanterns && hash(a, b, saltBase + 81 + t * 7 + k) < 0.14f) {            // a lantern left hanging
                int lx = x + d[0] * len, lz = z + d[1] * len, ly = y0 + len / 2;
                w.set(lx, ly - 1, lz, B.CHAIN);
                w.set(lx, ly - 2, lz, hangLight);
            }
        }
    }

    private static void hollow(BlockSink w, int cx, int cz) {
        long a = cx, b = cz;
        int sx = cx * 16, sz = cz * 16;
        int trees = 1 + (int) (hash(a, b, 10) * 3);
        for (int t = 0; t < trees; t++) {
            int x = sx + 2 + (int) (hash(a, b, 11 + t) * 12), z = sz + 2 + (int) (hash(a, b, 21 + t) * 12);
            deadTree(w, a, b, t, x, z, OY, B.DARK_OAK_LOG, B.DARK_OAK_LOG_X, B.DARK_OAK_LOG_Z, true, B.SOUL_LANTERN_HANGING, 0);
            for (int[] d : HORIZ) if (hash(a, b, 300 + t * 5 + d[0] + d[1] * 2) < 0.6f) w.set(x + d[0], OY - 1, z + d[1], B.ROOTED_DIRT);
        }
        // fallen trees and stumps
        if (hash(a, b, 120) < 0.3f) {
            boolean alongX = hash(a, b, 121) < 0.5f;
            int x = sx + 3 + (int) (hash(a, b, 122) * 6), z = sz + 3 + (int) (hash(a, b, 123) * 6);
            int len = 4 + (int) (hash(a, b, 124) * 4);
            for (int i = 0; i < len; i++) w.set(alongX ? x + i : x, OY, alongX ? z : z + i, alongX ? B.DARK_OAK_LOG_X : B.DARK_OAK_LOG_Z);
        }
        if (hash(a, b, 130) < 0.35f) {
            int x = sx + (int) (hash(a, b, 131) * 16), z = sz + (int) (hash(a, b, 132) * 16);
            w.set(x, OY, z, B.DARK_OAK_LOG);
            if (hash(a, b, 133) < 0.5f) w.set(x, OY + 1, z, B.DARK_OAK_LOG);
        }
        // a row of graves
        if (hash(a, b, 140) < 0.13f) {
            int x = sx + 2 + (int) (hash(a, b, 141) * 4), z = sz + 3 + (int) (hash(a, b, 142) * 8);
            int n = 3 + (int) (hash(a, b, 143) * 3);
            for (int i = 0; i < n; i++) {
                int gx = x + i * 2;
                w.set(gx, OY, z, B.STONE_BRICK_WALL);
                if (hash(a, b, 144 + i) < 0.5f) w.set(gx, OY + 1, z, B.STONE_BRICK_WALL);
                else if (hash(a, b, 150 + i) < 0.4f) w.set(gx, OY + 1, z, B.SKELETON_SKULL);
                w.set(gx, OY - 1, z + 1, B.ROOTED_DIRT);
                w.set(gx, OY - 1, z + 2, B.ROOTED_DIRT);
            }
        }
        // a ruined cabin
        if (hash(a, b, 160) < 0.045f) {
            int x0 = sx + 3, z0 = sz + 4;
            for (int dx = 0; dx < 8; dx++)
                for (int dz = 0; dz < 6; dz++) {
                    boolean edge = dx == 0 || dx == 7 || dz == 0 || dz == 5;
                    if (!edge) {
                        if (hash(x0 + dx, z0 + dz, 161) < 0.25f) w.set(x0 + dx, OY + 3, z0 + dz, B.COBWEB);
                        continue;
                    }
                    if (dz == 0 && (dx == 3 || dx == 4)) continue;                                   // doorway
                    int hgt = (int) (hash(x0 + dx, z0 + dz, 162) * 5);
                    for (int y = 0; y < hgt; y++)
                        w.set(x0 + dx, OY + y, z0 + dz, hash(x0 + dx, y, 163) < 0.4f ? B.CRACKED_DEEPSLATE_BRICKS : B.DEEPSLATE_BRICKS);
                }
            w.set(x0 + 5, OY, z0 + 3, B.SOUL_LANTERN);
            w.set(x0 + 2, OY, z0 + 2, B.SKELETON_SKULL);
        }
        // a black pool
        if (hash(a, b, 170) < 0.09f) {
            int x = sx + 4 + (int) (hash(a, b, 171) * 6), z = sz + 4 + (int) (hash(a, b, 172) * 6);
            for (int dx = 0; dx < 4; dx++)
                for (int dz = 0; dz < 3; dz++) {
                    if ((dx == 0 || dx == 3) && (dz == 0 || dz == 2)) continue;
                    w.set(x + dx, OY - 1, z + dz, B.WATER);
                    w.set(x + dx, OY, z + dz, B.AIR);
                }
        }
        // ground clutter
        int bits = 4 + (int) (hash(a, b, 80) * 6);
        for (int k = 0; k < bits; k++) {
            int x = sx + (int) (hash(a, b, 81 + k) * 16), z = sz + (int) (hash(a, b, 91 + k) * 16);
            if (!w.isAir(x, OY, z)) continue;
            float r = hash(a, b, 101 + k);
            B s = r < 0.4f ? B.DEAD_BUSH : r < 0.58f ? B.COBWEB : r < 0.72f ? B.BONE_BLOCK : r < 0.82f ? B.SKELETON_SKULL
                    : r < 0.92f ? B.BROWN_MUSHROOM : B.COBBLED_DEEPSLATE_WALL;
            w.set(x, OY, z, s);
        }
    }

    private static void hollowExit(BlockSink w, int ex, int ez) {
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++)
                for (int y = 0; y < 9; y++) w.set(ex + dx, OY + y, ez + dz, B.AIR);
        for (int y = 0; y < 4; y++) {
            w.set(ex - 2, OY + y, ez, B.POLISHED_BLACKSTONE_BRICKS);
            w.set(ex + 2, OY + y, ez, B.POLISHED_BLACKSTONE_BRICKS);
        }
        for (int dx = -2; dx <= 2; dx++) w.set(ex + dx, OY + 4, ez, B.POLISHED_BLACKSTONE_BRICKS);
        for (int dx = -1; dx <= 1; dx++) w.set(ex + dx, OY - 1, ez, B.CRYING_OBSIDIAN);
        w.set(ex - 1, OY + 3, ez, B.SOUL_LANTERN_HANGING);
        w.set(ex + 1, OY + 3, ez, B.SOUL_LANTERN_HANGING);
    }

    // ================================================================== THE FLESH

    private static final int FY = 11, FTOP = 22;       // open from FY to FTOP inclusive

    /** Cave walls: where this is high, it's solid meat from floor to ceiling. */
    private static float meat(int x, int z) {
        return fbm(x, z, 11f, 500) + 0.18f * (fbm(x, z, 40f, 510) - 0.5f);
    }

    private static boolean wallAt(int x, int z) { return meat(x, z) > 0.6f; }

    private static void flesh(BlockSink w, int cx, int cz) {
        long a = cx, b = cz;
        int sx = cx * 16, sz = cz * 16;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = sx + lx, z = sz + lz;
                float m = meat(x, z);
                if (m > 0.6f) {
                    boolean face = !wallAt(x + 1, z) || !wallAt(x - 1, z) || !wallAt(x, z + 1) || !wallAt(x, z - 1);
                    for (int y = FY; y <= FTOP; y++) {
                        B blk = B.NETHER_WART_BLOCK;
                        if (face) {
                            float vein = Math.abs(fbm(x + y * 0.7f, z - y * 0.5f, 4f, 520) - 0.5f);
                            if (vein < 0.035f) blk = B.CRIMSON_HYPHAE;
                            else if (hash(x * 31 + z, y, 521) < 0.12f) blk = B.RED_MUSHROOM_BLOCK;
                            float r = hash(x * 17 + y, z, 522);
                            if (r < 0.012f) blk = B.SHROOMLIGHT;
                            else if (r < 0.02f) blk = B.MAGMA_BLOCK;
                            else if (r < 0.024f) blk = B.PEARLESCENT_FROGLIGHT;
                        }
                        w.set(x, y, z, blk);
                    }
                } else if (m > 0.54f) {
                    // the wall swells out at the floor and ceiling: organic, sloping walls
                    int bulge = (int) ((m - 0.54f) / 0.06f * 4f);
                    for (int i = 0; i < bulge; i++) {
                        w.set(x, FY + i, z, B.NETHER_WART_BLOCK);
                        w.set(x, FTOP - i, z, B.NETHER_WART_BLOCK);
                    }
                } else {
                    // open cave: teeth from the ceiling and floor, clutter, glowing growths overhead
                    float r = hash(x, z, 530);
                    if (r < 0.05f) {
                        int len = 1 + (int) (hash(x, z, 531) * 3);
                        B[] down = len == 1 ? new B[]{B.DRIP_DOWN_TIP} : len == 2 ? new B[]{B.DRIP_DOWN_FRUSTUM, B.DRIP_DOWN_TIP}
                                : new B[]{B.DRIP_DOWN_BASE, B.DRIP_DOWN_FRUSTUM, B.DRIP_DOWN_TIP};
                        for (int i = 0; i < down.length; i++) w.set(x, FTOP - i, z, down[i]);
                    } else if (r < 0.075f) {
                        int len = 1 + (int) (hash(x, z, 532) * 2);
                        B[] up = len == 1 ? new B[]{B.DRIP_UP_TIP} : new B[]{B.DRIP_UP_FRUSTUM, B.DRIP_UP_TIP};
                        for (int i = 0; i < up.length; i++) w.set(x, FY + i, z, up[i]);
                    } else if (r < 0.085f) {
                        w.set(x, FTOP, z, B.SHROOMLIGHT);
                    } else if (r < 0.095f) {
                        w.set(x, FY, z, B.BONE_BLOCK);
                    } else if (r < 0.1f) {
                        w.set(x, FY, z, B.SKELETON_SKULL);
                    }
                    if (hash(x, z, 533) < 0.08f) w.set(x, FY - 1, z, B.RED_MUSHROOM_BLOCK);
                }
            }
        }
        // a ribcage tunnel: parallel bone arches across an open stretch
        if (hash(a, b, 540) < 0.4f) {
            boolean alongX = hash(a, b, 541) < 0.5f;
            int ribs = 3 + (int) (hash(a, b, 542) * 3);
            int base = 3 + (int) (hash(a, b, 543) * 4);
            int mid = 8;
            for (int r = 0; r < ribs; r++) {
                int along = base + r * 2;
                for (int s = -5; s <= 5; s++) {
                    double t = (s + 5) / 10.0 * Math.PI;
                    int y = FY + (int) Math.round(Math.sin(t) * 10.0);
                    int x = sx + (alongX ? along : mid + s), z = sz + (alongX ? mid + s : along);
                    if (y > FTOP) y = FTOP;
                    if (w.isAir(x, y, z)) w.set(x, y, z, alongX ? B.BONE_BLOCK_Z : B.BONE_BLOCK_X);
                    if (y + 1 <= FTOP && w.isAir(x, y + 1, z) && Math.abs(s) >= 4) w.set(x, y + 1, z, B.BONE_BLOCK);
                }
            }
        }
        // a pool of blood
        if (hash(a, b, 550) < 0.3f) {
            int x = sx + 4 + (int) (hash(a, b, 551) * 6), z = sz + 4 + (int) (hash(a, b, 552) * 6);
            boolean open = true;
            for (int dx = -1; dx <= 4 && open; dx++)
                for (int dz = -1; dz <= 3 && open; dz++) open = !wallAt(x + dx, z + dz);
            if (open) {
                for (int dx = 0; dx < 4; dx++)
                    for (int dz = 0; dz < 3; dz++) {
                        if ((dx == 0 || dx == 3) && (dz == 0 || dz == 2)) continue;
                        w.set(x + dx, FY - 1, z + dz, B.WATER);
                        w.set(x + dx, FY - 2, z + dz, B.WATER);
                        w.set(x + dx, FY, z + dz, B.AIR);
                    }
            }
        }
    }

    private static void fleshExit(BlockSink w, int ex, int ez) {
        // "the mouth": a round chamber ringed with teeth, a pale light above, crying obsidian in the middle
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz > 18) continue;
                for (int y = FY; y <= FTOP; y++) w.set(ex + dx, y, ez + dz, B.AIR);
                w.set(ex + dx, FY - 1, ez + dz, B.NETHER_WART_BLOCK);
            }
        for (int k = 0; k < 16; k++) {
            double ang = k / 16.0 * Math.PI * 2;
            int x = ex + (int) Math.round(Math.cos(ang) * 3.4), z = ez + (int) Math.round(Math.sin(ang) * 3.4);
            w.set(x, FY, z, B.DRIP_UP_FRUSTUM);
            w.set(x, FY + 1, z, B.DRIP_UP_TIP);
            w.set(x, FTOP, z, B.DRIP_DOWN_FRUSTUM);
            w.set(x, FTOP - 1, z, B.DRIP_DOWN_TIP);
        }
        w.set(ex, FY - 1, ez, B.CRYING_OBSIDIAN);
        w.set(ex, FTOP, ez, B.PEARLESCENT_FROGLIGHT);
        w.set(ex + 1, FTOP, ez, B.PEARLESCENT_FROGLIGHT);
        w.set(ex, FTOP, ez + 1, B.PEARLESCENT_FROGLIGHT);
    }

    // ================================================================== THE STILL WATER

    private static final int WY = 63;    // the water layer: you stand in it

    private static void stillWater(BlockSink w, int cx, int cz) {
        long a = cx, b = cz;
        int sx = cx * 16, sz = cz * 16;
        int trees = (int) (hash(a, b, 600) * 3);
        for (int t = 0; t < trees; t++) {
            int x = sx + 2 + (int) (hash(a, b, 601 + t) * 12), z = sz + 2 + (int) (hash(a, b, 611 + t) * 12);
            boolean mangrove = hash(a, b, 621 + t) < 0.5f;
            B log = mangrove ? B.MANGROVE_LOG : B.DARK_OAK_LOG;
            deadTree(w, a, b, t, x, z, WY, log, mangrove ? B.MANGROVE_LOG : B.DARK_OAK_LOG_X, mangrove ? B.MANGROVE_LOG : B.DARK_OAK_LOG_Z,
                    true, B.LANTERN_HANGING, 600);
            for (int[] d : HORIZ) w.set(x + d[0], WY, z + d[1], B.MANGROVE_ROOTS_WET);
            for (int[] d : HORIZ) if (hash(a, b, 640 + t * 4 + d[0] + d[1] * 2) < 0.4f) w.set(x + d[0] * 2, WY, z + d[1] * 2, B.MANGROVE_ROOTS_WET);
        }
        // sunken ruins: broken pillars and arches of old stone
        if (hash(a, b, 650) < 0.22f) {
            int x = sx + 3 + (int) (hash(a, b, 651) * 8), z = sz + 3 + (int) (hash(a, b, 652) * 8);
            boolean arch = hash(a, b, 653) < 0.45f;
            int pillars = arch ? 2 : 1;
            for (int p = 0; p < pillars; p++) {
                int px = x + p * 4;
                int hgt = arch ? 5 : 2 + (int) (hash(a, b, 654 + p) * 5);
                for (int y = 0; y < hgt; y++) {
                    float r = hash(px, y, 655 + z);
                    w.set(px, WY + y, z, r < 0.35f ? B.MOSSY_STONE_BRICKS : r < 0.6f ? B.CRACKED_STONE_BRICKS : B.STONE_BRICKS);
                }
            }
            if (arch) for (int dx = 0; dx <= 4; dx++) if (hash(x + dx, z, 656) < 0.8f) w.set(x + dx, WY + 5, z, B.MOSSY_STONE_BRICKS);
        }
        // an old fence line, half drowned
        if (hash(a, b, 660) < 0.18f) {
            int x = sx + 2 + (int) (hash(a, b, 661) * 6), z = sz + 2 + (int) (hash(a, b, 662) * 12);
            int n = 3 + (int) (hash(a, b, 663) * 5);
            for (int i = 0; i < n; i++) {
                if (hash(x + i, z, 664) < 0.25f) continue;
                w.set(x + i, WY, z, B.SPRUCE_FENCE_WET);
                if (hash(x + i, z, 665) < 0.6f) w.set(x + i, WY + 1, z, B.SPRUCE_FENCE);
            }
        }
        // lily pads and weed
        int bits = 6 + (int) (hash(a, b, 670) * 10);
        for (int k = 0; k < bits; k++) {
            int x = sx + (int) (hash(a, b, 671 + k) * 16), z = sz + (int) (hash(a, b, 701 + k) * 16);
            if (w.get(x, WY, z) != B.WATER || !w.isAir(x, WY + 1, z)) continue;
            if (hash(a, b, 731 + k) < 0.55f) w.set(x, WY + 1, z, B.LILY_PAD);
            else w.set(x, WY, z, B.SEAGRASS);
        }
    }

    private static void waterExit(BlockSink w, int ex, int ez) {
        // a little island of old stone, an archway hung with lanterns, crying obsidian at its heart
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = 1; y < 9; y++) w.set(ex + dx, WY + y, ez + dz, B.AIR);
                if (Math.abs(dx) == 3 && Math.abs(dz) == 3) continue;
                w.set(ex + dx, WY, ez + dz, B.MOSSY_COBBLESTONE);
            }
        for (int y = 1; y <= 4; y++) {
            w.set(ex - 2, WY + y, ez, B.STONE_BRICKS);
            w.set(ex + 2, WY + y, ez, B.MOSSY_STONE_BRICKS);
        }
        for (int dx = -2; dx <= 2; dx++) w.set(ex + dx, WY + 5, ez, B.MOSSY_STONE_BRICKS);
        w.set(ex - 1, WY + 4, ez, B.LANTERN_HANGING);
        w.set(ex + 1, WY + 4, ez, B.LANTERN_HANGING);
        w.set(ex, WY, ez, B.CRYING_OBSIDIAN);
    }
}
