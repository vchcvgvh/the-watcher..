package com.thewatcher.horror;

import com.thewatcher.TheWatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Decides when something happens to whom. Runs once a second per player:
 * raises their dread, and every so often rolls a horror event appropriate to their stage.
 */
public final class HorrorDirector {
    private HorrorDirector() {}

    /** Seconds between events at each stage (min, max). Things get busier as dread rises. */
    private static final int[][] COOLDOWN = {
            {0, 0},       // stage 0: nothing yet
            {55, 110},    // stage 1
            {30, 75},
            {18, 45},
            {10, 30},     // stage 4
    };

    private static final Text[] STAGE_MESSAGES = {
            Text.empty(),
            Text.literal("You feel watched.").formatted(Formatting.GRAY, Formatting.ITALIC),
            Text.literal("Something is wrong here.").formatted(Formatting.GRAY, Formatting.ITALIC),
            Text.literal("It knows where you sleep.").formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
            Text.literal("It's done waiting.").formatted(Formatting.DARK_RED, Formatting.ITALIC),
    };

    private static final Map<UUID, Integer> cooldowns = new HashMap<>();
    private static final Map<UUID, Integer> lastStage = new HashMap<>();
    private static final Map<UUID, Integer> sleepTicks = new HashMap<>();
    private static final List<Scheduled> scheduled = new ArrayList<>();

    private record Scheduled(int[] ticksLeft, Runnable action) {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HorrorDirector::onTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            cooldowns.clear();
            lastStage.clear();
            sleepTicks.clear();
            scheduled.clear();
        });
    }

    /** Run {@code action} after {@code ticks} server ticks. */
    public static void later(int ticks, Runnable action) {
        scheduled.add(new Scheduled(new int[]{ticks}, action));
    }

    private static void onTick(MinecraftServer server) {
        // Delayed effects (footsteps, fake "left the game" etc.)
        if (!scheduled.isEmpty()) {
            List<Runnable> due = new ArrayList<>();
            for (Iterator<Scheduled> it = scheduled.iterator(); it.hasNext(); ) {
                Scheduled s = it.next();
                if (--s.ticksLeft()[0] <= 0) {
                    due.add(s.action());
                    it.remove();
                }
            }
            for (Runnable r : due) {
                try {
                    r.run();
                } catch (Exception e) {
                    TheWatcher.LOGGER.warn("Scheduled horror effect failed", e);
                }
            }
        }

        // The other places: build them around you, check for exits, fill them with things.
        if (server.getTicks() % 10 == 0) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                Dimensions.tickPlayer(player);
            }
        }

        if (server.getTicks() % 20 != 0) return;

        DreadState dread = DreadState.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            if (!world.getGameRules().getBoolean(TheWatcher.ENABLED)) continue;
            if (player.isCreative() || player.isSpectator()) continue;

            UUID id = player.getUuid();
            dread.addDread(id, dreadGain(world, player));
            int stage = dread.getStage(id);

            // Announce a new stage (quietly).
            Integer previous = lastStage.put(id, stage);
            if (previous == null || previous != stage) Scares.send(player, com.thewatcher.net.ScarePayload.STAGE, stage);
            if (previous != null && stage > previous) {
                Fx.whisper(player, STAGE_MESSAGES[stage]);
                cooldowns.put(id, 20);
                continue;
            }

            if (stage == 0) continue;

            // Sleeping isn't safe any more. Sometimes you don't wake up where you lay down.
            if (player.isSleeping() && stage >= 2 && !Dimensions.isHorrorDim(world)) {
                int slept = sleepTicks.merge(id, 1, Integer::sum);
                if (slept == 3 && player.getRandom().nextFloat() < (stage >= 4 ? 0.6f : 0.35f)) {
                    com.thewatcher.gen.Realm[] dreams = {com.thewatcher.gen.Realm.HOLLOW, com.thewatcher.gen.Realm.FLESH,
                            com.thewatcher.gen.Realm.STILL_WATER, com.thewatcher.gen.Realm.HALLWAYS};
                    Dimensions.send(player, dreams[player.getRandom().nextInt(dreams.length)]);
                    continue;
                }
            } else {
                sleepTicks.remove(id);
            }

            int cd = cooldowns.getOrDefault(id, randomCooldown(player.getRandom(), stage) / 2);
            if (--cd > 0) {
                cooldowns.put(id, cd);
                continue;
            }
            boolean happened = runRandomEvent(player, stage);
            cooldowns.put(id, happened ? randomCooldown(player.getRandom(), stage) : 10);
        }
    }

    private static int dreadGain(ServerWorld world, ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        int gain = 1;
        if (world.getLightLevel(pos) < 5) gain++;                       // standing in the dark
        if (pos.getY() < 40 && !world.isSkyVisible(pos)) gain++;         // deep underground
        if (!world.isDay()) gain++;                                      // night
        return gain;
    }

    private static int randomCooldown(Random random, int stage) {
        int[] range = COOLDOWN[stage];
        return range[0] + random.nextInt(range[1] - range[0] + 1);
    }

    /** Picks a weighted-random event the player qualifies for and tries it (a few attempts). */
    public static boolean runRandomEvent(ServerPlayerEntity player, int stage) {
        boolean griefing = player.getServerWorld().getGameRules().getBoolean(TheWatcher.GRIEFING);
        List<HorrorEvent> pool = new ArrayList<>();
        int total = 0;
        for (HorrorEvent e : HorrorEvent.values()) {
            if (e.minStage <= stage && (griefing || !e.griefing)) {
                pool.add(e);
                total += e.weight;
            }
        }
        if (pool.isEmpty()) return false;

        Random random = player.getRandom();
        for (int attempt = 0; attempt < 4; attempt++) {
            int roll = random.nextInt(total);
            for (HorrorEvent e : pool) {
                roll -= e.weight;
                if (roll < 0) {
                    if (e.run(player, stage)) return true;
                    break;
                }
            }
        }
        return false;
    }
}
