package me.dreamvoid.chat2qq.bukkit.listener;

import me.dreamvoid.chat2qq.bukkit.BukkitPlugin;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.internal.httpapi.MiraiHttpAPI;
import me.dreamvoid.miraimc.internal.httpapi.exception.AbnormalStatusException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.NoSuchElementException;

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
                    String message = plugin.getConfig().getString("bot.player-advancement-done-message")
                            .replace("%player%", e.getPlayer().getName())
                            .replace("%advancement_title%", advancement_title.toString())
                            .replace("%advancement_description%", advancement_description.toString());
                    plugin.getConfig().getLongList("bot.bot-accounts")
                            .forEach(bot -> plugin.getConfig().getLongList("bot.group-ids").forEach(group -> {
                                try {
                                    MiraiBot.getBot(bot).getGroup(group).sendMessageMirai(message);
                                } catch (NoSuchElementException e) {
                                    if (MiraiHttpAPI.Bots.containsKey(bot)) {
                                        try {
                                            MiraiHttpAPI.INSTANCE.sendGroupMessage(MiraiHttpAPI.Bots.get(bot), group,
                                                    message);
                                        } catch (IOException | AbnormalStatusException ex) {
                                            plugin.getLogger().warning("使用" + bot + "发送消息时出现异常，原因: " + ex);
                                        }
                                    } else
                                        plugin.getLogger().warning("指定的机器人" + bot + "不存在，是否已经登录了机器人？");
                                }
                            }));
                }
            }.runTaskAsynchronously(plugin);
        }
    }
}
