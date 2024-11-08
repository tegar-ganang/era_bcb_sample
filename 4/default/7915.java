import java.io.IOException;
import java.util.Scanner;

/**
 * A simple CLI IRC client at the moment
 * @author James McMahon
 *
 */
public class IRHat {

    /**
	 * ReadLoop class (so can thread read and auto reply while waiting for input)
	 * @author James McMahon
	 *
	 */
    private static class ReadLoop implements Runnable {

        private IRCServer server;

        /**
		 * The Constructor
		 * @param server the server to read from
		 */
        public ReadLoop(IRCServer server) {
            this.server = server;
        }

        /**
		 * constantly reads lines from server. Outputs the output if required
		 */
        public void run() {
            String line = "";
            while (true) try {
                if (!(line = server.readline()).equals("no output")) System.out.println(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class WriteLoop implements Runnable {

        private IRCServer server;

        /**
		 * Constructor to give access to writer
		 * @param writer the server's writer
		 */
        public WriteLoop(IRCServer server) {
            this.server = server;
        }

        public void run() {
            Scanner scanner = new Scanner(System.in);
            String input = "";
            while (true) {
                if ((input = scanner.nextLine()) != null) {
                    try {
                        server.writeline("PRIVMSG " + server.getChannel() + " :" + input + "\r\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
	 * 
	 * @param args server channel - always uses port 6667 and only connects to one channel
	 * if args are missing defaults to irc.sinirc.org/#irhat
	 * if only channel is missing defaults to #irhat on the selected server
	 * 
	 * @throws Exception Dunno really
	 */
    public static void main(String[] args) throws Exception {
        String server;
        String[] channels = new String[10];
        if (args[0] != null) {
            server = args[0];
        } else {
            server = "irc.synirc.org";
        }
        if (args[1] != null) {
            for (int i = 1; i < args.length; i++) {
                channels[i - 1] = args[i];
            }
        } else channels[0] = "#irhat";
        IRCServer ircServer = new IRCServer(server, 6667, "IRHat", "IRHat", channels);
        ircServer.connect();
        Thread r = new Thread(new ReadLoop(ircServer));
        r.start();
        Thread w = new Thread(new WriteLoop(ircServer));
        w.start();
    }
}
