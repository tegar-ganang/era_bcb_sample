package nz.ac.waikato.mcennis.rat.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nz.ac.waikato.mcennis.rat.crawler.Authenticator;
import nz.ac.waikato.mcennis.rat.parser.Parser;

/**



 * Base class implementing basic web access capabilities.  It executes in the 



 * current thread and accesses pages sequentially.  It is designed to be the base



 * class for other web-accessing crawlers.  In particular, it is the superclass



 * of the WebCrawler crawler.



 *



 * @author Daniel McEnnis



 *



 */
public class CrawlerBase implements Crawler {

    /**



     * Parser array handling all parsers utilized in processing documents.



     */
    protected Parser[] parser = null;

    /**



     * Is this object operating as a spider (automatically following links).



     * This is utilized to determine which procedure to call on the parsers



     */
    protected boolean spider;

    /**



     * Is there a proxy that needs to be used in collecting data.



     */
    protected boolean proxy;

    /**



     * Perform caching so each parser gets cached copy of original.



     * FIXME: always performs caching regardless of caching value



     */
    protected boolean cache = true;

    String proxyHost = "proxy.scms.waikato.ac.nz";

    String proxyPort = "80";

    String proxyType = "4";

    /**



     * Base constructor.  The parser must be set before this object can be used



     * or a null pointer exception will be thrown on parse.



     */
    public CrawlerBase() {
        proxy = true;
    }

    /**



     * Takes the current array of parsers and creates a copy of them utilizing



     * the duplicate method on each parser.  



     *



     * @param parser Array of parsers to be duplicated and set for parsing 



     *  documents



     */
    public void set(Parser[] parser) {
        this.parser = new Parser[parser.length];
        for (int i = 0; i < this.parser.length; ++i) {
            this.parser[i] = parser[i].duplicate();
        }
    }

    /**



     * Identical to crawl except all parsers are used



     * @param site site to be crawled



     * @throws java.net.MalformedURLException id site URL is invalid



     * @throws java.io.IOException error occurs during retrieval



     */
    public void crawl(String site) throws MalformedURLException, IOException {
        String[] parsers = new String[parser.length];
        for (int i = 0; i < parser.length; ++i) {
            parsers[i] = parser[i].getName();
        }
        crawl(site, parsers);
    }

    public void crawl(String site, String[] parsers) throws MalformedURLException, IOException {
        if (proxy) {
            System.setProperty("http.proxySet", "true");
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("http.proxyType", proxyType);
            Authenticator def = new Authenticator();
            java.net.Authenticator.setDefault(def);
        }
        java.net.URL connection = new java.net.URL(site);
        java.net.URLConnection connected = connection.openConnection();
        java.io.InputStream raw_data = connected.getInputStream();
        byte[] buffer = new byte[10240];
        java.io.ByteArrayOutputStream contents = new java.io.ByteArrayOutputStream();
        int num_read = 0;
        while ((num_read = raw_data.read(buffer)) > 0) {
            contents.write(buffer, 0, num_read);
        }
        raw_data.close();
        contents.close();
        buffer = contents.toByteArray();
        try {
            doParse(buffer, parsers, site);
        } catch (Exception ex) {
            Logger.getLogger(CrawlerBase.class.getName()).log(Level.SEVERE, "Exception in File " + site + ": " + ex.getMessage());
            ex.printStackTrace();
        }
        raw_data.close();
    }

    /**

     * Return the string to be used to determine the proxy host for this connection

     * @return proxy host descriptor

     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**

     * Sets the string to be used to determine the proxy host for this connection

     * @param proxyHost proxy host descriptor

     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**

     * Returns the string describing the port the proxy is operating on

     * @return port of proxy

     */
    public String getProxyPort() {
        return proxyPort;
    }

    /**

     * Sets the string describing the port the proxy is operating on

     * @param proxyPort port of the proxy

     */
    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**

     * Returns the string the crawler will use to set the system property 

     * determining the proxy type.

     * @return proxyType type of the proxy

     */
    public String getProxyType() {
        return proxyType;
    }

    /**

     * Sets the string the crawler will use to set the system property 

     * determining the proxy type.

     * @param proxyType type of the proxy

     */
    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    /**



     * Helper function separated from public parse to allow easy overloading.



     * Parses the given data into a byte array and passes a copy to every 



     * parser.



     *



     * @param raw_data 



     * @throws java.io.IOException 



     * @throws java.lang.Exception 



     */
    protected void doParse(byte[] raw_data, String[] parsers, String site) throws IOException, Exception {
        LinkedList<Parser> parserList = new LinkedList<Parser>();
        for (int i = 0; i < parsers.length; ++i) {
            for (int j = 0; j < parser.length; ++j) {
                if (parser[j].getName().contentEquals(parsers[i])) {
                    parserList.add(parser[j]);
                    break;
                }
            }
        }
        for (Parser p : parserList) {
            java.io.ByteArrayInputStream data = new java.io.ByteArrayInputStream(raw_data);
            if (spider) {
                p.parse(data, this, site);
            } else {
                p.parse(data, site);
            }
            data.close();
        }
    }

    /**



     * Returns the parsers that are associated with this crawler.  Returns null



     * if no parsers have been set



     *



     * @return Parser array containing the parsers used to parse documents.



     *



     */
    public Parser[] getParser() {
        return parser;
    }

    /**



     * Set or unset whether the crawler should be going through a proxy to 



     * access the internet.



     *



     * @param proxy Should a proxy be used to access the internet



     *



     */
    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }

    /**



     * Sets caching on or off



     * FIXME: currently caching permanently on



     * 



     * @param b should caching be enabled



     */
    public void setCaching(boolean b) {
        cache = b;
    }

    @Override
    public boolean isCaching() {
        return true;
    }

    @Override
    public void setSpidering(boolean s) {
        spider = s;
    }

    @Override
    public boolean isSpidering() {
        return spider;
    }
}
