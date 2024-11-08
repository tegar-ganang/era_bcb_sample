package testing;

import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.ddth.xconfig.XConfig;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TestXConfig {

    public static void main(String[] argv) throws IOException, SAXException, ParserConfigurationException {
        FileInputStream fis = new FileInputStream("test.xml");
        InputSource is = new InputSource(fis);
        XConfig config = new XConfig(is);
        System.out.println(fis.getChannel().isOpen());
        fis.close();
    }
}
