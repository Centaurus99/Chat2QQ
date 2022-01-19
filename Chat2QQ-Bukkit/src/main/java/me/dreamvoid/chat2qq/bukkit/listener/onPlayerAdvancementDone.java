package me.dreamvoid.chat2qq.bukkit.listener;

import me.dreamvoid.chat2qq.bukkit.BukkitPlugin;
import me.dreamvoid.miraimc.api.MiraiBot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class onPlayerAdvancementDone implements Listener {
    private final BukkitPlugin plugin;

    public onPlayerAdvancementDone(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerAdvancementDoneEvent(PlayerAdvancementDoneEvent e) {
        if (plugin.getConfig().getBoolean("bot.send-player-advancement-done-message", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    org.bukkit.NamespacedKey key = e.getAdvancement().getKey();
                    Object advancement_title = plugin.lang_zh_cn.get(key.getKey() + "/title");
                    Object advancement_description = plugin.lang_zh_cn.get(key.getKey() + "/description");
                    if (advancement_title == null)
                        return;
                    if (advancement_description == null)
                        advancement_description = "";
                    MiraiBot.getBot(plugin.getConfig().getLong("bot.botaccount"))
                            .getGroup(plugin.getConfig().getLong("bot.groupid"))
                            .sendMessageMirai(plugin.getConfig().getString("bot.player-advancement-done-message")
                                    .replace("%player%", e.getPlayer().getName())
                                    .replace("%advancement_title%", advancement_title.toString())
                                    .replace("%advancement_description%", advancement_description.toString()));
                }
            }.runTaskAsynchronously(plugin);
        }
    }
}
