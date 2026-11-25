package com.thewatcher.entity;

import com.thewatcher.TheWatcher;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<WatcherEntity> WATCHER = Registry.register(Registries.ENTITY_TYPE, TheWatcher.id("watcher"),
            EntityType.Builder.create(WatcherEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7f, 2.9f).makeFireImmune().maxTrackingRange(16).build("watcher"));

    public static final EntityType<CrawlerEntity> CRAWLER = Registry.register(Registries.ENTITY_TYPE, TheWatcher.id("crawler"),
            EntityType.Builder.create(CrawlerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4f, 1.1f).maxTrackingRange(10).build("crawler"));

    public static final EntityType<FacelessEntity> FACELESS = Registry.register(Registries.ENTITY_TYPE, TheWatcher.id("faceless"),
            EntityType.Builder.create(FacelessEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7f, 2.9f).makeFireImmune().maxTrackingRange(12).build("faceless"));

    public static final EntityType<HoundEntity> HOUND = Registry.register(Registries.ENTITY_TYPE, TheWatcher.id("hollow_hound"),
            EntityType.Builder.create(HoundEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.9f, 1.1f).maxTrackingRange(10).build("hollow_hound"));

    public static final Item WATCHER_SPAWN_EGG = egg("watcher_spawn_egg", WATCHER, 0x0B0B0D, 0xEDEDED);
    public static final Item CRAWLER_SPAWN_EGG = egg("crawler_spawn_egg", CRAWLER, 0x3A3436, 0x6B0A0E);
    public static final Item FACELESS_SPAWN_EGG = egg("faceless_spawn_egg", FACELESS, 0x111111, 0xD8D4CE);
    public static final Item HOUND_SPAWN_EGG = egg("hollow_hound_spawn_egg", HOUND, 0x4A4446, 0xC8B890);

    private static Item egg(String name, EntityType<? extends MobEntity> type, int a, int b) {
        return Registry.register(Registries.ITEM, TheWatcher.id(name), new SpawnEggItem(type, a, b, new Item.Settings()));
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(WATCHER, WatcherEntity.createWatcherAttributes());
        FabricDefaultAttributeRegistry.register(CRAWLER, CrawlerEntity.createCrawlerAttributes());
        FabricDefaultAttributeRegistry.register(FACELESS, FacelessEntity.createFacelessAttributes());
        FabricDefaultAttributeRegistry.register(HOUND, HoundEntity.createHoundAttributes());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(WATCHER_SPAWN_EGG);
            entries.add(CRAWLER_SPAWN_EGG);
            entries.add(FACELESS_SPAWN_EGG);
            entries.add(HOUND_SPAWN_EGG);
        });
    }
}
