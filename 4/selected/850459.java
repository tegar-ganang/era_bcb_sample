package org.dcm4che.xam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.snia.xam.XAMException;
import org.snia.xam.XAMLibrary;
import org.snia.xam.XIterator;
import org.snia.xam.XSet;
import org.snia.xam.XStream;
import org.snia.xam.XSystem;
import org.snia.xam.XUID;
import org.snia.xam.base.DefaultXUID;
import org.snia.xam.base.XRI;
import org.snia.xam.util.SASLUtils;
import org.snia.xam.util.XAMLibraryFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Feb 3, 2009
 */
public class XamIO {

    public static final String JAVA_XAM_LIBRARY = "org.dcm4che.xam.XAMImplementation";

    public static final String C_XAM_LIBRARY = "org.snia.xam.XAMLibraryObj";

    private static final String USAGE = "xamio [options] -o {file... | < file-list} [> xuid-list]\n" + "xamio [options] -i {xuid... | < xuid-list}";

    private static final String DESCRIPTION = "\nArchive/Retrieve files to/from XAM Storage System. " + "If no filenames/XUIDs are specified as arguments, " + "filenames/XUIDs are read from stdin. Files of one directory " + "are archived into one XSet.\n.\nOptions:";

    private static final String EXAMPLE = "\nExamples:\n" + "$ xamio -Pdir=/var/local/xam_storage -u testuser testpassed " + "-o /media/cdrom/dicom  > xuid-list\n" + "Archive files in directory and sub-directories of " + "/media/cdrom/dicom to default XAM Storage System " + "(snia-xam://SNIA_Reference_VIM!localhost) with parameter " + "dir=/var/local/xam_storage, authentified by username " + "'testuser' and password 'testpasswd'." + "\n.\n" + "$ xamio -C --vim centera_vim -s 172.25.13.183 -u myName " + "myPassword -o /media/cdrom/dicom  > xuid-list\n" + "Archive files to Centera Storage System at 172.25.13.183, " + "authentified by username 'myName' and password 'myPassword'." + "\n.\n" + "$ xamio -Pdir=/var/local/xam_storage -u testuser testpassed " + "-i -d /tmp < xuid-list\n" + "Retrieve files in XSets, specified by XUIDs read from stdin, " + "from default XAM Storage System " + "(snia-xam://SNIA_Reference_VIM!localhost) with parameter " + "dir=/var/local/xam_storage to directory /tmp, authentified " + "by username 'testuser' and password 'testpasswd'";

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private boolean clib;

    private String mimeType = "application/dicom";

    private String openMode = XSet.MODE_READ_ONLY;

    private String destination;

    private boolean binding = false;

    private byte[] buffer;

    private XRI xri = new XRI();

    private XSystem xsys;

    public XamIO(int bufferSize) {
        xri.setVimInfo("SNIA_Reference_VIM");
        xri.setSystem("localhost");
        buffer = new byte[bufferSize];
    }

    public void setUseCLib(boolean clib) {
        this.clib = clib;
    }

    public void setVimInfo(String vimInfo) {
        xri.setVimInfo(vimInfo);
    }

    public void setSystem(String system) {
        xri.setSystem(system);
    }

    public void addParameter(String name, String value) {
        xri.addParameter(name, value);
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setOpenMode(String openMode) {
        this.openMode = openMode;
    }

    public void setBinding(boolean binding) {
        this.binding = binding;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    private static CommandLine parse(String[] args) {
        Options opts = new Options();
        OptionGroup cmdOpt = new OptionGroup();
        OptionBuilder.withDescription("archive files to XAM Storage System. " + "You must specify one of -oi options.");
        cmdOpt.addOption(OptionBuilder.create("o"));
        OptionBuilder.withDescription("retrieve files from XAM Storage System. " + "You must specify one of -oi options");
        cmdOpt.addOption(OptionBuilder.create("i"));
        opts.addOptionGroup(cmdOpt);
        OptionBuilder.hasArgs(2);
        OptionBuilder.withArgName("user_name password");
        OptionBuilder.withDescription("username and password used for authentication, " + "by default no authentification");
        opts.addOption(OptionBuilder.create("u"));
        opts.addOption("C", false, "Use C XAM Library via JNDI wrapper," + " use Java XAM Libray by default.");
        OptionBuilder.hasArg();
        OptionBuilder.withArgName("name");
        OptionBuilder.withDescription("System name specified in XAM XRI, by default: \"localhost\"");
        opts.addOption(OptionBuilder.create("s"));
        OptionBuilder.hasArg();
        OptionBuilder.withArgName("name");
        OptionBuilder.withDescription("VIM name specified in XAM XRI, " + "by default: \"SNIA_Reference_VIM\"");
        OptionBuilder.withLongOpt("vim");
        opts.addOption(OptionBuilder.create());
        OptionBuilder.hasArgs();
        OptionBuilder.withArgName("param=value");
        OptionBuilder.withValueSeparator('=');
        OptionBuilder.withDescription("Adds VIM parameter name value pair to XAM XRI");
        opts.addOption(OptionBuilder.create("P"));
        OptionBuilder.hasArg();
        OptionBuilder.withArgName("mime_type");
        OptionBuilder.withDescription("MIME type associated with archived files, " + "by default: \"application/dicom\"");
        opts.addOption(OptionBuilder.create("m"));
        OptionGroup modeOpt = new OptionGroup();
        OptionBuilder.withDescription("open XSet in restricted instead readonly mode.");
        OptionBuilder.withLongOpt("restricted");
        modeOpt.addOption(OptionBuilder.create());
        OptionBuilder.withDescription("open XSet in unrestricted instead readonly mode.");
        OptionBuilder.withLongOpt("unrestricted");
        modeOpt.addOption(OptionBuilder.create());
        opts.addOptionGroup(modeOpt);
        opts.addOption("b", false, "bind XStream fields, by default not bound");
        OptionBuilder.hasArg();
        OptionBuilder.withArgName("size");
        OptionBuilder.withType(Number.class);
        OptionBuilder.withDescription("buffer size, by default: 8192");
        OptionBuilder.withLongOpt("bs");
        opts.addOption(OptionBuilder.create());
        OptionBuilder.hasArg();
        OptionBuilder.withArgName("directory");
        OptionBuilder.withDescription("retrieve files to specified directory, " + "by default: \".\"");
        opts.addOption(OptionBuilder.create("d"));
        OptionBuilder.withDescription("print this message");
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false, "print the version information and exit");
        CommandLine cl = null;
        try {
            cl = new PosixParser().parse(opts, args);
        } catch (ParseException e) {
            exit("xam: " + e.getMessage());
            throw new RuntimeException("unreachable");
        }
        if (cl.hasOption('V')) {
            Package p = XamIO.class.getPackage();
            System.out.println("xamio v" + p.getImplementationVersion());
            System.exit(0);
        }
        if (cl.hasOption('h') || !cl.hasOption('i') && !cl.hasOption('o')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE, DESCRIPTION, opts, EXAMPLE);
            System.exit(0);
        }
        return cl;
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try 'xamio -h' for more information.");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        CommandLine cl = parse(args);
        XamIO xamio = new XamIO(cl.hasOption("bs") ? ((Number) cl.getOptionObject("bs")).intValue() : DEFAULT_BUFFER_SIZE);
        xamio.setUseCLib(cl.hasOption("C"));
        if (cl.hasOption("vim")) {
            xamio.setVimInfo(cl.getOptionValue("vim"));
        }
        if (cl.hasOption("s")) {
            xamio.setSystem(cl.getOptionValue("s"));
        }
        if (cl.hasOption("P")) {
            String[] params = cl.getOptionValues("P");
            for (int i = 1; i < params.length; i++, i++) {
                xamio.addParameter(params[i - 1], params[i]);
            }
        }
        if (cl.hasOption("m")) {
            xamio.setMimeType(cl.getOptionValue("m"));
        }
        if (cl.hasOption("restricted")) {
            xamio.setOpenMode(XSet.MODE_RESTRICTED);
        }
        if (cl.hasOption("unrestricted")) {
            xamio.setOpenMode(XSet.MODE_UNRESTRICTED);
        }
        xamio.setBinding(cl.hasOption("b"));
        if (cl.hasOption("d")) {
            xamio.setDestination(cl.getOptionValue("d"));
        }
        String[] remainingArgs = cl.getArgs();
        xamio.connect();
        try {
            if (cl.hasOption("u")) {
                String[] ss = cl.getOptionValues("u");
                xamio.authenticate(ss[0], ss[1]);
            }
            if (cl.hasOption("o")) {
                Map<String, Set<File>> map = new LinkedHashMap<String, Set<File>>();
                if (remainingArgs.length == 0) {
                    readFileList(new BufferedReader(new InputStreamReader(System.in)), map);
                } else {
                    selectFiles(null, remainingArgs, map);
                }
                for (Set<File> fileSet : map.values()) {
                    xamio.archive(fileSet);
                }
            }
            if (cl.hasOption("i")) {
                if (remainingArgs.length == 0) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String xuid;
                    while ((xuid = reader.readLine()) != null) {
                        if (xuid.length() != 0) {
                            xamio.retrieve(xuid);
                        }
                    }
                } else {
                    for (String xuid : remainingArgs) {
                        if (xuid.length() != 0) {
                            xamio.retrieve(xuid);
                        }
                    }
                }
            }
        } finally {
            xamio.close();
        }
    }

    private static void selectFiles(File basedir, String[] fnames, Map<String, Set<File>> map) {
        for (String fname : fnames) {
            File f = new File(basedir, fname);
            if (f.isDirectory()) {
                selectFiles(f, f.list(), map);
                continue;
            } else if (!f.isFile()) {
                System.err.println("No such file: " + fname + " - ignore");
                continue;
            }
            String key = f.getParent();
            Set<File> list = map.get(key);
            if (list == null) {
                map.put(key, list = new LinkedHashSet<File>());
            }
            list.add(f);
        }
    }

    private static void readFileList(BufferedReader reader, Map<String, Set<File>> map) throws IOException {
        String fname;
        while ((fname = reader.readLine()) != null) {
            if (fname.length() == 0) continue;
            File f = new File(fname);
            if (!f.isFile()) {
                System.err.println("No such file: " + fname + " - ignore");
                continue;
            }
            String key = f.getParent();
            Set<File> list = map.get(key);
            if (list == null) {
                map.put(key, list = new LinkedHashSet<File>());
            }
            list.add(f);
        }
    }

    public void connect() throws XAMException {
        if (xsys != null) {
            throw new IllegalStateException("Already connected");
        }
        XAMLibrary ximpl = XAMLibraryFactory.newXAMLibrary(clib ? C_XAM_LIBRARY : JAVA_XAM_LIBRARY);
        xsys = ximpl.connect(xri.toString());
    }

    private void assertConnected() {
        if (xsys == null) {
            throw new IllegalStateException("Not connected");
        }
    }

    public void authenticate(String username, String password) throws XAMException {
        assertConnected();
        SASLUtils.authenticatePlain(xsys, username, password);
    }

    public void close() throws XAMException {
        assertConnected();
        xsys.close();
        xsys = null;
    }

    public void archive(Set<File> fileSet) throws XAMException, IOException {
        XSet xset = xsys.createXSet(XSet.MODE_UNRESTRICTED);
        try {
            for (File file : fileSet) {
                InputStream in = new FileInputStream(file);
                try {
                    XStream out = xset.createXStream(file.getName(), binding, mimeType);
                    try {
                        copy(in, out);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            }
            XUID xuid = xset.commit();
            System.out.println(xuid.toString());
        } finally {
            xset.close();
        }
    }

    public void retrieve(String xuid) throws XAMException, IOException {
        XSet xset = xsys.openXSet(DefaultXUID.valueOf(xuid), openMode);
        try {
            XIterator iter = xset.openFieldIterator("");
            try {
                while (iter.hasNext()) {
                    String fieldName = (String) iter.next();
                    if (mimeType.equals(xset.getFieldType(fieldName))) {
                        XStream in = xset.openXStream(fieldName, XStream.MODE_READ_ONLY);
                        File f = toFile(xuid, fieldName);
                        try {
                            OutputStream out = new FileOutputStream(f);
                            try {
                                copy(in, out);
                            } finally {
                                out.close();
                            }
                        } finally {
                            in.close();
                        }
                        System.out.println(f);
                    }
                }
            } finally {
                iter.close();
            }
        } finally {
            xset.close();
        }
    }

    private File toFile(String xuid, String fieldname) throws IOException {
        File d = new File(destination, xuid);
        d.mkdirs();
        return new File(d, fieldname);
    }

    private void copy(InputStream in, XStream out) throws IOException, XAMException {
        int read;
        while ((read = in.read(buffer)) > 0) {
            int offset = 0;
            do {
                offset += out.write(buffer, offset, read - offset);
            } while (offset < read);
        }
    }

    private void copy(XStream in, OutputStream out) throws IOException, XAMException {
        int read;
        while ((read = (int) in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
    }
}
