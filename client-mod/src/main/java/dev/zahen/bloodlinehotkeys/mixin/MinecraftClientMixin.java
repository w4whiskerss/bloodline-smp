package dev.zahen.bloodlinehotkeys.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Unique
    private boolean bloodline$primaryHeld;
    @Unique
    private boolean bloodline$secondaryHeld;
    @Unique
    private boolean bloodline$specialHeld;
    @Unique
    private boolean bloodline$announced;

    @Inject(method = "tick", at = @At("TAIL"))
    private void bloodline$handleHotkeys(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null || client.getNetworkHandler() == null || client.currentScreen != null) {
            bloodline$primaryHeld = false;
            bloodline$secondaryHeld = false;
            bloodline$specialHeld = false;
            if (client.player == null || client.getNetworkHandler() == null) {
                bloodline$announced = false;
            }
            return;
        }

        if (!bloodline$announced) {
            client.getNetworkHandler().sendChatCommand("bloodlinemod");
            bloodline$announced = true;
        }

        boolean shift = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean primary = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_V);
        boolean secondary = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_B);
        boolean special = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_N);

        if (shift && primary && !bloodline$primaryHeld) {
            client.getNetworkHandler().sendChatCommand("ability1");
        }
        if (shift && secondary && !bloodline$secondaryHeld) {
            client.getNetworkHandler().sendChatCommand("ability2");
        }
        if (shift && special && !bloodline$specialHeld) {
            client.getNetworkHandler().sendChatCommand("ability3");
        }

        bloodline$primaryHeld = shift && primary;
        bloodline$secondaryHeld = shift && secondary;
        bloodline$specialHeld = shift && special;
    }
}
