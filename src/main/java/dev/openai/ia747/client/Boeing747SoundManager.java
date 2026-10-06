package dev.openai.ia747.client;

import dev.openai.ia747.entity.Boeing747Entity;
import dev.openai.ia747.sound.Boeing747Sounds;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Boeing747SoundManager {
    public static final Boeing747SoundManager INSTANCE = new Boeing747SoundManager();

    private final Map<Integer, JetSet> sets = new HashMap<>();

    private Boeing747SoundManager() {}

    public void tick(Minecraft client) {
        if (client.level == null) {
            stopAll();
            return;
        }

        Player listener = client.player;

        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Boeing747Entity aircraft)) {
                continue;
            }

            JetSet set = sets.computeIfAbsent(aircraft.getId(), id -> new JetSet(aircraft));
            boolean interior = listener != null
                    && aircraft.hasPassenger(listener)
                    && client.options.getCameraType() == CameraType.FIRST_PERSON;
            double distance = listener == null ? 9999.0 : listener.distanceTo(aircraft);
            set.update(interior, distance);
        }

        Iterator<Map.Entry<Integer, JetSet>> it = sets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, JetSet> entry = it.next();
            if (client.level.getEntity(entry.getKey()) instanceof Boeing747Entity) {
                continue;
            }
            entry.getValue().stopAll();
            it.remove();
        }
    }

    public void stopAll() {
        sets.values().forEach(JetSet::stopAll);
        sets.clear();
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static final class JetSet {
        private final Boeing747Entity aircraft;
        private JetLoop idle;
        private JetLoop thrust;
        private JetLoop inside;
        private JetLoop distant;
        private float previousTarget;

        private JetSet(Boeing747Entity aircraft) {
            this.aircraft = aircraft;
        }

        private void update(boolean interiorView, double distance) {
            float power = aircraft.getEnginePower();
            float target = aircraft.getEngineTarget();
            boolean running = power > 0.015F || target > 0.015F;

            if (previousTarget > 0.06F && target <= 0.02F) {
                aircraft.level().playLocalSound(
                        aircraft.getX(), aircraft.getY() + 1.5, aircraft.getZ(),
                        Boeing747Sounds.JET_STOP, SoundSource.NEUTRAL,
                        1.2F, 1.0F, false
                );
            }
            previousTarget = target;

            if (!running) {
                stopLoops();
                return;
            }

            float nearScale = interiorView ? 0.0F : clamp((float) (1.0 - distance / 115.0));
            float farIn = clamp((float) ((distance - 32.0) / 120.0));
            float farOut = clamp((float) (1.0 - Math.max(0.0, distance - 32.0) / 430.0));
            float farScale = interiorView ? 0.0F : farIn * farOut;

            float idleVol = nearScale * (0.62F - power * 0.28F);
            float thrustVol = nearScale * clamp((power - 0.14F) / 0.86F) * 0.95F;
            float insideVol = interiorView ? 0.50F + power * 0.38F : 0.0F;
            float distantVol = farScale * (0.34F + power * 0.52F);

            idle = updateLoop(idle, Boeing747Sounds.JET_IDLE, idleVol, 0.78F + power * 0.25F);
            thrust = updateLoop(thrust, Boeing747Sounds.JET_THRUST, thrustVol, 0.72F + power * 0.36F);
            inside = updateLoop(inside, Boeing747Sounds.JET_INSIDE, insideVol, 0.82F + power * 0.20F);
            distant = updateLoop(distant, Boeing747Sounds.JET_DISTANT, distantVol, 0.72F + power * 0.16F);
        }

        private JetLoop updateLoop(JetLoop loop, SoundEvent event, float volume, float pitch) {
            if (volume <= 0.008F) {
                if (loop != null) {
                    loop.finish();
                }
                return null;
            }

            if (loop == null || loop.isStopped()) {
                loop = new JetLoop(event, aircraft);
                Minecraft.getInstance().getSoundManager().play(loop);
            }

            loop.setMix(volume, pitch);
            return loop;
        }

        private void stopLoops() {
            if (idle != null) idle.finish();
            if (thrust != null) thrust.finish();
            if (inside != null) inside.finish();
            if (distant != null) distant.finish();
            idle = thrust = inside = distant = null;
        }

        private void stopAll() {
            stopLoops();
        }
    }

    private static final class JetLoop extends AbstractTickableSoundInstance {
        private final Boeing747Entity aircraft;
        private boolean finished;

        private JetLoop(SoundEvent event, Boeing747Entity aircraft) {
            super(event, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
            this.aircraft = aircraft;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.pitch = 1.0F;
            this.attenuation = Attenuation.NONE;
            sync();
        }

        @Override
        public void tick() {
            if (finished || aircraft.isRemoved()) {
                stop();
                return;
            }
            sync();
        }

        @Override
        public boolean isStopped() {
            return finished || aircraft.isRemoved() || super.isStopped();
        }

        private void sync() {
            this.x = (float) aircraft.getX();
            this.y = (float) (aircraft.getY() + 1.4);
            this.z = (float) aircraft.getZ();
        }

        private void setMix(float volume, float pitch) {
            this.volume = clamp(volume);
            this.pitch = Math.max(0.05F, pitch);
        }

        private void finish() {
            finished = true;
        }
    }
}
