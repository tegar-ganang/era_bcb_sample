package net.kano.joustsim.app.config;

import net.kano.joustsim.Screenname;
import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import org.bouncycastle.util.encoders.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PrefTools {

    private static final Logger logger = Logger.getLogger(PrefTools.class.getName());

    private PrefTools() {
    }

    public static String getBase64Decoded(String encoded) {
        if (encoded == null) return null;
        try {
            return BinaryTools.getAsciiString(ByteBlock.wrap(Base64.decode(encoded)));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getBase64Encoded(String pass) {
        DefensiveTools.checkNull(pass, "pass");
        byte[] encoded = Base64.encode(BinaryTools.getAsciiBytes(pass));
        return BinaryTools.getAsciiString(ByteBlock.wrap(encoded));
    }

    public static Properties loadProperties(File file) throws IOException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            lockIfPossible(fin.getChannel(), true);
            Properties props = new Properties();
            props.load(fin);
            return props;
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void writeProperties(File prefsFile, Properties props, String header) throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(prefsFile);
            lockIfPossible(fout.getChannel(), false);
            props.store(fout, header);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void lockIfPossible(FileChannel channel, boolean val) {
        try {
            channel.lock(0L, Long.MAX_VALUE, val);
        } catch (Exception nobigdeal) {
            logger.log(Level.WARNING, "Couldn't acquire lock for " + channel, nobigdeal);
        }
    }

    public static boolean deleteDir(File dir) {
        File candir;
        try {
            candir = dir.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }
        if (!candir.equals(dir.getAbsoluteFile())) {
            return false;
        }
        File[] files = candir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                boolean deleted = deleteFile(file);
                if (!deleted) {
                    if (file.isDirectory()) deleteDir(file);
                }
            }
        }
        return deleteFile(dir);
    }

    private static boolean deleteFile(File file) {
        return file.delete();
    }

    public static File getGlobalConfigDir(File configDir) {
        return new File(configDir, "global");
    }

    public static File getLocalConfigDir(File configDir) {
        return new File(configDir, "local");
    }

    public static File getConfigDir(File baseDir) {
        return new File(baseDir, "config");
    }

    public static File getLocalPrefsDirForScreenname(File localPrefsDir, Screenname sn) {
        String normal = sn.getNormal();
        if (normal.length() == 0) return null;
        return new File(localPrefsDir, normal);
    }

    public static File getLocalCertsDir(File keysDir) {
        return new File(keysDir, "certs");
    }

    public static File getTrustedSignersDir(File trustDir) {
        return new File(trustDir, "trusted-signers");
    }

    public static File getTrustedCertsDir(File trustDir) {
        return new File(trustDir, "trusted-certs");
    }

    public static File getLocalTrustDir(File localConfigDir) {
        return new File(localConfigDir, "trust");
    }

    public static File getLocalKeysDir(File localConfigDir) {
        return new File(localConfigDir, "local-keys");
    }
}
