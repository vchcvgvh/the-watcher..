package com.thewatcher.horror;

import com.thewatcher.entity.ModEntities;
import com.thewatcher.entity.WatcherEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

import java.util.List;

/**
 * Everything the Watcher can do to you.
 * minStage = earliest dread stage it can happen at; weight = how often it's picked;
 * griefing = touches your world/inventory (turned off by /gamerule watcherGriefing false).
 */
public enum HorrorEvent {
    // --- Psychological: sounds and messages, nothing actually there ---
    DISTANT_CAVE(1, 10, false),
    FOOTSTEPS(1, 10, false),
    TORCH_OUT(1, 6, true),
    STALK(1, 14, false),
    MINING(2, 5, false),
    CHEST(2, 4, false),
    DOOR(2, 6, true),
    FAKE_CHAT(2, 5, false),
    FAKE_JOIN(2, 3, false),
    WHISPER(2, 5, false),
    HOTBAR(2, 3, true),
    // --- Escalation ---
    CRAWLERS(2, 6, false),
    HEARTBEAT(3, 5, false),
    JUMPSCARE(2, 10, false),
    FACELESS(3, 5, false),
    TAKEN(3, 3, false),
    HUNT(3, 7, false),
    HOUNDS(4, 4, false),
    // screen scares (client side): text that pops up, faces that fill the screen, notes on your PC
    GLITCH_TEXT(1, 12, false),
    TYPED(1, 9, false),
    SUBLIMINAL(2, 8, false),
    SCREAMER(2, 10, false),
    PC_NOTE(2, 4, false),
    FORCED_LOOK(2, 7, false),
    FAKE_DEATH(3, 4, false);

    public final int minStage;
    public final int weight;
    public final boolean griefing;

    HorrorEvent(int minStage, int weight, boolean griefing) {
        this.minStage = minStage;
        this.weight = weight;
        this.griefing = griefing;
    }

    /** @return true if the event actually happened (e.g. TORCH_OUT fails if there are no torches nearby). */
    public boolean run(ServerPlayerEntity player, int stage) {
        ServerWorld world = player.getServerWorld();
        Random random = player.getRandom();
        return switch (this) {
            case DISTANT_CAVE -> caveNoise(player, random);
            case FOOTSTEPS -> footsteps(world, player);
            case TORCH_OUT -> torchOut(world, player, random);
            case STALK -> stalk(world, player, stage, random);
            case MINING -> mining(player, random);
            case CHEST -> chest(player, random);
            case DOOR -> door(world, player, random);
            case FAKE_CHAT -> fakeChat(player, random);
            case FAKE_JOIN -> fakeJoin(player, random);
            case WHISPER -> whisper(player, random);
            case HOTBAR -> hotbar(player, random);
            case HEARTBEAT -> heartbeat(player);
            case JUMPSCARE -> jumpscare(world, player);
            case HUNT -> stage >= 4 || random.nextInt(3) == 0 ? hunt(world, player, random) : false;
            case CRAWLERS -> crawlers(world, player, random);
            case FACELESS -> faceless(world, player);
            case HOUNDS -> hounds(world, player);
            case TAKEN -> taken(player, random);
            case GLITCH_TEXT -> Scares.glitchText(player, random);
            case TYPED -> Scares.typed(player, random);
            case SUBLIMINAL -> Scares.subliminal(player, random);
            case SCREAMER -> Scares.screamer(player, stage, random);
            case PC_NOTE -> Scares.pcNote(player, random);
            case FORCED_LOOK -> forcedLook(world, player, random);
            case FAKE_DEATH -> {
                Fx.effect(player, StatusEffects.DARKNESS, 100, 0);
                yield Scares.send(player, com.thewatcher.net.ScarePayload.FAKE_DEATH, 0);
            }
        };
    }

    // ------------------------------------------------------------------ sounds

    private static Vec3d randomAround(ServerPlayerEntity p, Random r, double min, double max) {
        Vec3d dir = Fx.rotateY(new Vec3d(1, 0, 0), r.nextDouble() * 360);
        double d = min + r.nextDouble() * (max - min);
        return p.getPos().add(dir.multiply(d)).add(0, r.nextDouble() * 4 - 2, 0);
    }

    private static boolean caveNoise(ServerPlayerEntity p, Random r) {
        if (r.nextInt(3) == 0) Fx.sound(p, com.thewatcher.ModSounds.DISTANT_SCREAM, randomAround(p, r, 10, 16), 1.5f, 0.8f + r.nextFloat() * 0.2f);
        else Fx.sound(p, SoundEvents.AMBIENT_CAVE.value(), randomAround(p, r, 6, 14), 1.0f, 0.5f + r.nextFloat() * 0.3f);
        return true;
    }

    /** Someone walks up behind you... and stops. */
    private static boolean footsteps(ServerWorld world, ServerPlayerEntity p) {
        Vec3d back = Fx.flatLook(p).multiply(-1);
        Vec3d start = p.getPos();
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            final double dist = 11.0 - i * 1.7;
            HorrorDirector.later(i * 9 + 1, () -> {
                if (p.isDisconnected()) return;
                Vec3d at = start.add(back.multiply(dist));
                BlockState ground = world.getBlockState(BlockPos.ofFloored(at).down());
                if (ground.isAir()) ground = Blocks.STONE.getDefaultState();
                Fx.sound(p, ground.getSoundGroup().getStepSound(), at, 0.5f, 0.85f);
            });
        }
        return true;
    }

    private static boolean mining(ServerPlayerEntity p, Random r) {
        Vec3d at = randomAround(p, r, 8, 14).add(0, -4, 0);
        int hits = 3 + r.nextInt(4);
        for (int i = 0; i < hits; i++) {
            HorrorDirector.later(i * 14 + 1, () -> {
                if (!p.isDisconnected()) Fx.sound(p, SoundEvents.BLOCK_STONE_HIT, at, 0.8f, 0.8f);
            });
        }
        HorrorDirector.later(hits * 14 + 4, () -> {
            if (!p.isDisconnected()) Fx.sound(p, SoundEvents.BLOCK_STONE_BREAK, at, 1.0f, 0.8f);
        });
        return true;
    }

    private static boolean chest(ServerPlayerEntity p, Random r) {
        Vec3d at = randomAround(p, r, 4, 9);
        Fx.sound(p, SoundEvents.BLOCK_CHEST_OPEN, at, 0.7f, 0.9f);
        HorrorDirector.later(50 + r.nextInt(40), () -> {
            if (!p.isDisconnected()) Fx.sound(p, SoundEvents.BLOCK_CHEST_CLOSE, at, 0.7f, 0.9f);
        });
        return true;
    }

    private static boolean heartbeat(ServerPlayerEntity p) {
        Fx.effect(p, StatusEffects.DARKNESS, 20 * 9, 0);
        for (int i = 0; i < 7; i++) {
            final float pitch = 0.7f + i * 0.06f;
            HorrorDirector.later(i * 22 + 1, () -> {
                if (!p.isDisconnected()) Fx.sound(p, com.thewatcher.ModSounds.HEARTBEAT, p.getPos(), 1.0f, pitch);
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ messages

    private static final String[] CHAT_LINES = {
            "behind you", "i like your house", "why did you leave the door open",
            "don't turn around", "i can see you", "you're not alone here",
            "it's getting dark", "keep digging", "you look tired", "i was here first",
            "who are you talking to", "wake up",
    };

    private static boolean fakeChat(ServerPlayerEntity p, Random r) {
        String line = CHAT_LINES[r.nextInt(CHAT_LINES.length)];
        // Sometimes the message comes from *you*.
        String sender = r.nextInt(3) == 0 ? p.getName().getString() : "null";
        p.sendMessage(Text.literal("<" + sender + "> " + line), false);
        return true;
    }

    private static boolean fakeJoin(ServerPlayerEntity p, Random r) {
        Text name = Text.literal("null");
        p.sendMessage(Text.translatable("multiplayer.player.joined", name).formatted(Formatting.YELLOW), false);
        HorrorDirector.later(60 + r.nextInt(80), () -> {
            if (!p.isDisconnected())
                p.sendMessage(Text.translatable("multiplayer.player.left", name).formatted(Formatting.YELLOW), false);
        });
        return true;
    }

    private static final String[] WHISPERS = {
            "turn around", "it's behind the wall", "don't go back there", "you heard that too",
            "it remembers", "it's under you", "go to sleep",
    };

    private static boolean whisper(ServerPlayerEntity p, Random r) {
        Fx.whisper(p, Text.literal(WHISPERS[r.nextInt(WHISPERS.length)]).formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        Fx.sound(p, com.thewatcher.ModSounds.WHISPER, p.getPos().add(Fx.flatLook(p).multiply(-1.5)), 0.7f, 0.9f + r.nextFloat() * 0.2f);
        return true;
    }

    // ------------------------------------------------------------------ world changes

    private static boolean isTorch(BlockState s) {
        return s.isOf(Blocks.TORCH) || s.isOf(Blocks.WALL_TORCH) || s.isOf(Blocks.SOUL_TORCH) || s.isOf(Blocks.SOUL_WALL_TORCH);
    }

    /** Puts out 1-3 torches somewhere you're not looking. */
    private static boolean torchOut(ServerWorld world, ServerPlayerEntity p, Random r) {
        return extinguish(world, p, 12, 1 + r.nextInt(3));
    }

    /** Removes up to {@code wanted} torches within {@code radius} that the player isn't looking at. */
    public static boolean extinguish(ServerWorld world, ServerPlayerEntity p, int radius, int wanted) {
        Random r = p.getRandom();
        int removed = 0;
        BlockPos center = p.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int span = radius * 2 + 1;
        for (int i = 0; i < 400 && removed < wanted; i++) {
            pos.set(center.getX() + r.nextInt(span) - radius, center.getY() + r.nextInt(9) - 4, center.getZ() + r.nextInt(span) - radius);
            BlockState state = world.getBlockState(pos);
            if (isTorch(state) && Fx.isBehind(p, Vec3d.ofCenter(pos))) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                Fx.sound(p, SoundEvents.BLOCK_FIRE_EXTINGUISH, Vec3d.ofCenter(pos), 0.3f, 1.4f);
                removed++;
            }
        }
        return removed > 0;
    }

    /** A door near you creaks open. You didn't open it. */
    private static boolean door(ServerWorld world, ServerPlayerEntity p, Random r) {
        BlockPos center = p.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 0; i < 400; i++) {
            pos.set(center.getX() + r.nextInt(29) - 14, center.getY() + r.nextInt(7) - 3, center.getZ() + r.nextInt(29) - 14);
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door && !state.get(DoorBlock.OPEN)
                    && p.getBlockPos().getSquaredDistance(pos) > 9) {
                door.setOpen(null, world, state, pos.toImmutable(), true);
                return true;
            }
        }
        return false;
    }

    /** Two items in your hotbar quietly trade places. */
    private static boolean hotbar(ServerPlayerEntity p, Random r) {
        PlayerInventory inv = p.getInventory();
        int a = r.nextInt(9), b = r.nextInt(9);
        if (a == b || a == inv.selectedSlot || b == inv.selectedSlot) return false;
        ItemStack sa = inv.getStack(a), sb = inv.getStack(b);
        if (sa.isEmpty() && sb.isEmpty()) return false;
        inv.setStack(a, sb);
        inv.setStack(b, sa);
        return true;
    }

    // ------------------------------------------------------------------ the Watcher itself

    private static boolean hasWatcher(ServerWorld world, ServerPlayerEntity p) {
        List<WatcherEntity> existing = world.getEntitiesByType(ModEntities.WATCHER,
                p.getBoundingBox().expand(160), w -> p.getUuid().equals(w.getTargetId()));
        return !existing.isEmpty();
    }

    private static boolean spawn(ServerWorld world, ServerPlayerEntity p, WatcherEntity.Mode mode, BlockPos at) {
        return spawn(world, p, mode, at, DreadState.get(p.server).getStage(p.getUuid()));
    }

    private static boolean spawn(ServerWorld world, ServerPlayerEntity p, WatcherEntity.Mode mode, BlockPos at, int stage) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        WatcherEntity w = ModEntities.WATCHER.create(world);
        if (w == null) return false;
        double dx = p.getX() - (at.getX() + 0.5), dz = p.getZ() - (at.getZ() + 0.5);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        w.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0f);
        w.setHeadYaw(yaw);
        w.setBodyYaw(yaw);
        w.setup(mode, p, stage);
        return world.spawnEntity(w);
    }

    /** It appears in the distance, off to the side, standing perfectly still. Closer each stage. */
    private static boolean stalk(ServerWorld world, ServerPlayerEntity p, int stage, Random r) {
        if (hasWatcher(world, p)) return false;
        int[][] range = {{0, 0}, {44, 60}, {30, 44}, {20, 30}, {14, 22}};
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = (r.nextBoolean() ? 1 : -1) * (55 + r.nextDouble() * 90); // periphery or behind
            Vec3d dir = Fx.rotateY(Fx.flatLook(p), angle);
            double dist = range[stage][0] + r.nextDouble() * (range[stage][1] - range[stage][0]);
            Vec3d at = p.getPos().add(dir.multiply(dist));
            BlockPos spot = Fx.findStandingSpot(world, at.x, at.z, p.getBlockY(), 12);
            if (spot != null && spawn(world, p, WatcherEntity.Mode.STALK, spot)) return true;
        }
        return false;
    }

    /** It stands right behind you. A single footstep. Don't turn around. */
    private static boolean jumpscare(ServerWorld world, ServerPlayerEntity p) {
        if (hasWatcher(world, p)) return false;
        Vec3d behind = p.getPos().subtract(Fx.flatLook(p).multiply(1.6));
        BlockPos spot = Fx.findStandingSpot(world, behind.x, behind.z, p.getBlockY(), 1);
        if (spot == null || !spawn(world, p, WatcherEntity.Mode.SCARE, spot)) return false;
        BlockState ground = world.getBlockState(spot.down());
        Fx.sound(p, ground.getSoundGroup().getStepSound(), behind, 0.6f, 0.8f);
        return true;
    }

    /**
     * It stands right behind you, breathing. Then something takes hold of your head and slowly turns it,
     * a few degrees at a time, until you're looking straight at it.
     */
    private static boolean forcedLook(ServerWorld world, ServerPlayerEntity p, Random r) {
        if (hasWatcher(world, p)) return false;
        Vec3d behind = p.getPos().subtract(Fx.flatLook(p).multiply(2.2));
        BlockPos spot = Fx.findStandingSpot(world, behind.x, behind.z, p.getBlockY(), 1);
        if (spot == null || !spawn(world, p, WatcherEntity.Mode.SCARE, spot)) return false;
        Vec3d at = Vec3d.ofBottomCenter(spot).add(0, 2.4, 0);
        Fx.sound(p, com.thewatcher.ModSounds.BREATHING, at, 0.9f, 0.85f);
        Fx.whisper(p, Text.literal("don't move").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        Fx.effect(p, StatusEffects.SLOWNESS, 70, 4);                          // you can't get away
        final int steps = 16;
        for (int i = 1; i <= steps; i++) {
            final int k = i;
            HorrorDirector.later(40 + i * 2, () -> {
                if (p.isDisconnected()) return;
                Vec3d d = at.subtract(p.getEyePos());
                float targetYaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
                float targetPitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
                float dy = net.minecraft.util.math.MathHelper.wrapDegrees(targetYaw - p.getYaw());
                float f = 1f / (steps - k + 1);                                    // each step covers a share of what's left
                float stutter = k % 4 == 0 ? 0.3f : 1f;                           // it fights you a little
                p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), p.getYaw() + dy * f * stutter,
                        p.getPitch() + (targetPitch - p.getPitch()) * f);
                if (k == 1) Fx.sound(p, com.thewatcher.ModSounds.BONE_CRACK, p.getEyePos(), 0.35f, 1.4f);   // your neck
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ the others

    /** Something skitters in the dark nearby. Then there are two of them. */
    private static boolean crawlers(ServerWorld world, ServerPlayerEntity p, Random r) {
        if (Dimensions.isHorrorDim(world) || world.getLightLevel(p.getBlockPos()) > 7 && world.isDay()) return false;
        if (Spawns.countNear(world, ModEntities.CRAWLER, p, 48) >= 3) return false;
        int n = Spawns.spawnAround(world, ModEntities.CRAWLER, p, 14, 22, true, 1 + r.nextInt(2), p.getBlockY(), 8);
        if (n > 0) Fx.sound(p, com.thewatcher.ModSounds.CRAWLER_SKITTER, p.getPos().subtract(Fx.flatLook(p).multiply(10)), 1.2f, 0.4f);
        return n > 0;
    }

    /** A tall robed figure, somewhere behind you. Don't stop looking at it. */
    private static boolean faceless(ServerWorld world, ServerPlayerEntity p) {
        if (Dimensions.isHorrorDim(world) || world.isDay() && world.isSkyVisible(p.getBlockPos())) return false;
        if (Spawns.countNear(world, ModEntities.FACELESS, p, 64) >= 1) return false;
        boolean ok = Spawns.spawnAround(world, ModEntities.FACELESS, p, 18, 26, true, 1, p.getBlockY(), 8) > 0;
        if (ok) Fx.whisper(p, Text.literal("don't blink").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        return ok;
    }

    /** A howl far away. Then paws. */
    private static boolean hounds(ServerWorld world, ServerPlayerEntity p) {
        if (Dimensions.isHorrorDim(world) || world.isDay()) return false;
        if (Spawns.countNear(world, ModEntities.HOUND, p, 64) >= 2) return false;
        int n = Spawns.spawnAround(world, ModEntities.HOUND, p, 30, 40, false, 3, p.getBlockY(), 12);
        if (n > 0) Fx.sound(p, com.thewatcher.ModSounds.HOUND_HOWL, p.getPos().add(Fx.flatLook(p).multiply(-30)), 3f, 0.9f);
        return n > 0;
    }

    /** You blink, and you're somewhere else. */
    private static boolean taken(ServerPlayerEntity p, Random r) {
        if (Dimensions.isHorrorDim(p.getWorld())) return false;
        com.thewatcher.gen.Realm[] all = com.thewatcher.gen.Realm.values();
        return Dimensions.send(p, all[r.nextInt(all.length)]);
    }

    /** Stage 4, at night, in the overworld: it comes for you. */
    private static boolean hunt(ServerWorld world, ServerPlayerEntity p, Random r) {
        if (world.isDay() || (world.getRegistryKey() != World.OVERWORLD && !Dimensions.isHorrorDim(world))) return false;
        if (hasWatcher(world, p)) return false;
        for (int attempt = 0; attempt < 12; attempt++) {
            Vec3d dir = Fx.rotateY(new Vec3d(1, 0, 0), r.nextDouble() * 360);
            Vec3d at = p.getPos().add(dir.multiply(26 + r.nextDouble() * 8));
            BlockPos spot = Fx.findStandingSpot(world, at.x, at.z, p.getBlockY(), 12);
            if (spot != null && spawn(world, p, WatcherEntity.Mode.HUNT, spot)) {
                Fx.sound(p, com.thewatcher.ModSounds.DISTANT_SCREAM, Vec3d.ofCenter(spot), 3.0f, 0.9f);
                Fx.whisper(p, Text.literal("run").formatted(Formatting.DARK_RED, Formatting.ITALIC));
                return true;
            }
        }
        return false;
    }
}
