package com.patientis.client.deploy;

import java.io.File;
import java.util.*;
import com.patientis.framework.utility.*;
import com.patientis.model.common.*;
import com.patientis.model.reference.*;
import com.patientis.client.service.reference.*;
import org.apache.commons.io.FileUtils;
import com.patientis.model.common.UserModel;

/**
 * One line class description
 *
 * 
 * <br/>  
 */
public class Website {

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.err.println("Website <functionality websitedir>");
                System.exit(1);
            }
            String targetDir = args[0];
            generateHtml(new File(targetDir), "doc", "pdf");
            generateConfigHtml(new File("C:\\dev\\patientis\\trunk\\database\\content\\en\\init"), new File("C:\\dev\\patientis\\trunk\\website\\src\\documentation\\content\\xdocs\\documentation"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Create website functionality xml files
	 * 
	 * @param functionalityDir
	 * @throws Exception
	 */
    private static void generateHtml(File srcDir, String ext1, String ext2) throws Exception {
        if (srcDir.isDirectory()) {
            FileSystemUtil.navigateFilesystem(srcDir, new IHandleFile() {

                public void handleFile(File file) throws Exception {
                    if (file.getAbsolutePath().endsWith("doc")) {
                        if (new File(file.getAbsolutePath().replace(".doc", ".pdf")).exists()) {
                            String line = "<p><b>" + file.getName().replace(".doc", "") + "</b> &nbsp; &nbsp;" + "<img src=\"pdfdoc.gif\"/>" + "<a href=\"" + file.getName().replace(".doc", ".pdf") + "\"> PDF format</a>" + " &nbsp; <img src=\"worddoc.gif\"/>" + "<a href=\"" + file.getName() + "\"> Word format</a></p>";
                            System.out.println(line);
                        }
                    }
                }

                public boolean isValidDir(File fileDir) throws Exception {
                    return true;
                }
            });
        }
    }

    /**
	 * Create website functionality xml files
	 * 
	 * @param functionalityDir
	 * @throws Exception
	 */
    private static void generateConfigHtml(File srcDir, final File targetDir) throws Exception {
        if (srcDir.isDirectory()) {
            FileSystemUtil.navigateFilesystem(srcDir, new IHandleFile() {

                public void handleFile(File file) throws Exception {
                    if (file.getAbsolutePath().endsWith("xls")) {
                        if (new File(file.getAbsolutePath().replace(".xls", ".csv")).exists()) {
                            String newFileName = file.getName().substring(3).replace(" ", "_").toLowerCase();
                            FileUtils.copyFile(file, new File(targetDir.getAbsolutePath() + "/" + newFileName));
                            FileUtils.copyFile(new File(file.getAbsolutePath().replace(".xls", ".csv")), new File(targetDir.getAbsolutePath() + "/" + newFileName.replace(".xls", ".csv")));
                            String line = "<p><b>" + file.getName().substring(3).replace(".xls", "") + " &nbsp; </b>" + "<link href=\"" + newFileName + "\">Excel Format</link> " + "&nbsp; &nbsp;<link href=\"" + newFileName.replace(".xls", ".csv") + "\">CSV Format</link></p>";
                            System.out.println(line);
                        }
                    }
                }

                public boolean isValidDir(File fileDir) throws Exception {
                    return true;
                }
            });
        }
    }

    /**
	 * 
	 * @param module
	 * @return
	 */
    private static String header(String module) {
        return "<?xml version=\"1.0\"?>\r\n" + "<!DOCTYPE document PUBLIC \"-//APACHE//DTD Documentation V1.3//EN\" \"http://forrest.apache.org/dtd/document-v13.dtd\">\r\n" + "<document>\r\n" + "<header><title>" + module + "</title></header>\r\n" + "<body>\r\n";
    }

    /**
	 * 
	 * @return
	 */
    private static String footer() {
        return "</body></document>\r\n";
    }
}
