package webuilder.webx.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import org.webx.project.ProjectDocument;

public class FilesUtil {

    public static String newline = "\r\n";

    public static String replaceAllWords(String original, String find, String replacement) {
        StringBuilder result = new StringBuilder(original.length());
        String delimiters = "+-*/(),. ";
        StringTokenizer st = new StringTokenizer(original, delimiters, true);
        while (st.hasMoreTokens()) {
            String w = st.nextToken();
            if (w.equals(find)) {
                result.append(replacement);
            } else {
                result.append(w);
            }
        }
        return result.toString();
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * copy the system file -SysFilePath- to -UsrFilePath- replace the -find- 
     * place holders in the system file with -replaceBy-
     * @param SysFilePath
     * @param UsrFilePath
     * @param find
     * @param replaceBy
     */
    public static void createCustomFile(String SysFilePath, String UsrFilePath, String[] find, String[] replaceBy) {
        try {
            File dest_temp = new File(UsrFilePath + "_tmp");
            File dest = new File(UsrFilePath);
            dest.createNewFile();
            copyFile(new File(SysFilePath), dest_temp);
            System.out.print("building " + SysFilePath + " file to " + UsrFilePath);
            BufferedReader bufRead = new BufferedReader(new FileReader(dest_temp));
            BufferedWriter bufWrite = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), "UTF8"));
            String all = new String("");
            String line;
            int count = 0;
            line = bufRead.readLine();
            while (line != null) {
                line = new String(line.getBytes(), "UTF8");
                for (int i = 0; i < find.length; i++) {
                    line = replaceAllWords(line, find[i], replaceBy[i]);
                    line = replaceAllWords(line, "<head>", "<head>\n" + WebXConfig.XSL_INSERT_JSCRIPT + "\n");
                }
                all += line + newline;
                count++;
                line = bufRead.readLine();
            }
            bufWrite.write(all);
            bufRead.close();
            bufWrite.close();
            dest_temp.delete();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("find and replaceBy must have equal lengths");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getDBConnectionString(String userName, String password, String hostname, String dbname) {
        return "jdbc:mysql://" + hostname + "/" + dbname + "?user=" + userName + "&password=" + password;
    }

    public static String getDBConnectionString(String userName, String password, String hostname, String dbname, String dbType) {
        return "jdbc:" + dbType + "://" + hostname + "/" + dbname + "?user=" + userName + "&password=" + password;
    }

    public static String getFileString(File f1) {
        String content = "";
        String line = "";
        int count = 0;
        try {
            BufferedReader bufRead = new BufferedReader(new FileReader(f1));
            line = bufRead.readLine();
            while (line != null) {
                line = new String(line.getBytes(), "UTF8");
                content += line + newline;
                count++;
                line = bufRead.readLine();
                System.out.println("line number" + count + ":" + line);
            }
            bufRead.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void copyDirectory(File source, File dest) throws IOException {
        if (!dest.isDirectory()) {
            System.err.println("dest is not a dir");
            return;
        }
        File[] toTrans = source.listFiles();
        for (int i = 0; i < toTrans.length; i++) {
            copyFile(toTrans[i], new File(dest.getAbsolutePath() + "/" + toTrans[i].getName()));
        }
    }

    public static int getFolderIndexByName(String name) {
        return 0;
    }

    public static void saveProjectXmlChanges(ProjectDocument.Project ProjDoc, File projFile) throws FileNotFoundException, IOException {
        FilesUtil.replaceAllWords(ProjDoc.xmlText(), "/xml-fragment", "/pj:project");
        String myXml = ProjDoc.toString().replaceFirst("<xml-fragment xmlns:pj=\"http://webx.org/project\">", "<pj:project xmlns:pj=\"http://webx.org/project\">");
        myXml = myXml.replaceFirst("</xml-fragment>", "</pj:project>");
        System.out.println(myXml);
        BufferedWriter bufWrite = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(projFile)));
        bufWrite.write(myXml);
        bufWrite.close();
    }

    public static void createFilesFromTemplate(File template, String projRoot, String fileName) {
        System.out.println("FilesUtils.createFilesFromTemplate() \n " + projRoot + " \n " + fileName);
        if (template == null) {
            try {
                template = new File(projRoot + "\\" + fileName + ".html");
                template.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String phpFname = FilesUtil.replaceAllWords(template.getName(), "html", "php");
        String xslFname = FilesUtil.replaceAllWords(template.getName(), "html", "xsl");
        File xslFile = new File(projRoot + phpFname);
        File phpFile = new File(projRoot + xslFname);
        try {
            xslFile.createNewFile();
            phpFile.createNewFile();
            FilesUtil.createCustomFile("code_core\\xslt\\new_page.xsl", projRoot + "\\" + xslFname, new String[] { WebXConfig.XSL_TEMPLATE_PLACEHOLDER }, new String[] { FilesUtil.getFileString(template) });
            FilesUtil.createCustomFile("code_core\\php\\new_page.php", projRoot + "\\" + phpFname, new String[] { WebXConfig.PHP_TEMPLATE_NAME_PLACEHOLDER }, new String[] { "'" + xslFname + "'" });
        } catch (IOException e) {
            e.printStackTrace();
        }
        template.delete();
    }

    public static void createFilesFromTemplate(File template, String projRoot) {
        createFilesFromTemplate(template, projRoot, null);
    }
}
