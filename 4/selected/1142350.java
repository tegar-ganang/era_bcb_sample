package it.rm.bracco.pipeline.indexingPipe.terrier;

import java.io.BufferedReader;
import org.terrier.utility.ApplicationSetup;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.swing.JPopupMenu.Separator;

public class BraccoTerrierUtils {

    public static void copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
            System.out.print("Overwrite existing file " + toFile.getName() + "? (Y/N): ");
            System.out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String response = in.readLine();
            if (!response.equals("Y") && !response.equals("y")) throw new IOException("FileCopy: " + "existing file was not overwritten.");
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**Converte un documento bracco per il momento in uno stream di caratteri*/
    public static ByteArrayInputStream convertToStream(Object obj) {
        ByteArrayInputStream result = null;
        if (obj instanceof it.rm.bracco.pipeline.structures.Document) {
            it.rm.bracco.pipeline.structures.Document documento;
            documento = (it.rm.bracco.pipeline.structures.Document) obj;
            String temp = documento.printDocument();
            String printDocument = "<DOC>" + "<DOCNO>" + documento.getDocID() + "</DOCNO>";
            for (int i = 0; i < temp.length(); i++) {
                printDocument = printDocument + temp.charAt(i);
            }
            printDocument = printDocument + "</DOC>";
            printDocument = new StringBuffer(printDocument).insert(printDocument.length(), "-1").toString();
            System.out.println(printDocument);
            result = new ByteArrayInputStream(printDocument.getBytes());
            printDocument = null;
        }
        return result;
    }

    public static boolean VerifyHomeTerrierDirectory() {
        boolean result = false;
        File dir = new File(".");
        try {
            if (new File(ApplicationSetup.TERRIER_HOME).getCanonicalPath().equals(dir.getCanonicalPath() + "/")) result = true; else {
                if (existElementInDirectory(dir, "var")) {
                    File varDir = new File(dir.getCanonicalPath() + "/" + "var");
                    if (existElementInDirectory(varDir, "index")) result = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean existElementInDirectory(File directory, String element) {
        boolean result = false;
        File[] listaFiles;
        listaFiles = directory.listFiles();
        if (listaFiles.length != 0) {
            for (File f : listaFiles) {
                if (Pattern.matches(element, f.getName())) {
                    result = true;
                }
            }
        }
        return result;
    }

    public static String GetBraccoDirectory() {
        String result = "";
        try {
            result = new File(".").getCanonicalPath() + "/";
        } catch (IOException e) {
            new TerrierIndexerException("Imposible Know the Bracco Directory\n");
            e.printStackTrace();
        }
        return result;
    }

    public static boolean CompatibleTypeIndexedFiles(String indexPath) {
        boolean result = false;
        if ((new File(indexPath + "data.properties")).exists()) {
            if (ApplicationSetup.BLOCK_INDEXING) {
                Properties props = new Properties();
                String indexingClass = "";
                try {
                    props.load(new FileInputStream(indexPath + "data.properties"));
                    if (props.getProperty("index.inverted.class") != null) indexingClass = props.getProperty("index.inverted.class");
                    if (indexingClass.equals("org.terrier.structures.BlockInvertedIndex")) result = true;
                } catch (Exception e) {
                    new TerrierIndexerException("Problemi nel caricamento file .properties");
                    e.printStackTrace();
                }
            } else {
                Properties props = new Properties();
                String indexingClass = "";
                try {
                    props.load(new FileInputStream(indexPath + "data.properties"));
                    if (props.getProperty("index.inverted.class") != null) indexingClass = props.getProperty("index.inverted.class");
                    if (indexingClass.equals("org.terrier.structures.InvertedIndex")) result = true;
                } catch (Exception e) {
                    new TerrierIndexerException("Problemi nel caricamento file .properties");
                    e.printStackTrace();
                }
            }
        } else {
            result = true;
        }
        return result;
    }

    public int getNumMerge(String indexPrefix, String braccoTempPrefix, String indexPath) {
        int result = 0;
        String number = "";
        int max = 0;
        File[] listaFiles;
        String readPrefix = indexPrefix + braccoTempPrefix + "[1-9][.](.*)";
        try {
            if ((new File(indexPath + "data.properties")).exists()) {
                listaFiles = new File(indexPath).listFiles();
                if (listaFiles.length != 0) {
                    for (File f : listaFiles) {
                        if (Pattern.matches(readPrefix, f.getName())) {
                            char c = 0;
                            int i = (indexPrefix + braccoTempPrefix).length();
                            if (f.getName().length() >= i) {
                                number = "";
                                while (c != '.') {
                                    if (i != (indexPrefix + braccoTempPrefix).length()) number = number + c;
                                    c = f.getName().charAt(i);
                                    i++;
                                }
                            }
                            try {
                                if (BraccoTerrierUtils.isInteger(number) && number != "") if (max < Integer.parseInt(number)) max = Integer.parseInt(number);
                            } catch (Exception e) {
                                new TerrierIndexerException("Problemi col Parsing del numero di Documenti fusi");
                                e.printStackTrace();
                            }
                        }
                    }
                }
                result = max;
            }
        } catch (Exception e) {
            new TerrierIndexerException("Problemi con la lettura dell/gli indice/i");
            e.printStackTrace();
        }
        return result;
    }
}
