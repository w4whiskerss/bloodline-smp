package dev.whiskers.bloodlinehotkeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BloodlineHotkeysClient implements ClientModInitializer {

    private static final Identifier CHANNEL = Identifier.of("bloodline", "main");
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("bloodline", "keybinds"));
    private static final int MIN_BOX_SIZE = 24;
    private static final int MAX_BOX_SIZE = 38;
    private static final String HANDSHAKE_COMMAND = "bloodlinehandshake";
    private static final String CONFIG_KEY_SERVERS = "auto_handshake_servers";

    private final HudState hudState = new HudState();
    private final Map<String, Identifier> iconTextureCache = new HashMap<>();
    private final ClientConfig clientConfig = new ClientConfig();
    private KeyBinding primaryKey;
    private KeyBinding secondaryKey;
    private KeyBinding specialKey;
    private KeyBinding fourthKey;
    private KeyBinding fifthKey;
    private KeyBinding configureServerKey;
    private boolean serverDetected;
    private boolean hotkeyHandshakeActive;
    private boolean handshakeCommandSent;
    private long lastHelloAt;

    @Override
    public void onInitializeClient() {
        clientConfig.load();
        primaryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.primary", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
        secondaryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.secondary", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        specialKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.special", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY));
        fourthKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.fourth", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));
        fifthKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.fifth", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
        configureServerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bloodline.configure_server", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY));

        PayloadTypeRegistry.playC2S().register(BloodlinePayload.ID, BloodlinePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BloodlinePayload.ID, BloodlinePayload.CODEC);
        ClientTickEvents.END_CLIENT_TICK.register(this::tickClient);
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> renderHud(drawContext));
        ClientPlayNetworking.registerGlobalReceiver(BloodlinePayload.ID, (payload, context) -> handlePayload(payload.payload()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            serverDetected = false;
            hotkeyHandshakeActive = false;
            handshakeCommandSent = false;
            lastHelloAt = 0L;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverDetected = false;
            hotkeyHandshakeActive = false;
            handshakeCommandSent = false;
            lastHelloAt = 0L;
            hudState.reset();
        });
    }

    private void tickClient(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            serverDetected = false;
            hotkeyHandshakeActive = false;
            handshakeCommandSent = false;
            lastHelloAt = 0L;
            hudState.reset();
            return;
        }

        while (configureServerKey.wasPressed()) {
            client.setScreen(new ServerHandshakeConfigScreen(client.currentScreen));
        }

        if (!serverDetected && shouldSendHello()) {
            sendOpcode("HELLO");
            lastHelloAt = System.currentTimeMillis();
        }
        if (!handshakeCommandSent) {
            attemptConfiguredHandshake(client);
        }
        if (!hotkeyHandshakeActive) {
            return;
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
        serverDetected = true;
        String[] sections = payload.split("\u001F");
        if (sections.length == 0) {
            return;
        }
        switch (sections[0]) {
            case "HELLO_ACK" -> {
                hudState.lastSyncAt = System.currentTimeMillis();
            }
            case "HOTKEYS_ACK" -> {
                hotkeyHandshakeActive = true;
                hudState.lastSyncAt = System.currentTimeMillis();
            }
            case "POPUP" -> {
                Map<String, String> data = parseData(sections);
                hudState.popupMessage = data.getOrDefault("message", "");
                hudState.popupColor = data.getOrDefault("color", "white");
                hudState.popupUntil = System.currentTimeMillis() + 2400L;
            }
            case "SYNC" -> {
                hotkeyHandshakeActive = true;
                applySync(parseData(sections));
            }
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
        hudState.primaryIcon = data.getOrDefault("primaryIcon", "");
        hudState.secondaryIcon = data.getOrDefault("secondaryIcon", "");
        hudState.specialIcon = data.getOrDefault("specialIcon", "");
        hudState.fourthIcon = data.getOrDefault("fourthIcon", "");
        hudState.fifthIcon = data.getOrDefault("fifthIcon", "");
        hudState.primaryRemaining = parseLong(data.get("primaryRemaining"), 0L);
        hudState.secondaryRemaining = parseLong(data.get("secondaryRemaining"), 0L);
        hudState.specialRemaining = parseLong(data.get("specialRemaining"), 0L);
        hudState.fourthRemaining = parseLong(data.get("fourthRemaining"), 0L);
        hudState.fifthRemaining = parseLong(data.get("fifthRemaining"), 0L);
        hudState.primaryTotal = parseLong(data.get("primaryTotal"), 0L);
        hudState.secondaryTotal = parseLong(data.get("secondaryTotal"), 0L);
        hudState.specialTotal = parseLong(data.get("specialTotal"), 0L);
        hudState.fourthTotal = parseLong(data.get("fourthTotal"), 0L);
        hudState.fifthTotal = parseLong(data.get("fifthTotal"), 0L);
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
        if (client.player == null || client.options.hudHidden || !hotkeyHandshakeActive) {
            return;
        }

        TextRenderer text = client.textRenderer;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        SlotView[] slots = visibleSlots();
        int slotCount = slots.length;
        int boxSize = Math.max(MIN_BOX_SIZE, Math.min(MAX_BOX_SIZE, Math.min(width, height) / 12));
        int gap = Math.max(4, boxSize / 5);
        int totalWidth = boxSize * slotCount + gap * (slotCount - 1);
        int startX = (width - totalWidth) / 2;
        int y = height - Math.max(boxSize + 64, (int) (height * 0.22F));

        for (int index = 0; index < slotCount; index++) {
            SlotView slot = slots[index];
            int x = startX + index * (boxSize + gap);
            drawAbilityBox(context, text, x, y, Integer.toString(index + 1), slot, boxSize);
        }

        String title = hudState.omniHeld
                ? "OmniBlade Override"
                : hudState.bloodlineName + " Lv." + hudState.level + " [" + hudState.mode + "]";
        context.fill(startX, y - 16, startX + totalWidth, y - 2, 0xA0101010);
        context.drawTextWithShadow(text, title, startX + 5, y - 14, hudState.worldDisabled ? 0xFF6666 : 0xFFFFFF);

        if (hudState.zeroCooldown) {
            context.drawTextWithShadow(text, "DEBUG: ZERO CD", startX + totalWidth - 88, y - 14, 0xFFD896FF);
        }

        if (hudState.omniHeld) {
            context.drawTextWithShadow(text, "I  II  III  IV  V", startX + 8, y + boxSize + 4, 0xFFE2C7FF);
        }

        if (hudState.timerRemaining > 0L && hudState.timerTotal > 0L) {
            int barX = startX;
            int barY = y - 27;
            int barWidth = totalWidth;
            float progress = Math.max(0.0F, Math.min(1.0F, remainingNow(hudState.timerRemaining) / (float) hudState.timerTotal));
            context.fill(barX, barY, barX + barWidth, barY + 8, 0x70101010);
            context.fill(barX, barY, barX + Math.max(1, (int) (barWidth * progress)), barY + 8, 0xFF3AA3FF);
            context.drawTextWithShadow(text, hudState.timerLabel + " " + formatMillis(remainingNow(hudState.timerRemaining)), barX + 4, barY - 10, 0xFFFFFFFF);
        }

        if (hudState.popupUntil > System.currentTimeMillis() && !hudState.popupMessage.isBlank()) {
            int popupWidth = Math.max(140, text.getWidth(hudState.popupMessage) + 18);
            int popupX = (width - popupWidth) / 2;
            int popupY = y - 52;
            context.fill(popupX, popupY, popupX + popupWidth, popupY + 18, 0xC0101010);
            context.drawTextWithShadow(text, hudState.popupMessage, popupX + 9, popupY + 5, popupColor(hudState.popupColor));
        }

        renderKeyConflictHint(context, text, width);
    }

    private void drawAbilityBox(DrawContext context, TextRenderer text, int x, int y, String slot, SlotView slotView, int size) {
        long remaining = slotView.remaining();
        long total = slotView.total();
        boolean unlocked = slotView.unlocked();
        int color = unlocked ? 0xC0181818 : 0xC0401010;
        context.fill(x, y, x + size, y + size, color);
        int border = unlocked ? 0xFF6A6A6A : 0xFFAA4444;
        context.fill(x, y, x + size, y + 1, border);
        context.fill(x, y + size - 1, x + size, y + size, border);
        context.fill(x, y, x + 1, y + size, border);
        context.fill(x + size - 1, y, x + size, y + size, border);
        context.drawTextWithShadow(text, slot, x + 5, y + 4, unlocked ? 0xFFD6D6D6 : 0xFFFF9999);

        int iconSize = Math.max(12, size - 12);
        int iconX = x + (size - iconSize) / 2;
        int iconY = y + (size - iconSize) / 2 + 1;
        drawAbilityIcon(context, text, iconX, iconY, iconSize, slotView.name(), slotView.iconKey(), unlocked);
        drawCooldownOverlay(context, x, y, size, remaining, total, unlocked);
    }

    private SlotView[] visibleSlots() {
        List<SlotView> slots = new ArrayList<>();
        slots.add(new SlotView(hudState.primaryName, hudState.primaryIcon, remainingNow(hudState.primaryRemaining), hudState.primaryTotal, hudState.primaryUnlocked));
        slots.add(new SlotView(hudState.secondaryNameForHud(), hudState.secondaryIcon, remainingNow(hudState.secondaryRemaining), hudState.secondaryTotal, hudState.secondaryUnlocked));
        slots.add(new SlotView(hudState.specialName, hudState.specialIcon, remainingNow(hudState.specialRemaining), hudState.specialTotal, hudState.specialUnlocked));

        if (hudState.omniHeld || bloodlineHasFourthAbility()) {
            slots.add(new SlotView(hudState.fourthName, hudState.fourthIcon, remainingNow(hudState.fourthRemaining), hudState.fourthTotal, hudState.fourthUnlocked));
        }
        if (hudState.omniHeld || bloodlineHasFifthAbility()) {
            slots.add(new SlotView(hudState.fifthName, hudState.fifthIcon, remainingNow(hudState.fifthRemaining), hudState.fifthTotal, hudState.fifthUnlocked));
        }

        return slots.toArray(SlotView[]::new);
    }

    private boolean bloodlineHasFourthAbility() {
        if (hudState.bloodlineName == null) {
            return false;
        }
        return switch (hudState.bloodlineName.trim().toLowerCase()) {
            case "spartan", "earthian", "voider" -> true;
            default -> false;
        };
    }

    private boolean bloodlineHasFifthAbility() {
        if (hudState.bloodlineName == null) {
            return false;
        }
        return switch (hudState.bloodlineName.trim().toLowerCase()) {
            case "spartan", "earthian", "voider" -> true;
            default -> false;
        };
    }

    private void drawAbilityIcon(DrawContext context, TextRenderer text, int x, int y, int size, String name, String iconKey, boolean unlocked) {
        Optional<Identifier> texture = resolveIconTexture(iconKey);
        if (texture.isPresent()) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, texture.get(), x, y, 0.0F, 0.0F, size, size, size, size);
            return;
        }

        context.fill(x, y, x + size, y + size, unlocked ? 0x44202020 : 0x66402020);
        String label = trim(name, 2).toUpperCase();
        int labelX = x + Math.max(0, (size - text.getWidth(label)) / 2);
        int labelY = y + Math.max(0, (size - 8) / 2);
        context.drawTextWithShadow(text, label, labelX, labelY, unlocked ? 0xFFFFFFFF : 0xFFBBBBBB);
    }

    private void drawCooldownOverlay(DrawContext context, int x, int y, int size, long remaining, long total, boolean unlocked) {
        if (!unlocked) {
            context.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xAA666666);
            return;
        }
        if (remaining <= 0L || total <= 0L) {
            return;
        }

        float fraction = Math.max(0.0F, Math.min(1.0F, remaining / (float) total));
        int overlayHeight = Math.max(1, Math.round((size - 2) * fraction));
        context.fill(x + 1, y + size - 1 - overlayHeight, x + size - 1, y + size - 1, 0x88FFFFFF);
    }

    private Optional<Identifier> resolveIconTexture(String iconKey) {
        if (iconKey == null || iconKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(iconTextureCache.computeIfAbsent(iconKey, this::findTexture));
    }

    private Identifier findTexture(String iconKey) {
        MinecraftClient client = MinecraftClient.getInstance();
        Identifier hotkeysTexture = Identifier.of("bloodline_hotkeys", "textures/abilities/" + iconKey + ".png");
        if (client.getResourceManager().getResource(hotkeysTexture).isPresent()) {
            return hotkeysTexture;
        }
        Identifier bloodlineTexture = Identifier.of("bloodline", "textures/abilities/" + iconKey + ".png");
        if (client.getResourceManager().getResource(bloodlineTexture).isPresent()) {
            return bloodlineTexture;
        }
        return null;
    }

    private void renderKeyConflictHint(DrawContext context, TextRenderer text, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        if (screen == null || !screen.getClass().getSimpleName().equals("KeybindsScreen")) {
            return;
        }
        String conflicts = conflictsSummary(client);
        if (conflicts.isBlank()) {
            return;
        }
        int messageWidth = text.getWidth(conflicts);
        int x = Math.max(4, (width - messageWidth) / 2);
        context.drawTextWithShadow(text, conflicts, x, 6, 0xFFFFAA55);
    }

    private String conflictsSummary(MinecraftClient client) {
        if (client.options == null || client.options.allKeys == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder("BloodlineSMP conflicts: ");
        boolean any = false;
        KeyBinding[] bloodline = new KeyBinding[]{primaryKey, secondaryKey, specialKey, fourthKey, fifthKey};
        for (KeyBinding ours : bloodline) {
            if (ours == null) {
                continue;
            }
            String ourBound = ours.getBoundKeyLocalizedText().getString();
            for (KeyBinding other : client.options.allKeys) {
                if (other == null || other == ours) {
                    continue;
                }
                String otherBound = other.getBoundKeyLocalizedText().getString();
                if (!ours.equals(other) && !ourBound.isBlank() && ourBound.equalsIgnoreCase(otherBound)) {
                    if (any) {
                        builder.append(", ");
                    }
                    builder.append(ourBound).append(" (").append(otherBound).append(")");
                    any = true;
                    break;
                }
            }
        }
        return any ? builder.toString() : "";
    }

    private long remainingNow(long syncedRemaining) {
        long elapsed = System.currentTimeMillis() - hudState.lastSyncAt;
        return Math.max(0L, syncedRemaining - elapsed);
    }

    private void attemptConfiguredHandshake(MinecraftClient client) {
        var serverEntry = client.getCurrentServerEntry();
        if (serverEntry == null || serverEntry.address == null) {
            return;
        }
        String currentAddress = normalizeServerAddress(serverEntry.address);
        if (!clientConfig.matches(currentAddress)) {
            return;
        }
        client.getNetworkHandler().sendChatCommand(HANDSHAKE_COMMAND);
        handshakeCommandSent = true;
    }

    private String trim(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + ".";
    }

    private String normalizeServerAddress(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean shouldSendHello() {
        return lastHelloAt == 0L || System.currentTimeMillis() - lastHelloAt >= 1000L;
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
        private String primaryIcon = "";
        private String secondaryIcon = "";
        private String specialIcon = "";
        private String fourthIcon = "";
        private String fifthIcon = "";
        private long primaryRemaining;
        private long secondaryRemaining;
        private long specialRemaining;
        private long fourthRemaining;
        private long fifthRemaining;
        private long primaryTotal;
        private long secondaryTotal;
        private long specialTotal;
        private long fourthTotal;
        private long fifthTotal;
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
            primaryIcon = "";
            secondaryIcon = "";
            specialIcon = "";
            fourthIcon = "";
            fifthIcon = "";
            primaryRemaining = 0L;
            secondaryRemaining = 0L;
            specialRemaining = 0L;
            fourthRemaining = 0L;
            fifthRemaining = 0L;
            primaryTotal = 0L;
            secondaryTotal = 0L;
            specialTotal = 0L;
            fourthTotal = 0L;
            fifthTotal = 0L;
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

    private final class ClientConfig {
        private final Path path = Path.of(MinecraftClient.getInstance().runDirectory.getAbsolutePath(), "config", "bloodline_hotkeys.properties");
        private final List<String> handshakeServerAddresses = new ArrayList<>();

        private void load() {
            handshakeServerAddresses.clear();
            if (!Files.exists(path)) {
                return;
            }
            Properties properties = new Properties();
            try (var input = Files.newInputStream(path)) {
                properties.load(input);
                String raw = properties.getProperty(CONFIG_KEY_SERVERS, "").trim();
                if (!raw.isBlank()) {
                    for (String value : raw.split(",")) {
                        String normalized = normalizeServerAddress(value);
                        if (!normalized.isBlank() && !handshakeServerAddresses.contains(normalized)) {
                            handshakeServerAddresses.add(normalized);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        private void save() {
            try {
                Files.createDirectories(path.getParent());
                Properties properties = new Properties();
                properties.setProperty(CONFIG_KEY_SERVERS, String.join(",", handshakeServerAddresses));
                try (var output = Files.newOutputStream(path)) {
                    properties.store(output, "Bloodline Hotkeys");
                }
            } catch (IOException ignored) {
            }
        }

        private boolean matches(String currentAddress) {
            for (String savedAddress : handshakeServerAddresses) {
                if (savedAddress.equals(currentAddress)) {
                    return true;
                }
            }
            return false;
        }

        private boolean addServer(String value) {
            String normalized = normalizeServerAddress(value);
            if (normalized.isBlank() || handshakeServerAddresses.contains(normalized)) {
                return false;
            }
            handshakeServerAddresses.add(normalized);
            return true;
        }

        private void removeServer(String value) {
            handshakeServerAddresses.remove(normalizeServerAddress(value));
        }
    }

    private final class ServerHandshakeConfigScreen extends Screen {
        private final Screen parent;
        private TextFieldWidget addressField;

        private ServerHandshakeConfigScreen(Screen parent) {
            super(Text.literal("Bloodline Handshake Server"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = width / 2;
            int centerY = height / 2;
            clearChildren();
            addressField = new TextFieldWidget(textRenderer, centerX - 110, centerY - 78, 220, 20, Text.literal("Server IP:Port"));
            addressField.setMaxLength(255);
            addressField.setText(clientConfig.handshakeServerAddresses.isEmpty() ? "" : clientConfig.handshakeServerAddresses.get(0));
            addSelectableChild(addressField);
            setInitialFocus(addressField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Add Server"), button -> {
                if (clientConfig.addServer(addressField.getText())) {
                    clientConfig.save();
                    init();
                }
            }).dimensions(centerX - 110, centerY - 50, 105, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
                if (client != null) {
                    client.setScreen(parent);
                }
            }).dimensions(centerX + 5, centerY - 50, 105, 20).build());

            int listY = centerY - 18;
            int maxRows = Math.min(7, clientConfig.handshakeServerAddresses.size());
            for (int index = 0; index < maxRows; index++) {
                String address = clientConfig.handshakeServerAddresses.get(index);
                int rowY = listY + index * 22;
                addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), button -> {
                    clientConfig.removeServer(address);
                    clientConfig.save();
                    init();
                }).dimensions(centerX + 40, rowY - 2, 70, 20).build());
            }
        }

        @Override
        public void close() {
            if (client != null) {
                client.setScreen(parent);
            }
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            int centerX = width / 2;
            int centerY = height / 2;
            context.drawCenteredTextWithShadow(textRenderer, title, centerX, centerY - 104, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Saved servers auto-run /bloodlinehandshake"), centerX, centerY - 92, 0xC8C8C8);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Saved: " + clientConfig.handshakeServerAddresses.size()), centerX, centerY - 80, 0xB05050);
            if (addressField != null) {
                addressField.render(context, mouseX, mouseY, delta);
            }
            if (clientConfig.handshakeServerAddresses.isEmpty()) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("No saved servers yet"), centerX, centerY - 8, 0x9A9A9A);
            } else {
                int listY = centerY - 18;
                for (int index = 0; index < Math.min(7, clientConfig.handshakeServerAddresses.size()); index++) {
                    context.drawTextWithShadow(textRenderer, clientConfig.handshakeServerAddresses.get(index), centerX - 110, listY + index * 22 + 4, 0xFFFFFF);
                }
            }
        }
    }

    private record SlotView(String name, String iconKey, long remaining, long total, boolean unlocked) {
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
