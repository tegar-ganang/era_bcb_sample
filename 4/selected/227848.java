package net.sf.keytabgui.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import net.sf.keytabgui.KeytabResources;
import net.sf.keytabgui.model.utils.FileUtils;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.RealmException;
import sun.security.krb5.internal.ktab.KeyTab;

/**
 * 
 * 
 * @author Kamil
 *
 */
@SuppressWarnings("restriction")
public class Keytab7Proxy extends KeytabProxy {

    private Logger log = Logger.getLogger(Keytab7Proxy.class.getCanonicalName());

    @SuppressWarnings("static-access")
    public void open(File file) {
        log.info("Opening file " + file);
        try {
            KeyTab.refresh();
            name = null;
            if (file != null && !file.exists()) throw new Exception("Key table " + file + " does not exist.");
            KeyTab kt = (file == null) ? KeyTab.getInstance() : KeyTab.getInstance(file);
            if (kt == null) if (file != null) {
                String msg = KeytabResources.get().getString("error.keytab.open.incorrectfile");
                msg.replace("$1", file.toString());
                sendNotification(MSG_FOR_USER, msg);
                tmpKeyTab = null;
                name = null;
                entries = null;
                throw new Exception(new StringBuilder().append("The format of key table ").append(file).append(" is incorrect.").toString());
            } else {
                String msg = KeytabResources.get().getString("error.keytab.open.default_keytab_not_found");
                sendNotification(MSG_FOR_USER, msg);
                tmpKeyTab = null;
                name = null;
                entries = null;
                throw new FileNotFoundException("Default keytab was not found");
            }
            name = tmpKeyTab.tabName();
            this.tmpKeyTab = getCopy(kt);
            this.entries = tmpKeyTab.getEntries();
        } catch (Exception e) {
            System.out.println("Error loading key table: " + e.getMessage());
            e.printStackTrace();
        }
        sendNotification(NAME);
    }

    @SuppressWarnings("static-access")
    private KeyTab getCopy(KeyTab kt) throws IOException {
        if (kt != null) {
            File srcFile = new File(tmpKeyTab.tabName());
            File destFile = File.createTempFile("keytab", "");
            FileUtils.copyFile(srcFile, destFile);
            KeyTab.refresh();
            return KeyTab.getInstance(destFile);
        }
        return null;
    }

    public void addEntry(PrincipalName principal, char[] arg1, Integer kvno) throws KrbException {
        try {
            Method jdk7 = KeyTab.class.getDeclaredMethod("addEntry", PrincipalName.class, char[].class, int.class, boolean.class);
            jdk7.invoke(tmpKeyTab, principal, arg1, kvno, true);
            sendNotification(NAME);
            sendNotification(MSG_FOR_USER, "");
        } catch (Exception expectedInJDK6) {
            log.warning("This code shouldn't be run in JDK6. Check method KeytabProxy.getInstance()");
            log.throwing(this.getClass().getCanonicalName(), "addEntry", expectedInJDK6);
        }
    }

    protected void createNewImpl() throws Exception {
        File f = File.createTempFile("tmp", "keytab");
        tmpKeyTab = KeyTab.create(f.getCanonicalPath());
        sendNotification(NAME);
        sendNotification(MSG_FOR_USER, "");
    }

    public void close() {
        tmpKeyTab = null;
        entries = null;
        sendNotification(NAME);
        sendNotification(MSG_FOR_USER, "");
    }

    @SuppressWarnings("static-access")
    public String saveAs(File selectedFile) throws IOException {
        String savedToFile = null;
        try {
            tmpKeyTab.save();
            FileUtils.copyFile(new File(tmpKeyTab.tabName()), selectedFile);
            savedToFile = selectedFile.getCanonicalPath();
            log.info("Saved file " + savedToFile);
            open(selectedFile);
        } catch (Exception e) {
            log.warning("Couldn't save to file " + selectedFile);
            log.throwing(this.getClass().getCanonicalName(), "saveAs", e);
            String msg = KeytabResources.get().getString("error.keytab.save.error");
            msg.replace("$1", selectedFile.toString());
            sendNotification(MSG_FOR_USER, msg);
        }
        return savedToFile;
    }

    public void deleteEntry(String principal) throws RealmException {
        PrincipalName pn = new PrincipalName(principal);
        try {
            Method jdk7 = KeyTab.class.getDeclaredMethod("deleteEntries", PrincipalName.class, int.class, int.class);
            for (int type = 0; type < 30; type++) {
                jdk7.invoke(tmpKeyTab, pn, type, -1);
            }
            sendNotification(NAME);
        } catch (Exception expectedInJDK6) {
        }
    }
}
