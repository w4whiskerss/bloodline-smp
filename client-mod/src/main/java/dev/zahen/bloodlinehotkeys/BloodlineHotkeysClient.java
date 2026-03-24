package dev.zahen.bloodlinehotkeys;

import net.fabricmc.api.ClientModInitializer;

public final class BloodlineHotkeysClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Key polling is handled from the MinecraftClient tick mixin.
    }
}
