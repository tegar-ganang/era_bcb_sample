package nz.ac.waikato.mcennis.rat.parser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import nz.ac.waikato.mcennis.rat.crawler.Crawler;
import nz.ac.waikato.mcennis.rat.crawler.WebCrawler;
import nz.ac.waikato.mcennis.rat.graph.Graph;
import nz.ac.waikato.mcennis.rat.graph.descriptors.Parameter;
import nz.ac.waikato.mcennis.rat.graph.descriptors.Properties;
import nz.ac.waikato.mcennis.rat.graph.descriptors.PropertiesInternal;
import nz.ac.waikato.mcennis.rat.graph.descriptors.PropertiesFactory;
import nz.ac.waikato.mcennis.rat.graph.descriptors.ParameterInternal;
import nz.ac.waikato.mcennis.rat.graph.descriptors.ParameterFactory;
import nz.ac.waikato.mcennis.rat.graph.descriptors.SyntaxObject;
import nz.ac.waikato.mcennis.rat.graph.descriptors.SyntaxCheckerFactory;

/**
 * Class for transforming WebPage data into bag-of-words format.
 * @author Daniel McEnnis
 * 
 */
public class BaseHTMLParser extends AbstractParser implements Parser {

    java.util.LinkedList<java.util.HashMap<String, Integer>> histogramList = new java.util.LinkedList<java.util.HashMap<String, Integer>>();

    public BaseHTMLParser() {
        super();
        properties.get("ParserClass").add("BaseHTMLParser");
        properties.get("Name").add("BaseHTMLParser");
    }

    /**
     * Parse the document into bag-of-words format.
     * @param data data stream to be parsed
     */
    public void parse(java.io.InputStream data, String site) {
        java.util.HashMap<String, Integer> histogram = new java.util.HashMap<String, Integer>();
        histogramList.add(histogram);
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(data));
            StringBuffer content = new StringBuffer();
            String buffer = reader.readLine();
            while ((buffer != null) && (buffer.contentEquals(""))) {
                content.append(buffer);
                buffer = reader.readLine();
            }
            java.util.regex.Pattern tags = java.util.regex.Pattern.compile("<[^>]*>");
            String[] tagged = tags.split(content.toString());
            content = new StringBuffer();
            for (int i = 0; i < tagged.length; ++i) {
                content.append(tagged[i]).append(" ");
            }
            java.util.regex.Pattern whitespace = java.util.regex.Pattern.compile("[^a-zA-Z]+");
            tagged = whitespace.split(content.toString());
            for (int i = 0; i < tagged.length; ++i) {
                if (!tagged[i].contentEquals("")) {
                    tagged[i] = tagged[i].toLowerCase();
                    if (histogram.containsKey(tagged[i])) {
                        histogram.put(tagged[i], histogram.get(tagged[i]) + 1);
                    } else {
                        histogram.put(tagged[i], 1);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse a data stream while spidering for more pages
     * @param data stream to be parsed
     * @param crawler crawler for crawling new pages
     */
    public void parse(java.io.InputStream data, Crawler crawler, String site) {
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int num_read = 0;
            while ((num_read = data.read(buffer)) > 0) {
                bytes.write(buffer, 0, num_read);
            }
            java.io.ByteArrayInputStream source = new java.io.ByteArrayInputStream(bytes.toByteArray());
            parse(source, site);
            source.mark(0);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(source));
            StringBuffer content = new StringBuffer();
            String line = reader.readLine();
            while ((line != null) && (!line.contentEquals(""))) {
                content.append(line).append(" ");
                line = reader.readLine();
            }
            processLinks(content.toString(), crawler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new URL from anchor text to crawl
     * @param content line containing the anchor text
     * @param crawler crawler to crawl the new URLs
     */
    protected void processLinks(String content, Crawler crawler) {
        String data = content;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<a href=\"([^\"]+)\">.*</a>(.*)");
        java.util.LinkedList<String> list = new java.util.LinkedList<String>();
        java.util.regex.Matcher match = pattern.matcher(data);
        while (match.matches()) {
            list.add(match.group(0));
            data = list.get(1);
            match = pattern.matcher(data);
        }
        java.util.Iterator<String> cursor = list.iterator();
        while (cursor.hasNext()) {
            try {
                crawler.crawl(cursor.next());
            } catch (MalformedURLException ex) {
                Logger.getLogger(BaseHTMLParser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BaseHTMLParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Parser duplicate() {
        BaseHTMLParser ret = new BaseHTMLParser();
        ret.properties = this.properties.duplicate();
        return ret;
    }

    /**
     * Return histogram object
     * FIX: currently returns null
     */
    public ParsedObject get() {
        return null;
    }

    @Override
    public void set(ParsedObject o) {
        ;
    }
}
