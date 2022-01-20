package me.dreamvoid.chat2qq.bukkit.listener;


import me.dreamvoid.chat2qq.bukkit.BukkitPlugin;
import me.dreamvoid.miraimc.api.MiraiBot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class onPlayerDie implements Listener{
    private final BukkitPlugin plugin;
    public onPlayerDie(BukkitPlugin plugin){
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerDieEvent(PlayerDeathEvent e){
        if(plugin.getConfig().getBoolean("bot.send-player-dead-message",false)){
            new BukkitRunnable() {
                @Override
                public void run() {
                    MiraiBot.getBot(plugin.getConfig().getLong("bot.botaccount"))
                    .getGroup(plugin.getConfig().getLong("bot.groupid"))
                    .sendMessageMirai(plugin.getConfig().getString("bot.player-die-message")
                    //.replace("%player%",e.getEntity().getName())
                    .replace("%death_description%",e.getDeathMessage())
                    );
                }
            }.runTaskAsynchronously(plugin);
        }
    }
}