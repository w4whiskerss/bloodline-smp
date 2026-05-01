package dev.zahen.bloodlinehotkeys;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class BloodlineHotkeysClient implements ClientModInitializer {

    private static final Identifier CHANNEL = Identifier.of("bloodline", "main");
    private static final String CATEGORY = "category.bloodline";

    private final HudState hudState = new HudState();
    private KeyBinding primaryKey;
    private KeyBinding secondaryKey;
    private KeyBinding specialKey;
    private KeyBinding fourthKey;
    private KeyBinding fifthKey;
    private boolean handshakeConfirmed;
    private long lastHelloAt;

    @Override
    public void onInitializeClient() {
        primaryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.primary", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyBinding.Category.MISC));
        secondaryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.secondary", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, KeyBinding.Category.MISC));
        specialKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.special", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, KeyBinding.Category.MISC));
        fourthKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.fourth", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyBinding.Category.MISC));
        fifthKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.fifth", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyBinding.Category.MISC));

        PayloadTypeRegistry.playC2S().register(BloodlinePayload.ID, BloodlinePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BloodlinePayload.ID, BloodlinePayload.CODEC);
        ClientTickEvents.END_CLIENT_TICK.register(this::tickClient);
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> renderHud(drawContext));
        ClientPlayNetworking.registerGlobalReceiver(BloodlinePayload.ID, (payload, context) -> handlePayload(payload.payload()));
    }

    private void tickClient(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            handshakeConfirmed = false;
            lastHelloAt = 0L;
            hudState.reset();
            return;
        }
        if (!handshakeConfirmed && shouldSendHello()) {
            sendOpcode("HELLO");
            lastHelloAt = System.currentTimeMillis();
        }

        while (primaryKey.wasPressed()) {
            sendAbility("PRIMARY");
        }
        while (secondaryKey.wasPressed()) {
            sendAbility("SECONDARY");
        }
        while (specialKey.wasPressed()) {
            sendAbility("SPECIAL");
        }
        while (fourthKey.wasPressed()) {
            sendAbility("FOURTH");
        }
        while (fifthKey.wasPressed()) {
            sendAbility("FIFTH");
        }
    }

    private void sendAbility(String ability) {
        sendOpcode("ABILITY\u001F" + ability);
    }

    private void sendOpcode(String payload) {
        if (!ClientPlayNetworking.canSend(BloodlinePayload.ID)) {
            return;
        }
        ClientPlayNetworking.send(new BloodlinePayload(payload));
    }

    private void handlePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        handshakeConfirmed = true;
        String[] sections = payload.split("\u001F");
        if (sections.length == 0) {
            return;
        }
        switch (sections[0]) {
            case "HELLO_ACK" -> {
                hudState.lastSyncAt = System.currentTimeMillis();
            }
            case "POPUP" -> {
                Map<String, String> data = parseData(sections);
                hudState.popupMessage = data.getOrDefault("message", "");
                hudState.popupColor = data.getOrDefault("color", "white");
                hudState.popupUntil = System.currentTimeMillis() + 2400L;
            }
            case "SYNC" -> applySync(parseData(sections));
            default -> {
            }
        }
    }

    private Map<String, String> parseData(String[] sections) {
        Map<String, String> data = new HashMap<>();
        for (int index = 1; index < sections.length; index++) {
            int equals = sections[index].indexOf('=');
            if (equals <= 0) {
                continue;
            }
            data.put(sections[index].substring(0, equals), sections[index].substring(equals + 1));
        }
        return data;
    }

    private void applySync(Map<String, String> data) {
        hudState.bloodlineName = data.getOrDefault("bloodlineName", "Bloodline");
        hudState.level = parseInt(data.get("level"), 1);
        hudState.mode = data.getOrDefault("mode", "SCRIPTED");
        hudState.worldDisabled = Boolean.parseBoolean(data.getOrDefault("worldDisabled", "false"));
        hudState.zeroCooldown = Boolean.parseBoolean(data.getOrDefault("zeroCooldown", "false"));
        hudState.omniHeld = Boolean.parseBoolean(data.getOrDefault("omniHeld", "false"));
        hudState.primaryName = data.getOrDefault("primaryName", "Primary");
        hudState.secondaryName = data.getOrDefault("secondaryName", "Secondary");
        hudState.specialName = data.getOrDefault("specialName", "Special");
        hudState.fourthName = data.getOrDefault("fourthName", "Fourth");
        hudState.fifthName = data.getOrDefault("fifthName", "Fifth");
        hudState.primaryRemaining = parseLong(data.get("primaryRemaining"), 0L);
        hudState.secondaryRemaining = parseLong(data.get("secondaryRemaining"), 0L);
        hudState.specialRemaining = parseLong(data.get("specialRemaining"), 0L);
        hudState.fourthRemaining = parseLong(data.get("fourthRemaining"), 0L);
        hudState.fifthRemaining = parseLong(data.get("fifthRemaining"), 0L);
        hudState.secondaryCharges = parseInt(data.get("secondaryCharges"), 0);
        hudState.timerLabel = data.getOrDefault("timerLabel", "");
        hudState.timerRemaining = parseLong(data.get("timerRemaining"), 0L);
        hudState.timerTotal = parseLong(data.get("timerTotal"), 0L);
        hudState.primaryUnlocked = Boolean.parseBoolean(data.getOrDefault("primaryUnlocked", "true"));
        hudState.secondaryUnlocked = Boolean.parseBoolean(data.getOrDefault("secondaryUnlocked", "false"));
        hudState.specialUnlocked = Boolean.parseBoolean(data.getOrDefault("specialUnlocked", "false"));
        hudState.fourthUnlocked = Boolean.parseBoolean(data.getOrDefault("fourthUnlocked", "false"));
        hudState.fifthUnlocked = Boolean.parseBoolean(data.getOrDefault("fifthUnlocked", "false"));
        hudState.lastSyncAt = System.currentTimeMillis();
    }

    private void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        TextRenderer text = client.textRenderer;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int boxWidth = 68;
        int totalWidth = boxWidth * 5 + 24;
        int startX = (width - totalWidth) / 2;
        int y = height - 64;

        drawAbilityBox(context, text, startX, y, "P", hudState.primaryName, remainingNow(hudState.primaryRemaining), hudState.primaryUnlocked);
        drawAbilityBox(context, text, startX + boxWidth + 6, y, "S", hudState.secondaryNameForHud(), remainingNow(hudState.secondaryRemaining), hudState.secondaryUnlocked);
        drawAbilityBox(context, text, startX + (boxWidth + 6) * 2, y, "X", hudState.specialName, remainingNow(hudState.specialRemaining), hudState.specialUnlocked);
        drawAbilityBox(context, text, startX + (boxWidth + 6) * 3, y, "4", hudState.fourthName, remainingNow(hudState.fourthRemaining), hudState.fourthUnlocked);
        drawAbilityBox(context, text, startX + (boxWidth + 6) * 4, y, "5", hudState.fifthName, remainingNow(hudState.fifthRemaining), hudState.fifthUnlocked);

        String title = hudState.omniHeld
                ? "OmniBlade Override"
                : hudState.bloodlineName + " Lv." + hudState.level + " [" + hudState.mode + "]";
        context.fill(startX, y - 18, startX + totalWidth, y - 2, 0xA0101010);
        context.drawTextWithShadow(text, title, startX + 6, y - 15, hudState.worldDisabled ? 0xFF6666 : 0xFFFFFF);

        if (hudState.zeroCooldown) {
            context.drawTextWithShadow(text, "DEBUG: ZERO CD", startX + totalWidth - 88, y - 15, 0xFFD896FF);
        }

        if (hudState.omniHeld) {
            context.drawTextWithShadow(text, "I  II  III  IV  V", startX + 8, y + 39, 0xFFE2C7FF);
        }

        if (hudState.timerRemaining > 0L && hudState.timerTotal > 0L) {
            int barX = startX;
            int barY = y - 30;
            int barWidth = totalWidth;
            float progress = Math.max(0.0F, Math.min(1.0F, remainingNow(hudState.timerRemaining) / (float) hudState.timerTotal));
            context.fill(barX, barY, barX + barWidth, barY + 8, 0x70101010);
            context.fill(barX, barY, barX + Math.max(1, (int) (barWidth * progress)), barY + 8, 0xFF3AA3FF);
            context.drawTextWithShadow(text, hudState.timerLabel + " " + formatMillis(remainingNow(hudState.timerRemaining)), barX + 4, barY - 10, 0xFFFFFFFF);
        }

        if (hudState.popupUntil > System.currentTimeMillis() && !hudState.popupMessage.isBlank()) {
            int popupWidth = Math.max(140, text.getWidth(hudState.popupMessage) + 18);
            int popupX = (width - popupWidth) / 2;
            int popupY = y - 58;
            context.fill(popupX, popupY, popupX + popupWidth, popupY + 18, 0xC0101010);
            context.drawTextWithShadow(text, hudState.popupMessage, popupX + 9, popupY + 5, popupColor(hudState.popupColor));
        }
    }

    private void drawAbilityBox(DrawContext context, TextRenderer text, int x, int y, String slot, String name, long remaining, boolean unlocked) {
        int color = unlocked ? 0xC0181818 : 0xC0401010;
        context.fill(x, y, x + 68, y + 34, color);
        int border = unlocked ? 0xFF6A6A6A : 0xFFAA4444;
        context.fill(x, y, x + 68, y + 1, border);
        context.fill(x, y + 33, x + 68, y + 34, border);
        context.fill(x, y, x + 1, y + 34, border);
        context.fill(x + 67, y, x + 68, y + 34, border);
        context.drawTextWithShadow(text, slot, x + 5, y + 4, unlocked ? 0xFFD6D6D6 : 0xFFFF9999);
        context.drawTextWithShadow(text, trim(name, 10), x + 18, y + 4, unlocked ? 0xFFFFFFFF : 0xFFBBBBBB);
        String footer = !unlocked ? "LOCKED" : remaining <= 0L ? "READY" : formatMillis(remaining);
        context.drawTextWithShadow(text, footer, x + 5, y + 20, remaining <= 0L ? 0xFF7FFF96 : 0xFFFFD36D);
    }

    private long remainingNow(long syncedRemaining) {
        long elapsed = System.currentTimeMillis() - hudState.lastSyncAt;
        return Math.max(0L, syncedRemaining - elapsed);
    }

    private boolean shouldSendHello() {
        return lastHelloAt == 0L || System.currentTimeMillis() - lastHelloAt >= 1000L;
    }

    private String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + ".";
    }

    private int popupColor(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "red" -> 0xFFFF7777;
            case "green" -> 0xFF8FFF8F;
            case "yellow" -> 0xFFFFE27A;
            case "light_purple" -> 0xFFE1AAFF;
            case "aqua" -> 0xFF8DEEFF;
            default -> 0xFFFFFFFF;
        };
    }

    private String formatMillis(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return value == null ? fallback : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static final class HudState {
        private String bloodlineName = "Bloodline";
        private int level = 1;
        private String mode = "SCRIPTED";
        private boolean worldDisabled;
        private boolean zeroCooldown;
        private boolean omniHeld;
        private String primaryName = "Primary";
        private String secondaryName = "Secondary";
        private String specialName = "Special";
        private String fourthName = "Fourth";
        private String fifthName = "Fifth";
        private long primaryRemaining;
        private long secondaryRemaining;
        private long specialRemaining;
        private long fourthRemaining;
        private long fifthRemaining;
        private int secondaryCharges;
        private String timerLabel = "";
        private long timerRemaining;
        private long timerTotal;
        private boolean primaryUnlocked = true;
        private boolean secondaryUnlocked;
        private boolean specialUnlocked;
        private boolean fourthUnlocked;
        private boolean fifthUnlocked;
        private String popupMessage = "";
        private String popupColor = "white";
        private long popupUntil;
        private long lastSyncAt;

        private void reset() {
            bloodlineName = "Bloodline";
            level = 1;
            mode = "SCRIPTED";
            worldDisabled = false;
            zeroCooldown = false;
            omniHeld = false;
            primaryName = "Primary";
            secondaryName = "Secondary";
            specialName = "Special";
            fourthName = "Fourth";
            fifthName = "Fifth";
            primaryRemaining = 0L;
            secondaryRemaining = 0L;
            specialRemaining = 0L;
            fourthRemaining = 0L;
            fifthRemaining = 0L;
            secondaryCharges = 0;
            timerLabel = "";
            timerRemaining = 0L;
            timerTotal = 0L;
            primaryUnlocked = true;
            secondaryUnlocked = false;
            specialUnlocked = false;
            fourthUnlocked = false;
            fifthUnlocked = false;
            popupMessage = "";
            popupColor = "white";
            popupUntil = 0L;
            lastSyncAt = 0L;
        }

        private String secondaryNameForHud() {
            if ("Void Send".equals(secondaryName) && secondaryCharges > 0) {
                return secondaryName + " x" + secondaryCharges;
            }
            return secondaryName;
        }
    }

    public record BloodlinePayload(String payload) implements CustomPayload {
        public static final CustomPayload.Id<BloodlinePayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, BloodlinePayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING,
                BloodlinePayload::payload,
                BloodlinePayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
