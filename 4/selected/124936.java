package nz.ac.waikato.mcennis.rat.crawler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import nz.ac.waikato.mcennis.rat.parser.Parser;

/**


 *


 * @author Daniel McEnnis


 */
public class GZipFileCrawler implements Crawler {

    /**


     * Set of parsers to be utilized by this crawler when parsing documents


     */
    Parser[] parser;

    boolean cache = false;

    boolean spidering = false;

    /** Creates a new instance of GZipFileCrawler */
    public GZipFileCrawler() {
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
            if (!cache) {
                for (Parser p : parserList) {
                    java.util.zip.GZIPInputStream inputFile = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(site), 102400);
                    try {
                        if (spidering) {
                            p.parse(inputFile, this, site);
                        } else {
                            p.parse(inputFile, site);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(GZipFileCrawler.class.getName()).log(Level.SEVERE, "Exception in " + site + ": " + ex.getMessage());
                    } finally {
                        try {
                            inputFile.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            } else {
                java.util.zip.GZIPInputStream inputFile = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(site), 102400);
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
                            p.parse(source, this, site);
                        } else {
                            p.parse(source, site);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(GZipFileCrawler.class.getName()).log(Level.SEVERE, "Exception in " + site + ": " + ex.getMessage());
                    }
                    source.mark(0);
                }
                source.close();
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(GZipFileCrawler.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(GZipFileCrawler.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Override
    public void set(Parser[] parser) {
        this.parser = new Parser[parser.length];
        for (int i = 0; i < parser.length; ++i) {
            this.parser[i] = parser[i].duplicate();
        }
    }

    @Override
    public Parser[] getParser() {
        return parser;
    }

    @Override
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
}
