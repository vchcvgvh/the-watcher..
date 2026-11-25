package com.thewatcher.horror;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * /watcher status                       - your dread and stage
 * /watcher stage <players> <0-4>        - jump straight to a stage
 * /watcher trigger <event>              - fire an event on yourself right now (for testing)
 * /watcher send <hallways|hollow|flesh|still_water> - get taken
 * /watcher escape                       - come back
 */
public final class WatcherCommand {
    private WatcherCommand() {}

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> sendCommand() {
        var send = literal("send");
        for (com.thewatcher.gen.Realm r : com.thewatcher.gen.Realm.values()) {
            send.then(literal(r.id).executes(ctx -> sendTo(ctx.getSource(), Dimensions.key(r))));
        }
        return send;
    }

    private static int sendTo(ServerCommandSource src, net.minecraft.registry.RegistryKey<net.minecraft.world.World> key)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = src.getPlayerOrThrow();
        if (!Dimensions.send(player, key)) {
            src.sendError(Text.literal("Couldn't send you there (already in one?)."));
            return 0;
        }
        return 1;
    }

    /** /watcher jumpscare <watcher|faceless|crawler|hound>: see one creature's jumpscare right now. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> jumpscareCommand() {
        var js = literal("jumpscare");
        String[] names = {"watcher", "faceless", "crawler", "hound"};
        for (int i = 0; i < names.length; i++) {
            final int v = i;
            js.then(literal(names[i]).executes(ctx -> {
                Scares.send(ctx.getSource().getPlayerOrThrow(), com.thewatcher.net.ScarePayload.JUMPSCARE, v);
                return 1;
            }));
        }
        return js;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("watcher")
                .requires(src -> src.hasPermissionLevel(2))
                .then(literal("status").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    DreadState state = DreadState.get(ctx.getSource().getServer());
                    int dread = state.getDread(player.getUuid());
                    int stage = DreadState.stageFor(dread);
                    ctx.getSource().sendFeedback(() -> Text.literal(
                            "Dread: " + dread + "  |  Stage: " + stage + "/4"), false);
                    return stage;
                }))
                .then(sendCommand())
                .then(jumpscareCommand())
                .then(literal("escape").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    if (!Dimensions.isHorrorDim(player.getWorld())) {
                        ctx.getSource().sendError(Text.literal("You're not anywhere you need to escape from. Yet."));
                        return 0;
                    }
                    Dimensions.returnHome(player, true);
                    return 1;
                }))
                .then(literal("stage")
                        .then(argument("targets", EntityArgumentType.players())
                                .then(argument("stage", IntegerArgumentType.integer(0, 4))
                                        .executes(ctx -> {
                                            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(ctx, "targets");
                                            int stage = IntegerArgumentType.getInteger(ctx, "stage");
                                            DreadState state = DreadState.get(ctx.getSource().getServer());
                                            for (ServerPlayerEntity p : players) {
                                                state.setDread(p.getUuid(), DreadState.STAGE_THRESHOLDS[stage]);
                                            }
                                            ctx.getSource().sendFeedback(() -> Text.literal(
                                                    "Set " + players.size() + " player(s) to stage " + stage), true);
                                            return players.size();
                                        }))))
                .then(literal("trigger")
                        .then(argument("event", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                        Arrays.stream(HorrorEvent.values()).map(e -> e.name().toLowerCase(Locale.ROOT)), builder))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    String name = StringArgumentType.getString(ctx, "event").toUpperCase(Locale.ROOT);
                                    HorrorEvent event;
                                    try {
                                        event = HorrorEvent.valueOf(name);
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendError(Text.literal("Unknown event: " + name.toLowerCase(Locale.ROOT)));
                                        return 0;
                                    }
                                    int stage = Math.max(event.minStage,
                                            DreadState.get(ctx.getSource().getServer()).getStage(player.getUuid()));
                                    boolean ok = event.run(player, stage);
                                    if (!ok) {
                                        ctx.getSource().sendError(Text.literal(
                                                "Couldn't run that here (no torches/doors nearby, no space, daytime, or one is already out?)"));
                                    }
                                    return ok ? 1 : 0;
                                }))));
    }
}
