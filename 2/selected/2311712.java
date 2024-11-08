package net.suberic.pooka.resource;

import net.suberic.util.*;
import net.suberic.pooka.*;
import net.suberic.pooka.ssl.*;
import javax.activation.*;
import java.io.*;
import java.util.*;
import java.net.*;

/**
 * A PookaResourceManager which uses no files.
 */
public class DisklessResourceManager extends ResourceManager {

    /**
   * Creates a VariableBundle to be used.
   */
    public VariableBundle createVariableBundle(String fileName, VariableBundle defaults) {
        return defaults;
    }

    /**
   * Creates a MailcapCommandMap to be used.
   */
    public MailcapCommandMap createMailcap(String fileName) {
        return new FullMailcapCommandMap();
    }

    /**
   * Creates a PookaTrustManager.
   */
    public PookaTrustManager createPookaTrustManager(javax.net.ssl.TrustManager[] pTrustManagers, String fileName) {
        return new PookaTrustManager(pTrustManagers, null, false);
    }

    /**
   * Creates an output file which includes only resources that are appropriate
   * to a Diskless client.
   */
    public static void exportResources(File pOutputFile, boolean pIncludePasswords) throws IOException {
        VariableBundle sourceBundle = Pooka.getResources();
        pOutputFile.createNewFile();
        VariableBundle newWritableProperties = new VariableBundle(pOutputFile, null);
        List allStores = Pooka.getStoreManager().getStoreList();
        List toRemoveList = new ArrayList();
        List keepList = new ArrayList();
        Iterator iter = allStores.iterator();
        while (iter.hasNext()) {
            StoreInfo current = (StoreInfo) iter.next();
            if (current.getProtocol() != null && current.getProtocol().toLowerCase().startsWith("imap")) {
                newWritableProperties.setProperty(current.getStoreProperty() + ".cachingEnabled", "false");
                keepList.add(current.getStoreID());
            } else {
                toRemoveList.add(current.getStoreID());
            }
        }
        Enumeration names = sourceBundle.getProperties().propertyNames();
        while (names.hasMoreElements()) {
            String current = (String) names.nextElement();
            boolean keep = true;
            if (current.startsWith("Store")) {
                if ((!pIncludePasswords) && current.endsWith("password")) {
                    keep = false;
                } else if (current.endsWith("cachingEnabled")) {
                    keep = false;
                }
                for (int i = 0; keep && i < toRemoveList.size(); i++) {
                    if (current.startsWith("Store." + (String) toRemoveList.get(i))) {
                        keep = false;
                    }
                }
            }
            if (keep) {
                newWritableProperties.setProperty(current, sourceBundle.getProperty(current));
            }
        }
        newWritableProperties.setProperty("Pooka.useLocalFiles", "false");
        newWritableProperties.setProperty("Store", VariableBundle.convertToString(keepList));
        newWritableProperties.saveProperties();
    }

    /**
   * Gets a resource for reading.  pFileName could be a URL or a file name
   * or some similar identifier that the 
   */
    public java.io.InputStream getInputStream(String pFileName) throws java.io.IOException {
        try {
            URL url = new URL(pFileName);
            return url.openStream();
        } catch (MalformedURLException mue) {
            throw new IOException("Error opening URL:  " + mue.getMessage());
        }
    }

    public java.io.OutputStream getOutputStream(String pFileName) throws java.io.IOException {
        throw new IOException("Diskless mode:  no file modification available.");
    }
}
