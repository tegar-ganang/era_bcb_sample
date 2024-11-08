package photobook.data.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * <p>Cette classe offre des m�thodes de compression/d�compression et de manipulation de
 * fichiers compress�s. Le format manipul� est le format ZIP.</p>
 * TODO Checksum
 * @author Tony Khosravi Dehkourdi
 * @version 0.1
 *
 */
public class ZipUtil {

    private static final int BUFFER = 2048;

    private static final int COMPRESSION_METHOD = ZipOutputStream.DEFLATED;

    private static final int COMPRESSION_LEVEL = Deflater.NO_COMPRESSION;

    private static final String SYSTEM_SEP = System.getProperty("file.separator");

    private static final String TEMP_DIR_PATH = System.getProperty("user.dir") + SYSTEM_SEP + "temp";

    private static final String TAG = ZipUtil.class.getName();

    private static Logger logger = Logger.getLogger(TAG);

    /**
	 * <p>Cette m�thode permet de compresser une liste de fichiers vers le chemin 
	 * sp�cifi�. Si l'archive existe d�j� au chemin, elle est remplac�e.</p>
	 * @param files la liste de fichiers � compresser
	 * @param output le chemin de destination de l'archive
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void zipFiles(File[] files, String output) throws FileNotFoundException, IOException {
        File previousFile = new File(output);
        if (previousFile.exists()) previousFile.delete();
        FileOutputStream dest = new FileOutputStream(output);
        BufferedOutputStream out = new BufferedOutputStream(dest);
        ZipOutputStream zipout = new ZipOutputStream(out);
        zipout.setMethod(COMPRESSION_METHOD);
        zipout.setLevel(COMPRESSION_LEVEL);
        for (int i = 0; i < files.length; i++) zipFile(files[i], zipout, null);
        zipout.close();
        out.close();
        dest.close();
    }

    /**
	 * Cette m�thode permet d'ajouter un fichier � un flux de type ZipOutputStream. Il
	 * est possible d'ajouter le fichier dans un r�pertoire pour conserver une 
	 * arborescence gr�ce au param�tre <em>parentDir</em>. 
	 * @param file le fichier � rajouter
	 * @param zipout le flux auquel rajouter le fichier
	 * @param parentDir le r�pertoire parent dans lequel mettre le fichier. Si null, le
	 * fichier sera plac� � la racine du r�pertoire courant.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static void zipFile(File file, ZipOutputStream zipout, String parentDir) throws FileNotFoundException, IOException {
        if (file.isDirectory()) {
            logger.info(TAG + ": Le fichier " + file.getName() + " est un r�pertoire.");
            String finalFileName;
            if (parentDir == null) finalFileName = file.getName(); else finalFileName = parentDir + SYSTEM_SEP + file.getName();
            ZipEntry entry = new ZipEntry(finalFileName + "/");
            zipout.putNextEntry(entry);
            logger.info(TAG + ": Ajout de l'entr�e " + entry.getName());
            File[] dirFiles = file.listFiles();
            for (int i = 0; i < dirFiles.length; i++) zipFile(dirFiles[i], zipout, finalFileName);
            zipout.closeEntry();
        } else {
            byte data[] = new byte[BUFFER];
            FileInputStream src = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(src, BUFFER);
            String dir;
            if (parentDir == null) dir = ""; else dir = parentDir + SYSTEM_SEP;
            logger.info(TAG + ": Valeur du r�pertoire parent : " + dir);
            ZipEntry entry = new ZipEntry(dir + file.getName());
            zipout.putNextEntry(entry);
            logger.info(TAG + ": Ajout de l'entr�e " + entry.getName());
            int count;
            while ((count = in.read(data, 0, BUFFER)) != -1) zipout.write(data, 0, count);
            zipout.closeEntry();
            in.close();
        }
    }

    /**
	 * Cette m�thode permet d'extraire les fichiers d'une archive dans un r�pertoire en
	 * respectant l'arborescence des entr�es et de renvoyer la liste des fichiers
	 * @param input l'archive � extraire
	 * @param tempDirectory le r�pertoire de destination de l'extraction
	 * @return la liste des fichiers extraits sous forme d'une Map<String, File>. Le couple
	 * <String, File> correspond au couple <Entr�e, Fichier>. Si un r�pertoire est extrait 
	 * de l'archive, seul le r�pertoire au plus au point de l'arborescence est renvoy�.
	 * @throws IOException
	 */
    public static Map<String, File> extractFiles(String input, File tempDirectory) throws IOException {
        byte data[] = new byte[BUFFER];
        BufferedOutputStream out = null;
        FileInputStream src = new FileInputStream(input);
        BufferedInputStream in = new BufferedInputStream(src);
        ZipInputStream zipin = new ZipInputStream(in);
        Map<String, File> files = new HashMap<String, File>();
        ZipEntry entry;
        while ((entry = zipin.getNextEntry()) != null) {
            logger.info(TAG + ": entr�e " + entry.getName() + " r�pertoire ? " + entry.isDirectory());
            if (entry.isDirectory()) {
                logger.info(TAG + ": Ajout de l'entr�e pour le r�pertoire: " + entry.getName());
                files.put(entry.getName(), extractDirectory(entry.getName(), zipin, tempDirectory));
                File f = files.get(entry.getName());
                if (f == null) logger.info(TAG + ": NULLL: ");
                continue;
            }
            File tempFile = new File(tempDirectory, entry.getName());
            if (tempFile.exists()) tempFile.delete();
            tempFile.createNewFile();
            FileOutputStream dest = new FileOutputStream(tempFile);
            out = new BufferedOutputStream(dest, BUFFER);
            int count;
            for (int c = zipin.read(); c != -1; c = zipin.read()) dest.write(c);
            logger.info(TAG + ": Ajout de l'entr�e: " + entry.getName() + " du fichier: " + tempFile.getAbsolutePath());
            files.put(entry.getName(), tempFile);
            out.close();
            dest.close();
        }
        zipin.close();
        in.close();
        src.close();
        return files;
    }

    /**
	 * Cette m�thode permet d'extraire un r�pertoire contenu dans un archive en respectant
	 * l'arborescence
	 * @param directoryName le nom du r�pertoire � extraire
	 * @param zipin le zipinputstream dont est extrait le r�pertoire
	 * @param parentDir le r�pertoire parent du r�pertoire
	 * @return le r�pertoire extrait
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static File extractDirectory(String directoryName, ZipInputStream zipin, File parentDir) throws FileNotFoundException, IOException {
        byte data[] = new byte[BUFFER];
        BufferedOutputStream out = null;
        String formattedDirectoryName = directoryName.substring(0, directoryName.length() - 1);
        logger.info(TAG + ": Nom du r�pertoire: " + formattedDirectoryName);
        logger.info(TAG + ": Nom du r�pertoire parent: " + parentDir.getAbsolutePath());
        File f = new File(parentDir, formattedDirectoryName);
        if (!f.exists()) {
            if (!new File(parentDir, formattedDirectoryName).mkdir()) {
                logger.warning(TAG + ": Impossible de cr�er le fichier: ");
                return null;
            }
        }
        ZipEntry entry;
        while ((entry = zipin.getNextEntry()) != null && entry.getName().startsWith(formattedDirectoryName)) {
            if (entry.isDirectory()) {
                new File(parentDir, entry.getName()).mkdir();
                continue;
            }
            File tempFile = new File(parentDir, entry.getName());
            if (tempFile.exists()) tempFile.delete();
            tempFile.createNewFile();
            FileOutputStream dest = new FileOutputStream(tempFile);
            out = new BufferedOutputStream(dest, BUFFER);
            int count;
            while ((count = zipin.read(data, 0, BUFFER)) != -1) out.write(data, 0, BUFFER);
            out.flush();
            out.close();
            zipin.closeEntry();
        }
        return new File(parentDir, formattedDirectoryName);
    }

    /**
	 * Cette m�thode permet de r�cup�rer la liste des entr�e d'une archive. L'int�r�t
	 * de cette m�thode r�side dans les tests pour v�rifier que les entr�es ont �t�
	 * correctement entr�e.
	 * @param inputPath l'archive source
	 * @return la liste des entr�es de l'archive
	 * @throws IOException
	 */
    public static String[] getEntriesName(String inputPath) throws IOException {
        ZipFile zipfile = new ZipFile(inputPath);
        Enumeration entries = zipfile.entries();
        String[] entriesName = new String[zipfile.size()];
        int count = 0;
        while (entries.hasMoreElements()) {
            ZipEntry e = ((ZipEntry) entries.nextElement());
            entriesName[count] = e.getName();
            count++;
        }
        zipfile.close();
        return entriesName;
    }
}
