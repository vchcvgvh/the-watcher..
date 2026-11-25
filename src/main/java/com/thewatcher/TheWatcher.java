package com.thewatcher;

import com.thewatcher.entity.ModEntities;
import com.thewatcher.horror.HorrorDirector;
import com.thewatcher.horror.WatcherCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheWatcher implements ModInitializer {
    public static final String MOD_ID = "thewatcher";
    public static final Logger LOGGER = LoggerFactory.getLogger("The Watcher");

    /** /gamerule watcherEnabled false  -> turns every horror event off. */
    public static final GameRules.Key<GameRules.BooleanRule> ENABLED =
            GameRuleRegistry.register("watcherEnabled", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    /** /gamerule watcherGriefing false -> it can no longer put out torches, open doors or touch your hotbar. */
    public static final GameRules.Key<GameRules.BooleanRule> GRIEFING =
            GameRuleRegistry.register("watcherGriefing", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    /** /gamerule watcherGore false -> no blood particles (drips, sprays). */
    public static final GameRules.Key<GameRules.BooleanRule> GORE =
            GameRuleRegistry.register("watcherGore", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(true));

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C()
                .register(com.thewatcher.net.ScarePayload.ID, com.thewatcher.net.ScarePayload.CODEC);
        ModSounds.register();
        ModEntities.register();
        HorrorDirector.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                WatcherCommand.register(dispatcher));
        LOGGER.info("It's watching.");
    }
}
