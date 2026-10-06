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
    public void tick() {
        super.tick();

        // IA's groundPitch is useful during the takeoff roll, but a heavy airliner
        // should sit level at the gate with engines at idle. This prevents the
        // nose wheel from visually lifting the moment the pilot mounts.
        double vx = getDeltaMovement().x;
        double vz = getDeltaMovement().z;
        double groundSpeedSq = vx * vx + vz * vz;
        if (onGround() && getEngineTarget() < 0.05F && groundSpeedSq < 0.0025D) {
            setXRot(0.0F);
        }
    }

    @Override
    public Item asItem() {
        return Boeing747Addon.BOEING_747_400_ITEM;
    }

    @Override
    public double getZoom() {
        return 12.0;
    }
}
