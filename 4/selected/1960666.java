package com.aol.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Changelist {

    private Properties defaults = null;

    private Properties props = null;

    private String dirA = null;

    private String dirB = null;

    private String outputDir = null;

    private String patchFile = null;

    private long versionA = -1;

    private long versionB = -1;

    private String action = null;

    private String style = null;

    private Pattern imgDir = null;

    private Pattern imgExt = null;

    private Pattern comma = null;

    private Pattern backslash = null;

    private Pattern subDirFile = null;

    public Changelist() {
        init();
    }

    public void init() {
        Mml.setBaseURL("");
        defaults = new Properties();
        defaults.put("changelist.patch.file", "changelist_patch.xml");
        defaults.put("changelist.style.pfc.file", "turtle.pfc");
        defaults.put("changelist.style.pfc.packager.path", "//");
        imgDir = Pattern.compile("\\$\\{ImageDir\\}");
        imgExt = Pattern.compile("\\$\\{ImageExt\\}");
        comma = Pattern.compile(",");
        backslash = Pattern.compile("\\\\");
        subDirFile = Pattern.compile(".*[/\\\\]([^/\\\\]+)[/\\\\]([^/\\\\]+)$");
    }

    public void readProperties(String propFilename) {
        props = new Properties(defaults);
        try {
            props.load(new FileInputStream(propFilename));
        } catch (FileNotFoundException fnfe) {
            System.err.print("Properties file " + propFilename + " not found!");
            return;
        } catch (IOException ioe) {
            System.err.print("Error loading properties file " + propFilename);
            return;
        }
        String prop = props.getProperty("changelist.repository.from");
        if (prop != null) setDirectoryA(prop);
        prop = props.getProperty("changelist.repository.to");
        if (prop != null) setDirectoryB(prop);
        prop = props.getProperty("changelist.repository.from.version");
        if (prop != null) setVersionA(prop);
        prop = props.getProperty("changelist.repository.to.version");
        if (prop != null) setVersionB(prop);
        prop = props.getProperty("changelist.output.dir");
        if (prop != null) setOutputDirectory(prop);
        prop = props.getProperty("changelist.patch.file");
        if (prop != null) setPatchFile(prop);
        prop = props.getProperty("changelist.action");
        if (prop != null) setAction(prop);
        prop = props.getProperty("changelist.style");
        if (prop != null) setStyle(prop);
    }

    public void setDirectoryA(String a) {
        if ((a.charAt(a.length() - 1) == '/') || (a.charAt(a.length() - 1) == '\\')) dirA = a.substring(0, a.length() - 1); else dirA = a;
    }

    public void setDirectoryB(String b) {
        if ((b.charAt(b.length() - 1) == '/') || (b.charAt(b.length() - 1) == '\\')) dirB = b.substring(0, b.length() - 1); else dirB = b;
    }

    public void setOutputDirectory(String out) {
        this.outputDir = out;
    }

    public void setPatchFile(String p) {
        patchFile = p;
    }

    public void setVersionA(long a) {
        versionA = a;
    }

    public void setVersionB(long b) {
        versionB = b;
    }

    public void setVersionA(String a) {
        try {
            long l = Long.parseLong(a, 16);
            setVersionA(l);
        } catch (NumberFormatException nfe) {
            System.err.print("Invalid format of Version A argument! ");
            throw nfe;
        }
    }

    public void setVersionB(String b) {
        try {
            long l = Long.parseLong(b, 16);
            setVersionB(l);
        } catch (NumberFormatException nfe) {
            System.err.print("Invalid format of Version B argument! ");
            throw nfe;
        }
    }

    public void setAction(String a) {
        if ("c".equals(a) || "m".equals(a) || "i".equals(a) || "g".equals(a) || "a".equals(a)) action = a;
    }

    public void setStyle(String s) {
        style = s;
    }

    public static void main(String[] args) {
        try {
            Changelist cl = new Changelist();
            parseCommandLine(cl, args);
            System.out.println("Generating changelist " + cl.createChangelist("reg", "hi", "png"));
            System.out.println("Generating changelist " + cl.createChangelist("lite", "med", "png"));
            System.out.println("Generating changelist " + cl.createChangelist("nano", "lo", "png"));
            System.out.println("Generating changelist " + cl.createChangelist("nano_nosoft", "lo", "png"));
        } catch (Exception e) {
            StackTraceElement[] ste = e.getStackTrace();
            System.err.println(e.getMessage());
            for (int i = 0; i < ste.length; i++) System.err.println(ste[i]);
        }
    }

    public static void parseCommandLine(Changelist cl, String[] args) {
        int len = args.length;
        int i = 0;
        while (i < len && args[i].startsWith("-")) {
            if (args[i].startsWith("-P") && (len > (i + 1))) {
                cl.readProperties(args[++i]);
            } else if (args[i].startsWith("-o") && (len > (i + 1))) {
                cl.setOutputDirectory(args[++i]);
            } else if (args[i].startsWith("-a") && (len > (i + 1))) {
                cl.setAction(args[++i]);
            } else if (args[i].startsWith("-s") && (len > (i + 1))) {
                cl.setStyle(args[++i]);
            } else if (args[i].startsWith("-verA") && (len > (i + 1))) {
                cl.setVersionA(args[++i]);
            } else if (args[i].startsWith("-verB") && (len > (i + 1))) {
                cl.setVersionB(args[++i]);
            }
            i++;
        }
        if (i < len) cl.setDirectoryA(args[i++]);
        if (i < len) cl.setDirectoryB(args[i++]);
    }

    public String createChangelist(String markup, String imageDir, String imageExt) throws Exception {
        if (outputDir == null || outputDir.length() == 0) throw new Exception("Output directory is not specified!");
        LinkedHashSet workingSet = createWorkingSet(markup, imageDir, imageExt);
        LinkedList changes = getChanges(workingSet, markup, imageDir, imageExt);
        if (workingSet.size() > 0) {
            System.err.println("\nWARNING! Orphaned files:");
            Iterator i = workingSet.iterator();
            while (i.hasNext()) System.err.println("\t" + (String) i.next());
        }
        String pfcFile = null;
        if ("pfc".equals(style)) {
            pfcFile = createPFC(changes, markup, imageDir, imageExt);
            System.out.println("Generated PFC file " + pfcFile);
            Matcher m = subDirFile.matcher(pfcFile);
            m.matches();
            String pkgSubDir = m.group(1);
            String pfcFilename = props.getProperty("changelist.style.pfc.file");
            pfcFile = m.replaceFirst(pkgSubDir + "/" + pfcFilename);
            String packagerPath = props.getProperty("changelist.style.pfc.packager.path");
            if (!packagerPath.endsWith("/")) packagerPath += "/";
            StringBuffer extraDeps = new StringBuffer();
            String prop = "changelist.style.pfc.dependencies." + markup + "_" + imageDir + "_" + imageExt;
            if (props.containsKey(prop)) {
                String _deps = props.getProperty(prop);
                String[] deps = _deps.split(",");
                String outDir = m.group().substring(0, m.start(2));
                String pkgDir = packagerPath + pkgSubDir;
                for (int i = 0; i < deps.length; i++) {
                    Matcher _m = subDirFile.matcher(deps[i]);
                    _m.matches();
                    File out = new File(outDir, _m.group(2));
                    System.err.println("Copying " + deps[i] + "\n        " + out.getPath());
                    try {
                        writeFile(out, readFile(new File(deps[i])));
                    } catch (FileNotFoundException fnfe) {
                        fnfe.printStackTrace();
                    }
                    extraDeps.append(pkgDir + "/" + _m.group(2)).append(",");
                }
            }
            changes.clear();
            StringBuffer changeline = new StringBuffer();
            changeline.append(packagerPath).append(pfcFile).append(",");
            if (extraDeps.length() > 0) changeline.append(extraDeps);
            changeline.append("__IGNORE__");
            String home = props.getProperty("changelist.style.pfc.homepage");
            if (home != null) {
                if (home.endsWith(".mml")) home = home.substring(0, home.length() - 1) + "c";
                changes.add("markup/" + markup + "/" + home + ":" + changeline);
            } else System.out.println("WARNING: PFC style changelist has no home page to give dependency " + pfcFilename);
            home = props.getProperty("changelist.style.pfc.demo_homepage");
            if (home != null) {
                if (home.endsWith(".mml")) home = home.substring(0, home.length() - 1) + "c";
                changes.add("markup/" + markup + "/" + home + ":" + changeline);
            } else System.out.println("WARNING: PFC style changelist has no demo home page to give dependency " + pfcFilename);
        }
        return writeChangelist(changes, markup, imageDir, imageExt);
    }

    public LinkedHashSet createWorkingSet(final String markup, final String imageDir, String imageExt) throws Exception {
        if (dirA == null) throw new Exception("Directory A is not specified!");
        if (dirB == null) throw new Exception("Directory B is not specified!");
        File _dirA = new File(dirA);
        File _dirB = new File(dirB);
        if (!_dirA.exists()) throw new Exception("Directory A, '" + dirA + "', does not exist!");
        if (!_dirB.exists()) throw new Exception("Directory B, '" + dirB + "', does not exist!");
        LinkedList filesA = new LinkedList();
        findFiles(_dirA, filesA);
        filesA = filterFiles(filesA, markup, imageDir);
        LinkedList filesB = new LinkedList();
        findFiles(_dirB, filesB);
        filesB = filterFiles(filesB, markup, imageDir);
        stripPrefix(dirA + File.separator, filesA);
        stripPrefix(dirB + File.separator, filesB);
        Iterator i;
        LinkedHashSet setA, setB;
        LinkedHashSet workingSet = new LinkedHashSet();
        setA = new LinkedHashSet(filesA);
        setB = new LinkedHashSet(filesB);
        setB.removeAll(setA);
        workingSet.addAll(setB);
        setA = new LinkedHashSet(filesA);
        setB = new LinkedHashSet(filesB);
        setA.retainAll(setB);
        i = setA.iterator();
        while (i.hasNext()) {
            String file = (String) i.next();
            if (file.endsWith(".mmc")) continue;
            if (differ(new File(dirA, file), new File(dirB, file))) workingSet.add(file);
        }
        return workingSet;
    }

    public String writeChangelist(LinkedList changes, final String markup, final String imageDir, final String imageExt) throws Exception {
        PrintWriter out = null;
        File file = null;
        try {
            new File(outputDir).mkdirs();
            file = new File(outputDir, markup + "_" + imageDir + "_" + imageExt + ".chg");
            out = new PrintWriter(new FileOutputStream(file));
            String s;
            s = "version=0x" + toVersionString(versionB);
            out.print(s);
            out.print("\r\n");
            if (action != null) {
                s = "action=" + action;
                out.print(s);
                out.print("\r\n");
            }
            Iterator i = changes.iterator();
            while (i.hasNext()) {
                s = "page=" + (String) i.next();
                out.print(s);
                out.print("\r\n");
            }
            out.print("\r\n");
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } finally {
            if (out != null) out.close();
        }
        return (file != null ? file.getPath() : null);
    }

    public String createPFC(LinkedList changes, final String markup, final String imageDir, final String imageExt) throws Exception {
        FilePackager packager = new FilePackager();
        LinkedHashSet files = new LinkedHashSet();
        Iterator i = changes.iterator();
        while (i.hasNext()) {
            String line = (String) i.next();
            String[] _files = line.split(":|,");
            for (int f = 0; f < _files.length; f++) files.add(_files[f]);
        }
        String[] _files = (String[]) files.toArray(new String[files.size()]);
        byte[] pkg = packager.build(dirB, _files);
        FileOutputStream out = null;
        File outfile = null;
        String _outdir = props.getProperty("changelist.style.pfc.output.dir");
        if (_outdir == null) _outdir = outputDir;
        try {
            new File(_outdir).mkdirs();
            String outDir = _outdir + File.separator + markup + "_" + imageDir + "_" + imageExt;
            new File(outDir).mkdirs();
            String pfc = props.getProperty("changelist.style.pfc.file");
            outfile = new File(outDir, pfc);
            out = new FileOutputStream(outfile);
            out.write(pkg);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            if (out != null) out.close();
        }
        return (outfile != null ? outfile.getPath() : null);
    }

    public String toVersionString(long ver) {
        String version = Long.toHexString(ver);
        return version;
    }

    public LinkedList getChanges(LinkedHashSet workingSet, final String markup, final String imageDir, final String imageExt) {
        LinkedList changeLines = new LinkedList();
        Pattern markMatch = Pattern.compile("[/\\\\]" + markup + "[/\\\\].*\\.mml$");
        ChangelistPatch patch = new ChangelistPatch(patchFile);
        Iterator i = patch.getChanges(markup + "_" + imageDir + "_" + imageExt);
        HashMap forcedDeps = new HashMap();
        while (i != null && i.hasNext()) {
            ChangelistPatch.ChangeLineRec r = (ChangelistPatch.ChangeLineRec) i.next();
            String m = r.markup;
            if (m.endsWith(".mmc")) m = m.substring(0, m.length() - 1) + "l";
            workingSet.add(m);
            if (r.dependencies != null) {
                LinkedHashSet forced = new LinkedHashSet();
                String[] tmp = comma.split(r.dependencies);
                for (int n = 0; n < tmp.length; n++) forced.add(tmp[n]);
                forcedDeps.put(m, forced);
            }
        }
        LinkedHashSet handledFiles = new LinkedHashSet();
        i = workingSet.iterator();
        while (i.hasNext()) {
            String file = (String) i.next();
            Matcher m = markMatch.matcher(file);
            if (m.find()) {
                if (Mml.ReadMarkup(dirB + File.separator + file) == 0) {
                    Mml.collectResources();
                    State.PageElement pe = Mml.getPageElement();
                    Matcher m2 = imgDir.matcher(pe.prefetchList);
                    String s = m2.replaceAll(imageDir);
                    m2 = imgExt.matcher(s);
                    s = m2.replaceAll(imageExt);
                    String[] tmp = comma.split(s);
                    LinkedHashSet auto = new LinkedHashSet();
                    for (int j = 0; j < tmp.length; j++) auto.add(tmp[j]);
                    LinkedHashSet forced = null;
                    if (forcedDeps.containsKey(file)) forced = (LinkedHashSet) forcedDeps.get(file);
                    if (forced != null) auto.removeAll(forced);
                    StringBuffer d = new StringBuffer();
                    d.append(file.substring(0, file.length() - 1) + "c:");
                    handledFiles.add(file);
                    Iterator _i;
                    if (forced != null) {
                        _i = forced.iterator();
                        while (_i.hasNext()) {
                            String _dep = (String) _i.next();
                            d.append(_dep).append(",");
                            handledFiles.add(_dep);
                        }
                    }
                    _i = auto.iterator();
                    while (_i.hasNext()) {
                        String _dep = (String) _i.next();
                        if (workingSet.contains(_dep)) {
                            d.append(_dep).append(",");
                            handledFiles.add(_dep);
                        }
                    }
                    if ((d.charAt(d.length() - 1) == ',')) changeLines.add(d.substring(0, d.length() - 1)); else changeLines.add(d.toString());
                }
            }
        }
        workingSet.removeAll(handledFiles);
        return changeLines;
    }

    private boolean differ(File fileA, File fileB) {
        if (fileA.length() != fileB.length()) return true;
        try {
            ByteBuffer a = readFile(fileA);
            ByteBuffer b = readFile(fileB);
            return !(a.equals(b));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }
        return false;
    }

    private void findFiles(File dir, LinkedList files) {
        String f[] = dir.list();
        if (f == null) return;
        int len = f.length;
        File file = null;
        for (int i = 0; i < len; i++) {
            file = new File(dir, f[i]);
            if (file.isFile()) {
                Matcher slash = backslash.matcher(file.getPath());
                String _file = slash.replaceAll("/");
                files.add(_file);
            } else findFiles(file, files);
        }
    }

    public LinkedList filterFiles(final LinkedList src, final String markup, final String imageDir) {
        Pattern mark = Pattern.compile("markup[/\\\\]" + markup + "[/\\\\].*\\.mml$");
        Pattern img = Pattern.compile("images[/\\\\]" + imageDir + "[/\\\\].*");
        Pattern ignore = Pattern.compile("([/\\\\]CVS[/\\\\].*|.*\\.db$)");
        LinkedList dst = new LinkedList();
        Iterator i = src.iterator();
        while (i.hasNext()) {
            String file = (String) i.next();
            Matcher m1 = mark.matcher(file);
            Matcher m2 = img.matcher(file);
            if (m1.find() || m2.find()) {
                Matcher m3 = ignore.matcher(file);
                if (!m3.find()) dst.add(file);
            }
        }
        return dst;
    }

    /**
	 * Removes prefix from each entry in list, in place.
	 */
    private void stripPrefix(String prefix, LinkedList list) {
        int len = prefix.length();
        String[] entries = (String[]) list.toArray(new String[list.size()]);
        list.clear();
        int e = entries.length;
        for (int i = 0; i < e; i++) list.add(entries[i].substring(len));
    }

    private ByteBuffer readFile(File file) throws FileNotFoundException, IOException {
        IOException ex = null;
        ByteBuffer contents = null;
        RandomAccessFile rfile = null;
        FileChannel chan = null;
        try {
            rfile = new RandomAccessFile(file, "r");
            chan = rfile.getChannel();
            int size = (int) chan.size();
            contents = ByteBuffer.allocate(size);
            while (contents.hasRemaining()) {
                int read = chan.read(contents);
                if (read == 0) throw new IOException("Read failure: " + file.getPath());
            }
            contents.flip();
        } catch (FileNotFoundException fnfe) {
            ex = fnfe;
        } catch (IOException ioe) {
            ex = ioe;
        } finally {
            try {
                if (chan != null) chan.close();
                if (rfile != null) rfile.close();
            } catch (IOException ioe) {
                if (ex == null) ex = ioe;
            }
            rfile = null;
            chan = null;
        }
        if (ex != null) throw ex;
        return contents;
    }

    private void writeFile(File file, ByteBuffer buffer) throws FileNotFoundException, IOException {
        IOException ex = null;
        RandomAccessFile rfile = null;
        FileChannel chan = null;
        try {
            rfile = new RandomAccessFile(file, "rw");
            chan = rfile.getChannel();
            chan.write(buffer);
        } catch (IOException ioe) {
            ex = ioe;
        } finally {
            try {
                if (chan != null) chan.close();
                if (rfile != null) rfile.close();
            } catch (IOException ioe) {
                if (ex == null) ex = ioe;
            }
            rfile = null;
            chan = null;
        }
        if (ex != null) throw ex;
    }
}
