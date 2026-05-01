package dev.zahen.bloodline.network;

import dev.zahen.bloodline.BloodlinePlugin;
import java.nio.charset.StandardCharsets;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class ClientChannelBridge implements PluginMessageListener {

    public static final String CHANNEL = "bloodline:main";

    private final BloodlinePlugin plugin;

    public ClientChannelBridge(BloodlinePlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || player == null) {
            return;
        }
        String payload = decodeString(message);
        if (payload != null) {
            plugin.getBloodlineManager().handleClientPayload(player, payload);
        }
    }

    private String decodeString(byte[] message) {
        if (message == null || message.length == 0) {
            return null;
        }
        int length = 0;
        int shift = 0;
        int index = 0;
        while (index < message.length) {
            int current = message[index++] & 0xFF;
            length |= (current & 0x7F) << shift;
            if ((current & 0x80) == 0) {
                break;
            }
            shift += 7;
            if (shift > 28) {
                return null;
            }
        }
        if (index + length > message.length) {
            return null;
        }
        return new String(message, index, length, StandardCharsets.UTF_8);
    }
}
