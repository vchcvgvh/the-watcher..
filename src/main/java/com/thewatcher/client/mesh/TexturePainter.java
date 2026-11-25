package com.thewatcher.client.mesh;

/**
 * Paints every material the creatures use, in code (the mod ships no image files).
 * A 2560x1024 atlas of ten 512px cells; everything except HAIR and EYE tiles seamlessly in both directions.
 * Each material also produces a height map, which is used to emboss the colour (light from the upper left), so
 * pores, wrinkles and cracks sink in and veins, scars, fibres and fur stand out.
 *
 *   SKIN     corpse-grey human skin: sallow and bruised mottling, branching veins, pores, creases,
 *            age spots, pale scars, runs of dried blood
 *   BONE     yellowed ivory: porous, stained, enamel striations, hairline cracks (teeth, exposed ribs)
 *   HAIR     fine wet black strands with transparent gaps and uneven wispy ends (hair cards)
 *   CLOTH    rotten black weave: threads, dust and mud stains, moth holes (the Faceless's robe)
 *   PALE     porcelain-white skin with faint blue veins and fine crazing (the Faceless)
 *   FLESH    raw muscle: fibres, white fascia, dark clots, wet glints (gums, wounds)
 *   EYE      bloodshot yellowed eyeball, cloudy grey iris with fibres, pinprick pupil (painted for sphere UVs)
 *   SCALP    dark skin thick with hair roots
 *   KERATIN  dirty claw and nail: dark, striated, chipped
 *   HIDE     mangy grey-brown dog hide: short bristly fur, bald scabby patches (the Hound)
 *
 * Returns ARGB pixels, row-major. Independent of Minecraft so it can be previewed outside the game.
 */
public final class TexturePainter {
    private TexturePainter() {}

    public static final int CELL = 512;
    public static final int WIDTH = CELL * MeshBuilder.Swatch.COLS, HEIGHT = CELL * MeshBuilder.Swatch.ROWS;

    /** @param glow true = the full-bright overlay (only irises/pupils are visible). */
    /** Height of the last painted pixel (set by each material function). */
    private static float curH;

    public static int[] paint(boolean glow) {
        int[] px = new int[WIDTH * HEIGHT];
        float[] height = new float[WIDTH * HEIGHT];
        MeshBuilder.Swatch[] cells = new MeshBuilder.Swatch[MeshBuilder.Swatch.COLS * MeshBuilder.Swatch.ROWS];
        for (MeshBuilder.Swatch s : MeshBuilder.Swatch.values()) cells[s.row * MeshBuilder.Swatch.COLS + s.col] = s;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                float u = (x % CELL + 0.5f) / CELL, v = (y % CELL + 0.5f) / CELL;
                MeshBuilder.Swatch s = cells[(y / CELL) * MeshBuilder.Swatch.COLS + (x / CELL)];
                int c;
                curH = 0f;
                if (glow) {
                    c = s == MeshBuilder.Swatch.EYE ? eyeGlow(u, v) : 0;
                } else {
                    c = switch (s) {
                        case SKIN -> skin(u, v);
                        case BONE -> bone(u, v);
                        case HAIR -> hair(u, v, x);
                        case CLOTH -> cloth(u, v);
                        case PALE -> pale(u, v);
                        case FLESH -> flesh(u, v);
                        case EYE -> eye(u, v);
                        case SCALP -> scalp(u, v);
                        case KERATIN -> keratin(u, v);
                        case HIDE -> hide(u, v);
                    };
                }
                px[y * WIDTH + x] = c;
                height[y * WIDTH + x] = curH;
            }
        }
        if (!glow) emboss(px, height, cells);
        return px;
    }

    /** Fake relief: brighten slopes facing the upper left, darken the others (wraps within each cell). */
    private static void emboss(int[] px, float[] h, MeshBuilder.Swatch[] cells) {
        int[] out = px.clone();
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                MeshBuilder.Swatch s = cells[(y / CELL) * MeshBuilder.Swatch.COLS + (x / CELL)];
                float k = switch (s) {
                    case SKIN, PALE -> 2.2f;
                    case HIDE, CLOTH, FLESH, SCALP -> 2.6f;
                    case BONE, KERATIN -> 2.0f;
                    default -> 0f;
                };
                if (k == 0f) continue;
                int cx = (x / CELL) * CELL, cy = (y / CELL) * CELL;
                int x0 = cx + Math.floorMod(x - cx - 1, CELL), x1 = cx + Math.floorMod(x - cx + 1, CELL);
                int y0 = cy + Math.floorMod(y - cy - 1, CELL), y1 = cy + Math.floorMod(y - cy + 1, CELL);
                float d = (h[y0 * WIDTH + x0] - h[y1 * WIDTH + x1]) * k;
                float f = Math.max(0.55f, Math.min(1.45f, 1f + d));
                int c = px[y * WIDTH + x];
                int a = c >>> 24, r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                out[y * WIDTH + x] = argb(a, (int) (r * f), (int) (g * f), (int) (b * f));
            }
        }
        System.arraycopy(out, 0, px, 0, px.length);
    }

    // =================================================================== materials

    private static int skin(float u, float v) {
        float n = fbm(u, v, 3, 1, 5);
        float[] c = mix(rgb3(64, 60, 60), rgb3(104, 98, 90), n);                    // ashen grey
        c = mix(c, rgb3(98, 96, 72), smooth(0.45f, 0.75f, fbm(u, v, 2, 2, 3)) * 0.45f); // sallow, jaundiced
        c = mix(c, rgb3(84, 52, 66), smooth(0.58f, 0.82f, fbm(u, v, 4, 3, 4)) * 0.6f);  // bruising
        c = mix(c, rgb3(70, 40, 50), smooth(0.7f, 0.9f, fbm(u, v, 2, 4, 3)) * 0.5f);    // livor mortis
        // branching veins at two scales, stronger where the skin is thin
        float veinMask = smooth(0.35f, 0.7f, fbm(u, v, 3, 5, 3));
        float v1 = smooth(0.962f, 0.99f, vein(u, v, 4, 6));
        float v2 = smooth(0.972f, 0.994f, vein(u, v, 9, 7));
        c = mix(c, rgb3(52, 48, 78), v1 * 0.6f * veinMask);
        c = mix(c, rgb3(66, 58, 84), v2 * 0.35f * veinMask);
        // soft folds around the limb (no hard lines)
        float crease = smooth(0.55f, 0.85f, fbm(u, v, 2, 26, 8, 3)) * fbm(u, v, 3, 17, 2);
        c = scale(c, 1f - crease * 0.22f);
        // pores: tiny, uneven, at random
        float[] w = worley(u, v, 110, 9);
        float pore = smooth(0.2f, 0.05f, w[0]) * (0.3f + 0.7f * hashF(cellId(u, v, 110), 9));
        c = scale(c, 1f - pore * 0.16f);
        // a few soft age spots
        float[] spots = worley(u, v, 9, 10);
        c = mix(c, rgb3(70, 54, 44), smooth(0.2f, 0.02f, spots[0]) * smooth(0.82f, 0.92f, hashF(cellId(u, v, 9), 11)) * 0.45f);
        // pale scars, faint
        c = mix(c, rgb3(132, 120, 114), smooth(0.99f, 0.998f, vein(u, v, 3, 12)) * smooth(0.65f, 0.78f, fbm(u, v, 2, 18, 2)) * 0.3f);
        // runs of dried blood, dripping along the body
        float drip = smooth(0.72f, 0.9f, fbm(u, v, 22, 2, 13, 2)) * smooth(0.62f, 0.78f, fbm(u, v, 2, 14, 2));
        c = mix(c, mix(rgb3(72, 14, 14), rgb3(40, 8, 8), fbm(u, v, 8, 15, 2)), drip * 0.85f);
        // fresher blood: wet spatter and smears that haven't dried yet
        float[] sp = worley(u, v, 7, 23);
        float wetBlood = smooth(0.5f, 0.2f, sp[0] + (fbm(u, v, 9, 24, 3) - 0.5f) * 0.45f) * smooth(0.86f, 0.93f, hashF(cellId(u, v, 7), 25));
        c = mix(c, mix(rgb3(70, 6, 8), rgb3(105, 12, 14), fbm(u, v, 12, 28, 2)), wetBlood * 0.25f);   // faint old bruising only; real blood is 3D
        // skin micro-relief: the fine criss-cross of real skin, plus goosebumpy unevenness
        float[] cells = worley(u, v, 70, 19);
        float micro = smooth(0.06f, 0.0f, cells[1] - cells[0]);
        c = scale(c, 1f - micro * 0.12f);
        // subsurface: a faint red flush where the skin is thin, around veins
        c = mix(c, rgb3(110, 62, 64), (v1 * 0.3f + smooth(0.6f, 0.85f, fbm(u, v, 5, 20, 2)) * 0.12f) * veinMask);
        // subtle overall grime / micro detail
        c = scale(c, 0.9f + 0.2f * fbm(u, v, 32, 16, 2));
        curH = 0.035f * fbm(u, v, 12, 21, 3) + 0.03f * v1 + 0.02f * v2 - 0.03f * crease + 0.02f * wetBlood
                - 0.02f * pore - 0.015f * micro;
        return rgb(c);
    }

    private static int hide(float u, float v) {
        float n = fbm(u, v, 3, 20, 4);
        float[] skinCol = mix(rgb3(104, 90, 88), rgb3(124, 106, 100), n);          // bald, raw-ish skin
        float[] furBase = mix(rgb3(34, 31, 30), rgb3(62, 57, 52), fbm(u, v, 6, 21, 3));
        // short bristly fur: three layers of randomly placed, angled, overlapping strands
        float[] fs = fur(u, v, 22);
        float strand = fs[0];
        float[] fur = mix(scale(furBase, 0.7f), scale(furBase, 0.75f + fs[1] * 0.9f), strand);
        // mange: bald patches
        float mange = smooth(0.6f, 0.72f, fbm(u, v, 4, 23, 4));
        float[] c = mix(fur, skinCol, mange);
        // scabs and sores on the bald bits
        float[] w = worley(u, v, 14, 24);
        float scab = smooth(0.18f, 0.08f, w[0]) * smooth(0.55f, 0.75f, hashF(cellId(u, v, 14), 25)) * mange;
        c = mix(c, mix(rgb3(88, 24, 20), rgb3(52, 20, 16), fbm(u, v, 16, 26, 2)), scab);
        // veins showing through bald skin
        c = mix(c, rgb3(80, 64, 92), smooth(0.965f, 0.99f, vein(u, v, 5, 27)) * mange * 0.5f);
        float matted = smooth(0.66f, 0.82f, fbm(u, v, 3, 30, 3));
        c = mix(c, mix(rgb3(60, 10, 10), rgb3(96, 14, 14), fs[1]), matted * 0.7f);       // fur matted with blood
        c = scale(c, 0.88f + 0.22f * fbm(u, v, 24, 28, 2));
        curH = (1f - mange) * strand * 0.05f + scab * 0.04f + 0.02f * fbm(u, v, 10, 29, 2);
        return rgb(c);
    }

    private static int pale(float u, float v) {
        float n = fbm(u, v, 3, 30, 4);
        float[] c = mix(rgb3(196, 192, 190), rgb3(226, 224, 220), n);                 // porcelain
        c = mix(c, rgb3(186, 190, 204), smooth(0.5f, 0.8f, fbm(u, v, 2, 31, 3)) * 0.5f); // cold blue undertone
        c = mix(c, rgb3(150, 160, 192), smooth(0.97f, 0.993f, vein(u, v, 4, 32)) * smooth(0.3f, 0.7f, fbm(u, v, 3, 35, 2)) * 0.45f); // faint veins
        float[] w = worley(u, v, 18, 33);
        float craze = smooth(0.05f, 0.0f, w[1] - w[0]);
        c = scale(c, 1f - craze * 0.25f);                                              // fine crazing
        c = scale(c, 0.94f + 0.1f * fbm(u, v, 40, 34, 2));
        curH = -0.03f * craze + 0.015f * fbm(u, v, 8, 36, 2);
        return rgb(c);
    }

    private static int cloth(float u, float v) {
        float warp = 0.5f + 0.5f * (float) Math.sin(u * Math.PI * 2 * 72 + fbm(u, v, 4, 40, 2) * 2f);
        float weft = 0.5f + 0.5f * (float) Math.sin(v * Math.PI * 2 * 72 + fbm(u, v, 4, 41, 2) * 2f);
        float checker = ((int) (u * 72) + (int) (v * 72)) % 2 == 0 ? warp : weft;
        float[] c = mix(rgb3(14, 13, 16), rgb3(36, 34, 38), checker * 0.7f + fbm(u, v, 6, 42, 3) * 0.3f);
        c = mix(c, rgb3(58, 52, 46), smooth(0.58f, 0.8f, fbm(u, v, 3, 43, 4)) * 0.55f);   // dust and dried mud
        c = mix(c, rgb3(42, 22, 20), smooth(0.7f, 0.85f, fbm(u, v, 2, 44, 3)) * 0.4f);   // old stains
        float[] w = worley(u, v, 7, 45);
        float hole = smooth(0.16f, 0.1f, w[0]) * smooth(0.7f, 0.85f, hashF(cellId(u, v, 7), 46));
        c = mix(c, rgb3(4, 4, 5), hole);                                               // moth holes
        c = mix(c, rgb3(62, 60, 64), smooth(0.97f, 0.995f, ridged(u, v, 2, 24, 47, 2)) * 0.5f); // loose threads
        float folds = fbm(u, v, 3, 48, 3);
        c = scale(c, 0.8f + 0.4f * folds);                                             // creased, hanging folds
        curH = checker * 0.03f - hole * 0.05f + folds * 0.06f;
        return rgb(c);
    }

    private static int bone(float u, float v) {
        float n = fbm(u, v, 4, 50, 4);
        float[] c = mix(rgb3(164, 150, 116), rgb3(222, 212, 184), n);
        float stria = 0.5f + 0.5f * (float) Math.sin(u * Math.PI * 2 * 20 + fbm(u, v, 3, 51, 2) * 5f);
        c = scale(c, 0.92f + 0.12f * stria);                                            // enamel striations
        c = mix(c, rgb3(96, 72, 44), smooth(0.55f, 0.82f, fbm(u, v, 3, 52, 3)) * 0.7f);  // brown staining
        c = mix(c, rgb3(70, 22, 18), smooth(0.75f, 0.9f, fbm(u, v, 2, 53, 3)) * 0.45f);  // old blood
        float[] w = worley(u, v, 30, 54);
        c = scale(c, 1f - smooth(0.12f, 0.03f, w[0]) * 0.25f);                          // porous pits
        float crack = smooth(0.984f, 0.997f, vein(u, v, 6, 55)) * smooth(0.45f, 0.7f, fbm(u, v, 3, 56, 2));
        c = mix(c, rgb3(52, 40, 28), crack * 0.85f); // cracks
        curH = stria * 0.02f - crack * 0.05f - smooth(0.12f, 0.03f, w[0]) * 0.03f;
        return rgb(c);
    }

    private static int keratin(float u, float v) {
        float stria = 0.5f + 0.5f * (float) Math.sin(u * Math.PI * 2 * 26 + fbm(u, v, 3, 60, 2) * 6f);
        float[] c = mix(rgb3(30, 24, 20), rgb3(70, 58, 44), stria * 0.5f + fbm(u, v, 4, 61, 3) * 0.5f);
        c = mix(c, rgb3(104, 90, 70), smooth(0.8f, 0.95f, fbm(u, v, 3, 62, 3)) * 0.6f);  // chipped, lighter
        c = mix(c, rgb3(18, 14, 12), smooth(0.6f, 0.85f, fbm(u, v, 5, 63, 3)) * 0.6f);   // grime
        c = mix(c, rgb3(60, 14, 12), smooth(0.78f, 0.92f, fbm(u, v, 2, 64, 3)) * 0.5f);  // blood
        curH = stria * 0.04f - smooth(0.8f, 0.95f, fbm(u, v, 3, 62, 3)) * 0.04f;
        return rgb(c);
    }

    private static int flesh(float u, float v) {
        float n = fbm(u, v, 4, 70, 4);
        float[] c = mix(rgb3(54, 6, 10), rgb3(140, 24, 30), n);
        float fib = 0.5f + 0.5f * (float) Math.sin(u * Math.PI * 2 * 40 + fbm(u, v, 4, 71, 3) * 9f);
        c = mix(c, rgb3(36, 3, 6), smooth(0.7f, 1f, fib) * 0.55f);                        // muscle fibres
        c = mix(c, rgb3(200, 150, 148), smooth(0.978f, 0.995f, vein(u, v, 3, 72)) * 0.5f); // white fascia
        float[] w = worley(u, v, 10, 73);
        c = mix(c, rgb3(28, 2, 4), smooth(0.14f, 0.05f, w[0]) * 0.8f);                   // dark clots
        float wet = smooth(0.82f, 0.94f, fbm(u, v, 8, 74, 2));
        c = mix(c, rgb3(236, 170, 170), wet * 0.5f);                                       // wet glints
        curH = fib * 0.04f + smooth(0.14f, 0.05f, w[0]) * 0.04f + 0.02f * n;
        return rgb(c);
    }

    private static int scalp(float u, float v) {
        float[] c = mix(rgb3(26, 22, 22), rgb3(48, 42, 40), fbm(u, v, 6, 80, 3));
        float lines = 0.5f + 0.5f * (float) Math.sin(u * Math.PI * 2 * 90 + fbm(u, v, 3, 81, 2) * 10f);
        c = mix(c, rgb3(8, 7, 7), smooth(0.5f, 0.92f, lines) * 0.85f);
        float[] w = worley(u, v, 60, 82);
        c = scale(c, 1f - smooth(0.18f, 0.05f, w[0]) * 0.4f);
        curH = lines * 0.03f;
        return rgb(c);
    }

    private static int hair(float u, float v, int x) {
        float wave = 1.6f * (float) Math.sin(v * 9f + fbm(u, v, 2, 90, 2) * 4f);
        float coord = u * CELL + wave;
        float spacing = 6.5f;
        int id = (int) Math.floor(coord / spacing);
        float f = coord / spacing - id;
        float core = Math.abs(f - 0.5f) * 2f;
        float end = 0.4f + 0.6f * hashF(id, 91);
        float thick = 0.6f - 0.4f * v;
        boolean onStrand = core < thick && v < end;
        if (!onStrand) return 0;
        float[] c = mix(rgb3(8, 7, 7), rgb3(32, 27, 25), fbm(u, v, 8, 94, 2));
        if (hashF(id, 95) > 0.78f) c = mix(c, rgb3(86, 82, 82), 0.55f * (1f - core));     // wet sheen
        c = scale(c, 0.75f + 0.35f * (1f - core));
        return rgb(c);
    }

    // ---- eye: (u, v) are the eyeball's sphere coordinates; the front faces u = 0.75, v = 0.5

    private static float eyeAngle(float u, float v) {
        double th = Math.PI * v, ph = 2 * Math.PI * u;
        return (float) Math.acos(Math.max(-1, Math.min(1, -Math.sin(th) * Math.sin(ph))));
    }

    private static float eyeAround(float u, float v) {
        double th = Math.PI * v, ph = 2 * Math.PI * u;
        double x = Math.sin(th) * Math.cos(ph), y = -Math.cos(th);
        return (float) Math.atan2(y, x);
    }

    private static int eye(float u, float v) {
        float ang = eyeAngle(u, v), around = eyeAround(u, v);
        float[] c;
        if (ang < 0.13f) {
            c = rgb3(14, 12, 12);
        } else if (ang < 0.47f) {
            float fib = 0.5f + 0.5f * (float) Math.sin(around * 46f + fbm(u, v, 30, 100, 2) * 3f);
            float ring = (ang - 0.13f) / 0.34f;
            c = mix(rgb3(108, 116, 120), rgb3(200, 206, 208), fib * 0.6f + (1f - ring) * 0.3f);
            c = scale(c, 1f - smooth(0.4f, 0.6f, (float) Math.abs(Math.sin(around * 7f + ring * 4f))) * 0.15f); // crypts
            c = mix(c, rgb3(52, 54, 58), smooth(0.38f, 0.47f, ang));
        } else {
            c = mix(rgb3(178, 158, 112), rgb3(214, 198, 158), fbm(u, v, 8, 101, 3));
            float edge = smooth(0.6f, 1.7f, ang);
            float vein = Math.max(smooth(0.972f, 0.992f, vein(u, v, 6, 102)), smooth(0.982f, 0.996f, vein(u, v, 14, 103)) * 0.7f);
            c = mix(c, rgb3(160, 22, 26), vein * (0.35f + 0.65f * edge));
            c = mix(c, rgb3(130, 46, 40), edge * 0.5f);
        }
        return rgb(c);
    }

    private static int eyeGlow(float u, float v) {
        float ang = eyeAngle(u, v);
        if (ang < 0.16f) return argb(255, 255, 252, 235);
        if (ang < 0.47f) {
            int g = (int) (110 * (1f - smooth(0.16f, 0.47f, ang)) + 120);
            return argb(255, g, g, g - 6);
        }
        return 0;
    }

    // =================================================================== noise (all tileable)

    private static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static float hashF(int n, int seed) { return hash(n, n * 7 + 3, seed); }

    private static int cellId(float u, float v, int freq) {
        return Math.floorMod((int) Math.floor(u * freq), freq) * 977 + Math.floorMod((int) Math.floor(v * freq), freq);
    }

    private static float noise(float x, float y, int px, int py, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        int x0 = Math.floorMod(xi, px), x1 = Math.floorMod(xi + 1, px);
        int y0 = Math.floorMod(yi, py), y1 = Math.floorMod(yi + 1, py);
        float a = hash(x0, y0, seed), b = hash(x1, y0, seed), c = hash(x0, y1, seed), d = hash(x1, y1, seed);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    /** Tileable fbm; {@code freq} cells across the tile. */
    private static float fbm(float u, float v, int freq, int seed, int octaves) {
        return fbm(u, v, freq, freq, seed, octaves);
    }

    private static float fbm(float u, float v, int fu, int fv, int seed, int octaves) {
        float sum = 0, amp = 0.5f, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise(u * fu, v * fv, fu, fv, seed + o * 101);
            norm += amp;
            fu *= 2;
            fv *= 2;
            amp *= 0.5f;
        }
        return sum / norm;
    }

    /**
     * Fur: short strokes scattered at random, mostly running along v, in three overlapping layers.
     * Returns {coverage 0..1, brightness 0..1 of the top strand}.
     */
    private static float[] fur(float u, float v, int seed) {
        float cover = 0f, bright = 0.5f;
        for (int layer = 0; layer < 3; layer++) {
            int fu = 34 + layer * 14, fv = 14 + layer * 6;
            float x = u * fu, y = v * fv;
            int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int cx = xi + dx, cy = yi + dy;
                    int mx = Math.floorMod(cx, fu), my = Math.floorMod(cy, fv);
                    for (int k = 0; k < 2; k++) {
                        int sd = seed + layer * 31 + k * 7;
                        float px = cx + hash(mx, my, sd), py = cy + hash(mx, my, sd + 1);
                        float ang = (hash(mx, my, sd + 2) - 0.5f) * 0.9f;          // mostly along v
                        float len = 0.6f + hash(mx, my, sd + 3) * 0.9f;
                        float ddx = (float) Math.sin(ang) * len * fu / fv * 0.35f, ddy = (float) Math.cos(ang) * len;
                        // distance from (x, y) to segment p..p+d, measured in u-units
                        float qx = (x - px) / fu, qy = (y - py) / fv, sx = ddx / fu, sy = ddy / fv;
                        float t = Math.max(0f, Math.min(1f, (qx * sx + qy * sy) / (sx * sx + sy * sy)));
                        float ex = (qx - sx * t) * 512f, ey = (qy - sy * t) * 512f;
                        float dist = (float) Math.sqrt(ex * ex + ey * ey);
                        float a = smooth(1.3f, 0.3f, dist) * (1f - t * 0.6f);
                        if (a > cover) {
                            cover = a;
                            bright = hash(mx, my, sd + 4);
                        }
                    }
                }
            }
        }
        return new float[]{cover, bright};
    }

    /** Thin, branching, warped lines that wander and fade: veins, cracks, scars, fascia. 1 on the line. */
    private static float vein(float u, float v, int freq, int seed) {
        float wu = u + (fbm(u, v, 3, seed + 500, 3) - 0.5f) * 0.35f;
        float wv = v + (fbm(u, v, 3, seed + 600, 3) - 0.5f) * 0.35f;
        float a = 1f - Math.abs(2f * fbm(wu, wv, freq, seed, 3) - 1f);
        float b = 1f - Math.abs(2f * fbm(wu, wv, freq * 2, seed + 7, 2) - 1f);
        return Math.max(a, b * 0.985f);
    }

    private static float ridged(float u, float v, int freq, int seed, int octaves) {
        return 1f - Math.abs(2f * fbm(u, v, freq, seed, octaves) - 1f);
    }

    private static float ridged(float u, float v, int fu, int fv, int seed, int octaves) {
        return 1f - Math.abs(2f * fbm(u, v, fu, fv, seed, octaves) - 1f);
    }

    /** Tileable cellular noise: {nearest, second-nearest} feature distance (in cells). */
    private static float[] worley(float u, float v, int freq, int seed) {
        float x = u * freq, y = v * freq;
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float f1 = 9f, f2 = 9f;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cx = xi + dx, cy = yi + dy;
                int mx = Math.floorMod(cx, freq), my = Math.floorMod(cy, freq);
                float fx = cx + hash(mx, my, seed), fy = cy + hash(mx, my, seed + 1);
                float d = (float) Math.sqrt((fx - x) * (fx - x) + (fy - y) * (fy - y));
                if (d < f1) { f2 = f1; f1 = d; } else if (d < f2) f2 = d;
            }
        }
        return new float[]{f1, f2};
    }

    private static float smooth(float e0, float e1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    private static float[] rgb3(int r, int g, int b) { return new float[]{r, g, b}; }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static float[] scale(float[] c, float s) { return new float[]{c[0] * s, c[1] * s, c[2] * s}; }

    private static int rgb(float[] c) { return argb(255, (int) c[0], (int) c[1], (int) c[2]); }

    private static int argb(int a, int r, int g, int b) {
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
