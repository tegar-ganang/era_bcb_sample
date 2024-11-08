package org.timothyb89.jtelirc.karma;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.timothyb89.jtelirc.channel.Channel;
import org.timothyb89.jtelirc.channel.ChannelListener;
import org.timothyb89.jtelirc.message.Message;
import org.timothyb89.jtelirc.message.UserMessage;
import org.timothyb89.jtelirc.user.User;

/**
 *
 * @author tim
 */
public class KarmaListener implements ChannelListener {

    private XStream xstream;

    private Channel channel;

    /**
	 * The KarmaDatabase for this channel.
	 */
    private KarmaDatabase database;

    /**
	 * Creates a new KarmaListener for the given channel.
	 * @param channel The channel the new KarmaListener will listen to
	 */
    public KarmaListener(Channel channel) {
        this.channel = channel;
        xstream = new XStream();
        xstream.processAnnotations(KarmaDatabase.class);
        xstream.processAnnotations(KarmaEntry.class);
        File dbf = getDatabaseFile();
        if (dbf.exists()) {
            loadDatabase(dbf);
        } else {
            database = new KarmaDatabase();
            saveDatabase();
        }
        channel.addListener(this);
    }

    /**
	 * Gets the channel this KarmaListener is for.
	 * @return The channel this KarmaListener is for.
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * Gets the KarmaDatabase for this KarmaListener.
	 * @return The current KarmaDatabase.
	 */
    public KarmaDatabase getDatabase() {
        return database;
    }

    /**
	 * Called when a new message has been received in the channel. This checks
	 * the message for the karma syntax (nick++, nick--) and handles it
	 * appropriately.
	 * @param c The channel the message was sent to.
	 * @param message The message sent
	 */
    public void onChannelMessageReceived(Channel c, Message message) {
        if (message instanceof UserMessage) {
            UserMessage m = (UserMessage) message;
            User user = m.getUser();
            String text = m.getText();
            Pattern pat = Pattern.compile("([\\S]+)(\\+\\+|\\-\\-|\\+\\=|\\-\\=)(\\d+)?");
            Matcher mat = pat.matcher(text);
            if (mat.matches()) {
                for (User u : channel.getUsers()) {
                    if (mat.group(1).equalsIgnoreCase(u.getNick())) {
                        if (u.equals(m.getUser())) {
                            m.reply("You can't change your own karma!");
                            break;
                        }
                        KarmaEntry entry = database.getEntry(u.getNick());
                        if (entry == null) {
                            entry = new KarmaEntry(u);
                            database.addEntry(entry);
                        }
                        String op = mat.group(2);
                        if (op.equals("++")) {
                            entry.givePositive(user);
                        } else if (op.equals("--")) {
                            entry.giveNegative(user);
                        } else if (op.equals("+=")) {
                            entry.givePositive(Integer.parseInt(mat.group(3)), u);
                        } else if (op.equals("-=")) {
                            entry.giveNegative(Integer.parseInt(mat.group(3)), u);
                        }
                        saveDatabase();
                        break;
                    }
                }
            }
            pat = Pattern.compile("([\\S]+)(\\+\\=|\\-\\=)(\\d+)");
            mat = pat.matcher(text);
            if (mat.matches()) {
                if (!user.isAdmin()) {
                    m.reply("Sorry, admins only.");
                    return;
                }
                for (User u : channel.getUsers()) {
                    if (mat.group(1).equalsIgnoreCase(u.getNick())) {
                        if (u.equals(m.getUser())) {
                            m.reply("You can't change your own karma!");
                            break;
                        }
                        KarmaEntry entry = database.getEntry(u.getNick());
                        if (entry == null) {
                            entry = new KarmaEntry(u);
                            database.addEntry(entry);
                        }
                        String op = mat.group(2);
                        if (op.equals("++")) {
                            entry.givePositive(user);
                        } else if (op.equals("--")) {
                            entry.giveNegative(user);
                        }
                    }
                }
            }
        }
    }

    /**
	 * Gets the file containing the karma database.
	 * @return The karma database file for the current channel
	 */
    public File getDatabaseFile() {
        return new File("conf/karma/" + channel.getName() + ".xml");
    }

    /**
	 * Loads the karma database from the given file
	 * @param file The file to load the karma database from
	 */
    public void loadDatabase(File file) {
        try {
            if (file.exists()) {
                FileReader in = new FileReader(file);
                Object obj = xstream.fromXML(in);
                in.close();
                if (obj instanceof KarmaDatabase) {
                    database = (KarmaDatabase) obj;
                } else {
                    System.err.println("Invalid karma database!");
                }
            }
        } catch (Exception ex) {
            System.err.println("Couldn't load karma database");
        }
    }

    /**
	 * Saves the karma database to the database file (getDatabaseFile()).
	 */
    public void saveDatabase() {
        try {
            if (database == null) {
                System.err.println("Can't save null karma database, skipping...");
                return;
            }
            File f = getDatabaseFile();
            File p = f.getParentFile();
            if (!p.exists()) {
                p.mkdirs();
            }
            if (f.exists()) {
                f.delete();
            }
            FileWriter out = new FileWriter(f);
            xstream.toXML(database, out);
            out.close();
        } catch (Exception ex) {
            System.err.println("Couldn't save karma database: " + channel.getName());
        }
    }

    public void onChannelTopicChanged(Channel c, String topic, User u) {
    }

    public void onChannelUserJoined(Channel c, User u) {
    }

    public void onChannelUserLeft(Channel c, User u, String msg, boolean quit) {
    }

    public void onChannelModeChanged(Channel c, String mode, String param, boolean state) {
    }
}
