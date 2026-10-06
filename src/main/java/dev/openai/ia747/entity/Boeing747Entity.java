package dev.openai.ia747.entity;

import dev.openai.ia747.Boeing747Addon;
import dev.openai.ia747.sound.Boeing747Sounds;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.AirplaneEntity;
import net.minecraft.sounds.SoundEvent;
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

        // A parked 747 sits level. IA's groundPitch is only allowed to matter
        // after the engines are actually spooling and the aircraft is rolling.
        double vx = getDeltaMovement().x;
        double vz = getDeltaMovement().z;
        double groundSpeedSq = vx * vx + vz * vz;
        if (onGround() && getEngineTarget() < 0.05F && groundSpeedSq < 0.0025D) {
            setXRot(0.0F);
        }
    }

    @Override
    protected SoundEvent getEngineStartSound() {
        return Boeing747Sounds.JET_START;
    }

    @Override
    protected SoundEvent getEngineSound() {
        // The normal IA propeller loop is replaced by Boeing747SoundManager.
        return Boeing747Sounds.JET_SILENT;
    }

    @Override
    protected float getEngineVolume() {
        return 0.0F;
    }

    @Override
    protected float getEngineReactionSpeed() {
        return 70.0F;
    }

    @Override
    public Item asItem() {
        return Boeing747Addon.BOEING_747_400_ITEM;
    }

    @Override
    public double getZoom() {
        return 13.0;
    }
}
