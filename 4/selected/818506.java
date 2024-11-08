package org.hardtokenmgmt.buildtools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility that inserts a module into the application.xml file in a .ear file.
 * 
 * 
 * The program takes three (four if web module) parameters:
 * path-to-ear-file
 * moduletype ('web' or 'ejb')
 * modulename 
 * contextroot (web module only)
 * 
 * @author Philip Vendil
 *
 */
public class UpdateApplicationXML {

    private static final int FROMEAR = 0;

    private static final int TOEAR = 1;

    private static final int MODULETYPE = 2;

    private static final int MODULENAME = 3;

    private static final int CONTEXTROOT = 4;

    private static final String MODULETYPE_WEB = "web";

    private static final String MODULETYPE_EJB = "ejb";

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            displayUsageAndExit();
        }
        File fromEAR = new File(args[FROMEAR]);
        if (!fromEAR.exists() || !fromEAR.canRead() || !fromEAR.isFile()) {
            System.out.println("Error reading EAR, make sure the file " + args[FROMEAR] + " is a file and readable for the user.");
            System.exit(-1);
        }
        File toEAR = new File(args[TOEAR]);
        String moduleType = args[MODULETYPE].toLowerCase();
        if (!moduleType.equals(MODULETYPE_WEB) && !moduleType.equals(MODULETYPE_EJB)) {
            System.out.println("Error, unsupported module type " + args[MODULETYPE] + ".");
            System.exit(-1);
        }
        String moduleName = args[MODULENAME];
        String contextRoot = "";
        if (moduleType.equals(MODULETYPE_WEB)) {
            if (args.length != 5) {
                System.out.println("Error, context root must be specified if web module is inserted .");
                System.exit(-1);
            } else {
                contextRoot = args[CONTEXTROOT];
            }
        }
        insertModuleInEar(fromEAR, toEAR, moduleType, moduleName, contextRoot);
    }

    private static void insertModuleInEar(File fromEar, File toEar, String moduleType, String moduleName, String contextRoot) throws Exception {
        ZipInputStream earFile = new ZipInputStream(new FileInputStream(fromEar));
        FileOutputStream fos = new FileOutputStream(toEar);
        ZipOutputStream tempZip = new ZipOutputStream(fos);
        ZipEntry next = earFile.getNextEntry();
        while (next != null) {
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            byte[] data = new byte[30000];
            int numberread;
            while ((numberread = earFile.read(data)) != -1) {
                content.write(data, 0, numberread);
            }
            if (next.getName().equals("META-INF/application.xml")) {
                content = insertModule(earFile, next, content, moduleType, moduleName, contextRoot);
                next = new ZipEntry("META-INF/application.xml");
            }
            tempZip.putNextEntry(next);
            tempZip.write(content.toByteArray());
            next = earFile.getNextEntry();
        }
        earFile.close();
        tempZip.close();
        fos.close();
    }

    private static ByteArrayOutputStream insertModule(ZipInputStream earFile, ZipEntry next, ByteArrayOutputStream content, String moduleType, String moduleName, String contextRoot) throws IOException {
        String application = new String(content.toByteArray(), "UTF-8");
        ByteArrayOutputStream retval = new ByteArrayOutputStream();
        if (!application.contains(moduleName)) {
            if (moduleType.equals(MODULETYPE_WEB)) {
                String moduleText = "<module><web><web-uri>" + moduleName + "</web-uri><context-root>" + contextRoot + "</context-root></web></module>";
                application = application.replace("</application>", moduleText + "\n</application>");
            }
            if (moduleType.equals(MODULETYPE_EJB)) {
                String moduleText = "<module><ejb>" + moduleName + "</ejb></module>";
                application = application.replace("</application>", moduleText + "\n</application>");
            }
        }
        retval.write(application.getBytes("UTF-8"));
        return retval;
    }

    private static void displayUsageAndExit() {
        System.out.println("Usage :  <from-ear> <to-ear> <module type ('web' or 'ear')> <modulename> <contextroot (web type only)>\n\n" + "\n" + "This program will insert an module into application.xml of an ear file");
        System.exit(-1);
    }
}
