package net.sourceforge.ondex.restful;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import net.sourceforge.ondex.restful.util.MyShutdown;
import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

public class LaunchApp {

    public static String DATABASEDIR = "";

    private static int getPort(int defaultPort) {
        String port = System.getenv("JERSEY_HTTP_PORT");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        }
        return defaultPort;
    }

    private static URI getBaseURI() {
        return UriBuilder.fromUri("http://localhost/").port(getPort(9998)).build();
    }

    public static final URI BASE_URI = getBaseURI();

    protected static SelectorThread startServer() throws IOException {
        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.packages", "net.sourceforge.ondex.restful.resources" + ";" + "net.sourceforge.ondex.restful.resources.writers" + ";" + "net.sourceforge.ondex.restful.resources.readers" + ";" + "net.sourceforge.ondex.restful.resources.injectable");
        System.out.println("Starting grizzly...");
        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(BASE_URI, initParams);
        return threadSelector;
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java -jar restful.jar LaunchApp PathToOXLFiles");
            return;
        }
        DATABASEDIR = args[0];
        SelectorThread threadSelector = startServer();
        System.out.println("Jersey started");
        MyShutdown sh = new MyShutdown(threadSelector);
        Runtime.getRuntime().addShutdownHook(sh);
    }
}
