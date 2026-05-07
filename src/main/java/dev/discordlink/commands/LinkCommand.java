package dev.discordlink.commands;

import dev.discordlink.DiscordLink;
import dev.discordlink.managers.CodeManager;
import dev.discordlink.managers.LinkManager;
import dev.discordlink.managers.MessageManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;

public class LinkCommand implements CommandExecutor, TabCompleter {

    private final DiscordLink plugin;
    private final CodeManager codeManager;
    private final LinkManager linkManager;
    private final MessageManager msg;

    public LinkCommand(DiscordLink plugin) {
        this.plugin = plugin;
        this.codeManager = plugin.getCodeManager();
        this.linkManager = plugin.getLinkManager();
        this.msg = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("discordlink.use")) {
            player.sendMessage(msg.get("no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(msg.get("usage"));
            return true;
        }

        if (linkManager.isLinked(player.getUniqueId())) {
            player.sendMessage(msg.get("already-linked"));
            return true;
        }

        String code = args[0].toUpperCase();
        String discordUserId = codeManager.consumeCode(code);

        if (discordUserId == null) {
            player.sendMessage(msg.get("invalid-code"));
            return true;
        }

        if (linkManager.isDiscordLinked(discordUserId)) {
            player.sendMessage(msg.get("already-linked"));
            return true;
        }

        linkManager.link(player.getUniqueId(), discordUserId);

        player.sendMessage(msg.get("link-success"));

        playFireworkSounds(player);

        executeRewardCommand(player);

        return true;
    }

    private void playFireworkSounds(Player player) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 3) {
                    cancel();
                    return;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.2f);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void executeRewardCommand(Player player) {
        String rewardCommand = plugin.getConfig().getString("link.reward-command", "");
        if (rewardCommand == null || rewardCommand.isEmpty()) return;

        String finalCommand = rewardCommand.replace("{player}", player.getName());

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCommand);
            if (!success && plugin.getConfig().getBoolean("settings.debug")) {
                plugin.getLogger().warning("Reward command failed for player: " + player.getName());
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
