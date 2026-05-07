package dev.discordlink.managers;

import dev.discordlink.DiscordLink;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MessageManager {

    private final DiscordLink plugin;
    private FileConfiguration msgConfig;
    private File msgFile;

    public MessageManager(DiscordLink plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        msgFile = new File(plugin.getDataFolder(), "msg.yml");
        if (!msgFile.exists()) {
            plugin.saveResource("msg.yml", false);
        }
        msgConfig = YamlConfiguration.loadConfiguration(msgFile);
    }

    public String get(String key) {
        String raw = msgConfig.getString("messages." + key, "&cMessage not found: " + key);
        return colorize(raw);
    }

    public String getRaw(String key) {
        return msgConfig.getString("messages." + key, "Message not found: " + key);
    }

    private String colorize(String text) {
        return text.replace("&", "\u00a7");
    }
}
