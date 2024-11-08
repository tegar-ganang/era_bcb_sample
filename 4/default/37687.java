import java.io.*;
import java.net.*;

/**
 * The IRC server class.
 * Contains info about connected server, and info to read/write to channels.
 * Method to actually connect to server is missing, it is done externally via
 * read and write methods (bad code - needs cleaning)
 * 
 * Contains information that will be specific to each server.
 * @author James McMahon
 *
 */
public class IRCServer {

    private String nick = "";

    private String login = "";

    private Socket socket;

    private BufferedWriter writer;

    private BufferedReader reader;

    private String[] channels = new String[10];

    private IRCMessage msg = new IRCMessage();

    private IRCInput input = new IRCInput();

    /**
	 * The constructor method.
	 * Connects to the server (and channels)
	 * @param server The server to be connected to
	 * @param port The port on the server to connect to
	 * @param nick The user's nick
	 * @param login Something to do with connecting
	 * @throws Exception I have no idea what this does
	 */
    public IRCServer(String server, int port, String nick, String login, String[] channels) throws Exception {
        this.nick = nick;
        this.login = login;
        this.socket = new Socket(server, port);
        this.channels = channels;
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
	 * Sets a new Nick
	 * @param nick The new nick
	 * @throws IOException It seems this is needed wherever there is a read or write
	 */
    public void setNick(String nick) throws IOException {
        this.nick = nick;
        writeline("NICK " + nick + "\r\n");
    }

    /**
	 * Connect to the server and channel
	 * @throws IOExcepton Still don't know why we do this
	 */
    public void connect() throws IOException {
        writer.write("NICK " + nick + "\r\n");
        writer.write("USER " + login + " boo boo :IRhat \r\n");
        writer.flush();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.indexOf("004") >= 0) {
                break;
            } else if (line.indexOf("433") >= 0) {
                System.out.println("Nickname is already in use.");
                return;
            } else if (line.startsWith("PING ")) {
                writer.write("PONG " + line.substring(5) + "\r\n");
                writer.flush();
                System.out.println(line);
            } else System.out.println(line);
        }
        writer.write("JOIN " + channels[0] + "\r\n");
        writer.flush();
    }

    /**
	 * Reads a line from the server and passes to IRCMessage for parsing
	 * @return the output of the parse, or "no output" if the output was sent to the server (e.g. for a pong)
	 * @throws IOException
	 */
    public String readline() throws IOException {
        msg.setRaw(reader.readLine());
        if (msg.parse()) return msg.getOutput(); else {
            writeline(msg.getOutput());
            return "no output";
        }
    }

    /**
	 * Writes the given string to the channel
	 * @param output the string to go to the channel
	 * @throws IOException
	 */
    public void writeline(String output) throws IOException {
        writer.write(output);
        writer.flush();
    }

    public String getChannel() {
        return channels[0];
    }
}
