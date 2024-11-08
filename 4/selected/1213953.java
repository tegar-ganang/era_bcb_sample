package javasape;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class SapeConnection {

    private static final Logger log = Logger.getLogger(SapeConnection.class);

    private final String version = "1.0.3";

    private final List<String> serverList = Arrays.asList("dispenser-01.sape.ru", "dispenser-02.sape.ru");

    private final String dispenserPath;

    private final String userAgent;

    private final int socketTimeout;

    private final int cacheLifeTime;

    public SapeConnection(String dispenserPath, String userAgent, int socketTimeout, int cacheLifeTime) {
        this.dispenserPath = dispenserPath;
        this.userAgent = userAgent;
        this.socketTimeout = socketTimeout;
        this.cacheLifeTime = cacheLifeTime;
    }

    protected String fetchRemoteFile(String host, String path) throws IOException {
        Reader r = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) ((new URL(("http://" + host + path)).openConnection()));
            if (socketTimeout > 0) {
                connection.setConnectTimeout(socketTimeout);
                connection.setReadTimeout(socketTimeout);
            }
            connection.addRequestProperty("User-Agent", userAgent + ' ' + version);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.connect();
            r = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringWriter sw = new StringWriter();
            int b;
            while ((b = r.read()) != -1) sw.write(b);
            return sw.toString();
        } finally {
            if (r != null) r.close();
        }
    }

    Map<String, Object> cached;

    long cacheUpdated;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getData() {
        if (cacheLifeTime <= (System.currentTimeMillis() - cacheUpdated) / 1000) {
            for (String server : serverList) {
                String data;
                try {
                    data = fetchRemoteFile(server, dispenserPath + "&charset=UTF-8");
                } catch (IOException e1) {
                    continue;
                }
                if (data.startsWith("FATAL ERROR:")) {
                    log.error("Sape responded with error: " + data);
                    continue;
                }
                try {
                    cached = (Map<String, Object>) new SerializedPhpParser(data).parse();
                } catch (Exception e) {
                    log.error("Can't parse Sape data", e);
                    continue;
                }
                cacheUpdated = System.currentTimeMillis();
                return cached;
            }
            log.error("Unable to fetch Sape data");
            return new HashMap<String, Object>();
        }
        return cached;
    }
}
