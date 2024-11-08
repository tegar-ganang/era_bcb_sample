package tsr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Reads teamspeak information aloud.
 * 
 * @author andiinthehouse
 */
public class TeamspeakReader {

    /**
     * @param args three cmdline arguments: "hostname or ip" "udp port" "target name"
     * @throws InterruptedException
     */
    public static void main(final String[] args) {
        if (args.length < 3) {
            System.out.println("usage: java -jar tsr.jar <hostname or ip> <udp port> <target name>");
            System.exit(0);
        }
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String targetName = args[2];
        AudioPlayer.play(AudioPlayer.introSound);
        for (final String s : getPlayersInMyChannel(host, port, targetName)) {
            AudioPlayer.play(s);
        }
    }

    /**
     * Returns a list of player names that are in the channel with the specified player.
     * 
     * @param myName the target player
     * @return list of player names
     */
    public static List<String> getPlayersInMyChannel(String host, int udpport, final String myName) {
        final List<String> players = new ArrayList<String>();
        final Map<String, Integer> playersWithChannels = new HashMap<String, Integer>();
        TeamspeakInfo tsi = TeamspeakConnector.getTeamSpeakInfo(host, udpport);
        for (final PlayerInfo pi : tsi.getPi()) {
            final String name = pi.getNick().replaceAll("\\\\|\\/|:|\\*|\\?|\"|<|>|\\|| ", "");
            final Integer channel = pi.getChannelId();
            playersWithChannels.put(name, channel);
        }
        System.out.println(playersWithChannels);
        final Integer myChannel = playersWithChannels.get(myName);
        for (final Entry<String, Integer> e : playersWithChannels.entrySet()) {
            if (e.getValue().equals(myChannel)) players.add(e.getKey());
        }
        System.out.println(players);
        return players;
    }
}
