package dev.openai.ia747.entity;

import dev.openai.ia747.Boeing747Addon;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.AirplaneEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class Boeing747Entity extends AirplaneEntity {
    public Boeing747Entity(EntityType<? extends AircraftEntity> entityType, Level level) {
        super(entityType, level, true);
    }

    @Override
    public Item asItem() {
        return Boeing747Addon.BOEING_747_400_ITEM;
    }

    @Override
    public double getZoom() {
        return 11.0;
    }
}
