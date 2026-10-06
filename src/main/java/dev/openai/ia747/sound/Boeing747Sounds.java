package dev.openai.ia747.sound;

import dev.openai.ia747.Boeing747Addon;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class Boeing747Sounds {
    public static final SoundEvent JET_START = register("jet_start");
    public static final SoundEvent JET_STOP = register("jet_stop");
    public static final SoundEvent JET_IDLE = register("jet_idle");
    public static final SoundEvent JET_THRUST = register("jet_thrust");
    public static final SoundEvent JET_INSIDE = register("jet_inside");
    public static final SoundEvent JET_DISTANT = register("jet_distant");
    public static final SoundEvent JET_WHINE = register("jet_whine");
    public static final SoundEvent JET_SILENT = register("jet_silent");

    private Boeing747Sounds() {}

    public static void init() {
    }

    private static SoundEvent register(String name) {
        ResourceLocation id = Boeing747Addon.id(name);
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)
        );
    }
}
