package autoExport;

import java.io.*;
import java.util.Hashtable;
import com.linuxense.javadbf.*;

/*** The secret is into at file S7CONTAI.DBF (old DataBases file): 
 *   There is a pairing of the file name given by STEP 7 and symbolic name (human readable)!
 *
 * Verion 1.0: 
 *   AutoExport use javaDbf.jar [http://sarovar.org/projects/javadbf] extract the pairing and make a copy of file 
 *   from "Siemens Step7 style" to human Readable.
 *   Eg: From 000023.AWL to FBTest.AWL
 *
 * Version 2.0:
 *   Added functionality opposite: copy files with the symbolic name with the name given STEP7.
 *   Eg: From FBTest.AWL to 000023.AWL
 *   
 * @author PaoloG77
 *
 */
public class AutoExportFile {

    String NameFile = new String("S7CONTAI.DBF");

    String DirIn = new String("");

    String DirOut = new String("");

    String S7ContaiDir = new String("");

    boolean dirDivision = true;

    boolean export = true;

    Hashtable<String, String> correspondance = new Hashtable<String, String>();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("\n\nUsage:");
            System.out.println("  Export file from logicPATH into ExportPATH:");
            System.out.println("  \tjava -jar AutoExport-version.jar LogicPATH ExportPATH");
            System.out.println("  or import:");
            System.out.println("  \tjava -jar AutoExport-version.jar -r S7ContaiPATH ExportPATH DestPATH");
            System.out.println("\n\nEXAMPLE export:");
            System.out.println("java -jar AutoExport-1.0.jar c:\\logicDir\\s7asrcom\\0000000e c:\\logicDir\\export");
            System.out.println("OR");
            System.out.println("java -jar AutoExport-1.0.jar c:/logicDir/s7asrcom/0000000e c:/logicDir/export\n");
            System.out.println("\nEXAMPLE import:");
            System.out.println("java -jar AutoExport-1.0.jar -r c:\\logicDir c:\\logicDir\\export c:\\logicDir\\Temp");
            System.out.println("OR");
            System.out.println("java -jar AutoExport-1.0.jar -r c:/logicDir c:/logicDir/export c:/logicDir/Temp");
            System.exit(1);
        }
        if (!args[0].equalsIgnoreCase("-r")) {
            AutoExportFile ae = new AutoExportFile(args[0], args[1]);
            ae.extractName(false);
            ae.exportFile();
        } else {
            System.out.println("--------------------IMPORT--------------------");
            AutoExportFile ae = new AutoExportFile(args[1], args[2], args[3]);
            if (args[4].length() == 4) ae.dirDivision = Boolean.getBoolean(args[4]);
            ae.setExport(false);
            ae.extractName(false);
            ae.importFile();
        }
    }

    public AutoExportFile(String dirIN, String dirOut) {
        DirIn = new String(dirIN + "/");
        DirOut = new String(dirOut + "/");
        S7ContaiDir = DirIn;
    }

    public AutoExportFile(String S7contaiPATH, String dirIN, String dirOut) {
        DirIn = new String(dirIN + "/");
        DirOut = new String(dirOut + "/");
        S7ContaiDir = new String(S7contaiPATH + "/");
    }

    public void extractFields() {
        try {
            InputStream inputStream = new FileInputStream(S7ContaiDir + NameFile);
            DBFReader reader = new DBFReader(inputStream);
            int numberOfFields = reader.getFieldCount();
            for (int i = 0; i < numberOfFields; i++) {
                DBFField field = reader.getField(i);
                System.out.println(field.getName());
            }
        } catch (DBFException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /*** Extract from DBf file all interesting data and put them into a Hastable ***/
    public void extractName(boolean print) {
        try {
            InputStream inputStream = new FileInputStream(S7ContaiDir + "/" + NameFile);
            DBFReader reader = new DBFReader(inputStream);
            Object[] rowObjects;
            while ((rowObjects = reader.nextRecord()) != null) {
                if (rowObjects[2] != null && rowObjects[7] != null) {
                    if (print) System.out.println(rowObjects[2] + " , " + rowObjects[7]);
                    putIntoTable(((String) rowObjects[7]).trim(), ((String) rowObjects[2]).trim());
                }
            }
            inputStream.close();
        } catch (DBFException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void printTable() {
        System.out.println(this.correspondance.toString());
    }

    public void putIntoTable(String key, String value) {
        int index = 0;
        index = key.indexOf('.');
        if (index > 0) {
            value += key.substring(index);
        }
        if (export) correspondance.put(key, value); else correspondance.put(value, key);
    }

    /*** Make a copy from file @param pathFileIn to file @param pathFileOut***/
    public void copy(String pathFileIn, String pathFileOut) {
        try {
            File inputFile = new File(pathFileIn);
            File outputFile = new File(pathFileOut);
            FileReader in = new FileReader(inputFile);
            File outDir = new File(DirOut);
            if (!outDir.exists()) outDir.mkdirs();
            FileWriter out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
            this.printColumn(inputFile.getName(), outputFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*** List all file in the directory and make the copy with the "new" name ***/
    public void exportFile() {
        File dir = new File(DirIn);
        File subDir = null;
        File[] files;
        FileFilter fileFilter = new FileFilter() {

            public boolean accept(File file) {
                if (!file.getName().startsWith(".") && !file.isDirectory()) {
                    if (file.getName().endsWith(".AWL") || file.getName().endsWith(".SCL")) return true;
                }
                return false;
            }
        };
        files = dir.listFiles(fileFilter);
        for (int i = 0; i < files.length; i++) {
            if (this.correspondance.containsKey(files[i].getName())) {
                if (this.dirDivision) subDir = makeSubDir(files[i].getName()); else subDir = null;
                if (subDir != null) copy(DirIn + files[i].getName(), subDir.getPath() + "/" + correspondance.get(files[i].getName())); else copy(DirIn + files[i].getName(), DirOut + correspondance.get(files[i].getName()));
            }
        }
    }

    /** If "-" are present in nameFile (Eg: FBTest-HI.awl) => make a subdirectory with the same name without "-xx"
	 * (Eg: FBTest) and put file into. 
	 * You can ovverride this method to change the algorithm that creates the subdirectory*/
    public File makeSubDir(String nameFile) {
        File subDir = null;
        int index = (correspondance.get(nameFile)).indexOf("-");
        if (index > 0) {
            String name = correspondance.get(nameFile);
            subDir = new File(DirOut + name.substring(0, index));
            if (!subDir.exists()) subDir.mkdir();
        }
        return subDir;
    }

    public void importFile() {
        importFile(new File(this.DirIn));
    }

    public void importFile(File dir) {
        File[] entries = dir.listFiles();
        System.out.println("dir.toString(): " + dir.toString());
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].isDirectory()) {
                    importFile(entries[i]);
                } else {
                    if (entries[i].getName().endsWith(".AWL") || entries[i].getName().endsWith(".SCL")) {
                        if (this.correspondance.containsKey(entries[i].getName())) {
                            copy(dir + "/" + entries[i].getName(), DirOut + correspondance.get(entries[i].getName()));
                        }
                    }
                }
            }
        }
    }

    public void printColumn(String val1, String val2) {
        String ris = val1;
        if (export) for (int i = 0; i < 20 - val1.length(); i++) ris += " "; else for (int i = 0; i < 30 - val1.length(); i++) ris += " ";
        System.out.println(ris + " --> " + val2);
    }

    public void setExport(boolean export) {
        this.export = export;
    }
}
