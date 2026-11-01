package de.buddydog.client;

import de.buddydog.BuddyDogMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BuddyDogClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BuddyDogMod.PUPPY, PuppyRenderer::new);
    }
}
