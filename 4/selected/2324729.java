package org.openejb.alt.assembler.modern.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Loads DTDs from disk so you don't have to hit the web to validate
 * configuration files.  This class will fail silently if the files aren't
 * available locally, and you'll end up hitting the web anyway.
 *
 * @author Aaron Mulder (ammulder@alumni.princeton.edu)
 * @version $Revision: 1.3 $
 */
public class DTDResolver implements EntityResolver {

    public static HashMap dtds = new HashMap();

    static {
        byte[] file = getEJBDTD();
        if (file != null) {
            dtds.put("ejb-jar.dtd", file);
            dtds.put("ejb-jar_1_1.dtd", file);
        }
        file = getConnectorDTD();
        if (file != null) {
            dtds.put("connector.dtd", file);
            dtds.put("connector_1_0.dtd", file);
        }
        file = getOpenEjbDTD();
        if (file != null) {
            dtds.put("openejb-jar.dtd", file);
            dtds.put("openejb-jar-1_1.dtd", file);
        }
        file = getOpenEjbServerDTD();
        if (file != null) {
            dtds.put("openejb-config.dtd", file);
            dtds.put("openejb-config-1_1.dtd", file);
        }
        file = getOpenEjbConnectorDTD();
        if (file != null) {
            dtds.put("openejb-rar.dtd", file);
            dtds.put("openejb-rar-1_1.dtd", file);
        }
    }

    public static byte[] getEJBDTD() {
        InputStream in = DTDResolver.class.getClassLoader().getResourceAsStream("ejb-jar_1_1.dtd");
        if (in != null) return getBytes(in);
        return null;
    }

    public static byte[] getConnectorDTD() {
        InputStream in = DTDResolver.class.getClassLoader().getResourceAsStream("connector_1_0.dtd");
        if (in != null) return getBytes(in);
        return null;
    }

    public static byte[] getOpenEjbDTD() {
        InputStream in = DTDResolver.class.getClassLoader().getResourceAsStream("openejb-jar_1_1.dtd");
        if (in != null) return getBytes(in);
        return null;
    }

    public static byte[] getOpenEjbServerDTD() {
        InputStream in = DTDResolver.class.getClassLoader().getResourceAsStream("openejb-config_1_1.dtd");
        if (in != null) return getBytes(in);
        return null;
    }

    public static byte[] getOpenEjbConnectorDTD() {
        InputStream in = DTDResolver.class.getClassLoader().getResourceAsStream("openejb-rar_1_1.dtd");
        if (in != null) return getBytes(in);
        return null;
    }

    private static byte[] getBytes(InputStream source) {
        byte[] buf = new byte[512];
        try {
            BufferedInputStream in = new BufferedInputStream(source);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int count;
            while ((count = in.read(buf)) > -1) out.write(buf, 0, count);
            in.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        int pos = systemId.lastIndexOf('/');
        String name;
        if (pos > -1) name = systemId.substring(pos + 1); else name = systemId;
        byte[] data = (byte[]) dtds.get(name);
        if (data != null) {
            return new InputSource(new ByteArrayInputStream(data));
        }
        return null;
    }
}
