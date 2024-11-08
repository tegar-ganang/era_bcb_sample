package gov.lanl.arc.heritrixImpl;

import gov.lanl.util.MimeTypeMapper;
import java.io.File;
import java.util.Hashtable;

public class ARCFileUtilities {

    static final int ERRORCODE_OK = 0;

    static final int ERRORCODE_CMDERR = 1;

    static final int VERBCODE_IDX = 0;

    static final int VERBCODE_GET = 1;

    static final String CMD_VERB = "verb";

    static final String CMD_ARC = "arcfile";

    static final String VERB_INDEX = "index";

    static final String VERB_GET = "get";

    static final String CMD_GET_IDENTIFER = "id";

    static final String CMD_EXPORT_DIR = "dir";

    static final String CMD_EXPORT_FILE_NAME = "filename";

    static int verb;

    static String arcFile;

    static String id;

    static String exportDir;

    static String exportFileName;

    public static void main(String[] argv) throws Exception {
        Hashtable parahash = new Hashtable();
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.equals("--h")) {
                printUsage();
                System.exit(ERRORCODE_OK);
            }
            if (arg.startsWith("--")) parahash.put(arg.substring(2), argv[++i]); else {
                printUsage();
                System.exit(ERRORCODE_CMDERR);
            }
        }
        readParameters(parahash);
        ARCFileReader reader;
        if (verb == VERBCODE_IDX) {
            reader = new ARCFileReader(arcFile, false);
            reader.generateIndex();
            System.out.println("Indexing Complete");
        }
        if (verb == VERBCODE_GET) {
            reader = new ARCFileReader(arcFile, true);
            String mimeType = reader.getCdxInstance().getMimeTypeforIdentifier(id);
            MimeTypeMapper mapper = new MimeTypeMapper();
            File dir = new File(exportDir);
            if (!dir.exists()) dir.mkdir();
            String exportFile = new File(exportDir, exportFileName + mapper.getExtension(mimeType)).getAbsolutePath();
            reader.writeResource(id, exportFile);
            System.out.println("Exported file of type " + mimeType + " to " + exportFile);
        }
    }

    private static void readParameters(Hashtable ht) {
        if ((ht.get(CMD_VERB) == null)) {
            printUsage();
            System.exit(ERRORCODE_CMDERR);
        }
        if ((ht.get(CMD_ARC) == null)) {
            printUsage();
            System.exit(ERRORCODE_CMDERR);
        } else {
            String verbtext = (String) (ht.get(CMD_VERB));
            if (verbtext.equals(VERB_GET)) {
                verb = VERBCODE_GET;
                if ((ht.get(CMD_GET_IDENTIFER) == null)) {
                    printUsage();
                    System.exit(ERRORCODE_CMDERR);
                }
                if ((ht.get(CMD_EXPORT_DIR) == null)) {
                    printUsage();
                    System.exit(ERRORCODE_CMDERR);
                }
                id = (String) (ht.get(CMD_GET_IDENTIFER));
                exportDir = (String) (ht.get(CMD_EXPORT_DIR));
                exportFileName = (String) (ht.get(CMD_EXPORT_FILE_NAME));
                if (exportFileName == null) exportFileName = "exportedResource";
            } else if (verbtext.equals(VERB_INDEX)) verb = VERBCODE_IDX; else {
                printUsage();
                System.exit(ERRORCODE_CMDERR);
            }
        }
        arcFile = (String) (ht.get(CMD_ARC));
    }

    public static void printUsage() {
        System.out.println("usage: java gov.lanl.arc.heritrixImpl.ARCFileUtilities [--help] [--verb <verb> ] [--arcfile <arcfile> ] [--id <id> ] [--dir <destDir> ]");
        System.out.println("example1: Index ARCFile");
        System.out.println("java gov.lanl.arc.heritrixImpl.ARCFileUtilities --verb index --arcfile /lanl/data/arc/sci2006.arc");
        System.out.println("example2: Extract Resource from ARCFile");
        System.out.println("java gov.lanl.arc.heritrixImpl.ARCFileUtilities --verb get --arcfile /lanl/data/arc/sci2006.arc --id info:lanl-repo/ds/ed49ae2b-74d1-41bd-963f-aa4c10ebd805 --dir /lanl/extract/ --filename exportedResource");
    }
}
