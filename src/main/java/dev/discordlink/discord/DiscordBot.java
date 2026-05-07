package dev.discordlink.discord;

import dev.discordlink.DiscordLink;
import dev.discordlink.managers.CodeManager;
import dev.discordlink.managers.LinkManager;
import dev.discordlink.managers.MessageManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;

public class DiscordBot extends ListenerAdapter {

    private final DiscordLink plugin;
    private final CodeManager codeManager;
    private final LinkManager linkManager;
    private final MessageManager msg;
    private JDA jda;
    private String allowedChannelId;

    public DiscordBot(DiscordLink plugin) {
        this.plugin = plugin;
        this.codeManager = plugin.getCodeManager();
        this.linkManager = plugin.getLinkManager();
        this.msg = plugin.getMessageManager();
    }

    public void start() {
        String token = plugin.getConfig().getString("discord.token", "");
        allowedChannelId = plugin.getConfig().getString("discord.channel-id", "");

        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().severe("Discord bot token is not set in config.yml! The Discord bot will not start.");
            return;
        }

        if (allowedChannelId.isEmpty() || allowedChannelId.equals("YOUR_CHANNEL_ID_HERE")) {
            plugin.getLogger().severe("Discord channel ID is not set in config.yml! The Discord bot will not start.");
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(this)
                        .build();

                jda.awaitReady();

                jda.updateCommands()
                        .addCommands(Commands.slash("link", "Link your Discord account to your Minecraft account"))
                        .queue();

                plugin.getLogger().info("Discord bot started successfully as: " + jda.getSelfUser().getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to start Discord bot: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("link")) return;

        if (!event.getChannel().getId().equals(allowedChannelId)) {
            event.reply(msg.getRaw("channel-only")).setEphemeral(true).queue();
            return;
        }

        String discordUserId = event.getUser().getId();

        if (linkManager.isDiscordLinked(discordUserId)) {
            event.reply(msg.getRaw("already-linked-discord")).setEphemeral(true).queue();
            return;
        }

        String code = codeManager.generateCode(discordUserId);
        int expireMinutes = plugin.getConfig().getInt("link.code-expire-minutes", 10);

        String codeMessage = msg.getRaw("code-generated").replace("{code}", code);
        String embedDesc = msg.getRaw("link-embed-description")
                .replace("{expire}", String.valueOf(expireMinutes))
                .replace("{code}", code);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(msg.getRaw("link-embed-title"));
        embed.setDescription(embedDesc);
        embed.addField("Your Code", "```" + code + "```", false);
        embed.setColor(new Color(0x5865F2));
        embed.setFooter(msg.getRaw("link-embed-footer"));

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(allowedChannelId)) return;
        if (!plugin.getConfig().getBoolean("discord.delete-non-commands", true)) return;

        boolean isSlashCommand = false;
        if (event.getMessage().getInteraction() != null) {
            isSlashCommand = true;
        }

        if (!isSlashCommand) {
            event.getMessage().delete().queue(
                    success -> {},
                    failure -> {
                        if (plugin.getConfig().getBoolean("settings.debug")) {
                            plugin.getLogger().warning("Could not delete message in Discord channel: " + failure.getMessage());
                        }
                    }
            );
        }
    }

    public JDA getJDA() {
        return jda;
    }

    public String getAllowedChannelId() {
        return allowedChannelId;
    }

    public void sendChannelMessage(String message) {
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(allowedChannelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }
}
