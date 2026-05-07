package dev.discordlink.managers;

import dev.discordlink.DiscordLink;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class CodeManager {

    private final DiscordLink plugin;
    private final Map<String, CodeEntry> activeCodes = new HashMap<>();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final Random random = new Random();

    public CodeManager(DiscordLink plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    public String generateCode(String discordUserId) {
        for (Map.Entry<String, CodeEntry> entry : activeCodes.entrySet()) {
            if (entry.getValue().discordUserId.equals(discordUserId)) {
                activeCodes.remove(entry.getKey());
                break;
            }
        }

        String code;
        do {
            code = randomCode();
        } while (activeCodes.containsKey(code));

        int expireMinutes = plugin.getConfig().getInt("link.code-expire-minutes", 10);
        long expireAt = System.currentTimeMillis() + (expireMinutes * 60 * 1000L);
        activeCodes.put(code, new CodeEntry(discordUserId, expireAt));
        return code;
    }

    public String consumeCode(String code) {
        CodeEntry entry = activeCodes.get(code.toUpperCase());
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireAt) {
            activeCodes.remove(code.toUpperCase());
            return null;
        }
        activeCodes.remove(code.toUpperCase());
        return entry.discordUserId;
    }

    public boolean isValidCode(String code) {
        CodeEntry entry = activeCodes.get(code.toUpperCase());
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expireAt) {
            activeCodes.remove(code.toUpperCase());
            return false;
        }
        return true;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, CodeEntry>> it = activeCodes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CodeEntry> entry = it.next();
                if (now > entry.getValue().expireAt) {
                    it.remove();
                }
            }
        }, 1200L, 1200L);
    }

    private static class CodeEntry {
        final String discordUserId;
        final long expireAt;

        CodeEntry(String discordUserId, long expireAt) {
            this.discordUserId = discordUserId;
            this.expireAt = expireAt;
        }
    }
}
