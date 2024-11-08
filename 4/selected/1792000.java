package jonelo.jacksum.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import jonelo.jacksum.JacksumAPI;
import jonelo.jacksum.algorithm.AbstractChecksum;
import jonelo.jacksum.ui.CheckFile;
import jonelo.jacksum.ui.ExitStatus;
import jonelo.jacksum.ui.MetaInfo;
import jonelo.jacksum.ui.MetaInfoVersionException;
import jonelo.jacksum.ui.Summary;
import jonelo.jacksum.ui.Verbose;
import jonelo.jacksum.util.Service;
import jonelo.sugar.util.EncodingException;
import jonelo.sugar.util.ExitException;
import jonelo.sugar.util.GeneralProgram;
import jonelo.sugar.util.GeneralString;

/** This is the Jacksum Command Line Interface (CLI). */
public class Jacksum {

    public static final String DEFAULT = "default";

    public static final String TIMESTAMPFORMAT_DEFAULT = "yyyyMMddHHmmss";

    private AbstractChecksum checksum = null;

    private String checksumArg = null;

    private String expected = null;

    private String format = null;

    private String outputFile = null;

    private String errorFile = null;

    private char fileseparatorChar = '/';

    private char groupingChar = ' ';

    private boolean _f = false, _x = false, _X = false, _r = false, _t = false, _m = false, _p = false, _o = false, _I = false, _O = false, _u = false, _U = false, _l = false, _d = false, _S = false, _e = false, _F = false, _alternate = false, _P = false, _g = false, _G = false, _V = false, _w = false;

    private MetaInfo metaInfo = null;

    private Verbose verbose = null;

    private Summary summary = null;

    private String workingDir = null;

    private int workingdirlen = 0;

    private boolean windows = false;

    /** 
     * Jacksum's main method for the CLI
     * @param args command line arguments
     */
    public static void main(String args[]) {
        try {
            new jonelo.jacksum.cli.Jacksum(args);
        } catch (ExitException e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            }
            System.exit(e.getExitCode());
        }
    }

    /** recursive method to traverse folders
     * @param dirItem visit this folder
     */
    private void recursDir(String dirItem) {
        String list[];
        File file = new File(dirItem);
        if (file.isDirectory()) {
            if (!_d || (_d && !Service.isSymbolicLink(file))) {
                list = file.list();
                if (list == null) {
                    System.err.println("Jacksum: Can't access file system folder \"" + file + "\"");
                    summary.addErrorDir();
                } else {
                    summary.addDir();
                    if (!_e && !_p && !_S) {
                        String tmp = file.toString();
                        if (_w && tmp.length() > workingdirlen) tmp = tmp.substring(workingdirlen); else if (_w && tmp.length() < workingdirlen) tmp = "";
                        if (_P) System.out.println("\n" + tmp.replace(File.separatorChar, fileseparatorChar) + ":"); else System.out.println("\n" + tmp + ":");
                    }
                    ArrayList vd = new ArrayList();
                    ArrayList vf = new ArrayList();
                    Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
                    String dirname = file.toString();
                    boolean doit = true;
                    if ((dirname.length() > 0) && !dirname.endsWith(File.separator)) {
                        if (windows && dirname.endsWith(":")) doit = false;
                        if (doit) dirname += File.separator;
                    }
                    for (int i = 0; i < list.length; i++) {
                        File f = new File(dirname + list[i]);
                        if (f.isDirectory()) vd.add(list[i]); else vf.add(list[i]);
                    }
                    if (verbose.getDetails() && !_e && !_m && !_p && !_S) {
                        StringBuffer tmp = new StringBuffer(32);
                        tmp.append(vf.size());
                        tmp.append(" file");
                        if (vf.size() != 1) tmp.append('s');
                        if (!_f) {
                            tmp.append(", ");
                            tmp.append(vd.size());
                            tmp.append(" director");
                            if (vd.size() != 1) tmp.append("ies"); else tmp.append("y");
                        }
                        System.err.println(tmp.toString());
                    }
                    for (int a = 0; a < vf.size(); a++) {
                        recursDir(dirname + vf.get(a));
                    }
                    if (!_f) {
                        for (int c = 0; c < vd.size(); c++) System.err.println("Jacksum: " + vd.get(c) + ": Is a directory");
                    }
                    for (int d = 0; d < vd.size(); d++) {
                        recursDir(dirname + vd.get(d));
                    }
                }
            }
        } else processItem(dirItem);
    }

    /** process one folder
     * @param dirItem visit this folder
     */
    private void oneDir(String dirItem) {
        String list[];
        File file = new File(dirItem);
        if (file.isDirectory()) {
            list = file.list();
            if (list == null) {
                System.err.println("Jacksum: Can't access file system folder \"" + file + "\"");
                summary.addErrorDir();
            } else {
                summary.addDir();
                Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
                for (int i = 0; i < list.length; i++) {
                    String tmp = dirItem + (dirItem.endsWith(File.separator) ? "" : File.separator) + list[i];
                    File f = new File(tmp);
                    if (f.isDirectory()) {
                        if (!_f) System.err.println("Jacksum: " + list[i] + ": Is a directory");
                    } else {
                        processItem(tmp);
                    }
                }
            }
        } else processItem(dirItem);
    }

    /** print a formatted checksum line
     * @param filename process this file
     */
    private void processItem(String filename) {
        File f = new File(filename);
        if (f.isFile()) {
            if (_o || _O) {
                try {
                    if (new File(outputFile).getCanonicalPath().equals(f.getCanonicalPath())) return;
                } catch (Exception e) {
                    System.err.println("Jacksum: Error: " + e);
                }
            }
            if (_u || _U) {
                try {
                    if (new File(errorFile).getCanonicalPath().equals(f.getCanonicalPath())) return;
                } catch (Exception e) {
                    System.err.println("Jacksum: Error: " + e);
                }
            }
            try {
                if (_S) {
                    long bytesread = checksum.readFile(filename, false);
                    if (checksum.isTimestampWanted()) checksum.update(checksum.getTimestampFormatted().getBytes("ISO-8859-1"));
                    String tmp = _w ? filename.substring(workingdirlen) : filename;
                    if (File.separatorChar != '/') tmp = tmp.replace(File.separatorChar, '/');
                    checksum.update(tmp.getBytes("ISO-8859-1"));
                    summary.addBytes(bytesread);
                } else {
                    if (_e) {
                        checksum.readFile(filename, true);
                        expectationContinue(checksum, expected);
                    } else {
                        String ret = getChecksumOutput(filename);
                        if (ret != null) {
                            if (_P && File.separatorChar != fileseparatorChar) ret = ret.replace(File.separatorChar, fileseparatorChar);
                            System.out.println(ret);
                        }
                    }
                    summary.addBytes(checksum.getLength());
                }
                summary.addFile();
            } catch (Exception e) {
                summary.addErrorFile();
                String detail = null;
                if (verbose.getDetails()) {
                    detail = filename + " [" + e.getMessage() + "]";
                } else {
                    detail = filename;
                }
                System.err.println("Jacksum: Error: " + detail);
            }
        } else {
            if (!_f) {
                summary.addErrorFile();
                System.err.println("Jacksum: " + filename + ": Is not a regular file");
            }
        }
    }

    /** get a formatted checksum line
     * @return a full formatted checksum line
     * @param filename process this file
     */
    private String getChecksumOutput(String filename) throws IOException {
        checksum.readFile(filename, true);
        File f = new File(filename);
        if (_r && !_p) {
            checksum.setFilename(f.getName());
        } else {
            if (_w) filename = filename.substring(workingdirlen);
            checksum.setFilename(filename);
        }
        return (_F ? checksum.format(format) : checksum.toString());
    }

    private void expectation(AbstractChecksum checksum, String expected) throws ExitException {
        String value = checksum.getFormattedValue();
        if (checksum.getEncoding().equalsIgnoreCase(AbstractChecksum.BASE64) ? value.equals(expected) : value.equalsIgnoreCase(expected)) {
            System.out.println("[OK]");
            throw new ExitException(null, ExitStatus.OK);
        } else {
            System.out.println("[MISMATCH]");
            throw new ExitException(null, ExitStatus.MISMATCH);
        }
    }

    private void expectationContinue(AbstractChecksum checksum, String expected) {
        String value = checksum.getFormattedValue();
        if (checksum.getEncoding().equalsIgnoreCase("base64") ? value.equals(expected) : value.equalsIgnoreCase(expected)) {
            System.out.println(_F ? checksum.format(format) : checksum.toString());
        }
    }

    private static String decodeQuoteAndSeparator(String format, String separator) {
        String temp = format;
        if (separator != null) {
            temp = GeneralString.replaceAllStrings(temp, "#SEPARATOR", separator);
        }
        temp = GeneralString.replaceAllStrings(temp, "#QUOTE", "\"");
        return temp;
    }

    /**
     * Creates the Jacksum program (CLI).
     *
     * @param args the program arguments
     */
    public Jacksum(String args[]) throws ExitException {
        jonelo.sugar.util.GeneralProgram.requiresMinimumJavaVersion("1.3.1");
        boolean stdin = false, _s = false, _D = false, _q = false, _c = false, _E = false;
        String arg = null;
        String timestampFormat = null;
        String sequence = null;
        String checkfile = null;
        String grouparg = null;
        String encoding = null;
        int firstfile = 0;
        metaInfo = new MetaInfo();
        verbose = new Verbose();
        summary = new Summary();
        if (args.length == 0) JacksumHelp.printHelpShort(); else if (args.length > 0) {
            while (firstfile < args.length && args[firstfile].startsWith("-")) {
                arg = args[firstfile++];
                if (arg.equals("-a")) {
                    if (firstfile < args.length) {
                        arg = args[firstfile++].toLowerCase();
                        checksumArg = arg;
                    } else {
                        throw new ExitException("Option -a requires an algorithm. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-s")) {
                    if (firstfile < args.length) {
                        _s = true;
                        arg = args[firstfile++];
                        metaInfo.setSeparator(jonelo.sugar.util.GeneralString.translateEscapeSequences(arg));
                    } else {
                        throw new ExitException("Option -s requires a separator string. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-f")) {
                    _f = true;
                } else if (arg.equals("-")) {
                    stdin = true;
                } else if (arg.equals("-r")) {
                    _r = true;
                } else if (arg.equals("-x")) {
                    _x = true;
                } else if (arg.equals("-X")) {
                    _X = true;
                } else if (arg.equals("-m")) {
                    _m = true;
                } else if (arg.equals("-p")) {
                    _p = true;
                } else if (arg.equals("-l")) {
                    _l = true;
                } else if (arg.equals("-d")) {
                    _d = true;
                } else if (arg.equals("-E")) {
                    if (firstfile < args.length) {
                        _E = true;
                        arg = args[firstfile++];
                        encoding = arg;
                    } else {
                        throw new ExitException("Option -b requires an argument", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-A")) {
                    _alternate = true;
                } else if (arg.equals("-S")) {
                    _S = true;
                } else if (arg.equals("-q")) {
                    if (firstfile < args.length) {
                        _q = true;
                        arg = args[firstfile++];
                        sequence = arg;
                    } else {
                        throw new ExitException("Option -q requires a hex sequence argument", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-g")) {
                    if (firstfile < args.length) {
                        _g = true;
                        arg = args[firstfile++];
                        grouparg = arg;
                    } else {
                        throw new ExitException("Option -g requires an integer argument", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-G")) {
                    if (firstfile < args.length) {
                        _G = true;
                        arg = args[firstfile++];
                        if (arg.length() != 1) {
                            throw new ExitException("Option -G requires exactly one character", ExitStatus.PARAMETER);
                        } else {
                            groupingChar = arg.charAt(0);
                        }
                    } else {
                        throw new ExitException("Option -G requires an argument", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-P")) {
                    if (firstfile < args.length) {
                        _P = true;
                        arg = args[firstfile++];
                        if (arg.length() != 1) {
                            throw new ExitException("Option -P requires exactly one character", ExitStatus.PARAMETER);
                        } else {
                            if (arg.charAt(0) == '/' || arg.charAt(0) == '\\') fileseparatorChar = arg.charAt(0); else {
                                throw new ExitException("Option -P requires / or \\", ExitStatus.PARAMETER);
                            }
                        }
                    } else {
                        throw new ExitException("Option -P requires an argument", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-F")) {
                    if (firstfile < args.length) {
                        _F = true;
                        arg = args[firstfile++];
                        format = arg;
                    } else {
                        throw new ExitException("Option -F requires an argument", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-w")) {
                    if (firstfile < args.length) {
                        _w = true;
                        workingDir = args[firstfile];
                        if (firstfile + 1 < args.length) {
                            throw new ExitException("Option -w <directory> has to be the last parameter", ExitStatus.PARAMETER);
                        }
                    } else {
                        throw new ExitException("Option -w requires a directory parameter", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-c")) {
                    if (firstfile < args.length) {
                        _c = true;
                        arg = args[firstfile++];
                        checkfile = arg;
                    } else {
                        throw new ExitException("Option -c requires a filename parameter", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-e")) {
                    if (firstfile < args.length) {
                        _e = true;
                        _f = true;
                        arg = args[firstfile++];
                        expected = arg;
                    } else {
                        throw new ExitException("Option -e requires an argument", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-h")) {
                    String code = "en";
                    String search = null;
                    if (firstfile < args.length) {
                        code = args[firstfile++].toLowerCase();
                        if (code.equals("en") || code.equals("de")) {
                            if (firstfile < args.length) {
                                search = args[firstfile++].toLowerCase();
                            }
                        } else {
                            search = code;
                            code = "en";
                        }
                    }
                    JacksumHelp.help(code, search);
                } else if (arg.equals("-t")) {
                    _t = true;
                    if (firstfile < args.length) {
                        timestampFormat = args[firstfile++];
                        if (timestampFormat.equals(DEFAULT)) timestampFormat = TIMESTAMPFORMAT_DEFAULT;
                    } else {
                        throw new ExitException("Option -t requires a format string. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-v")) {
                    JacksumHelp.printVersion();
                    throw new ExitException(null, ExitStatus.OK);
                } else if (arg.equals("-V")) {
                    _V = true;
                    if (firstfile < args.length) {
                        String verbosetmp = args[firstfile++];
                        if (!verbosetmp.equals(DEFAULT)) {
                            StringTokenizer st = new StringTokenizer(verbosetmp, ",");
                            while (st.hasMoreTokens()) {
                                String s = st.nextToken();
                                if (s.equals("warnings")) verbose.setWarnings(true); else if (s.equals("nowarnings")) verbose.setWarnings(false); else if (s.equals("details")) verbose.setDetails(true); else if (s.equals("nodetails")) verbose.setDetails(false); else if (s.equals("summary")) verbose.setSummary(true); else if (s.equals("nosummary")) verbose.setSummary(false); else {
                                    throw new ExitException("Option -V requires valid parameters. Use -h for help. Exit.", ExitStatus.PARAMETER);
                                }
                            }
                        }
                    }
                } else if (arg.equals("-o")) {
                    _o = true;
                    if (firstfile < args.length) {
                        outputFile = args[firstfile++];
                    } else {
                        throw new ExitException("Option -o requires a parameter. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-O")) {
                    _O = true;
                    if (firstfile < args.length) {
                        outputFile = args[firstfile++];
                    } else {
                        throw new ExitException("Option -O requires a parameter. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-u")) {
                    _u = true;
                    if (firstfile < args.length) {
                        errorFile = args[firstfile++];
                    } else {
                        throw new ExitException("Option -u requires a parameter. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-U")) {
                    _U = true;
                    if (firstfile < args.length) {
                        errorFile = args[firstfile++];
                    } else {
                        throw new ExitException("Option -U requires a parameter. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else if (arg.equals("-I")) {
                    _I = true;
                    if (firstfile < args.length) {
                        metaInfo.setCommentchars(args[firstfile++]);
                    } else {
                        throw new ExitException("Option -I requires a parameter. Use -h for help. Exit.", ExitStatus.PARAMETER);
                    }
                } else {
                    throw new ExitException("Unknown argument. Use -h for help. Exit.", ExitStatus.PARAMETER);
                }
            }
        }
        if (_V && args.length == 1) {
            JacksumHelp.printVersion();
            throw new ExitException(null, ExitStatus.OK);
        }
        windows = System.getProperty("os.name").toLowerCase(Locale.US).startsWith("windows");
        PrintStream streamShared = null;
        boolean isShared = false;
        if ((_o || _O) && (_u || _U) && outputFile.equals(errorFile)) {
            if (_m) throw new ExitException("Jacksum: Error: stdout and stderr may not equal if -m is wanted.", ExitStatus.PARAMETER);
            try {
                streamShared = new PrintStream(new FileOutputStream(outputFile));
                isShared = true;
            } catch (Exception e) {
                throw new ExitException(e.getMessage(), ExitStatus.IO);
            }
        }
        if (_o || _O) {
            try {
                File f = new File(outputFile);
                if (!_O && f.exists()) {
                    throw new ExitException("Jacksum: Error: the file " + f + " already exists. Specify the file by -O to overwrite it.", ExitStatus.IO);
                }
                if (isShared) {
                    System.setOut(streamShared);
                } else {
                    PrintStream out = new PrintStream(new FileOutputStream(outputFile));
                    System.setOut(out);
                }
            } catch (Exception e) {
                throw new ExitException(e.getMessage(), ExitStatus.IO);
            }
        }
        if (_u || _U) {
            try {
                File f = new File(errorFile);
                if (!_U && f.exists()) {
                    throw new ExitException("Jacksum: Error: the file " + f + " already exists. Specify the file by -U to overwrite it.", ExitStatus.IO);
                }
                if (isShared) {
                    System.setErr(streamShared);
                } else {
                    PrintStream err = new PrintStream(new FileOutputStream(errorFile));
                    System.setErr(err);
                }
            } catch (Exception e) {
                throw new ExitException(e.getMessage(), ExitStatus.IO);
            }
        }
        if (checksumArg == null) {
            checksumArg = "sha1";
        }
        if (_e && checksumArg.equals("none")) {
            throw new ExitException("-a none and -e cannot go together.", ExitStatus.PARAMETER);
        }
        if (!_alternate) {
            if (!GeneralProgram.isJ2SEcompatible()) _alternate = true;
        }
        try {
            checksum = JacksumAPI.getChecksumInstance(checksumArg, _alternate);
        } catch (NoSuchAlgorithmException nsae) {
            throw new ExitException(nsae.getMessage() + "\nUse -a <code> to specify a valid one.\nFor help and a list of all supported algorithms use -h.\nExit.", ExitStatus.PARAMETER);
        }
        summary.setEnabled(verbose.getSummary());
        if (_s) checksum.setSeparator(metaInfo.getSeparator());
        if (_g) {
            try {
                int group = Integer.parseInt(grouparg);
                if (group > 0) {
                    checksum.setEncoding(AbstractChecksum.HEX);
                    checksum.setGroup(group);
                    if (_G) {
                        checksum.setGroupChar(groupingChar);
                    }
                } else {
                    if (verbose.getWarnings()) System.err.println("Jacksum: Warning: Ignoring -g, because parameter is not greater than 0.");
                }
            } catch (NumberFormatException nfe) {
                throw new ExitException(grouparg + " is not a decimal number.", ExitStatus.PARAMETER);
            }
        }
        if (_x) {
            checksum.setEncoding(AbstractChecksum.HEX);
        }
        if (_X) {
            checksum.setEncoding(AbstractChecksum.HEX_UPPERCASE);
        }
        if (_E) {
            try {
                if (encoding.length() == 0) throw new EncodingException("Encoding not supported");
                checksum.setEncoding(encoding);
            } catch (EncodingException e) {
                throw new ExitException("Jacksum: " + e.getMessage(), ExitStatus.PARAMETER);
            }
        }
        if (_t && !_q) {
            try {
                timestampFormat = decodeQuoteAndSeparator(timestampFormat, metaInfo.getSeparator());
                Format timestampFormatter = new SimpleDateFormat(timestampFormat);
                timestampFormatter.format(new Date());
                checksum.setTimestampFormat(timestampFormat);
            } catch (IllegalArgumentException iae) {
                throw new ExitException("Option -t is wrong (" + iae.getMessage() + ")", ExitStatus.PARAMETER);
            }
        }
        if (_m && _S) {
            throw new ExitException("Jacksum: -S and -m can't go together, it is not supported.", ExitStatus.PARAMETER);
        }
        if (_m && _G && groupingChar == ';') {
            throw new ExitException("Jacksum: Option -G doesn't allow a semicolon when -m has been specified", ExitStatus.PARAMETER);
        }
        if (_m || _c) {
            if (_F && verbose.getWarnings()) {
                System.err.println("Jacksum: Warning: Ignoring -F, because -m or -c has been specified.");
            }
            metaInfo.setVersion(JacksumAPI.VERSION);
            metaInfo.setRecursive(_r);
            metaInfo.setEncoding(checksum.getEncoding());
            metaInfo.setPathInfo(_p);
            metaInfo.setTimestampFormat(_t ? checksum.getTimestampFormat() : null);
            metaInfo.setFilesep(_P ? fileseparatorChar : File.separatorChar);
            metaInfo.setGrouping(_g ? checksum.getGroup() : 0);
            if (_g && _G) metaInfo.setGroupChar(checksum.getGroupChar());
            metaInfo.setAlgorithm(checksum.getName());
            metaInfo.setAlternate(_alternate);
        }
        if (_m) {
            if (_t) {
                if (timestampFormat.indexOf(";") > -1) {
                    throw new ExitException("Option -t contains a semicolon. This is not supported with -m.", ExitStatus.PARAMETER);
                }
            }
            if (_I) {
                if (metaInfo.getCommentchars().length() == 0) {
                    throw new ExitException("Option -I has been set to an empty string. This is not supported with -m.", ExitStatus.PARAMETER);
                }
                if (metaInfo.getCommentchars().indexOf(";") > -1) {
                    throw new ExitException("Option -I contains a semicolon. This is not supported with -m.", ExitStatus.PARAMETER);
                }
            }
            if (_s) {
                if (metaInfo.getSeparator().indexOf(";") > -1) {
                    throw new ExitException("Option -s contains a semicolon. This is not supported with -m.", ExitStatus.PARAMETER);
                }
                checksum.setSeparator(metaInfo.getSeparator());
            }
            _F = false;
            System.out.println(metaInfo);
            System.out.println(metaInfo.getComment());
        }
        String ret = null;
        String filename = null;
        if (_q) {
            if (_t) {
                if (verbose.getWarnings()) System.err.println("Jacksum: Warning: Option -t will be ignored, because option -q is used.");
                _t = false;
                checksum.setTimestampFormat(null);
            }
            byte[] bytearr = null;
            checksum.setFilename("");
            String seqlower = sequence.toLowerCase();
            if (seqlower.startsWith("txt:")) {
                sequence = sequence.substring(4);
                bytearr = sequence.getBytes();
            } else if (seqlower.startsWith("dec:")) {
                sequence = sequence.substring(4);
                if (sequence.length() == 0) {
                    bytearr = sequence.getBytes();
                } else {
                    int count = GeneralString.countChar(sequence, ',');
                    bytearr = new byte[count + 1];
                    StringTokenizer st = new StringTokenizer(sequence, ",");
                    int x = 0;
                    while (st.hasMoreTokens()) {
                        int temp = 0;
                        String stemp = null;
                        try {
                            stemp = st.nextToken();
                            temp = Integer.parseInt(stemp);
                        } catch (NumberFormatException nfe) {
                            throw new ExitException(stemp + " is not a decimal number.", ExitStatus.PARAMETER);
                        }
                        if (temp < 0 || temp > 255) {
                            throw new ExitException("The number " + temp + " is out of range.", ExitStatus.PARAMETER);
                        }
                        bytearr[x++] = (byte) temp;
                    }
                }
            } else {
                if (seqlower.startsWith("hex:")) sequence = sequence.substring(4);
                if ((sequence.length() % 2) == 1) {
                    throw new ExitException("An even number of nibbles was expected.\nExit.", ExitStatus.PARAMETER);
                }
                try {
                    bytearr = new byte[sequence.length() / 2];
                    int x = 0;
                    for (int i = 0; i < sequence.length(); ) {
                        String str = sequence.substring(i, i += 2);
                        bytearr[x++] = (byte) Integer.parseInt(str, 16);
                    }
                } catch (NumberFormatException nfe) {
                    throw new ExitException("Not a hex number. " + nfe.getMessage(), ExitStatus.PARAMETER);
                }
            }
            checksum.update(bytearr);
            if (_e) expectation(checksum, expected); else System.out.println(_F ? checksum.format(format) : checksum.toString());
            throw new ExitException(null, 0);
        } else {
            if (_c) {
                _F = false;
                File f = new File(checkfile);
                if (!f.exists()) {
                    throw new ExitException("Jacksum: " + checkfile + ": No such file or directory. Exit.", ExitStatus.IO);
                } else {
                    int error = ExitStatus.OK;
                    if (f.isDirectory()) {
                        throw new ExitException("Parameter is a directory, but a filename was expected. Exit.", ExitStatus.PARAMETER);
                    } else {
                        CheckFile cf = null;
                        try {
                            cf = new CheckFile(checkfile);
                            if (_w) cf.setWorkingDir(workingDir);
                            cf.setMetaInfo(metaInfo);
                            cf.setVerbose(verbose);
                            cf.setSummary(summary);
                            cf.setList(_l);
                            cf.perform();
                        } catch (MetaInfoVersionException e) {
                            throw new ExitException(e.getMessage(), ExitStatus.CHECKFILE);
                        } catch (ExitException exex) {
                            throw new ExitException(exex.getMessage(), exex.getExitCode());
                        } catch (Exception e) {
                            error = ExitStatus.CHECKFILE;
                            System.err.println(e);
                        }
                        if ((error == ExitStatus.OK) && (cf.getRemoved() + cf.getModified() > 0)) {
                            error = ExitStatus.MISMATCH;
                        }
                    }
                    summary.print();
                    throw new ExitException(null, error);
                }
            } else {
                if (args.length - firstfile == 1) {
                    String dir = args[firstfile];
                    File f = new File(dir);
                    if (!f.exists()) {
                        throw new ExitException("Jacksum: " + dir + ": No such file or directory. Exit.", ExitStatus.IO);
                    } else {
                        if (f.isDirectory()) {
                            _D = true;
                        } else {
                            if (f.isFile()) {
                                if (_e) {
                                    try {
                                        checksum.readFile(dir);
                                        expectation(checksum, expected);
                                    } catch (IOException ioe) {
                                        throw new ExitException(ioe.getMessage(), ExitStatus.IO);
                                    }
                                }
                            } else {
                                throw new ExitException("Jacksum: \"" + dir + "\" is not a normal file", ExitStatus.IO);
                            }
                        }
                    }
                }
            }
        }
        if (_r || _D) {
            String dir = null;
            if (args.length - firstfile == 1) dir = args[firstfile]; else if (args.length == firstfile) dir = "."; else {
                throw new ExitException("Too many parameters. One directory was expeced. Exit.", ExitStatus.PARAMETER);
            }
            File f = new File(dir);
            if (!f.exists()) {
                throw new ExitException("Jacksum: " + dir + ": No such file or directory. Exit.", ExitStatus.IO);
            } else {
                if (f.isDirectory()) {
                    if (_m) {
                        System.out.println(metaInfo.getCommentchars() + " param dir=" + dir);
                    }
                    if (_w) workingdirlen = getWorkingdirLength(f.toString());
                    if (_r) recursDir(f.toString()); else oneDir(f.toString());
                    if (_S) printS();
                } else {
                    throw new ExitException("Parameter is a file, but a directory was expected. Exit.", ExitStatus.PARAMETER);
                }
            }
        } else {
            if (stdin || (firstfile == args.length)) {
                if (_t) {
                    if (verbose.getWarnings()) System.err.println("Jacksum: Warning: Option -t will be ignored, because standard input is used.");
                    _t = false;
                    checksum.setTimestampFormat(null);
                }
                checksum.setFilename("");
                String s = null;
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                try {
                    do {
                        s = in.readLine();
                        if (s != null) {
                            StringBuffer sb = new StringBuffer(s.length() + 1);
                            sb.insert(0, s);
                            sb.insert(s.length(), '\n');
                            checksum.update(sb.toString().getBytes());
                        }
                    } while (s != null);
                    summary.addBytes(checksum.getLength());
                    if (_e) expectation(checksum, expected); else System.out.println(checksum.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                for (int i = firstfile; i < args.length; i++) {
                    filename = args[i];
                    try {
                        File file = new File(filename);
                        ret = null;
                        if (!file.exists()) {
                            ret = "Jacksum: " + filename + ": No such file or directory";
                        } else {
                            if (file.isDirectory()) {
                                if (!_f) ret = "Jacksum: " + filename + ": Is a directory";
                            } else {
                                processItem(filename);
                            }
                        }
                        if (ret != null) System.err.println(ret);
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
                if (_S) printS();
            }
        }
        summary.print();
    }

    private int getWorkingdirLength(String parent) {
        if (parent == null) return 0;
        boolean doit = true;
        if (!parent.endsWith(File.separator)) {
            if (windows && parent.endsWith(":")) doit = false;
            if (doit) parent += File.separator;
        }
        return parent.length();
    }

    private void printS() throws ExitException {
        checksum.setFilename("");
        checksum.setTimestampFormat("");
        checksum.setSeparator("");
        if (_e) expectation(checksum, expected); else {
            System.out.println(_F ? checksum.format(format) : checksum.format("#CHECKSUM"));
        }
    }
}
