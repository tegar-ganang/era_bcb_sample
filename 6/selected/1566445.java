package org.jgetfile.crawler.parser.ftpclient;

import java.net.InetAddress;
import java.net.URL;
import javolution.util.FastCollection;
import javolution.util.FastList;
import org.apache.log4j.Logger;
import org.jgetfile.crawler.link.Link;
import org.jgetfile.crawler.manager.ObjectManager;
import org.jgetfile.crawler.util.JGetFileUtils;
import com.enterprisedt.net.ftp.FTPClient;

/**
 * A simple parser based on Apache Jakarta Commons Net
 */
public class SimpleFtpClientParser {

    private static final transient Logger logger = Logger.getLogger(SimpleFtpClientParser.class.getName());

    /** A global object manager that stores references to relevant objects of use */
    private static final transient ObjectManager objectManager = ObjectManager.getInstance();

    private String host = "";

    /**
	 * The constructor of SimpleHttpClientParser.
	 */
    public SimpleFtpClientParser() {
    }

    /**
	 * @see org.jgetfile.crawler.parser.IParser#load(org.jgetfile.crawler.link.Link)
	 */
    @SuppressWarnings("static-access")
    public FastCollection<String> load(Link link) {
        URL url = null;
        FastCollection<String> links = new FastList<String>();
        FTPClient ftp = null;
        try {
            String address = link.getURI();
            address = JGetFileUtils.removeTrailingString(address, "/");
            url = new URL(address);
            host = url.getHost();
            String folder = url.getPath();
            logger.info("Traversing: " + address);
            ftp = new FTPClient(host);
            if (!ftp.connected()) {
                ftp.connect();
            }
            ftp.login("anonymous", "me@mymail.com");
            logger.info("Connected to " + host + ".");
            logger.debug("changing dir to " + folder);
            ftp.chdir(folder);
            String[] files = ftp.dir();
            for (String file : files) {
                links.add(address + "/" + file);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.debug(e.getStackTrace());
        } finally {
            try {
                ftp.quit();
            } catch (Exception e) {
                logger.error("Failed to logout or disconnect from the ftp server: ftp://" + host);
            }
        }
        return links;
    }
}
