package org.fuin.auction.infrastructure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import org.apache.activemq.console.Main;
import org.apache.commons.io.IOUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

/**
 * Helper class that starts the necessary infrastructure components to run the
 * Axon Auction Example.
 */
public final class InfrastructureStart {

    /**
	 * Private constructor to avoid instantiation.
	 */
    private InfrastructureStart() {
        throw new UnsupportedOperationException("You cannot create an instance of a utility class!");
    }

    /**
	 * Starts the application.
	 * 
	 * @param args
	 *            Command line arguments.
	 */
    public static void main(final String[] args) {
        final Runnable startDerby = new Runnable() {

            public void run() {
                try {
                    final NetworkServerControl control = new NetworkServerControl(InetAddress.getByName("localhost"), 1527);
                    control.start(new PrintWriter(System.out));
                } catch (final Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        new Thread(startDerby).start();
        final Runnable startActiveMq = new Runnable() {

            public void run() {
                Main.main(new String[] { "start", "xbean:file:active-mq-config.xml" });
            }
        };
        new Thread(startActiveMq).start();
        final Runnable startMailServer = new Runnable() {

            public void run() {
                final SimpleMessageListener listener = new SimpleMessageListener() {

                    public final boolean accept(final String from, final String recipient) {
                        return true;
                    }

                    public final void deliver(final String from, final String recipient, final InputStream data) throws TooMuchDataException, IOException {
                        System.out.println("FROM: " + from);
                        System.out.println("TO: " + recipient);
                        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
                        final File file = new File(tmpDir, recipient);
                        final FileWriter fw = new FileWriter(file);
                        try {
                            IOUtils.copy(data, fw);
                        } finally {
                            fw.close();
                        }
                    }
                };
                final SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(listener));
                smtpServer.start();
                System.out.println("Started SMTP Server");
            }
        };
        new Thread(startMailServer).start();
    }
}
