package net.WMisiedjan.WirelessRedstone;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WirelessCommands implements CommandExecutor {

    private final WirelessRedstone plugin;

    public WirelessCommands(WirelessRedstone plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        String commandName = command.getName().toLowerCase();
        Player player;
        if (!(sender instanceof Player)) {
            return true;
        }
        player = (Player) sender;
        if (commandName.equals("wrhelp")) {
            return performHelp(sender, args, player);
        } else if (commandName.equals("wrt")) {
            return performCreateTransmitter(sender, args, player);
        } else if (commandName.equals("wrr")) {
            return performCreateReceiver(sender, args, player);
        } else if (commandName.equals("wrc")) {
            return performChannelAdmin(sender, args, player);
        } else if (commandName.equals("wrremove")) {
            return performRemoveChannel(sender, args, player);
        }
        return true;
    }

    public ArrayList<String> generateCommandList(Player player) {
        ArrayList<String> commands = new ArrayList<String>();
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrt") || plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics")) {
            commands.add("/WRt channelname - Creates transmitter sign.");
        }
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrr") || plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics")) {
            commands.add("/WRr channelname - Creates receiver sign.");
        }
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrremove") || plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics")) {
            commands.add("/WRremove channelname - Removes a channel.");
        }
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrc") || plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics")) {
            commands.add("/WRc - Channel admin commands. Execute for more info.");
        }
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrlist")) {
            commands.add("/WRlist - Lists all the channels with the owners.");
        }
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrcleanup")) {
            commands.add("/WRcleanup - cleansup the database for errors and other things.");
        }
        return commands;
    }

    private boolean performWRlist(CommandSender sender, String[] args, Player player) {
        ArrayList<String> list = new ArrayList<String>();
        player.sendMessage("WirelessRedstone Channel List(" + plugin.WireBox.getChannels().size() + ")");
        for (WirelessChannel channel : plugin.WireBox.getChannels()) {
            String item = channel.getName() + ": ";
            for (String owner : channel.getOwners()) {
                item += owner + ", ";
            }
            list.add(item);
            list.add("Receivers: " + channel.getReceivers().size() + " | Transmitters: " + channel.getTransmitters().size());
        }
        if (args.length >= 1) {
            int pagenumber;
            try {
                pagenumber = Integer.parseInt(args[0]);
            } catch (Exception e) {
                player.sendMessage("This page number is not an number!");
                return true;
            }
            player.sendMessage("WirelessRedstone Channel List(" + plugin.WireBox.getChannels().size() + " Channels)");
            ShowList(list, pagenumber, player);
            player.sendMessage("/wrlist pagenumber for next page!");
            return true;
        } else {
            player.sendMessage("WirelessRedstone Channel List(" + plugin.WireBox.getChannels().size() + " Channels)");
            ShowList(list, 1, player);
            player.sendMessage("/wrlist pagenumber for next page!");
        }
        return false;
    }

    private boolean performChannelAdmin(CommandSender sender, String[] args, Player player) {
        if (args.length >= 3) {
            String channelname = args[0];
            String subcommand = args[1];
            String playername = args[2];
            if (subcommand.equalsIgnoreCase("addowner")) {
                if (plugin.WireBox.hasAccessToChannel(player, channelname)) {
                    WirelessChannel channel = plugin.WireBox.getChannel(channelname);
                    channel.addOwner(playername);
                    plugin.WireBox.SaveChannel(channel);
                    return true;
                } else {
                    player.sendMessage("[WirelessRedstone] You don't have access to this channel.");
                }
            } else if (subcommand.equalsIgnoreCase("removeowner")) {
                if (plugin.WireBox.hasAccessToChannel(player, channelname)) {
                    WirelessChannel channel = plugin.WireBox.getChannel(channelname);
                    channel.removeOwner(playername);
                    plugin.WireBox.SaveChannel(channel);
                    return true;
                } else {
                    player.sendMessage("[WirelessRedstone] You don't have access to this channel.");
                }
            } else {
                player.sendMessage("[WirelessRedstone] Unknown sub command!");
            }
        } else {
            player.sendMessage("Channel Admin Commands:");
            player.sendMessage("/WRc channelname addowner playername - Add a player to channel.");
            player.sendMessage("/WRc channelname removeowner playername - Add a player to channel.");
        }
        return true;
    }

    private boolean performHelp(CommandSender sender, String[] args, Player player) {
        if (!plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics") || !plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.help")) {
            player.sendMessage("You don't have the permissions to use this command.");
            return true;
        }
        ArrayList<String> commands = generateCommandList(player);
        if (args.length >= 1) {
            int pagenumber;
            try {
                pagenumber = Integer.parseInt(args[0]);
            } catch (Exception e) {
                player.sendMessage("This page number is not an number!");
                return true;
            }
            player.sendMessage("WirelessRedstone User Commands:");
            ShowList(commands, pagenumber, player);
            player.sendMessage("/WRhelp pagenumber for next page!");
            return true;
        } else {
            player.sendMessage("WirelessRedstone User Commands:");
            ShowList(commands, 1, player);
            player.sendMessage("/WRhelp pagenumber for next page!");
        }
        return true;
    }

    public boolean performCreateTransmitter(CommandSender sender, String[] args, Player player) {
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrt") || plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics")) {
            if (args.length >= 1) {
                String channelname = args[0];
                if (plugin.WireBox.hasAccessToChannel(player, channelname)) {
                    player.getLocation().getBlock();
                    player.getLocation().getBlock().setType(Material.SIGN_POST);
                    Sign sign = (Sign) player.getLocation().getBlock().getState();
                    sign.setLine(0, "[WRt]");
                    sign.setLine(1, channelname);
                    sign.update(true);
                    plugin.WireBox.addWirelessTransmitter(channelname, player.getLocation().getBlock(), player);
                }
            }
        }
        return true;
    }

    public boolean performRemoveChannel(CommandSender sender, String[] args, Player player) {
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrremove") || plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics")) {
            if (args.length >= 1) {
                if (plugin.WireBox.hasAccessToChannel(player, args[0])) {
                    plugin.WireBox.removeChannel(args[0]);
                }
            }
        }
        return true;
    }

    public boolean performCreateReceiver(CommandSender sender, String[] args, Player player) {
        if (plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.commands.wrr") || plugin.permissionsHandler.hasPermission(player, "WirelessRedstone.basics")) {
            if (args.length >= 1) {
                String channelname = args[0];
                if (plugin.WireBox.hasAccessToChannel(player, channelname)) {
                    player.getLocation().getBlock();
                    player.getLocation().getBlock().setType(Material.SIGN_POST);
                    Sign sign = (Sign) player.getLocation().getBlock().getState();
                    sign.setLine(0, "[WRr]");
                    sign.setLine(1, channelname);
                    sign.update(true);
                    plugin.WireBox.AddWirelessReceiver(channelname, player.getLocation().getBlock(), player);
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public void ShowList(ArrayList<String> list, int cpage, Player player) {
        int itemsonlist = list.size() + 1;
        int maxitems = 5;
        int currentpage = cpage;
        int totalpages = (int) Math.ceil(itemsonlist / maxitems);
        int currentitem = ((cpage * maxitems) - maxitems);
        player.sendMessage("Page " + currentpage + "/" + totalpages);
        if (totalpages == 0) {
            player.sendMessage("there are no items on this list!");
        } else {
            for (int i = currentitem; i < maxitems; i++) {
                player.sendMessage(list.get(i));
            }
        }
    }
}
