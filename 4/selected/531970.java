package servers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import org.grlea.log.SimpleLogger;

public class SMTPServer implements IServer {

    private static final SimpleLogger log = new SimpleLogger(SMTPServer.class);

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        log.entry("main");
        SMTPServer ps = new SMTPServer();
        log.exit("main");
    }

    public SMTPServer() {
    }

    public void listen() {
        try {
            int port = 25;
            ServerSocket srv = new ServerSocket(port);
            int i = 0;
            log.debug("starting to listen on port" + port);
            while (i++ < 2) {
                Socket socket = srv.accept();
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                System.out.println("got a req");
                bw.write("220 POPAnything SMTP Server is ready n kicking\r\n");
                bw.flush();
                SMTPService ss = new SMTPService();
                ss.setSocket(socket);
                ss.run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
