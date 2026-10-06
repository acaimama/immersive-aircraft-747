package dev.openai.ia747.client;

import dev.openai.ia747.Boeing747Addon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class Boeing747Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                Boeing747Addon.BOEING_747_400_ENTITY,
                Boeing747Renderer::new
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> Boeing747SoundManager.INSTANCE.tick(client)
        );
    }
}
