package dev.openai.ia747;

import dev.openai.ia747.entity.Boeing747Entity;
import dev.openai.ia747.sound.Boeing747Sounds;
import immersive_aircraft.item.AircraftItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class Boeing747Addon implements ModInitializer {
    public static final String MOD_ID = "ia747";

    public static final EntityType<Boeing747Entity> BOEING_747_400_ENTITY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("boeing_747_400"),
            EntityType.Builder.<Boeing747Entity>of(Boeing747Entity::new, MobCategory.MISC)
                    .sized(3.2F, 3.6F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(id("boeing_747_400").toString())
    );

    public static final Item BOEING_747_400_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            id("boeing_747_400"),
            new AircraftItem(
                    new Item.Properties().stacksTo(1),
                    level -> new Boeing747Entity(BOEING_747_400_ENTITY, level)
            )
    );

    @Override
    public void onInitialize() {
        Boeing747Sounds.init();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(BOEING_747_400_ITEM));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
