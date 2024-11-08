package com.fujitsu.arcon.gateway;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import org.unicore.Vsite;
import com.fujitsu.arcon.gateway.logger.Logger;
import com.fujitsu.arcon.gateway.logger.LoggerManager;

/**
 * A factory for connections that pass requests (fileTransferRequest) to a AFT
 * running inside the NJS (this is a minimal implementation).
 *
 * @author Sven van den Berghe, fujitsu
 *
 * @version $Id: AFTVsiteConnectionFactory.java,v 1.2 2004/06/06 18:41:39 svenvdb Exp $
 *
 **/
public class AFTVsiteConnectionFactory implements VsiteConnectionFactory {

    /**
     * Give the NJS class loader something to call, complete
     * initialisation is through init().
     *
     **/
    public AFTVsiteConnectionFactory() {
    }

    private String vsite_name;

    public String getName() {
        return vsite_name;
    }

    private InetAddress host_address;

    private int host_port;

    private Logger logger;

    /**
     * Initialise the instance using a definition line from the Vsites file.
     * <p>
     * Expect the init string to contain the NJS host name and  NJS port number
     *
     * @return Success of initialisation
     *
     **/
    public boolean init(Vsite vsite, String init_string) {
        logger = LoggerManager.get("com.fujitsu.arcon.gateway.VsiteConnectionFactoryImpl");
        vsite_name = vsite.getName();
        StringTokenizer st = new StringTokenizer(init_string);
        int token_count = st.countTokens();
        if (token_count < 2) {
            logger.severe("AFT Vsites initialisation line is incorrect for <" + vsite_name + "> expects at least 2 words in this part <" + init_string + ">");
            return false;
        }
        String host_name = st.nextToken();
        String s_port = st.nextToken();
        try {
            host_address = InetAddress.getByName(host_name);
        } catch (UnknownHostException uhex) {
            logger.severe("Vsite initialisation. Unknown host <" + host_name + "> for Vsite <" + vsite_name + "> ");
            return false;
        }
        try {
            host_port = (new Integer(s_port)).intValue();
        } catch (NumberFormatException nfex) {
            logger.severe("Vsite initialisation. Port number not an integer <" + s_port + "> for Vsite <" + vsite_name + "> ");
            return false;
        }
        logger.config("Vsite <" + vsite_name + "> will use <" + host_name + ":" + s_port + ">");
        return true;
    }

    public String toString() {
        return "AFT Vsite <" + vsite_name + "> at <" + host_address.getHostName() + ":" + host_port + ">";
    }

    /**
     * Get a connection to the Vsite
     *
     **/
    public VsiteConnection getConnection() throws Exception {
        return new MyVsiteConnections(host_address, host_port, vsite_name);
    }

    private class MyVsiteConnections implements VsiteConnection {

        private String vsite_name;

        private Socket to_njs;

        public MyVsiteConnections(InetAddress host_address, int host_port, String vsite_name) throws Exception {
            to_njs = new Socket(host_address, host_port);
            this.vsite_name = vsite_name;
        }

        public void processRequest(java.io.ObjectOutputStream oos, java.io.ObjectInputStream ois, java.net.Socket socket, org.unicore.upl.ServerRequest sr) throws Exception {
            ObjectOutputStream to_njs_oos = null;
            ObjectInputStream from_njs_ois = null;
            try {
                to_njs_oos = new ObjectOutputStream(to_njs.getOutputStream());
                to_njs_oos.writeObject(ois.readObject());
                to_njs_oos.writeObject(ois.readObject());
                to_njs_oos.writeObject(ois.readObject());
                to_njs_oos.flush();
                from_njs_ois = new ObjectInputStream(to_njs.getInputStream());
                Integer hdr = (Integer) from_njs_ois.readObject();
                oos.writeObject(hdr);
                if (hdr.intValue() == 0) {
                    oos.writeObject(from_njs_ois.readObject());
                    oos.writeObject(from_njs_ois.readObject());
                    oos.writeObject(from_njs_ois.readObject());
                } else {
                    oos.writeObject(from_njs_ois.readObject());
                }
            } finally {
                try {
                    to_njs_oos.close();
                    from_njs_ois.close();
                } catch (Exception ex) {
                }
            }
        }

        public void close() {
            try {
                to_njs.close();
            } catch (Exception ex) {
            }
        }

        public String getTarget() {
            return vsite_name;
        }
    }
}
