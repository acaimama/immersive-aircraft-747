package dev.openai.ia747.entity;

import dev.openai.ia747.Boeing747Addon;
import dev.openai.ia747.sound.Boeing747Sounds;
import immersive_aircraft.entity.AircraftEntity;
import immersive_aircraft.entity.AirplaneEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class Boeing747Entity extends AirplaneEntity {
    // 1 block = 1 metre. 13.85 blocks/tick = 997.2 km/h.
    private static final double ABSOLUTE_MAX_BLOCKS_PER_TICK = 13.85;

    public Boeing747Entity(EntityType<? extends AircraftEntity> type, Level level) {
        super(type, level, true);
    }

    @Override
    public void tick() {
        super.tick();

        double vx=getDeltaMovement().x, vz=getDeltaMovement().z;
        double groundSpeedSq=vx*vx+vz*vz;
        if(onGround() && getEngineTarget()<.05F && groundSpeedSq<.0025D) setXRot(0.0F);

        applyJetCruiseEnvelope();
    }

    private void applyJetCruiseEnvelope() {
        if (!isVehicle()) return;
        float target=getEngineTarget();
        float power=getEnginePower();
        if (target<.03F || power<.03F) return;

        Vector3f f=getForwardDirection();
        Vec3 dir=new Vec3(f.x(),f.y(),f.z()).normalize();
        Vec3 velocity=getDeltaMovement();
        double forward=Math.max(0.0,velocity.dot(dir));

        // 88% throttle ~= 923 km/h; 100% ~= 997 km/h.
        double commandedMax=ABSOLUTE_MAX_BLOCKS_PER_TICK*(0.35+0.65*target);
        if (onGround()) commandedMax=Math.min(commandedMax,4.15*target); // rotation region ~299 km/h

        if (forward<commandedMax) {
            double accel=(onGround()?0.018:0.012)*power*power;
            double fade=Math.max(0.08,1.0-forward/Math.max(.01,commandedMax));
            setDeltaMovement(velocity.add(dir.scale(accel*fade)));
        } else if (forward>commandedMax*1.015) {
            double excess=forward-commandedMax;
            setDeltaMovement(velocity.add(dir.scale(-Math.min(.035,excess*.025))));
        }
    }

    @Override protected SoundEvent getEngineStartSound(){ return Boeing747Sounds.JET_START; }
    @Override protected SoundEvent getEngineSound(){ return Boeing747Sounds.JET_SILENT; }
    @Override protected float getEngineVolume(){ return 0.0F; }
    @Override protected float getEngineReactionSpeed(){ return 85.0F; }
    @Override public Item asItem(){ return Boeing747Addon.BOEING_747_400_ITEM; }
    @Override public double getZoom(){ return 38.0; }
}
