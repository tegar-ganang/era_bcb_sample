package mudstrate.user;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import mudstrate.engine.LoginTracker;
import mudstrate.engine.UserHandler;
import mudstrate.mudobj.mobile.Player;
import mudstrate.ui.layers.uiLayer;
import mudstrate.util.Format;
import mudstrate.util.Util;

/**
 * An <code>User</code> represents the top level of a user's presence in game. <br>
 * This class holds general information about the user including the following: <br><br><b>
 * - Username<br>
 * - Unique Vnum number<br>
 * - List of vnums for each player registered with this <code>User</code><br>
 * - The <code>SocketChannel</code> by which this <code>User</code> is logged in<br>
 * - A <code>uiLayer</code> which represents the currently occupied <code>uiLayer</code><br>
 * - A <code>LinkedList</code> of <code>uiLayer</code>s which represents the <code>User</code>'s layer stack<br>
 * - An <code>int</code> which represents the Vnum of the player last played on this <code>User</code><br>
 * - A <code>Player</code> object which represents the physical player being played by the user<br>
 * @author Taylor
 *
 */
public class User {

    public static void send(String display, Collection<User> users) {
        for (User u : users) {
            u.send(display);
        }
    }

    private SocketChannel channel;

    private uiLayer currentLayer;

    private int id = -1;

    private Player currentPlayer;

    public volatile StringBuffer inputBuffer = new StringBuffer();

    public volatile StringBuffer outputBuffer = new StringBuffer();

    private int lastPlayedPlayerVnum;

    private LinkedList<uiLayer> layerStack = new LinkedList<uiLayer>();

    private String password;

    private ArrayList<Integer> playerVnums = new ArrayList<Integer>();

    private String username;

    private int godStatus = 0;

    public User(LoginTracker login) {
        this.username = login.getUsername();
        this.password = login.getPassword();
        if (login.isNewUser()) {
        }
    }

    public void disconnect() {
        try {
            this.channel.close();
        } catch (IOException e) {
            Util.echo("Failed to close SocketChannel. User: (" + this.username + ")", this);
            return;
        }
    }

    public boolean isLinkless() {
        return !this.channel.isConnected();
    }

    /**
	 * Places the currently occupied <code>uiLayer</code> on top of the <code>layerStack</code>, sets the <br>
	 * specified <code>uiLayer</code> as this <code>User</code>'s currently occupied layer, sets the specified <br>
	 * <code>uiLayer</code>'s <code>user</code> to this <code>User</code>, and sends the new <code>uiLayer</code>'s <code>display()</code><br> to the <code>User</code>'s channel.
	 * @param layer <br>The <code>uiLayer</code> to move into.
	 * @return The newly occupied <code>uiLayer</code>.
	 */
    public uiLayer moveIntoLayer(uiLayer layer) {
        if (!layer.testPreCondition(this)) {
            layer.respondToFailedPreCondition(this);
            Util.echo("User failed " + layer.getClass().getSimpleName() + " layer precondition: '" + this.username + "'", this);
            return this.currentLayer;
        }
        if (this.currentLayer != null) {
            this.layerStack.push(this.currentLayer);
        }
        this.currentLayer = layer;
        this.currentLayer.setUser(this);
        this.currentLayer.display();
        Util.echo("Moving user into " + layer.getClass().getSimpleName() + ": '" + this.username + "'", this);
        return this.currentLayer;
    }

    /**
	 * Peels off the current <code>uiLayer</code> by removing the top layer in the stack <br>
	 * and setting it as the current layer.
	 */
    public void peelOffLayer() {
        if (this.layerStack.peek() != null) {
            this.currentLayer = this.layerStack.pop();
            Util.echo("Backing user into " + this.currentLayer.getClass().getSimpleName() + ": '" + this.username + "'", this);
            this.currentLayer.display();
        } else {
            this.quit();
        }
    }

    public void quit() {
        UserHandler.getInstance().removeUser(this.id);
        this.send("Until next time, " + this.username + ". Goodbye.");
        this.disconnect();
    }

    /**
	 * Simply appends the specified <code>String</code> to this <code>User</code>'s outputBuffer.
	 * @param display
	 */
    public void send(String display) {
        this.outputBuffer.append(display);
        return;
    }

    public SocketChannel getChannel() {
        return this.channel;
    }

    public uiLayer getCurrentLayer() {
        return this.currentLayer;
    }

    public int getId() {
        if (this.id == -1) {
        }
        return this.id;
    }

    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    public int getLastPlayedPlayerVnum() {
        return this.lastPlayedPlayerVnum;
    }

    public LinkedList<uiLayer> getLayerStack() {
        return this.layerStack;
    }

    public String getPassword() {
        return this.password;
    }

    public ArrayList<Integer> getPlayerVnums() {
        return this.playerVnums;
    }

    public String getUsername() {
        return this.username;
    }

    public int getGodStatus() {
        return this.godStatus;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public void setCurrentLayer(uiLayer currentLayer) {
        this.currentLayer = currentLayer;
        this.currentLayer.setUser(this);
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCurrentPlayer(Player inGamePlayer) {
        this.currentPlayer = inGamePlayer;
        this.currentPlayer.setUser(this);
    }

    public void setLastPlayedPlayerVnum(int lastPlayedPlayerVnum) {
        this.lastPlayedPlayerVnum = lastPlayedPlayerVnum;
    }

    public void setLayerStack(LinkedList<uiLayer> layerStack) {
        this.layerStack = layerStack;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPlayerVnums(ArrayList<Integer> playerVnums) {
        this.playerVnums = playerVnums;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setGodStatus(int i) {
        this.godStatus = i;
    }
}
