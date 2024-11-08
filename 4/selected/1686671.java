package br.com.manish.ahy.kernel.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Blob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.rowset.serial.SerialBlob;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import br.com.manish.ahy.kernel.ddl.Parser;
import br.com.manish.ahy.kernel.ddl.ParserMySQL;
import br.com.manish.ahy.kernel.exception.OopsException;

public final class JPAUtil {

    private static Log log = LogFactory.getLog(JPAUtil.class);

    public static final String BLOB_BATCH_PREFIX = "#{blob}:";

    private JPAUtil() {
        super();
    }

    private static Map<String, Class<? extends Parser>> parserMap = new HashMap<String, Class<? extends Parser>>();

    static {
        parserMap.put("org.hibernate.dialect.MySQL5Dialect", ParserMySQL.class);
    }

    public static Blob bytesToBlob(byte[] bytes) {
        Blob ret = null;
        try {
            ret = new SerialBlob(bytes);
        } catch (Exception e) {
            throw new OopsException(e, "Error reading file data.");
        }
        return ret;
    }

    public static byte[] blobToBytes(Blob blob) {
        byte[] byteData = null;
        try {
            InputStream is = blob.getBinaryStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
            byte[] bytes = new byte[512];
            int readBytes;
            while ((readBytes = is.read(bytes)) > 0) {
                os.write(bytes, 0, readBytes);
            }
            byteData = os.toByteArray();
            is.close();
            os.close();
        } catch (Exception e) {
            throw new OopsException(e, "Error reading file data.");
        }
        return byteData;
    }

    public static Blob resourceToBlob(String path) throws IOException {
        byte[] stream = FileUtil.readResourceAsBytes(path);
        Blob ret = bytesToBlob(stream);
        return ret;
    }

    public static String getDatabaseTypeConfig() {
        String ret = null;
        SAXBuilder sb = new SAXBuilder();
        Document doc;
        try {
            URL url = JPAUtil.class.getResource("/META-INF/persistence.xml");
            doc = sb.build(url);
            String dialect = "";
            Element root = doc.getRootElement();
            Element persUnit = root.getChild("persistence-unit", root.getNamespace());
            Element prop = persUnit.getChild("properties", root.getNamespace());
            for (Element el : (List<Element>) prop.getChildren()) {
                if (el.getAttributeValue("name").equals("hibernate.dialect")) {
                    dialect = el.getAttributeValue("value");
                    break;
                }
            }
            if (parserMap.get(dialect) != null) {
                ret = dialect;
            } else {
                String acceptedValues = "";
                for (String key : parserMap.keySet()) {
                    acceptedValues += key + ",";
                }
                acceptedValues = acceptedValues.substring(0, acceptedValues.length() - 1);
                throw new OopsException("DataBase: [" + dialect + "] not supported yet, allowed: " + acceptedValues);
            }
            log.info("Database type: [" + ret + "]");
        } catch (Exception e) {
            throw new OopsException(e, "Error when discovering database config.");
        }
        return ret;
    }

    public static Parser getParser() {
        String baseConfig = getDatabaseTypeConfig();
        Parser ret = null;
        try {
            ret = (Parser) parserMap.get(baseConfig).newInstance();
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            throw new OopsException(e, "Error on getting parser: [" + baseConfig + "]");
        }
        return ret;
    }
}
