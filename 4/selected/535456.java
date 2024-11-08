package consciouscode.seedling.config;

import consciouscode.seedling.NodeLocation;
import consciouscode.seedling.Root;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.IOUtils;

/**
    A configuration resource stored as a <code>.properties</code> file.
*/
public class PropertiesConfigResource implements ConfigResource {

    public PropertiesConfigResource(URL url, InputStream propertiesData) throws IOException {
        myUrl = url;
        myIdentifier = url.toString();
        load(propertiesData);
    }

    public PropertiesConfigResource(String identifier, InputStream propertiesData) throws IOException {
        myIdentifier = identifier;
        load(propertiesData);
    }

    public PropertiesConfigResource(String identifier, Properties properties) {
        myIdentifier = identifier;
        myProperties = properties;
    }

    public String getIdentifier() {
        return myIdentifier;
    }

    public void setIdentifier(String id) {
        myIdentifier = id;
    }

    public ConfigInterpreter getInterpreter(NodeLocation location) {
        if (location.getGlobalPath().equals(Root.GLOBAL_ROOT_PATH)) {
            return new PropertiesConfigInterpreter() {

                @Override
                protected Object constructNode(ConfigInterpreterContext context) {
                    return context.getLocation().getBaseBranch();
                }
            };
        }
        return ((Root) location.getBaseBranch().getGlobalRoot()).getEvaluator();
    }

    public void writeConfiguration(Writer out) throws IOException {
        if (myUrl == null) {
            out.append("# Unable to print configuration resource\n");
        } else {
            InputStream in = myUrl.openStream();
            if (in != null) {
                try {
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            } else {
                out.append("# Unable to print configuration resource\n");
            }
        }
    }

    public Properties getProperties() {
        return myProperties;
    }

    public String getProperty(String property) {
        return myProperties.getProperty(property, null);
    }

    private void load(InputStream in) throws IOException {
        myProperties = new Properties();
        try {
            myProperties.load(in);
        } finally {
            in.close();
        }
    }

    private URL myUrl;

    private String myIdentifier;

    private Properties myProperties;
}
