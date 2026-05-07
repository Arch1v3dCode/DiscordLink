package dev.discordlink.managers;

import dev.discordlink.DiscordLink;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LinkManager {

    private final DiscordLink plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, String> linkedAccounts = new HashMap<>();
    private final Map<String, UUID> discordToMinecraft = new HashMap<>();

    public LinkManager(DiscordLink plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "linked.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create linked.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.getConfigurationSection("links") != null) {
            for (String uuidStr : dataConfig.getConfigurationSection("links").getKeys(false)) {
                String discordId = dataConfig.getString("links." + uuidStr);
                UUID uuid = UUID.fromString(uuidStr);
                linkedAccounts.put(uuid, discordId);
                discordToMinecraft.put(discordId, uuid);
            }
        }
        plugin.getLogger().info("Loaded " + linkedAccounts.size() + " linked account(s).");
    }

    public void save() {
        for (Map.Entry<UUID, String> entry : linkedAccounts.entrySet()) {
            dataConfig.set("links." + entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save linked.yml: " + e.getMessage());
        }
    }

    public boolean isLinked(UUID uuid) {
        return linkedAccounts.containsKey(uuid);
    }

    public boolean isDiscordLinked(String discordId) {
        return discordToMinecraft.containsKey(discordId);
    }

    public void link(UUID uuid, String discordId) {
        linkedAccounts.put(uuid, discordId);
        discordToMinecraft.put(discordId, uuid);
        save();
    }

    public String getDiscordId(UUID uuid) {
        return linkedAccounts.get(uuid);
    }

    public UUID getMinecraftUUID(String discordId) {
        return discordToMinecraft.get(discordId);
    }
}
