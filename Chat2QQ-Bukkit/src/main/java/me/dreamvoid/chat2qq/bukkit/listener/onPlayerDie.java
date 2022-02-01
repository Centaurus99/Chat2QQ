package me.dreamvoid.chat2qq.bukkit.listener;

import me.dreamvoid.chat2qq.bukkit.BukkitPlugin;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.internal.httpapi.MiraiHttpAPI;
import me.dreamvoid.miraimc.internal.httpapi.exception.AbnormalStatusException;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.NoSuchElementException;

public class onPlayerDie implements Listener {
    private final BukkitPlugin plugin;

    public onPlayerDie(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDieEvent(PlayerDeathEvent e) {
        if (plugin.getConfig().getBoolean("bot.send-player-dead-message", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Statistic statistic = Statistic.DEATHS;
                    String deaths;
                    deaths = String.format("%d", e.getEntity().getStatistic(statistic));
                    String message = plugin.getConfig().getString("bot.player-die-message")
                            .replace("%death_description%", e.getDeathMessage())
                            .replace("%player%", e.getEntity().getName())
                            .replace("%deaths%", deaths);
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