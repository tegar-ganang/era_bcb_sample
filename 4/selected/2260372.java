package fr.bibiche.mediaInfos.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.log4j.Logger;

/**
 * <b>Auteur : </b> FT/NCPI/DPS/DDP <br>
 * <b>Fichier : </b> Outils.java <b>du projet</b> MediaInfo <br>
 * <b>Date de création :</b> 22 avr. 10 <br>
 * <b>Description : </b>Divers outils<br>
 */
public class Outils {

    /**
     * <code>LOG</code> : logger.
     */
    private static final Logger LOG = Logger.getLogger(Outils.class);

    /**
     * @param is
     * @return le inputstream convertit en string.
     */
    public static final String convertStreamToString(final InputStream is) {
        String ret = "";
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                ret = sb.toString();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return ret;
    }

    /**
     * Convertir un inputstream en file
     * @param filePath
     *            chemin du fichier
     * @param inputStream
     *            inputstream
     * @return le chemin absolu du fichier
     */
    public static final String convertStreamToFile(final String filePath, final InputStream inputStream) {
        String ret = "";
        try {
            File f = new File(filePath);
            OutputStream out = new FileOutputStream(f);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
            inputStream.close();
            ret = f.getAbsolutePath();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return ret;
    }
}
