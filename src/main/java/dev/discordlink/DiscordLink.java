package dev.discordlink;

import dev.discordlink.commands.LinkCommand;
import dev.discordlink.discord.DiscordBot;
import dev.discordlink.managers.CodeManager;
import dev.discordlink.managers.LinkManager;
import dev.discordlink.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordLink extends JavaPlugin {

    private static DiscordLink instance;
    private DiscordBot discordBot;
    private CodeManager codeManager;
    private LinkManager linkManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("msg.yml", false);

        messageManager = new MessageManager(this);
        codeManager = new CodeManager(this);
        linkManager = new LinkManager(this);

        LinkCommand linkCommand = new LinkCommand(this);
        getCommand("link").setExecutor(linkCommand);
        getCommand("link").setTabCompleter(linkCommand);

        discordBot = new DiscordBot(this);
        discordBot.start();

        getLogger().info("DiscordLink enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (discordBot != null) {
            discordBot.shutdown();
        }
        if (linkManager != null) {
            linkManager.save();
        }
        getLogger().info("DiscordLink disabled.");
    }

    public static DiscordLink getInstance() {
        return instance;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public CodeManager getCodeManager() {
        return codeManager;
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
