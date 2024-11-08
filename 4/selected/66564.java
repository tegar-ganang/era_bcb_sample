package com.nhncorp.cubridqa.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.eclipse.swt.widgets.TreeItem;
import com.nhncorp.cubridqa.console.util.CommandUtil;
import com.nhncorp.cubridqa.model.ExcelModel;
import com.nhncorp.cubridqa.model.Resource;
import com.nhncorp.cubridqa.model.ScheduleModel;
import com.nhncorp.cubridqa.replication.config.ShellFileMaker;

/**
 * 
 * The utility class provides file operation types.
 * @ClassName: FileUtil
 * @date 2009-9-1
 * @version V1.0 Copyright (C) www.nhn.com
 */
public class FileUtil {

    private static String localPath = PropertiesUtil.getValue("local.path");

    private static final String cronFilePath = System.getProperty("user.home") + "/cron.cubrid";

    private static String rootNodeName = PropertiesUtil.getValue("rootnode.name");

    /**
	 * replace file separation character "\" to "/"
	 * 
	 * @param filePath
	 * @return
	 */
    public static String changeFilePath(String filePath) {
        return filePath.replaceAll("\\\\", "/");
    }

    /**
	 * 
	 */
    public static void createXml() {
        Element element = FileUtil.createDesc(new File(PropertiesUtil.getValue("local.path")));
        Document d = DocumentHelper.createDocument();
        d.add(element);
        addProcedure(d);
        File descFile = new File(FilenameUtils.concat(PropertiesUtil.getValue("local.path"), "scenario.xml"));
        OutputStream os = null;
        try {
            os = new FileOutputStream(descFile);
            String str = d.asXML();
            os.write(str.getBytes("utf8"));
        } catch (Exception e1) {
            System.out.println(e1.getStackTrace());
        } finally {
            Closer.close(os);
        }
    }

    public static void addProcedure(Document document) {
        Node chooseBuildNode = document.selectSingleNode(PropertiesUtil.getValue("rootnode.name") + "/procedure/choose_build");
        Node chooseTestCaseNode = document.selectSingleNode(PropertiesUtil.getValue("rootnode.name") + "/procedure/choose_test_case");
        Node configurationNode = document.selectSingleNode(PropertiesUtil.getValue("rootnode.name") + "/procedure/configuration");
        Node createDbNode = document.selectSingleNode(PropertiesUtil.getValue("rootnode.name") + "/procedure/create_db");
        Node prePostWorkNode = document.selectSingleNode(PropertiesUtil.getValue("rootnode.name") + "/procedure/pre_post_work");
        Node regularNode = document.selectSingleNode(PropertiesUtil.getValue("rootnode.name") + "/schedule/regular");
        Node irregularNode = document.selectSingleNode(PropertiesUtil.getValue("rootnode.name") + "/schedule/irregular");
        File chooseBuildFile = new File(PropertiesUtil.getValue("local.path") + "/procedure/choose_build");
        File chooseTestCaseFile = new File(PropertiesUtil.getValue("local.path") + "/procedure/choose_test_case");
        File configurationFile = new File(PropertiesUtil.getValue("local.path") + "/procedure/configuration");
        File createDbFile = new File(PropertiesUtil.getValue("local.path") + "/procedure/create_db");
        File prePostWorkFile = new File(PropertiesUtil.getValue("local.path") + "/procedure/pre_post_work");
        File regularFile = new File(PropertiesUtil.getValue("local.path") + "/schedule/regular");
        File irregularFile = new File(PropertiesUtil.getValue("local.path") + "/schedule/irregular");
        Map<File, Node> map = new HashMap<File, Node>();
        map.put(chooseBuildFile, chooseBuildNode);
        map.put(chooseTestCaseFile, chooseTestCaseNode);
        map.put(configurationFile, configurationNode);
        map.put(createDbFile, createDbNode);
        map.put(prePostWorkFile, prePostWorkNode);
        map.put(regularFile, regularNode);
        map.put(irregularFile, irregularNode);
        Set<File> set = map.keySet();
        for (File file : set) {
            if (file != null && file.exists()) {
                Node node = map.get(file);
                File[] files = file.listFiles();
                Arrays.sort(files);
                if (files != null && files.length > 0) {
                    for (File subFile : files) {
                        if (subFile.isFile() && subFile.getAbsolutePath().endsWith(".xml")) {
                            ((Element) node).add(DocumentHelper.createElement(subFile.getName()));
                        }
                    }
                }
            }
        }
    }

    /**
	 * generate the element of special directory Recursively.
	 * <p>
	 * for example :{ a/c/d => <a><b><c></c></b><a> }
	 * 
	 * @param f
	 *            the directory path
	 * @return
	 */
    public static Element createDesc(File f) {
        Element e = DocumentHelper.createElement(f.getName());
        Pattern pattern = Pattern.compile("[0-9]*");
        if (f.isDirectory() && hasDirectory(f)) {
            File[] files = f.listFiles();
            Arrays.sort(files);
            for (File file : files) {
                if (file.isDirectory() && file.getAbsolutePath().indexOf(".svn") < 0 && !file.getName().startsWith(".") && !pattern.matcher(file.getName().substring(0, 1)).matches()) {
                    e.add(createDesc(file));
                }
            }
        }
        return e;
    }

    /**
	 * determine if the file contains directory .
	 * 
	 * @param f
	 * @return boolean
	 */
    public static boolean hasDirectory(File f) {
        File[] files = f.listFiles();
        Arrays.sort(files);
        for (File file : files) {
            if (file.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    /**
	 * determine if the file is directory .
	 * 
	 * @param path
	 * @return
	 */
    public static boolean isDirectory(String path) {
        return new File(path).isDirectory();
    }

    /**
	 * get the file Object through the tree item .
	 * 
	 * @param item
	 * @return
	 */
    public static File getFile(TreeItem item) {
        String string = "";
        String text = item.getText();
        while (item.getParentItem() != null) {
            string = getPath(string, item.getParentItem());
            item = item.getParentItem();
        }
        String[] strings = string.split("/");
        String path = "";
        for (int i = strings.length - 1; i > 0; i--) {
            path = path + "/" + strings[i];
        }
        path = PropertiesUtil.getValue("local.path") + path.substring(FilenameUtils.getPrefixLength(path)).toLowerCase() + "/" + text.substring(FilenameUtils.getPrefixLength(text)).toLowerCase();
        File file = new File(path);
        return file;
    }

    /**
	 * get the scenario file through the tree item .
	 * 
	 * @param item
	 * @return
	 */
    public static File getScenarioFile(TreeItem item) {
        String string = "";
        String text = item.getText();
        while (item.getParentItem() != null) {
            string = getPath(string, item.getParentItem());
            item = item.getParentItem();
        }
        String[] strings = string.split("/");
        String path = "";
        for (int i = strings.length - 1; i > 0; i--) {
            path = path + "/" + strings[i];
        }
        path = PropertiesUtil.getValue("local.path") + "scenario/" + path.substring(FilenameUtils.getPrefixLength(path)).toLowerCase() + "/" + text.substring(FilenameUtils.getPrefixLength(text)).toLowerCase();
        File file = new File(path);
        return file;
    }

    /**
	 * get the absolute path of the string through the resource.
	 * 
	 * @param resource
	 * @param string
	 * @return
	 */
    public static String getPath(Resource resource, String string) {
        String path = "";
        if (resource == null) {
            path = localPath + string;
        } else {
            String parentPath = resource.getName();
            path = parentPath + "/" + string;
        }
        return path;
    }

    /**
	 * get the path of the tree item .
	 * 
	 * @param string
	 * @param treeItem
	 * @return
	 */
    public static String getPath(String string, TreeItem treeItem) {
        string = string + "/" + treeItem.getText();
        return string;
    }

    /**
	 * 
	 * 
	 * @param resource
	 * @return
	 */
    public static String getXpath(Resource resource) {
        String xpath = resource.getName();
        xpath = xpath.substring(xpath.indexOf(rootNodeName), xpath.length());
        return xpath;
    }

    /**
	 * read the file content and convert to string.
	 * 
	 * @param filePath
	 * @return
	 */
    public static String readFileContent(String filePath) {
        String text = "";
        BufferedReader reader = null;
        try {
            File file = new File(filePath);
            if (file != null && file.exists() && file.isFile()) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "utf-8"));
                StringBuffer buffer = new StringBuffer();
                int n;
                while ((n = reader.read()) != -1) {
                    buffer.append((char) n);
                }
                text = buffer.toString();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return text;
    }

    /**
	 * read the file content and convert to Object list.
	 * 
	 * @param filePath
	 * @param object
	 * @return
	 */
    public static List<Object> readFile(String filePath, Object object) {
        if (filePath == null) {
            filePath = "";
        }
        filePath = filePath.replaceAll(" ", "");
        File file = new File(filePath);
        List<Object> list = new ArrayList<Object>();
        if (file != null && file.getAbsolutePath() != null && !file.getAbsolutePath().equals("") && file.exists() && !file.isDirectory()) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "utf-8"));
                String buffer = "";
                while ((buffer = bufferedReader.readLine()) != null) {
                    list.add(buffer);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (list.size() == 0) {
                list.add("");
            }
            return list;
        } else {
            return new ArrayList<Object>();
        }
    }

    /**
	 * get the file content convert to list .
	 * 
	 * @param filePath
	 * @param object
	 * @return
	 */
    public static List<Object> readFileAddLine(String filePath, Object object) {
        if (filePath == null) {
            filePath = "";
        }
        filePath = filePath.replaceAll(" ", "");
        File file = new File(filePath);
        List<Object> list = new ArrayList<Object>();
        if (file != null && file.getAbsolutePath() != null && !file.getAbsolutePath().equals("") && file.exists() && !file.isDirectory()) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "utf-8"));
                String buffer = "";
                int lineNumber = 1;
                while ((buffer = bufferedReader.readLine()) != null) {
                    if (buffer.startsWith("==========")) {
                        if (lineNumber < 10) {
                            buffer = buffer.replaceFirst("=", "[  " + lineNumber + "  ]  ");
                        } else if (lineNumber < 100) {
                            buffer = buffer.replaceFirst("=", "[  " + lineNumber + " ]  ");
                        } else {
                            buffer = buffer.replaceFirst("=", "[ " + lineNumber + " ]  ");
                        }
                        lineNumber++;
                    }
                    list.add(buffer);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (list.size() == 0) {
                list.add("");
            }
            return list;
        } else {
            return new ArrayList<Object>();
        }
    }

    /**
	 * read the file content and convert to string.
	 * 
	 * @deprecated
	 * @param filePath
	 * @param object
	 *            default is null.
	 * @return
	 */
    public static String readFileAddLines(String filePath, Object object) {
        if (filePath == null) {
            filePath = "";
        }
        filePath = filePath.replaceAll(" ", "");
        File file = new File(filePath);
        StringBuffer stringBuffer = new StringBuffer("");
        if (file != null && file.getAbsolutePath() != null && !file.getAbsolutePath().equals("") && file.exists() && !file.isDirectory()) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "utf-8"));
                String buffer = "";
                int lineNumber = 1;
                while ((buffer = bufferedReader.readLine()) != null) {
                    if (buffer.startsWith("==========")) {
                        if (lineNumber < 10) {
                            buffer = buffer.replaceFirst("=", "[  " + lineNumber + "  ]  ");
                        } else if (lineNumber < 100) {
                            buffer = buffer.replaceFirst("=", "[  " + lineNumber + " ]  ");
                        } else {
                            buffer = buffer.replaceFirst("=", "[ " + lineNumber + " ]  ");
                        }
                        lineNumber++;
                    }
                    stringBuffer.append(buffer + System.getProperty("line.separator"));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return stringBuffer.toString();
    }

    /**
	 * get object list from file .
	 * 
	 * @param filePath
	 * @return
	 */
    public static Object[] readFile(String filePath) {
        if (filePath == null) {
            filePath = "";
        }
        filePath = filePath.replaceAll(" ", "");
        File file = new File(filePath);
        List<String> list = new ArrayList<String>();
        if (file != null && file.getAbsolutePath() != null && !file.getAbsolutePath().equals("") && file.exists() && !file.isDirectory()) {
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "utf-8"));
                String buffer = "";
                while ((buffer = bufferedReader.readLine()) != null) {
                    list.add(buffer);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return list.toArray();
        } else {
            return new String[0];
        }
    }

    /**
	 * recursively get the file list.
	 * 
	 * @param list
	 * @param rootFile
	 * @return
	 */
    public static List<File> getAllFile(List<File> list, File rootFile) {
        if (rootFile.getAbsolutePath().indexOf(".svn") < 0) {
            list.add(rootFile);
            if (rootFile.isDirectory()) {
                File[] files = rootFile.listFiles();
                Arrays.sort(files);
                for (File file : files) {
                    getAllFile(list, file);
                }
            }
        }
        return list;
    }

    /**
	 * Recursively delete the .svn file in the root path.
	 * 
	 * @param rootPath
	 */
    public static void disconnect(String rootPath) {
        File rootFile = new File(rootPath);
        if (!rootFile.isDirectory()) {
            if (rootPath.indexOf(".svn") >= 0) {
                rootFile.delete();
            }
        } else {
            if (rootPath.indexOf(".svn") > 0) {
                FileDeleteUtil.deleteFileAndFolder(rootPath);
            } else {
                File[] files = rootFile.listFiles();
                Arrays.sort(files);
                for (File file : files) {
                    disconnect(file.getAbsolutePath());
                }
            }
        }
    }

    /**
	 * write the content to file .
	 * 
	 * @param content
	 *            the content be written to file .
	 * @param path
	 *            the file path .
	 * @return
	 */
    public static File writeFile(String content, String path) {
        File f = new File(path);
        if (!f.exists()) {
            try {
                if (f.isFile()) {
                    File dir = new File(f.getParent());
                    if (!dir.exists()) dir.mkdirs();
                }
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return f;
    }

    /**
	 * write multiline data to file .
	 * 
	 * @param lines
	 * @param path
	 */
    public static void writeFile(List<String> lines, String path) {
        File f = new File(path);
        BufferedWriter bw = null;
        String separator = System.getProperty("line.separator");
        if (!f.exists() && lines.size() > 0) {
            try {
                f.createNewFile();
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
                for (String line : lines) {
                    bw.write(line + separator);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
	 * delete the schedule which be used to execute on special time through the
	 * scheduleModel .
	 * 
	 * @param scheduleModel
	 */
    public static void deleteSchedule(ScheduleModel scheduleModel) {
        if (scheduleModel.getParent().getId().equalsIgnoreCase("regular") || scheduleModel.getParent().getId().equalsIgnoreCase("irregular")) {
            String cmd = "";
            String systemInfo = EnvGetter.getenv().get("OS");
            if (systemInfo != null) {
                cmd = "SCHTASKS /delete /TN " + scheduleModel.getId() + " /F";
                CommandUtil.execute(new String[] { cmd, "y" }, null);
            } else {
                String separator = System.getProperty("line.separator");
                List<String> list = new Vector<String>();
                for (String line : list) {
                    if (!line.replaceAll(" ", "").endsWith(scheduleModel.getId() + ".xml")) {
                        cmd = cmd + line;
                    }
                }
                cmd = cmd + separator;
                FileUtil.writeFile(cmd, cronFilePath);
                String setCron = "crontab " + cronFilePath;
                executeCmd(new String[] { "/bin/bash", "-c", setCron });
                FileDeleteUtil.deleteFile(new File(cronFilePath));
            }
        }
    }

    /**
	 * execute the console command and get the response of execute .
	 * 
	 * @param commands
	 * @return
	 */
    public static List<String> executeCmd(String[] commands) {
        ShellFileMaker.executeShell(commands);
        List<String> list = ShellFileMaker.getList();
        return list;
    }

    /**
	 * copy file to another place .
	 * 
	 * @param sourceFile
	 * @param aimFile
	 */
    public static void copyFile(String sourceFile, String aimFile) {
        writeFile(readFileContent(sourceFile), aimFile);
    }

    /**
	 * generate excel workbook through ExcelModel
	 * 
	 * @param model
	 * @return
	 */
    public static HSSFWorkbook generateExcel(ExcelModel model) {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Compare Result");
        List<List<String>> lines = model.getLines();
        for (int i = 0; i < lines.size(); i++) {
            HSSFRow row = sheet.createRow(i);
            List<String> values = lines.get(i);
            for (int j = 0; j < values.size(); j++) {
                HSSFCell cell = row.createCell((short) j);
                cell.setCellType(HSSFCell.CELL_TYPE_STRING);
                cell.setEncoding(HSSFCell.ENCODING_UTF_16);
                cell.setCellValue(values.get(j));
            }
        }
        return workbook;
    }

    /**
	 * determine if the os is linux or not .
	 * 
	 * @return
	 */
    public static boolean isLinux() {
        boolean isLinux = true;
        String systemInfo = EnvGetter.getenv().get("OS");
        if (systemInfo != null) {
            String string = EnvGetter.getenv().get("OS").toLowerCase();
            if (string.indexOf("windows") >= 0) {
                isLinux = false;
            }
        }
        return isLinux;
    }

    /**
	 * get the target file located in a directory .
	 * 
	 * @param srcDirectory
	 * @param aimFile
	 * @return
	 */
    public static List<File> getYourWantFiles(String srcDirectory, String aimFile) {
        List<File> list = new ArrayList<File>();
        File src = new File(srcDirectory);
        if (src.exists() && src.isDirectory()) {
            getChildrenFiles(src, list, aimFile);
        }
        return list;
    }

    /**
	 * get the target file in child directory.
	 * 
	 * @param file
	 *            the parent directory
	 * @param list
	 *            the reference to the file result .
	 * @param aimFile
	 */
    private static void getChildrenFiles(File file, List<File> list, String aimFile) {
        if (file.exists() && file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File child : listFiles) {
                if (child.isDirectory()) {
                    if (child.getName().equals(aimFile)) {
                        list.add(child);
                    } else {
                        getChildrenFiles(child, list, aimFile);
                    }
                }
            }
        }
    }

    /**
	 * get the target file which contain special contents from source directory.
	 * 
	 * @param srcDirectory
	 * @param contents
	 * @return
	 */
    public static List<File> getYourWantFilesByContents(String srcDirectory, String contents) {
        List<File> list = new ArrayList<File>();
        File src = new File(srcDirectory);
        if (src.exists() && src.isDirectory()) {
            getChildrenFilesByContents(src, list, contents);
        }
        return list;
    }

    /**
	 * set the files to list which contain special content in special directory.
	 * 
	 * @param file
	 * @param list
	 * @param contents
	 */
    private static void getChildrenFilesByContents(File file, List<File> list, String contents) {
        if (file.exists() && file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File child : listFiles) {
                if (child.isDirectory()) {
                    getChildrenFilesByContents(child, list, contents);
                } else {
                    String readFileContent = readFileContent(child.getAbsolutePath());
                    if (readFileContent.contains(contents) && !child.getAbsolutePath().endsWith(".class") && !child.getAbsolutePath().endsWith(".svn-base")) {
                        list.add(child);
                    }
                }
            }
        }
    }

    /**
	 * replace the content in file with new content you specified.
	 * 
	 * @param filePath
	 * @param oldContents
	 * @param newContents
	 */
    public static void changeContents(String filePath, String oldContents, String newContents) {
        Object[] lines = readFile(filePath);
        List<String> list = new ArrayList<String>();
        for (int i = 1; i < lines.length + 1; i++) {
            String line = (String) lines[i - 1];
            if (line.contains(oldContents)) {
                line = line.replaceAll(oldContents, newContents);
            }
            list.add(line);
        }
        FileDeleteUtil.deleteFile(filePath);
        writeFile(list, filePath);
    }
}
