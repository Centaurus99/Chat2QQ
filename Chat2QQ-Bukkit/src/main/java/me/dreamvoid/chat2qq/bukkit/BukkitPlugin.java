package me.dreamvoid.chat2qq.bukkit;

import me.clip.placeholderapi.PlaceholderAPI;
import me.dreamvoid.chat2qq.bukkit.listener.onGroupMessage;
import me.dreamvoid.chat2qq.bukkit.listener.onPlayerJoin;
import me.dreamvoid.chat2qq.bukkit.listener.onPlayerMessage;
import me.dreamvoid.chat2qq.bukkit.listener.onPlayerQuit;
import me.dreamvoid.chat2qq.bukkit.listener.onPlayerAdvancementDone;
import me.dreamvoid.chat2qq.bukkit.listener.onPlayerDie;
import me.dreamvoid.chat2qq.bukkit.utils.Metrics;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.internal.httpapi.MiraiHttpAPI;
import me.dreamvoid.miraimc.internal.httpapi.exception.AbnormalStatusException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.IOException;
import java.util.NoSuchElementException;

public class BukkitPlugin extends JavaPlugin implements Listener, CommandExecutor {
    public JSONObject lang_zh_cn;

    @Override // 加载插件
    public void onLoad() {
        saveDefaultConfig();
        reloadConfig();
        // 加载资源
        InputStream lang_json = getResource("lang.json");
        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject lang = (JSONObject) jsonParser.parse(new InputStreamReader(lang_json, "UTF-8"));
            lang_zh_cn = (JSONObject) jsonParser.parse(lang.get("zh_cn").toString());
            if (!lang_zh_cn.get("lang").toString().equals("zh_cn"))
                throw new Exception("lang.json is in wrong format.");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Failed to load lang.json.");
        }
    }

    @Override // 启用插件
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new onGroupMessage(this), this);
        Bukkit.getPluginManager().registerEvents(new onPlayerMessage(this), this);
        Bukkit.getPluginManager().registerEvents(new onPlayerJoin(this), this);
        Bukkit.getPluginManager().registerEvents(new onPlayerQuit(this), this);
        Bukkit.getPluginManager().registerEvents(new onPlayerAdvancementDone(this), this);
        Bukkit.getPluginManager().registerEvents(new onPlayerDie(this), this);
        getCommand("qchat").setExecutor(this);
        getCommand("chat2qq").setExecutor(this);
        if (getConfig().getBoolean("general.allow-bStats", true)) {
            int pluginId = 12193;
            new Metrics(this, pluginId);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
            @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("qchat")) {
            String playerName;
            boolean allowWorld = false;
            boolean isPlayer = false;
            boolean allowConsole = getConfig().getBoolean("general.allow-console-chat", false);

            if (sender instanceof Player) {
                isPlayer = true;
                Player player = (Player) sender;
                playerName = player.getDisplayName();
                // 判断玩家所处世界
                for (String world : getConfig().getStringList("general.available-worlds")) {
                    if (player.getWorld().getName().equalsIgnoreCase(world)) {
                        allowWorld = true;
                        break;
                    }
                }
                if (getConfig().getBoolean("general.available-worlds-use-as-blacklist"))
                    allowWorld = !allowWorld;

            } else {
                if (allowConsole) {
                    playerName = getConfig().getString("general.console-name", "控制台");
                    allowWorld = true;
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c控制台不能执行此命令！"));
                    return true;
                }
            }

            if (allowWorld) {
                StringBuilder message = new StringBuilder();
                for (String arg : args) {
                    message.append(arg).append(" ");
                }
                String formatText = getConfig().getString("bot.group-chat-format")
                        .replace("%player%", playerName)
                        .replace("%message%", message);
                if (isPlayer && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    formatText = PlaceholderAPI.setPlaceholders((Player) sender, formatText);
                }
                String finalFormatText = formatText;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        getConfig().getLongList("bot.bot-accounts")
                                .forEach(bot -> getConfig().getLongList("bot.group-ids").forEach(group -> {
                                    try {
                                        MiraiBot.getBot(bot).getGroup(group).sendMessageMirai(finalFormatText);
                                    } catch (NoSuchElementException e) {
                                        if (MiraiHttpAPI.Bots.containsKey(bot)) {
                                            try {
                                                MiraiHttpAPI.INSTANCE.sendGroupMessage(MiraiHttpAPI.Bots.get(bot),
                                                        group, finalFormatText);
                                            } catch (IOException | AbnormalStatusException ex) {
                                                getLogger().warning("使用" + bot + "发送消息时出现异常，原因: " + ex);
                                            }
                                        } else
                                            getLogger().warning("指定的机器人" + bot + "不存在，是否已经登录了机器人？");
                                    }
                                }));
                    }
                }.runTaskAsynchronously(this);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a已发送QQ群聊天消息！"));
                if (getConfig().getBoolean("general.command-also-broadcast-to-chat") && sender instanceof Player) {
                    Player player = (Player) sender;
                    player.chat(message.toString());
                }
            }
        }
        if (command.getName().equalsIgnoreCase("chat2qq")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("miraimc.command.chat2qq")) {
                    reloadConfig();
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a配置文件已经重新载入！"));
                } else
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c你没有足够的权限使用此命令！"));
            } else {
                sender.sendMessage("This server is running " + getDescription().getName() + " version "
                        + getDescription().getVersion() + " by "
                        + getDescription().getAuthors().toString().replace("[", "").replace("]", "")
                        + " (MiraiMC version "
                        + Bukkit.getPluginManager().getPlugin("MiraiMC").getDescription().getVersion() + ")");
            }
        }
        return true;
    }
}
