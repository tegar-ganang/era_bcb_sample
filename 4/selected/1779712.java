package nz.ac.waikato.mcennis.rat.crawler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nz.ac.waikato.mcennis.rat.parser.Parser;

/**

 * Crawler designed to parse files from the local file system.  Utilizes native

 * file access mechanisms.

 *

 * @author Daniel McEnnis

 *

 */
public class FileListCrawler implements Crawler {

    /**

     * Set of parsers to be utilized by this crawler when parsing documents

     */
    Parser[] parser;

    /**

     * Perform caching so each parser gets cached copy of original.

     * FIXME: always performs caching regardless of caching value

     */
    protected boolean cache = true;

    boolean spidering = false;

    Crawler crawler = this;

    /**

     * Identical to crawl except all parsers are used

     * @param site site to be crawled

     * @throws java.net.MalformedURLException id site URL is invalid

     * @throws java.io.IOException error occurs during retrieval

     */
    public void crawl(String site) {
        String[] parsers = new String[parser.length];
        for (int i = 0; i < parser.length; ++i) {
            parsers[i] = parser[i].getName();
        }
        crawl(site, parsers);
    }

    /**

     * Retrieves the given document from the local filesystem

     *

     * @param site absolute file name of the file on the local file system

     *

     * @see nz.ac.waikato.mcennis.arm.crawler.Crawler#crawl(java.lang.String)

     */
    public void crawl(String site, String[] parsers) {
        LinkedList<Parser> parserList = new LinkedList<Parser>();
        for (int i = 0; i < parsers.length; ++i) {
            for (int j = 0; j < parser.length; ++j) {
                if (parser[j].getName().contentEquals(parsers[i])) {
                    parserList.add(parser[j]);
                    break;
                }
            }
        }
        try {
            if (cache) {
                java.io.FileInputStream inputFile = new java.io.FileInputStream(site);
                byte[] buffer = new byte[10240];
                java.io.ByteArrayOutputStream data_dump = new java.io.ByteArrayOutputStream();
                int num_read = -1;
                while ((num_read = inputFile.read(buffer)) > 0) {
                    data_dump.write(buffer, 0, num_read);
                }
                inputFile.close();
                java.io.ByteArrayInputStream source = new java.io.ByteArrayInputStream(data_dump.toByteArray());
                for (Parser p : parserList) {
                    try {
                        if (spidering) {
                            p.parse(source, crawler, site);
                        } else {
                            p.parse(source, site);
                        }
                    } catch (Exception e) {
                        Logger.getLogger(FileListCrawler.class.getName()).log(Level.SEVERE, "Exception in File " + site + ": " + e.getMessage());
                    }
                    source.mark(0);
                }
                source.close();
            } else {
                for (Parser p : parserList) {
                    java.io.FileInputStream inputFile = new java.io.FileInputStream(site);
                    try {
                        if (spidering) {
                            p.parse(inputFile, crawler, site);
                        } else {
                            p.parse(inputFile, site);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(FileListCrawler.class.getName()).log(Level.SEVERE, "Exception in " + site + ": " + ex.getMessage());
                    } finally {
                        try {
                            inputFile.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(FileListCrawler.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(FileListCrawler.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**

     *

     * Establishes the parsers to be used by this crawler when retrieving

     * parsers.

     *

     * @see nz.ac.waikato.mcennis.arm.crawler.Crawler#set(nz.ac.waikato.mcennis.arm.parser.Parser[])

     */
    public void set(Parser[] parser) {
        this.parser = new Parser[parser.length];
        for (int i = 0; i < parser.length; ++i) {
            this.parser[i] = parser[i].duplicate();
        }
    }

    /**

     * Retrieves the parsers that are associated with this crawler

     *

     * @return returns an array of Parsers utilized to parse files

     */
    public Parser[] getParser() {
        return parser;
    }

    /**

     * This has no effect on this crawler - there is no proxy for files

     *

     * @param proxy not utilized or read

     */
    public void setProxy(boolean proxy) {
    }

    @Override
    public void setCaching(boolean b) {
        cache = b;
    }

    @Override
    public boolean isCaching() {
        return cache;
    }

    @Override
    public void setSpidering(boolean s) {
        spidering = s;
    }

    @Override
    public boolean isSpidering() {
        return spidering;
    }

    public Crawler getCrawler() {
        return crawler;
    }

    public void setCrawler(Crawler c) {
        crawler = c;
    }
}
