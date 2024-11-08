package br.com.caelum.jambo.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Random;
import org.apache.log4j.Logger;
import org.vraptor.Interceptor;
import br.com.caelum.jambo.annotations.Ignore;

public class Utilities {

    private static final Logger logger = Logger.getLogger(Utilities.class);

    private static Random random = new Random();

    public static void copy(File src, File dest) throws IOException {
        OutputStream stream = new FileOutputStream(dest);
        FileInputStream fis = new FileInputStream(src);
        byte[] buffer = new byte[16384];
        while (fis.available() != 0) {
            int read = fis.read(buffer);
            stream.write(buffer, 0, read);
        }
        stream.flush();
    }

    /**
	 * Delete directory and all files and folders inside it
	 * 
	 * @param file
	 *            a directory
	 */
    public static void rmdirRecursive(File file) {
        logger.debug("Deleting file " + file.getName());
        if (!file.exists()) {
            logger.error("Folder " + file.getName() + " doesn't exist.");
            return;
        }
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                rmdirRecursive(f);
            } else {
                if (!f.delete()) {
                    logger.warn("File " + f.getName() + " wasn't deleted.");
                }
            }
        }
        if (!file.delete()) {
            logger.warn("Folder " + file.getName() + " wasn't deleted.");
        }
    }

    public static boolean isIgnored(Ignore ignore, Class<? extends Interceptor> interceptor) {
        if (ignore == null) {
            return false;
        }
        for (Class<? extends Interceptor> c : ignore.value()) {
            if (c.equals(interceptor)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }

    /**
	 * Generates an Hex String from 1000000 to fffffff
	 * 
	 * @return
	 */
    public static String generateKey() {
        String key = String.valueOf(Integer.toHexString(Utilities.random.nextInt(251658240) + 16777216));
        return key;
    }

    public static String getFileType(String fileName) {
        int index = fileName.length() - 1;
        while (index >= 0) {
            if (fileName.charAt(index) == '.') {
                return fileName.substring(index + 1).toLowerCase();
            } else {
                index--;
            }
        }
        return null;
    }

    /**
	 * To replace accented characters in a String by unaccented equivalents.
	 * From org.apache.lucene.analysis.ISOLatin1AccentFilter, changed for UTF8 support
	 */
    public static final String removeAccents(String input) {
        final StringBuffer output = new StringBuffer();
        for (int i = 0; i < input.length(); i++) {
            switch(input.charAt(i)) {
                case 'À':
                case 'Á':
                case 'Â':
                case 'Ã':
                case 'Ä':
                case 'Å':
                    output.append("A");
                    break;
                case 'Æ':
                    output.append("AE");
                    break;
                case 'Ç':
                    output.append("C");
                    break;
                case 'È':
                case 'É':
                case 'Ê':
                case 'Ë':
                    output.append("E");
                    break;
                case 'Ì':
                case 'Í':
                case 'Î':
                case 'Ï':
                    output.append("I");
                    break;
                case 'Ð':
                    output.append("D");
                    break;
                case 'Ñ':
                    output.append("N");
                    break;
                case 'Ò':
                case 'Ó':
                case 'Ô':
                case 'Õ':
                case 'Ö':
                case 'Ø':
                    output.append("O");
                    break;
                case 'Œ':
                    output.append("OE");
                    break;
                case 'Þ':
                    output.append("TH");
                    break;
                case 'Ù':
                case 'Ú':
                case 'Û':
                case 'Ü':
                    output.append("U");
                    break;
                case 'Ý':
                case 'Ÿ':
                    output.append("Y");
                    break;
                case 'à':
                case 'á':
                case 'â':
                case 'ã':
                case 'ä':
                case 'å':
                    output.append("a");
                    break;
                case 'æ':
                    output.append("ae");
                    break;
                case 'ç':
                    output.append("c");
                    break;
                case 'è':
                case 'é':
                case 'ê':
                case 'ë':
                    output.append("e");
                    break;
                case 'ì':
                case 'í':
                case 'î':
                case 'ï':
                    output.append("i");
                    break;
                case 'ð':
                    output.append("d");
                    break;
                case 'ñ':
                    output.append("n");
                    break;
                case 'ò':
                case 'ó':
                case 'ô':
                case 'õ':
                case 'ö':
                case 'ø':
                    output.append("o");
                    break;
                case 'œ':
                    output.append("oe");
                    break;
                case 'ß':
                    output.append("ss");
                    break;
                case 'þ':
                    output.append("th");
                    break;
                case 'ù':
                case 'ú':
                case 'û':
                case 'ü':
                    output.append("u");
                    break;
                case 'ý':
                case 'ÿ':
                    output.append("y");
                    break;
                default:
                    output.append(input.charAt(i));
                    break;
            }
        }
        return output.toString();
    }
}
