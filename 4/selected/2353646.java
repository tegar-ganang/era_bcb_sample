package play.go;

import java.util.Random;
import org.slim3.datastore.Datastore;
import play.go.model.Game;
import play.go.model.Player;
import play.go.rule.Board;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.Key;

public class GoServer {

    public static Game start(Player p1, Player p2) {
        Player black;
        Player white;
        if (new Random().nextBoolean()) {
            black = p2;
            white = p1;
        } else {
            black = p1;
            white = p2;
        }
        Game game = new Game();
        game.setBlack(black.getKey());
        game.setWhite(white.getKey());
        game.setBlackHama(0);
        game.setWhiteHama(0);
        game.setAb("");
        game.setAw("");
        game.setTurn(0);
        game.setPlayer(black.getKey());
        game.setGrid(new Board(9).getGrid());
        game.setRecord("");
        Datastore.put(game);
        sendMessages(game, "start");
        return game;
    }

    public static void play(Game game, Player player, int x, int y) {
        game.play(player, x, y);
        Datastore.put(game);
        sendMessages(game, "next");
    }

    public static void resign(Game game, Player player) {
        game.resign(player);
        Datastore.put(game);
        sendMessages(game, "finish");
    }

    private static void sendMessages(Game game, String command) {
        String msg = "{ \"command\": \"" + command + "\", \"gameId\": " + game.getKey().getId() + ", \"black\": " + id(game.getBlack()) + ", \"black_hama\": " + game.getBlackHama() + ", \"black_before\": " + array(game.getBlackBefore()) + ", \"white\": " + id(game.getWhite()) + ", \"white_hama\": " + game.getWhiteHama() + ", \"white_before\": " + array(game.getWhiteBefore()) + ", \"turn\": " + game.getTurn() + ", \"player\": " + id(game.getPlayer()) + ", \"grid\": " + toString(game.getGrid()) + ", \"winner\": " + id(game.getWinner()) + "}";
        System.out.println(msg);
        getChannelService().sendMessage(new ChannelMessage("" + game.getBlack().getId(), msg));
        getChannelService().sendMessage(new ChannelMessage("" + game.getWhite().getId(), msg));
    }

    private static String array(int[] a) {
        if (a == null) return "[]";
        return "[" + a[0] + "," + a[1] + "," + a[2] + "]";
    }

    static String id(Key key) {
        if (key == null) return "null";
        return "" + '"' + key.getId() + '"';
    }

    public static String toString(int[][] grid) {
        StringBuilder str = new StringBuilder();
        str.append("[");
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                str.append(grid[i][j]).append(",");
            }
        }
        str.setLength(str.length() - 1);
        str.append("]");
        return str.toString();
    }

    public static String createChannel(Player player) {
        return createChannel(player.getKey());
    }

    public static String createChannel(Key key) {
        return getChannelService().createChannel("" + key.getId());
    }

    private static ChannelService getChannelService() {
        return ChannelServiceFactory.getChannelService();
    }
}
