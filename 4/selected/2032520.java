package es.ua.dlsi.tradubi.main.server.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scalemt.rmi.router.ITradubiTranslationEngine;
import org.scalemt.rmi.transferobjects.LanguagePair;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import es.ua.dlsi.tradubi.main.client.services.ConfigurationService;
import es.ua.dlsi.tradubi.main.server.translationengine.TranslationEngineFactory;
import es.ua.dlsi.tradubi.main.server.util.ServerUtil;

/**
 * {@link es.ua.dlsi.tradubi.main.client.services.ConfigurationService} implementation 
 * 
 * @author vitaka
 *@see es.ua.dlsi.tradubi.main.client.services.ConfigurationService
 */
public class ConfigurationServiceImpl extends RemoteServiceServlet implements ConfigurationService {

    /**
	 * Required for serialization
	 */
    private static final long serialVersionUID = 2509132796582545536L;

    /**
	 * Commons logging logger
	 */
    static Log logger = LogFactory.getLog(ConfigurationServiceImpl.class);

    public List<String> getSupportedLanguages() {
        return ServerUtil.getSupportedLanguages();
    }

    public List<String> getSupportedPairs() {
        ITradubiTranslationEngine translationEngine = TranslationEngineFactory.getTranslationEngine();
        List<String> returnValue = new ArrayList<String>();
        try {
            for (LanguagePair l : translationEngine.getSupportedPairs()) returnValue.add(l.toString());
            Collections.sort(returnValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

    public void modifyApplicationMessage(String locale, String messageName, String messageValue) {
        Properties properties = new Properties();
        try {
            String i18nPath = ServerUtil.removelastResourceURL(ConfigurationServiceImpl.class.getResource("/es/ua/tranube/prototype/main/client/TranubeConstants_en.properties").getPath()).toString();
            File englishFile = new File(i18nPath + "TranubeConstants_en.properties");
            if (!englishFile.exists()) throw new Exception("English file not found");
            String propertiesFilePath = i18nPath + "TranubeConstants_" + locale + ".properties";
            File file = new File(propertiesFilePath);
            if (!file.exists()) {
                FileReader in = new FileReader(englishFile);
                FileWriter out = new FileWriter(file);
                int c;
                while ((c = in.read()) != -1) out.write(c);
                in.close();
                out.close();
            }
            InputStream is = ConfigurationServiceImpl.class.getResourceAsStream("/es/ua/tranube/prototype/main/client/TranubeConstants_" + locale + ".properties");
            BufferedReader breader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            StringBuilder strBuilder = new StringBuilder();
            boolean found = false;
            while ((line = breader.readLine()) != null) {
                if (line.startsWith("#")) strBuilder.append(line).append("\n"); else {
                    String[] pieces = line.split("=");
                    if (pieces.length == 2) {
                        if (pieces[0].trim().equals(messageName)) {
                            strBuilder.append(pieces[0].trim() + " = " + messageValue + "\n");
                            found = true;
                        } else strBuilder.append(line).append("\n");
                    } else strBuilder.append(line).append("\n");
                }
            }
            if (!found) strBuilder.append(messageName).append(" = ").append(messageValue).append("\n");
            breader.close();
            is.close();
            FileWriter writer = new FileWriter(file);
            writer.write(strBuilder.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
