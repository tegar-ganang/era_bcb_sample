package com.loribel.commons.business.accessor;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.tools.ant.filters.StringInputStream;
import com.loribel.commons.abstraction.ENCODING;
import com.loribel.commons.business.GB_BOFactoryTools;
import com.loribel.commons.business.abstraction.GB_BOAccessor;
import com.loribel.commons.business.abstraction.GB_SimpleBusinessObject;
import com.loribel.commons.exception.GB_LoadException;
import com.loribel.commons.exception.GB_SaveException;
import com.loribel.commons.exception.GB_SecurityException;
import com.loribel.commons.util.CTools;
import com.loribel.commons.util.GB_FileTools;
import com.loribel.commons.util.STools;

/**
 * Implementation de GB_BOAccessor bas� sur un r�pertoire FTP.
 * La persistence doit �tre assur� par des fichiers (1 par objet).
 * L'impl�mentation utilise des fichiers XML.
 *
 * @author Gregory Borelli
 */
public class GB_BOAccessorFtp implements GB_BOAccessor {

    private String ftp;

    private String username;

    private String password;

    private String homeDir = ".";

    private FTPClient clientFtp;

    public GB_BOAccessorFtp(String a_ftp, String a_homeDir) {
        ftp = a_ftp;
        homeDir = a_homeDir;
    }

    protected void checkConnection() throws GB_SecurityException {
        if ((clientFtp != null) && (clientFtp.isConnected())) {
            return;
        }
        if (username == null) {
            throw new GB_SecurityException("Erreur de s�curit�: Vous n'�tes pas encore identifi�!");
        }
        login(username, password);
    }

    public void clearCache() {
    }

    public void delete(GB_SimpleBusinessObject a_bo, String a_id) throws GB_SaveException {
        String l_name = idToName(a_id);
        try {
            checkConnection();
            clientFtp.remoteStore(l_name);
        } catch (Exception ex) {
            throw new GB_SaveException(ex);
        }
    }

    public String[] getIds(String a_pattern, int a_option) throws GB_LoadException {
        if ((a_pattern == null) || (!a_pattern.endsWith("/*"))) {
            throw new GB_LoadException("Pattern not supported by this accessor (FTP): " + a_pattern);
        }
        String l_pattern = idToName(a_pattern);
        l_pattern = STools.removeEnd(l_pattern, "/*");
        try {
            checkConnection();
            String[] l_names = clientFtp.listNames(l_pattern);
            if (l_names == null) {
                return new String[0];
            }
            List l_namesList = CTools.toList(l_names);
            l_namesList.remove(".");
            l_namesList.remove("..");
            return CTools.toArrayOfString(l_namesList);
        } catch (Exception ex) {
            throw new GB_LoadException(ex);
        }
    }

    public long getTs(String a_id, int a_option) {
        return -1;
    }

    public long[] getTs(String[] a_ids, int a_option) {
        int len = CTools.getSize(a_ids);
        long[] retour = new long[len];
        for (int i = 0; i < len; i++) {
            String l_id = a_ids[i];
            retour[i] = getTs(l_id, a_option);
        }
        return retour;
    }

    protected String idFromName(String a_name) {
        if (STools.isNull(homeDir)) {
            return a_name;
        }
        return STools.removeStart(a_name, homeDir + "/");
    }

    protected String idToName(String a_id) {
        if (STools.isNull(homeDir)) {
            return a_id;
        }
        return homeDir + "/" + a_id;
    }

    public boolean isExist(String a_id, int a_option) throws GB_LoadException {
        String l_name = idToName(a_id);
        try {
            checkConnection();
            String[] l_names = clientFtp.listNames(l_name);
            int len = CTools.getSize(l_names);
            return (len == 1);
        } catch (Exception ex) {
            throw new GB_LoadException(ex);
        }
    }

    public GB_SimpleBusinessObject load(String a_id, int a_option) throws GB_LoadException {
        String l_name = idToName(a_id);
        try {
            checkConnection();
            ByteArrayOutputStream l_out = new java.io.ByteArrayOutputStream();
            clientFtp.retrieveFile(l_name, l_out);
            String l_xml = l_out.toString();
            System.out.println("XML:\n" + l_name + "\n" + l_xml);
            Reader l_reader = new StringReader(l_xml);
            GB_SimpleBusinessObject[] l_bos = GB_BOFactoryTools.getFactory().loadFromReader(l_reader);
            return (GB_SimpleBusinessObject) CTools.getFirst(l_bos);
        } catch (Exception ex) {
            throw new GB_LoadException(ex);
        }
    }

    public void login(String a_username, String a_password) throws GB_SecurityException {
        Exception l_exception = null;
        try {
            if (clientFtp == null) {
                clientFtp = new FTPClient();
                clientFtp.connect("ftp://" + ftp);
            }
            boolean b = clientFtp.login(a_username, a_password);
            if (b) {
                username = a_username;
                password = a_password;
                return;
            }
        } catch (Exception ex) {
            l_exception = ex;
        }
        String l_msg = "Cannot login to ftp server with user [{1}], {2}";
        String[] l_replaces = new String[] { a_username, ftp };
        l_msg = STools.replace(l_msg, l_replaces);
        throw new GB_SecurityException(l_msg, l_exception);
    }

    public void save(GB_SimpleBusinessObject a_bo, String a_id, int a_option, boolean a_overwrite) throws GB_SaveException {
        String l_name = idToName(a_id);
        try {
            checkConnection();
            StringWriter l_writer = new StringWriter();
            GB_SimpleBusinessObject[] l_bos = { a_bo };
            GB_BOFactoryTools.getFactory().writeXmlFile(l_writer, l_bos, ENCODING.ISO_8859_1);
            StringInputStream l_input = new StringInputStream(l_writer.toString());
            String l_dir = GB_FileTools.getParent(l_name);
            clientFtp.mkd(l_dir);
            clientFtp.storeFile(l_name, l_input);
        } catch (Exception ex) {
            throw new GB_SaveException(ex);
        }
    }
}
