package nint22.basicbukkit;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;
import com.sk89q.bukkit.migration.PermissionsProvider;

public class BasicBukkit extends JavaPlugin implements PermissionsProvider {

    private BasicPlayerListener playerListener = null;

    private BasicBlockListener blockListener = null;

    private BasicEntityListener entityListener = null;

    private BasicVehicleListener vehicleListener = null;

    public Configuration configuration = null;

    public BasicUsers users = null;

    public BasicProtection protections = null;

    public BasicWarps warps = null;

    public BasicMessages messages = null;

    public BasicDaemon daemon = null;

    public BasicItems itemNames = null;

    public BasicLocks locks = null;

    public BasicRoleplay roleplay = null;

    private HashMap<String, Long> MessageTime;

    private File loadFile(String fileName) {
        File BasicDirectory = new File("plugins/BasicBukkit/");
        if (!BasicDirectory.exists()) {
            BasicDirectory.mkdir();
            System.out.println("### BasicBukkut has created the BasicBukkit plugin directory");
        }
        File config = new File("plugins/BasicBukkit/" + fileName);
        if (!config.exists()) {
            InputStream defaultFile = getClass().getClassLoader().getResourceAsStream(fileName);
            try {
                System.out.println("### BasicBukkit did not detect a config file: createed new file \"" + fileName + "\"");
                BufferedWriter out = new BufferedWriter(new FileWriter("plugins/BasicBukkit/" + fileName));
                while (defaultFile.available() > 0) out.write(defaultFile.read());
                out.close();
            } catch (Exception e) {
                System.out.println("### BasicBukkit warning: " + e.getMessage());
            }
            config = new File("plugins/BasicBukkit/" + fileName);
        }
        return config;
    }

    @Override
    public void onDisable() {
        messages.stop(true);
        daemon.stop(true);
        users.save();
        protections.save();
        warps.save();
        locks.save();
        roleplay.save();
        System.out.println("### BasicBukkit plugin disabled.");
    }

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        loadFile("config_sample_rpg.yml");
        configuration = new Configuration(loadFile("config.yml"));
        configuration.load();
        itemNames = new BasicItems(loadFile("items.csv"), configuration);
        users = new BasicUsers(this, new Configuration(loadFile("users.yml")), configuration);
        protections = new BasicProtection(new Configuration(loadFile("protections.yml")));
        warps = new BasicWarps(this, new Configuration(loadFile("warps.yml")));
        messages = new BasicMessages(this, configuration);
        daemon = new BasicDaemon(this, configuration);
        locks = new BasicLocks(this, new Configuration(loadFile("locks.yml")));
        roleplay = new BasicRoleplay(this, new Configuration(loadFile("experiance.yml")));
        MessageTime = new HashMap();
        playerListener = new BasicPlayerListener(this);
        pm.registerEvent(Event.Type.PLAYER_PRELOGIN, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_DROP_ITEM, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        blockListener = new BasicBlockListener(this);
        pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_FROMTO, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_IGNITE, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.BLOCK_BURN, blockListener, Priority.Normal, this);
        entityListener = new BasicEntityListener(this);
        pm.registerEvent(Event.Type.EXPLOSION_PRIME, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Normal, this);
        vehicleListener = new BasicVehicleListener(this);
        pm.registerEvent(Event.Type.VEHICLE_CREATE, vehicleListener, Priority.Normal, this);
        BasicMiscCommands MiscCommands = new BasicMiscCommands(this);
        getCommand("help").setExecutor(MiscCommands);
        getCommand("motd").setExecutor(MiscCommands);
        getCommand("clear").setExecutor(MiscCommands);
        getCommand("where").setExecutor(MiscCommands);
        getCommand("afk").setExecutor(MiscCommands);
        getCommand("msg").setExecutor(MiscCommands);
        getCommand("pm").setExecutor(MiscCommands);
        getCommand("mute").setExecutor(MiscCommands);
        getCommand("title").setExecutor(MiscCommands);
        BasicAdminCommands AdminCommands = new BasicAdminCommands(this);
        getCommand("op").setExecutor(AdminCommands);
        getCommand("vote").setExecutor(AdminCommands);
        getCommand("vkick").setExecutor(AdminCommands);
        getCommand("vban").setExecutor(AdminCommands);
        getCommand("kick").setExecutor(AdminCommands);
        getCommand("ban").setExecutor(AdminCommands);
        getCommand("unban").setExecutor(AdminCommands);
        getCommand("unkick").setExecutor(AdminCommands);
        getCommand("who").setExecutor(AdminCommands);
        getCommand("time").setExecutor(AdminCommands);
        getCommand("weather").setExecutor(AdminCommands);
        getCommand("kill").setExecutor(AdminCommands);
        getCommand("say").setExecutor(AdminCommands);
        getCommand("god").setExecutor(AdminCommands);
        getCommand("pvp").setExecutor(AdminCommands);
        getCommand("iclean").setExecutor(AdminCommands);
        getCommand("mclean").setExecutor(AdminCommands);
        getCommand("scout").setExecutor(AdminCommands);
        getCommand("hide").setExecutor(AdminCommands);
        BasicItemCommands ItemCommands = new BasicItemCommands(this);
        getCommand("kit").setExecutor(ItemCommands);
        getCommand("item").setExecutor(ItemCommands);
        getCommand("i").setExecutor(ItemCommands);
        getCommand("give").setExecutor(ItemCommands);
        getCommand("clean").setExecutor(ItemCommands);
        getCommand("cleanall").setExecutor(ItemCommands);
        BasicWorldCommands WorldCommands = new BasicWorldCommands(this);
        getCommand("tp").setExecutor(WorldCommands);
        getCommand("warp").setExecutor(WorldCommands);
        getCommand("list").setExecutor(WorldCommands);
        getCommand("setwarp").setExecutor(WorldCommands);
        getCommand("delwarp").setExecutor(WorldCommands);
        getCommand("home").setExecutor(WorldCommands);
        getCommand("sethome").setExecutor(WorldCommands);
        getCommand("spawn").setExecutor(WorldCommands);
        getCommand("setspawn").setExecutor(WorldCommands);
        getCommand("top").setExecutor(WorldCommands);
        getCommand("jump").setExecutor(WorldCommands);
        getCommand("mob").setExecutor(WorldCommands);
        BasicProtectionCommands Protection = new BasicProtectionCommands(this);
        getCommand("p1").setExecutor(Protection);
        getCommand("p2").setExecutor(Protection);
        getCommand("protect").setExecutor(Protection);
        getCommand("protectadd").setExecutor(Protection);
        getCommand("protectrem").setExecutor(Protection);
        getCommand("protectdel").setExecutor(Protection);
        getCommand("protectpvp").setExecutor(Protection);
        getCommand("protectlock").setExecutor(Protection);
        getCommand("protectinfo").setExecutor(Protection);
        getCommand("lock").setExecutor(Protection);
        getCommand("unlock").setExecutor(Protection);
        if (configuration.getBoolean("roleplay", false)) {
            BasicRoleplayCommands Roleplay = new BasicRoleplayCommands(this);
            getCommand("level").setExecutor(Roleplay);
            getCommand("exp").setExecutor(Roleplay);
            getCommand("ranks").setExecutor(Roleplay);
            getCommand("addexp").setExecutor(Roleplay);
            getCommand("remexp").setExecutor(Roleplay);
            getCommand("setexp").setExecutor(Roleplay);
        }
        getServer().setSpawnRadius(0);
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println("### BasicBukkiet (v." + pdfFile.getVersion() + ") plugin enabled. ");
    }

    public String[] GetMOTD() {
        List<Object> sourceMOTD = configuration.getList("motd");
        String[] motd = new String[sourceMOTD.size() + 1];
        for (int i = 0; i < sourceMOTD.size(); i++) motd[i] = (String) sourceMOTD.get(i);
        for (int i = 0; i < sourceMOTD.size(); i++) motd[i] = ColorString(motd[i]);
        int PlayerCount = this.getServer().getOnlinePlayers().length;
        if (PlayerCount <= 1) motd[motd.length - 1] = ChatColor.GRAY + "There is currently " + ChatColor.RED + PlayerCount + ChatColor.GRAY + " player online"; else motd[motd.length - 1] = ChatColor.GRAY + "There are currently " + ChatColor.RED + PlayerCount + ChatColor.GRAY + " players online";
        return motd;
    }

    public boolean IsCommand(Player player, Command command, String[] args, String commandName) {
        if (command.getName().compareToIgnoreCase(commandName) == 0) {
            String argsList = "[";
            for (int i = 0; i < args.length; i++) {
                argsList += args[i];
                if (i != args.length - 1) argsList += ", ";
            }
            argsList += "]";
            System.out.println(player.getName() + ": /" + commandName + " " + argsList);
            if (!users.CanExecute(player.getName(), commandName)) {
                System.out.println(player.getName() + ": Group \"" + users.GetGroupName(player.getName()) + "\" (GID " + users.GetGroupID(player.getName()) + ") cannot use this command.");
                player.sendMessage(ChatColor.RED + "Your group \"" + users.GetGroupName(player.getName()) + "\" (GID " + users.GetGroupID(player.getName()) + ") cannot use this command.");
                return false;
            }
            return true;
        }
        return false;
    }

    public String ColorString(String message) {
        return message.replaceAll("&([0-9a-f])", (char) 0xA7 + "$1");
    }

    public void BroadcastMessage(String message) {
        message = ColorString(message);
        System.out.println("Server log: " + message);
        getServer().broadcastMessage(message);
    }

    public void SendMessage(Player player, String message) {
        String key = player.getName() + "_" + message;
        long epochNow = System.currentTimeMillis() / 1000;
        boolean canSend = false;
        if (MessageTime.containsKey(key)) {
            Long epochTime = MessageTime.get(key);
            if (epochNow > epochTime.longValue()) {
                MessageTime.remove(key);
                MessageTime.put(key, new Long(epochNow + 3));
                canSend = true;
            }
        } else {
            MessageTime.put(key, new Long(epochNow + 3));
            canSend = true;
        }
        if (canSend) player.sendMessage(message);
    }

    public boolean CanSendChat(Player player, String message) {
        String key = "chat_" + player.getName() + "_" + message;
        long epochNow = System.currentTimeMillis() / 1000;
        boolean canSend = false;
        if (MessageTime.containsKey(key)) {
            Long epochTime = (Long) MessageTime.get(key);
            if (epochTime.longValue() <= epochNow) {
                MessageTime.remove(key);
                canSend = true;
            }
        } else {
            canSend = true;
            MessageTime.put(key, new Long(epochNow + 1));
        }
        return canSend;
    }

    /*** WorldEdit permissions system ***/
    @Override
    public boolean hasPermission(String string, String string1) {
        return users.CanWorldEdit(string);
    }

    @Override
    public boolean inGroup(String string, String string1) {
        return true;
    }

    @Override
    public String[] getGroups(String string) {
        return null;
    }
}
