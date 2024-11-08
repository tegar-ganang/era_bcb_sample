package tac.tools;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.openstreetmap.gui.jmapviewer.interfaces.MapSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import tac.mapsources.MapSourcesManager;
import tac.mapsources.impl.Google.GoogleEarth;
import tac.mapsources.impl.Google.GoogleMapMaker;
import tac.mapsources.impl.Google.GoogleMaps;
import tac.mapsources.impl.Google.GoogleMapsChina;
import tac.mapsources.impl.Google.GoogleTerrain;
import tac.program.Logging;
import tac.tools.MapSourcesTester.MapSourceTestFailed;
import tac.utilities.Utilities;

public class GoogleUrlUpdater {

    static Properties MAPSOURCES_PROPERTIES = new Properties();

    static List<String> KEYS = new ArrayList<String>();

    /**
	 * <p>
	 * Recalculates the tile urls for Google Maps, Earth and Terrain and prints
	 * it to std out. Other map sources can not be updated this way.
	 * </p>
	 * 
	 * Requires <a href="http://jtidy.sourceforge.net/">JTidy</a> library.
	 */
    public static void main(String[] args) {
        Logging.disableLogging();
        MapSourcesManager.loadMapSourceProperties(MAPSOURCES_PROPERTIES);
        KEYS.add("mapsources.Date");
        KEYS.add("mapsources.Rev");
        GoogleUrlUpdater g = new GoogleUrlUpdater();
        g.testMapSource(new UpdateableMapSource("http://maps.google.com/?ie=UTF8&ll=0,0&spn=0,0&z=2", GoogleMaps.class));
        g.testMapSource(new UpdateableMapSource("http://maps.google.com/?ie=UTF8&t=k&ll=0,0&spn=0,0&z=2", GoogleEarth.class));
        g.testMapSource(new UpdateableMapSource("http://maps.google.com/?ie=UTF8&t=p&ll=0,0&spn=0,0&z=2", GoogleTerrain.class));
        g.testMapSource(new UpdateableMapSource("", GoogleMapMaker.class) {

            @Override
            public String getUpdatedUrl(GoogleUrlUpdater g) {
                return g.getUppdatedGoogleMapMakerUrl();
            }
        });
        g.testMapSource(new UpdateableMapSource("", GoogleMapsChina.class) {

            @Override
            public String getUpdatedUrl(GoogleUrlUpdater g) {
                return g.getUpdateGoogleMapsChinaUrl();
            }
        });
        System.out.println("Updated map sources: " + g.updatedMapSources);
        if (g.updatedMapSources > 0) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(4096);
            PrintWriter pw = new PrintWriter(bo, true);
            for (String key : KEYS) {
                pw.println(key + "=" + MAPSOURCES_PROPERTIES.getProperty(key));
                MAPSOURCES_PROPERTIES.remove(key);
            }
            Enumeration<?> enu = MAPSOURCES_PROPERTIES.keys();
            while (enu.hasMoreElements()) {
                String key = (String) enu.nextElement();
                pw.println(key + "=" + MAPSOURCES_PROPERTIES.getProperty(key));
            }
            pw.flush();
            FileOutputStream fo = null;
            try {
                fo = new FileOutputStream("src/tac/mapsources.properties");
                fo.write(bo.toByteArray());
                System.out.println("mapsources.properties has been updated");
            } catch (IOException e) {
            } finally {
                Utilities.closeStream(fo);
            }
        }
    }

    protected int updatedMapSources = 0;

    public void testMapSource(UpdateableMapSource ums) {
        String key = ums.key;
        KEYS.add(key);
        String oldUrlTemplate = System.getProperty(key);
        if (oldUrlTemplate == null) throw new RuntimeException(ums.mapSourceClass + " " + key);
        String newUrlTemplate = ums.getUpdatedUrl(this);
        if (!oldUrlTemplate.equals(newUrlTemplate)) {
            try {
                System.setProperty(key, newUrlTemplate);
                MapSourcesTester.testMapSource(ums.mapSourceClass);
                System.out.println(ums.mapSourceClass.getSimpleName());
                MAPSOURCES_PROPERTIES.setProperty(key, newUrlTemplate);
                updatedMapSources++;
            } catch (MapSourceTestFailed e) {
                System.err.print("Test of new url failed: ");
                System.err.println(key + "=" + newUrlTemplate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> extractImgSrcList(String url) throws IOException, XPathExpressionException {
        LinkedList<String> list = new LinkedList<String>();
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        Tidy tidy = new Tidy();
        tidy.setErrout(new NullPrintWriter());
        Document doc = tidy.parseDOM(conn.getInputStream(), null);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile("//img[@src]");
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        for (int i = 0; i < nodes.getLength(); i++) {
            String imgUrl = nodes.item(i).getAttributes().getNamedItem("src").getNodeValue();
            if (imgUrl != null && imgUrl.length() > 0) list.add(imgUrl);
        }
        return list;
    }

    public List<String> extractUrlList(String url) throws IOException, XPathExpressionException {
        LinkedList<String> list = new LinkedList<String>();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        Tidy tidy = new Tidy();
        tidy.setErrout(new NullPrintWriter());
        Document doc = tidy.parseDOM(conn.getInputStream(), null);
        int len = conn.getContentLength();
        if (len <= 0) len = 32000;
        ByteArrayOutputStream bout = new ByteArrayOutputStream(len);
        PrintStream ps = new PrintStream(bout);
        tidy.pprint(doc, ps);
        ps.flush();
        String content = bout.toString();
        Pattern p = Pattern.compile("(http://[\\w\\\\\\./=&?;-]+)");
        Matcher m = p.matcher(content);
        while (m.find()) {
            list.add(m.group());
        }
        return list;
    }

    public String getUpdatedUrl(String serviceUrl, boolean useImgSrcUrlsOnly) {
        try {
            List<String> urls;
            if (useImgSrcUrlsOnly) urls = extractImgSrcList(serviceUrl); else urls = extractUrlList(serviceUrl);
            HashSet<String> tileUrlCandidates = new HashSet<String>();
            for (String imgUrl : urls) {
                try {
                    if (imgUrl.toLowerCase().startsWith("http://")) {
                        imgUrl = imgUrl.replaceAll("\\\\x26", "&");
                        imgUrl = imgUrl.replaceAll("\\\\x3d", "=");
                        URL tileUrl = new URL(imgUrl);
                        String host = tileUrl.getHost();
                        host = host.replaceFirst("[0-3]", "{\\$servernum}");
                        String path = tileUrl.getPath();
                        path = path.replaceFirst("x=\\d+", "x={\\$x}");
                        path = path.replaceFirst("y=\\d+", "y={\\$y}");
                        path = path.replaceFirst("z=\\d+", "z={\\$z}");
                        path = path.replaceFirst("hl=[^&]+", "hl={\\$lang}");
                        path = path.replaceFirst("&s=\\w*", "");
                        if (path.equalsIgnoreCase(tileUrl.getPath())) continue;
                        String candidate = "http://" + host + path;
                        tileUrlCandidates.add(candidate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String c1 = null;
            for (String c : tileUrlCandidates) {
                c1 = c;
            }
            return c1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getUppdatedGoogleMapMakerUrl() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://www.google.com/mapmaker").openConnection();
            InputStream in = c.getInputStream();
            String html = new String(Utilities.getInputBytes(in));
            in.close();
            Pattern p = Pattern.compile("\\\"gwm.([\\d]+)\\\"");
            Matcher m = p.matcher(html);
            if (!m.find()) throw new RuntimeException("pattern not found");
            String number = m.group(1);
            String url = "http://gt{$servernum}.google.com/mt/n=404&v=gwm." + number + "&x={$x}&y={$y}&z={$z}";
            c.disconnect();
            return url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getUpdateGoogleMapsChinaUrl() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://ditu.google.com/").openConnection();
            InputStream in = c.getInputStream();
            String html = new String(Utilities.getInputBytes(in));
            in.close();
            c.disconnect();
            Pattern p = Pattern.compile("\\\"(http://mt\\d.google.cn/vt/v[^\\\"]*)\\\"");
            Matcher m = p.matcher(html);
            if (!m.find()) throw new RuntimeException("pattern not found");
            String url = m.group(1);
            url = url.replaceAll("\\\\x26", "&");
            url = url.replaceFirst("[0-3]", "{\\$servernum}");
            if (!url.endsWith("&")) url += "&";
            url = url.replaceFirst("hl=[^&]+", "hl={\\$lang}");
            url += "x={$x}&y={$y}&z={$z}";
            return url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class UpdateableMapSource {

        public String updateUrl;

        public String key;

        public Class<? extends MapSource> mapSourceClass;

        public UpdateableMapSource(String updateUrl, Class<? extends MapSource> mapSourceClass) {
            super();
            this.updateUrl = updateUrl;
            this.key = mapSourceClass.getSimpleName() + ".url";
            this.mapSourceClass = mapSourceClass;
        }

        public String getUpdatedUrl(GoogleUrlUpdater g) {
            return g.getUpdatedUrl(updateUrl, true);
        }
    }

    public static class NullPrintWriter extends PrintWriter {

        public NullPrintWriter() throws FileNotFoundException {
            super(new Writer() {

                @Override
                public void close() throws IOException {
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                }
            });
        }
    }
}
