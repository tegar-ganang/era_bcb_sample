package mudstrate.engine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import mudstrate.init.MudEnvironment;
import mudstrate.ui.elements.Generics.uiAlert;
import mudstrate.ui.layers.UserLevel.ChatRoomLayer;
import mudstrate.user.User;
import mudstrate.util.Comm;
import mudstrate.util.Util;

public class UserHandler extends Thread {

    private static UserHandler instance = new UserHandler();

    public static synchronized UserHandler getInstance() {
        return instance;
    }

    private MudEnvironment environment = MudEnvironment.getInstance();

    private boolean keepRunning = true;

    private final int maxInputLength = 300;

    /** 
	 * Reads bytes from a <code>User</code>'s channel. 
	 **/
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private final int selectionInterest = SelectionKey.OP_READ;

    /** Contains all <code>User</code>s which have been registered with the UserHandler (their channels are registered with the selector). **/
    private HashMap<Integer, User> usersById;

    private Selector userSelector;

    private UserHandler() {
        super("UserHandler");
        boolean success = false;
        this.usersById = new HashMap<Integer, User>();
        try {
            this.userSelector = Selector.open();
            success = true;
        } catch (IOException e) {
            Util.echo("Could not open selector.", this);
            success = false;
        }
        if (success) {
            Util.echo("Initialization successful.", this);
        } else {
            this.environment.shutdown();
        }
    }

    /**
	 * Adds the <code>User</code> to the <code>UserHandler</code>'s <code>users</code> and wakes <br>
	 * up the <code>Selector</code>.
	 * @param newUser - <code>User</code> to add.
	 */
    public synchronized void addUser(User newUser) {
        this.usersById.put(newUser.getId(), newUser);
        this.userSelector.wakeup();
        ChatRoomLayer.alertChatters(uiAlert.display(newUser.getUsername() + " has just logged in."));
        Util.echo("'" + newUser.getUsername() + "' has become user #" + this.usersById.size() + ".", this);
    }

    public synchronized void removeUser(int userId) {
        User quitter = this.usersById.get(userId);
        if (quitter != null) {
            quitter.getChannel().keyFor(this.userSelector).cancel();
            this.usersById.remove(userId);
            Util.echo("User quitting: '" + quitter.getUsername() + "'", this);
        }
    }

    /**
	 * Attempts to reconnect a <code>User</code> that is stored in the <code>UserHandler</code>'s <code>HashMap</code>.
	 * @param userId - id of the <code>User</code> to look for.
	 * @param healthyChannel - the <code>SocketChannel</code> to reconnect the <code>User</code> with.
	 * @return
	 */
    public boolean attemptReconnect(int userId, SocketChannel healthyChannel) {
        User targetUser = this.usersById.get(userId);
        if (targetUser != null) {
            if (targetUser.isLinkless()) {
                Util.echo("Reconnecting linkless user: '" + targetUser.getUsername() + "'.", this);
                targetUser.setChannel(healthyChannel);
                targetUser.send(uiAlert.display("You have been reconnected.") + "\n");
                targetUser.getCurrentLayer().display();
                this.userSelector.wakeup();
            } else {
                Util.echo("Outside channel attempting to hack user: '" + targetUser.getUsername() + "'. Disconnecting hacker...", this);
                Comm.writeToChannel(healthyChannel, "Someone is already logged in with these credentials. Disconnecting...\n");
                targetUser.send("Someone has just tried to log in with your username and password.\nYou should change your password to avoid being hacked.");
                try {
                    healthyChannel.close();
                } catch (IOException e) {
                    Comm.writeToChannel(healthyChannel, Comm.connectionError());
                    Util.echo("Could not close channel.", this);
                }
            }
            return true;
        }
        return false;
    }

    public User findUserById(int userId) {
        return this.usersById.get(userId);
    }

    public ArrayList<User> getAllUsers() {
        return new ArrayList<User>(this.usersById.values());
    }

    public boolean isOnline() {
        return this.keepRunning;
    }

    public void shutdown() {
        this.keepRunning = false;
        this.userSelector.wakeup();
        Util.echo("Shutting down...", this);
    }

    /**
	 * Returns a list of command <code>String</code>s obtained from the specified <code>LoginTracker</code> or <code>User</code>'s <br> attachment().inputBuffer. Note: inputBuffer is edited as actual commands are read from it. It's remaining contents will only be input that is not yet a command.
	 * @return
	 */
    public ArrayList<String> parseCommands(SelectionKey source) {
        ArrayList<String> commands = new ArrayList<String>();
        Object u = source.attachment();
        try {
            Field inputBufferField = u.getClass().getField("inputBuffer");
            StringBuffer inputBuffer = (StringBuffer) inputBufferField.get(u);
            Util.echo("Parsing commands for: " + u.getClass().getSimpleName(), this);
            boolean newline;
            boolean carriage;
            boolean backspace;
            for (int i = 0; i < inputBuffer.length(); i++) {
                char thisChar = inputBuffer.charAt(i);
                newline = thisChar == '\n';
                carriage = thisChar == '\r';
                backspace = thisChar == '\b';
                if (newline || carriage) {
                    commands.add(inputBuffer.substring(0, i));
                    if ((i + 1) < inputBuffer.length()) {
                        char nextChar = inputBuffer.charAt(i + 1);
                        if ((newline && nextChar == '\r') || (carriage && nextChar == '\n')) {
                            i++;
                        }
                    }
                    inputBuffer.delete(0, i + 1);
                    i = 0;
                } else if (backspace) {
                    inputBuffer.delete(Math.max(0, --i), i + 2);
                }
            }
            inputBufferField.set(u, inputBuffer);
        } catch (SecurityException e) {
            Util.echo("Failed to access " + u.getClass().getSimpleName() + "'s inputBuffer: " + e.getMessage(), this);
        } catch (IllegalArgumentException e) {
            Util.echo("Failed to access " + u.getClass().getSimpleName() + "'s inputBuffer: " + e.getMessage(), this);
        } catch (NoSuchFieldException e) {
            Util.echo("Failed to access " + u.getClass().getSimpleName() + "'s inputBuffer: " + e.getMessage(), this);
        } catch (IllegalAccessException e) {
            Util.echo("Failed to access " + u.getClass().getSimpleName() + "'s inputBuffer: " + e.getMessage(), this);
        }
        return commands;
    }

    /**
	 * Looks at each <code>key</code> that has been selected by the <code>userSelector</code> and <br> stores the <code>User</code>'s input in their <code>inputBuffer</code> to be processed later.<br> This is also where commands that have been parsed from input are<br>sent through an interpreter to then perform actions associated with said commands.
	 */
    private void interpretCommands() {
        SelectionKey key;
        User thisUser;
        int bytesRead;
        ArrayList<String> commands;
        Iterator<SelectionKey> keyIterator = this.userSelector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            key = keyIterator.next();
            keyIterator.remove();
            thisUser = (User) key.attachment();
            Util.echo("Storing input from user: '" + thisUser.getUsername() + "'", this);
            try {
                bytesRead = thisUser.getChannel().read(readBuffer);
                if (bytesRead == -1) {
                    Util.echo("Disconnecting user '" + thisUser.getUsername() + "': no bytes to read.", this);
                    thisUser.disconnect();
                    return;
                } else if (bytesRead >= this.maxInputLength) {
                    readBuffer.clear();
                    Comm.writeToChannel(thisUser.getChannel(), uiAlert.display("Input too long. Disconnecting..."));
                    thisUser.disconnect();
                    return;
                }
                thisUser.inputBuffer.append(Comm.bytesToStr(readBuffer));
                commands = this.parseCommands(key);
                for (String command : commands) {
                    command = Util.trimWhiteSpace(command);
                    Util.echo("Command: '" + command + "'", this);
                    if (!thisUser.getCurrentLayer().interpretCommand(command)) {
                        Util.echo("Command was not recognized. User: '" + thisUser.getUsername() + "' - uiLayer: " + thisUser.getCurrentLayer().name + " - Command: '" + command + "'", this);
                        thisUser.send("Huh?\n");
                    }
                }
            } catch (IOException e) {
                Util.echo("Error in handling user input. Moving on...", this);
            }
        }
    }

    private void registerUsers() {
        Iterator<User> uIterator = this.usersById.values().iterator();
        while (uIterator.hasNext()) {
            User thisUser = uIterator.next();
            try {
                if (!this.userSelector.keys().contains(thisUser.getChannel().keyFor(this.userSelector)) && !thisUser.isLinkless()) {
                    thisUser.getChannel().register(this.userSelector, this.selectionInterest, thisUser);
                    Util.echo(thisUser.getUsername() + " registered with selector.", this);
                }
            } catch (ClosedChannelException e) {
                Util.echo("Cannot register a closed channel. Moving to next user.", this);
                continue;
            }
        }
    }

    @Override
    public void run() {
        while (this.keepRunning == true) {
            try {
                Util.echo("Ready to accept users and parse input...", this);
                this.registerUsers();
                int usersReady = this.userSelector.select();
                if (usersReady > 0) {
                    this.interpretCommands();
                }
                this.userSelector.selectedKeys().clear();
            } catch (IOException e) {
                Util.echo("Malfunction: " + e.getMessage(), this);
                this.environment.shutdown();
            }
        }
    }
}
