package uk.ac.ebi.intact.kickstart.psicquic;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Reads the registry to identify available PSICQUIC services.
 */
public class QueryRegistry {

    public static void main(String[] args) throws Exception {
        final URL url = new URL("http://www.ebi.ac.uk/Tools/webservices/psicquic/registry/registry?action=ACTIVE&format=txt");
        final InputStream is = url.openStream();
        final Properties services = new Properties();
        services.load(is);
        is.close();
        System.out.println("Found " + services.size() + " active service(s).");
        for (Object o : services.keySet()) {
            String key = (String) o;
            System.out.println(key + " -> " + services.getProperty(key));
        }
    }
}
