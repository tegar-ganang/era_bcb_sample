package com.google.doclava;

import com.google.clearsilver.jsilver.JSilver;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.resourceloader.ClassResourceLoader;
import com.google.clearsilver.jsilver.resourceloader.CompositeResourceLoader;
import com.google.clearsilver.jsilver.resourceloader.FileSystemResourceLoader;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.lang.reflect.Proxy;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

public class Doclava {

    private static final String SDK_CONSTANT_ANNOTATION = "android.annotation.SdkConstant";

    private static final String SDK_CONSTANT_TYPE_ACTIVITY_ACTION = "android.annotation.SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION";

    private static final String SDK_CONSTANT_TYPE_BROADCAST_ACTION = "android.annotation.SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION";

    private static final String SDK_CONSTANT_TYPE_SERVICE_ACTION = "android.annotation.SdkConstant.SdkConstantType.SERVICE_INTENT_ACTION";

    private static final String SDK_CONSTANT_TYPE_CATEGORY = "android.annotation.SdkConstant.SdkConstantType.INTENT_CATEGORY";

    private static final String SDK_CONSTANT_TYPE_FEATURE = "android.annotation.SdkConstant.SdkConstantType.FEATURE";

    private static final String SDK_WIDGET_ANNOTATION = "android.annotation.Widget";

    private static final String SDK_LAYOUT_ANNOTATION = "android.annotation.Layout";

    private static final int TYPE_NONE = 0;

    private static final int TYPE_WIDGET = 1;

    private static final int TYPE_LAYOUT = 2;

    private static final int TYPE_LAYOUT_PARAM = 3;

    public static final int SHOW_PUBLIC = 0x00000001;

    public static final int SHOW_PROTECTED = 0x00000003;

    public static final int SHOW_PACKAGE = 0x00000007;

    public static final int SHOW_PRIVATE = 0x0000000f;

    public static final int SHOW_HIDDEN = 0x0000001f;

    public static int showLevel = SHOW_PROTECTED;

    public static final String javadocDir = "reference/";

    public static String assetsOutputDir = "assets";

    public static String htmlExtension;

    public static RootDoc root;

    public static ArrayList<String[]> mHDFData = new ArrayList<String[]>();

    public static Map<Character, String> escapeChars = new HashMap<Character, String>();

    public static String title = "";

    public static SinceTagger sinceTagger = new SinceTagger();

    public static HashSet<String> knownTags = new HashSet<String>();

    public static FederationTagger federationTagger = new FederationTagger();

    private static boolean generateDocs = true;

    private static boolean generateSources = false;

    private static boolean parseComments = false;

    public static String apiVersion = null;

    public static JSilver jSilver = null;

    public static boolean checkLevel(int level) {
        return (showLevel & level) == level;
    }

    /**
   * Returns true if we should parse javadoc comments,
   * reporting errors in the process.
   */
    public static boolean parseComments() {
        return generateDocs || parseComments;
    }

    public static boolean checkLevel(boolean pub, boolean prot, boolean pkgp, boolean priv, boolean hidden) {
        int level = 0;
        if (hidden && !checkLevel(SHOW_HIDDEN)) {
            return false;
        }
        if (pub && checkLevel(SHOW_PUBLIC)) {
            return true;
        }
        if (prot && checkLevel(SHOW_PROTECTED)) {
            return true;
        }
        if (pkgp && checkLevel(SHOW_PACKAGE)) {
            return true;
        }
        if (priv && checkLevel(SHOW_PRIVATE)) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        com.sun.tools.javadoc.Main.execute(args);
    }

    public static boolean start(RootDoc r) {
        String keepListFile = null;
        String proofreadFile = null;
        String todoFile = null;
        String sdkValuePath = null;
        ArrayList<SampleCode> sampleCodes = new ArrayList<SampleCode>();
        String stubsDir = null;
        boolean offlineMode = false;
        String apiFile = null;
        String debugStubsFile = "";
        HashSet<String> stubPackages = null;
        ArrayList<String> knownTagsFiles = new ArrayList<String>();
        root = r;
        String[][] options = r.options();
        for (String[] a : options) {
            if (a[0].equals("-d")) {
                ClearPage.outputDir = a[1];
            } else if (a[0].equals("-templatedir")) {
                ClearPage.addTemplateDir(a[1]);
            } else if (a[0].equals("-hdf")) {
                mHDFData.add(new String[] { a[1], a[2] });
            } else if (a[0].equals("-knowntags")) {
                knownTagsFiles.add(a[1]);
            } else if (a[0].equals("-toroot")) {
                ClearPage.toroot = a[1];
            } else if (a[0].equals("-samplecode")) {
                sampleCodes.add(new SampleCode(a[1], a[2], a[3]));
            } else if (a[0].equals("-htmldir")) {
                ClearPage.htmlDirs.add(a[1]);
            } else if (a[0].equals("-title")) {
                Doclava.title = a[1];
            } else if (a[0].equals("-werror")) {
                Errors.setWarningsAreErrors(true);
            } else if (a[0].equals("-error") || a[0].equals("-warning") || a[0].equals("-hide")) {
                try {
                    int level = -1;
                    if (a[0].equals("-error")) {
                        level = Errors.ERROR;
                    } else if (a[0].equals("-warning")) {
                        level = Errors.WARNING;
                    } else if (a[0].equals("-hide")) {
                        level = Errors.HIDDEN;
                    }
                    Errors.setErrorLevel(Integer.parseInt(a[1]), level);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (a[0].equals("-keeplist")) {
                keepListFile = a[1];
            } else if (a[0].equals("-proofread")) {
                proofreadFile = a[1];
            } else if (a[0].equals("-todo")) {
                todoFile = a[1];
            } else if (a[0].equals("-public")) {
                showLevel = SHOW_PUBLIC;
            } else if (a[0].equals("-protected")) {
                showLevel = SHOW_PROTECTED;
            } else if (a[0].equals("-package")) {
                showLevel = SHOW_PACKAGE;
            } else if (a[0].equals("-private")) {
                showLevel = SHOW_PRIVATE;
            } else if (a[0].equals("-hidden")) {
                showLevel = SHOW_HIDDEN;
            } else if (a[0].equals("-stubs")) {
                stubsDir = a[1];
            } else if (a[0].equals("-stubpackages")) {
                stubPackages = new HashSet<String>();
                for (String pkg : a[1].split(":")) {
                    stubPackages.add(pkg);
                }
            } else if (a[0].equals("-sdkvalues")) {
                sdkValuePath = a[1];
            } else if (a[0].equals("-apixml")) {
                apiFile = a[1];
            } else if (a[0].equals("-nodocs")) {
                generateDocs = false;
            } else if (a[0].equals("-parsecomments")) {
                parseComments = true;
            } else if (a[0].equals("-since")) {
                sinceTagger.addVersion(a[1], a[2]);
            } else if (a[0].equals("-offlinemode")) {
                offlineMode = true;
            } else if (a[0].equals("-federate")) {
                try {
                    String name = a[1];
                    URL federationURL = new URL(a[2]);
                    federationTagger.addSiteUrl(name, federationURL);
                } catch (MalformedURLException e) {
                    System.err.println("Could not parse URL for federation: " + a[1]);
                    return false;
                }
            } else if (a[0].equals("-federationxml")) {
                String name = a[1];
                String file = a[2];
                federationTagger.addSiteXml(name, file);
            } else if (a[0].equals("-apiversion")) {
                apiVersion = a[1];
            } else if (a[0].equals("-assetsdir")) {
                assetsOutputDir = a[1];
            } else if (a[0].equals("-generatesources")) {
                generateSources = true;
            }
        }
        if (!readKnownTagsFiles(knownTags, knownTagsFiles)) {
            return false;
        }
        Converter.makeInfo(r);
        final File currentApiFile;
        if (!generateDocs && apiFile != null) {
            currentApiFile = new File(apiFile);
        } else if (generateDocs) {
            currentApiFile = new File(ensureSlash(ClearPage.outputDir) + javadocDir + FederatedSite.XML_API_PATH);
        } else {
            currentApiFile = null;
        }
        Stubs.writeStubsAndXml(stubsDir, currentApiFile, stubPackages);
        if (generateDocs && apiFile != null) {
            ClearPage.copyFile(currentApiFile, new File(apiFile));
        }
        if (generateDocs) {
            ClearPage.addBundledTemplateDir("assets/customizations");
            ClearPage.addBundledTemplateDir("assets/templates");
            List<ResourceLoader> resourceLoaders = new ArrayList<ResourceLoader>();
            List<String> templates = ClearPage.getTemplateDirs();
            for (String tmpl : templates) {
                resourceLoaders.add(new FileSystemResourceLoader(tmpl));
            }
            templates = ClearPage.getBundledTemplateDirs();
            for (String tmpl : templates) {
                resourceLoaders.add(new ClassResourceLoader(Doclava.class, '/' + tmpl));
            }
            ResourceLoader compositeResourceLoader = new CompositeResourceLoader(resourceLoaders);
            jSilver = new JSilver(compositeResourceLoader);
            if (!Doclava.readTemplateSettings()) {
                return false;
            }
            long startTime = System.nanoTime();
            if (apiVersion != null && sinceTagger.hasVersions()) {
                sinceTagger.addVersion(currentApiFile.getAbsolutePath(), apiVersion);
            }
            sinceTagger.tagAll(Converter.rootClasses());
            federationTagger.tagAll(Converter.rootClasses());
            if (proofreadFile != null) {
                Proofread.initProofread(proofreadFile);
            }
            if (todoFile != null) {
                TodoFile.writeTodoFile(todoFile);
            }
            if (!ClearPage.htmlDirs.isEmpty()) {
                for (String htmlDir : ClearPage.htmlDirs) {
                    File f = new File(htmlDir);
                    writeHTMLPages(f);
                }
            } else {
                JarFile thisJar = JarUtils.jarForClass(Doclava.class, null);
                if (thisJar != null) {
                    try {
                        JarUtils.copyResourcesToDirectory(thisJar, "assets/html", ClearPage.outputDir);
                    } catch (IOException e) {
                        System.err.println("Failed to copy html resources.");
                    }
                }
            }
            writeAssets();
            NavTree.writeNavTree(assetsOutputDir);
            writePackages(javadocDir + "packages" + htmlExtension);
            writeClassLists();
            writeClasses();
            writeHierarchy();
            writeLists();
            if (keepListFile != null) {
                writeKeepList(keepListFile);
            }
            for (SampleCode sc : sampleCodes) {
                sc.write(offlineMode);
            }
            writeIndex();
            Proofread.finishProofread(proofreadFile);
            if (sdkValuePath != null) {
                writeSdkValues(sdkValuePath);
            }
            long time = System.nanoTime() - startTime;
            System.out.println("DroidDoc took " + (time / 1000000000) + " sec. to write docs to " + ClearPage.outputDir);
        }
        Errors.printErrors();
        return !Errors.hadError;
    }

    private static void writeIndex() {
        Data data = makeHDF();
        ClearPage.write(data, "index.cs", javadocDir + "index" + htmlExtension);
    }

    private static boolean readTemplateSettings() {
        Data data = makeHDF();
        htmlExtension = ".html";
        int i = 0;
        while (true) {
            String k = data.getValue("template.escape." + i + ".key", "");
            String v = data.getValue("template.escape." + i + ".value", "");
            if ("".equals(k)) {
                break;
            }
            if (k.length() != 1) {
                System.err.println("template.escape." + i + ".key must have a length of 1: " + k);
                return false;
            }
            escapeChars.put(k.charAt(0), v);
            i++;
        }
        return true;
    }

    private static boolean readKnownTagsFiles(HashSet<String> knownTags, ArrayList<String> knownTagsFiles) {
        for (String fn : knownTagsFiles) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(fn));
                int lineno = 0;
                boolean fail = false;
                while (true) {
                    lineno++;
                    String line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    } else if (line.charAt(0) == '#') {
                        continue;
                    }
                    String[] words = line.split("\\s+", 2);
                    if (words.length == 2) {
                        if (words[1].charAt(0) != '#') {
                            System.err.println(fn + ":" + lineno + ": Only one tag allowed per line: " + line);
                            fail = true;
                            continue;
                        }
                    }
                    knownTags.add(words[0]);
                }
                if (fail) {
                    return false;
                }
            } catch (IOException ex) {
                System.err.println("Error reading file: " + fn + " (" + ex.getMessage() + ")");
                return false;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return true;
    }

    public static String escape(String s) {
        if (escapeChars.size() == 0) {
            return s;
        }
        StringBuffer b = null;
        int begin = 0;
        final int N = s.length();
        for (int i = 0; i < N; i++) {
            char c = s.charAt(i);
            String mapped = escapeChars.get(c);
            if (mapped != null) {
                if (b == null) {
                    b = new StringBuffer(s.length() + mapped.length());
                }
                if (begin != i) {
                    b.append(s.substring(begin, i));
                }
                b.append(mapped);
                begin = i + 1;
            }
        }
        if (b != null) {
            if (begin != N) {
                b.append(s.substring(begin, N));
            }
            return b.toString();
        }
        return s;
    }

    public static void setPageTitle(Data data, String title) {
        String s = title;
        if (Doclava.title.length() > 0) {
            s += " - " + Doclava.title;
        }
        data.setValue("page.title", s);
    }

    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    public static int optionLength(String option) {
        if (option.equals("-d")) {
            return 2;
        }
        if (option.equals("-templatedir")) {
            return 2;
        }
        if (option.equals("-hdf")) {
            return 3;
        }
        if (option.equals("-knowntags")) {
            return 2;
        }
        if (option.equals("-toroot")) {
            return 2;
        }
        if (option.equals("-samplecode")) {
            return 4;
        }
        if (option.equals("-htmldir")) {
            return 2;
        }
        if (option.equals("-title")) {
            return 2;
        }
        if (option.equals("-werror")) {
            return 1;
        }
        if (option.equals("-hide")) {
            return 2;
        }
        if (option.equals("-warning")) {
            return 2;
        }
        if (option.equals("-error")) {
            return 2;
        }
        if (option.equals("-keeplist")) {
            return 2;
        }
        if (option.equals("-proofread")) {
            return 2;
        }
        if (option.equals("-todo")) {
            return 2;
        }
        if (option.equals("-public")) {
            return 1;
        }
        if (option.equals("-protected")) {
            return 1;
        }
        if (option.equals("-package")) {
            return 1;
        }
        if (option.equals("-private")) {
            return 1;
        }
        if (option.equals("-hidden")) {
            return 1;
        }
        if (option.equals("-stubs")) {
            return 2;
        }
        if (option.equals("-stubpackages")) {
            return 2;
        }
        if (option.equals("-sdkvalues")) {
            return 2;
        }
        if (option.equals("-apixml")) {
            return 2;
        }
        if (option.equals("-nodocs")) {
            return 1;
        }
        if (option.equals("-parsecomments")) {
            return 1;
        }
        if (option.equals("-since")) {
            return 3;
        }
        if (option.equals("-offlinemode")) {
            return 1;
        }
        if (option.equals("-federate")) {
            return 3;
        }
        if (option.equals("-federationxml")) {
            return 3;
        }
        if (option.equals("-apiversion")) {
            return 2;
        }
        if (option.equals("-assetsdir")) {
            return 2;
        }
        if (option.equals("-generatesources")) {
            return 1;
        }
        return 0;
    }

    public static boolean validOptions(String[][] options, DocErrorReporter r) {
        for (String[] a : options) {
            if (a[0].equals("-error") || a[0].equals("-warning") || a[0].equals("-hide")) {
                try {
                    Integer.parseInt(a[1]);
                } catch (NumberFormatException e) {
                    r.printError("bad -" + a[0] + " value must be a number: " + a[1]);
                    return false;
                }
            }
        }
        return true;
    }

    public static Data makeHDF() {
        Data data = jSilver.createData();
        for (String[] p : mHDFData) {
            data.setValue(p[0], p[1]);
        }
        return data;
    }

    public static Data makePackageHDF() {
        Data data = makeHDF();
        ClassInfo[] classes = Converter.rootClasses();
        SortedMap<String, PackageInfo> sorted = new TreeMap<String, PackageInfo>();
        for (ClassInfo cl : classes) {
            PackageInfo pkg = cl.containingPackage();
            String name;
            if (pkg == null) {
                name = "";
            } else {
                name = pkg.name();
            }
            sorted.put(name, pkg);
        }
        int i = 0;
        for (String s : sorted.keySet()) {
            PackageInfo pkg = sorted.get(s);
            if (pkg.isHidden()) {
                continue;
            }
            Boolean allHidden = true;
            int pass = 0;
            ClassInfo[] classesToCheck = null;
            while (pass < 5) {
                switch(pass) {
                    case 0:
                        classesToCheck = pkg.ordinaryClasses();
                        break;
                    case 1:
                        classesToCheck = pkg.enums();
                        break;
                    case 2:
                        classesToCheck = pkg.errors();
                        break;
                    case 3:
                        classesToCheck = pkg.exceptions();
                        break;
                    case 4:
                        classesToCheck = pkg.getInterfaces();
                        break;
                    default:
                        System.err.println("Error reading package: " + pkg.name());
                        break;
                }
                for (ClassInfo cl : classesToCheck) {
                    if (!cl.isHidden()) {
                        allHidden = false;
                        break;
                    }
                }
                if (!allHidden) {
                    break;
                }
                pass++;
            }
            if (allHidden) {
                continue;
            }
            data.setValue("reference", "1");
            data.setValue("reference.apilevels", sinceTagger.hasVersions() ? "1" : "0");
            data.setValue("docs.packages." + i + ".name", s);
            data.setValue("docs.packages." + i + ".link", pkg.htmlPage());
            data.setValue("docs.packages." + i + ".since.key", SinceTagger.keyForName(pkg.getSince()));
            data.setValue("docs.packages." + i + ".since.name", pkg.getSince());
            TagInfo.makeHDF(data, "docs.packages." + i + ".shortDescr", pkg.firstSentenceTags());
            i++;
        }
        sinceTagger.writeVersionNames(data);
        return data;
    }

    private static void writeDirectory(File dir, String relative, JSilver js) {
        File[] files = dir.listFiles();
        int i, count = files.length;
        for (i = 0; i < count; i++) {
            File f = files[i];
            if (f.isFile()) {
                String templ = ensureSlash(relative) + f.getName();
                int len = templ.length();
                if (len > 3 && ".cs".equals(templ.substring(len - 3))) {
                    Data data = makeHDF();
                    String filename = templ.substring(0, len - 3) + htmlExtension;
                    ClearPage.write(data, templ, filename, js);
                } else if (len > 3 && ".jd".equals(templ.substring(len - 3))) {
                    String filename = templ.substring(0, len - 3) + htmlExtension;
                    DocFile.writePage(f.getAbsolutePath(), relative, filename);
                } else {
                    ClearPage.copyFile(f, new File(ensureSlash(ClearPage.outputDir) + templ));
                }
            } else if (f.isDirectory()) {
                writeDirectory(f, ensureSlash(relative) + f.getName() + "/", js);
            }
        }
    }

    public static void writeHTMLPages(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + dir);
        }
        ResourceLoader loader = new FileSystemResourceLoader(dir);
        JSilver js = new JSilver(loader);
        writeDirectory(dir, "", js);
    }

    public static void writeAssets() {
        JarFile thisJar = JarUtils.jarForClass(Doclava.class, null);
        if (thisJar != null) {
            try {
                List<String> templateDirs = ClearPage.getBundledTemplateDirs();
                for (String templateDir : templateDirs) {
                    String assetsDir = ensureSlash(templateDir) + "assets";
                    JarUtils.copyResourcesToDirectory(thisJar, assetsDir, ensureSlash(ClearPage.outputDir) + assetsOutputDir);
                }
            } catch (IOException e) {
                System.err.println("Error copying assets directory.");
                e.printStackTrace();
                return;
            }
        }
        List<String> templateDirs = ClearPage.getTemplateDirs();
        for (String templateDir : templateDirs) {
            File assets = new File(ensureSlash(templateDir) + "assets");
            if (assets.isDirectory()) {
                writeDirectory(assets, assetsOutputDir, null);
            }
        }
    }

    public static void writeLists() {
        Data data = makeHDF();
        ClassInfo[] classes = Converter.rootClasses();
        SortedMap<String, Object> sorted = new TreeMap<String, Object>();
        for (ClassInfo cl : classes) {
            if (cl.isHidden()) {
                continue;
            }
            sorted.put(cl.qualifiedName(), cl);
            PackageInfo pkg = cl.containingPackage();
            String name;
            if (pkg == null) {
                name = "";
            } else {
                name = pkg.name();
            }
            sorted.put(name, pkg);
        }
        int i = 0;
        for (String s : sorted.keySet()) {
            data.setValue("docs.pages." + i + ".id", "" + i);
            data.setValue("docs.pages." + i + ".label", s);
            Object o = sorted.get(s);
            if (o instanceof PackageInfo) {
                PackageInfo pkg = (PackageInfo) o;
                data.setValue("docs.pages." + i + ".link", pkg.htmlPage());
                data.setValue("docs.pages." + i + ".type", "package");
            } else if (o instanceof ClassInfo) {
                ClassInfo cl = (ClassInfo) o;
                data.setValue("docs.pages." + i + ".link", cl.htmlPage());
                data.setValue("docs.pages." + i + ".type", "class");
            }
            i++;
        }
        ClearPage.write(data, "lists.cs", javadocDir + "lists.js");
    }

    public static void cantStripThis(ClassInfo cl, HashSet<ClassInfo> notStrippable) {
        if (!notStrippable.add(cl)) {
            return;
        }
        ClassInfo supr = cl.superclass();
        if (supr != null) {
            cantStripThis(supr, notStrippable);
        }
        for (ClassInfo iface : cl.getInterfaces()) {
            cantStripThis(iface, notStrippable);
        }
    }

    private static String getPrintableName(ClassInfo cl) {
        ClassInfo containingClass = cl.containingClass();
        if (containingClass != null) {
            String baseName = cl.name();
            baseName = baseName.substring(baseName.lastIndexOf('.') + 1);
            return getPrintableName(containingClass) + '$' + baseName;
        }
        return cl.qualifiedName();
    }

    /**
   * Writes the list of classes that must be present in order to provide the non-hidden APIs known
   * to javadoc.
   *
   * @param filename the path to the file to write the list to
   */
    public static void writeKeepList(String filename) {
        HashSet<ClassInfo> notStrippable = new HashSet<ClassInfo>();
        ClassInfo[] all = Converter.allClasses();
        Arrays.sort(all);
        for (ClassInfo cl : all) {
            if (cl.isPublic() && !cl.isHidden()) {
                cantStripThis(cl, notStrippable);
            }
        }
        PrintStream stream = null;
        try {
            stream = new PrintStream(new BufferedOutputStream(new FileOutputStream(filename)));
            for (ClassInfo cl : notStrippable) {
                stream.println(getPrintableName(cl));
            }
        } catch (FileNotFoundException e) {
            System.err.println("error writing file: " + filename);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static PackageInfo[] sVisiblePackages = null;

    public static PackageInfo[] choosePackages() {
        if (sVisiblePackages != null) {
            return sVisiblePackages;
        }
        ClassInfo[] classes = Converter.rootClasses();
        SortedMap<String, PackageInfo> sorted = new TreeMap<String, PackageInfo>();
        for (ClassInfo cl : classes) {
            PackageInfo pkg = cl.containingPackage();
            String name;
            if (pkg == null) {
                name = "";
            } else {
                name = pkg.name();
            }
            sorted.put(name, pkg);
        }
        ArrayList<PackageInfo> result = new ArrayList<PackageInfo>();
        for (String s : sorted.keySet()) {
            PackageInfo pkg = sorted.get(s);
            if (pkg.isHidden()) {
                continue;
            }
            Boolean allHidden = true;
            int pass = 0;
            ClassInfo[] classesToCheck = null;
            while (pass < 5) {
                switch(pass) {
                    case 0:
                        classesToCheck = pkg.ordinaryClasses();
                        break;
                    case 1:
                        classesToCheck = pkg.enums();
                        break;
                    case 2:
                        classesToCheck = pkg.errors();
                        break;
                    case 3:
                        classesToCheck = pkg.exceptions();
                        break;
                    case 4:
                        classesToCheck = pkg.getInterfaces();
                        break;
                    default:
                        System.err.println("Error reading package: " + pkg.name());
                        break;
                }
                for (ClassInfo cl : classesToCheck) {
                    if (!cl.isHidden()) {
                        allHidden = false;
                        break;
                    }
                }
                if (!allHidden) {
                    break;
                }
                pass++;
            }
            if (allHidden) {
                continue;
            }
            result.add(pkg);
        }
        sVisiblePackages = result.toArray(new PackageInfo[result.size()]);
        return sVisiblePackages;
    }

    public static void writePackages(String filename) {
        Data data = makePackageHDF();
        int i = 0;
        for (PackageInfo pkg : choosePackages()) {
            writePackage(pkg);
            data.setValue("docs.packages." + i + ".name", pkg.name());
            data.setValue("docs.packages." + i + ".link", pkg.htmlPage());
            TagInfo.makeHDF(data, "docs.packages." + i + ".shortDescr", pkg.firstSentenceTags());
            i++;
        }
        setPageTitle(data, "Package Index");
        TagInfo.makeHDF(data, "root.descr", Converter.convertTags(root.inlineTags(), null));
        ClearPage.write(data, "packages.cs", filename);
        ClearPage.write(data, "package-list.cs", javadocDir + "package-list");
        Proofread.writePackages(filename, Converter.convertTags(root.inlineTags(), null));
    }

    public static void writePackage(PackageInfo pkg) {
        Data data = makePackageHDF();
        String name = pkg.name();
        data.setValue("package.name", name);
        data.setValue("package.since.key", SinceTagger.keyForName(pkg.getSince()));
        data.setValue("package.since.name", pkg.getSince());
        data.setValue("package.descr", "...description...");
        pkg.setFederatedReferences(data, "package");
        makeClassListHDF(data, "package.annotations", ClassInfo.sortByName(pkg.getAnnotations()));
        makeClassListHDF(data, "package.interfaces", ClassInfo.sortByName(pkg.getInterfaces()));
        makeClassListHDF(data, "package.classes", ClassInfo.sortByName(pkg.ordinaryClasses()));
        makeClassListHDF(data, "package.enums", ClassInfo.sortByName(pkg.enums()));
        makeClassListHDF(data, "package.exceptions", ClassInfo.sortByName(pkg.exceptions()));
        makeClassListHDF(data, "package.errors", ClassInfo.sortByName(pkg.errors()));
        TagInfo[] shortDescrTags = pkg.firstSentenceTags();
        TagInfo[] longDescrTags = pkg.inlineTags();
        TagInfo.makeHDF(data, "package.shortDescr", shortDescrTags);
        TagInfo.makeHDF(data, "package.descr", longDescrTags);
        data.setValue("package.hasLongDescr", TagInfo.tagsEqual(shortDescrTags, longDescrTags) ? "0" : "1");
        String filename = Doclava.javadocDir + pkg.relativePath();
        setPageTitle(data, name);
        ClearPage.write(data, "package.cs", filename);
        filename = javadocDir + pkg.fullDescriptionFile();
        setPageTitle(data, name + " Details");
        ClearPage.write(data, "package-descr.cs", filename);
        Proofread.writePackage(filename, pkg.inlineTags());
    }

    public static void writeClassLists() {
        int i;
        Data data = makePackageHDF();
        ClassInfo[] classes = PackageInfo.filterHidden(Converter.convertClasses(root.classes()));
        if (classes.length == 0) {
            return;
        }
        Sorter[] sorted = new Sorter[classes.length];
        for (i = 0; i < sorted.length; i++) {
            ClassInfo cl = classes[i];
            String name = cl.name();
            sorted[i] = new Sorter(name, cl);
        }
        Arrays.sort(sorted);
        int firstMatch = 0;
        String lastName = sorted[0].label;
        for (i = 1; i < sorted.length; i++) {
            String s = sorted[i].label;
            if (!lastName.equals(s)) {
                if (firstMatch != i - 1) {
                    for (int j = firstMatch; j < i; j++) {
                        PackageInfo pkg = ((ClassInfo) sorted[j].data).containingPackage();
                        if (pkg != null) {
                            sorted[j].label = sorted[j].label + " (" + pkg.name() + ")";
                        }
                    }
                }
                firstMatch = i;
                lastName = s;
            }
        }
        Arrays.sort(sorted);
        for (i = 0; i < sorted.length; i++) {
            String s = sorted[i].label;
            ClassInfo cl = (ClassInfo) sorted[i].data;
            char first = Character.toUpperCase(s.charAt(0));
            cl.makeShortDescrHDF(data, "docs.classes." + first + '.' + i);
        }
        setPageTitle(data, "Class Index");
        ClearPage.write(data, "classes.cs", javadocDir + "classes" + htmlExtension);
    }

    public static void writeHierarchy() {
        ClassInfo[] classes = Converter.rootClasses();
        ArrayList<ClassInfo> info = new ArrayList<ClassInfo>();
        for (ClassInfo cl : classes) {
            if (!cl.isHidden()) {
                info.add(cl);
            }
        }
        Data data = makePackageHDF();
        Hierarchy.makeHierarchy(data, info.toArray(new ClassInfo[info.size()]));
        setPageTitle(data, "Class Hierarchy");
        ClearPage.write(data, "hierarchy.cs", javadocDir + "hierarchy" + htmlExtension);
    }

    public static void writeClasses() {
        ClassInfo[] classes = Converter.rootClasses();
        if (generateSources) {
            mHDFData.add(new String[] { "doclava.generate.sources", "true" });
            for (ClassInfo cl : classes) {
                Data data = makePackageHDF();
                if (!cl.isHidden()) {
                    writeSource(cl, data);
                    writeClass(cl, data);
                }
            }
        } else {
            for (ClassInfo cl : classes) {
                Data data = makePackageHDF();
                if (!cl.isHidden()) {
                    writeClass(cl, data);
                }
            }
        }
    }

    public static void writeClass(ClassInfo cl, Data data) {
        cl.makeHDF(data);
        setPageTitle(data, cl.name());
        ClearPage.write(data, "class.cs", Doclava.javadocDir + cl.relativePath());
        Proofread.writeClass(cl.htmlPage(), cl);
    }

    public static void writeSource(ClassInfo cl, Data data) {
        try {
            cl.makeHDF(data);
            data.setValue("class.source", SampleTagInfo.escapeHtml(cl.getSource()));
            setPageTitle(data, cl.name());
            ClearPage.write(data, "source.cs", Doclava.javadocDir + cl.relativePath("-source"));
        } catch (IOException e) {
            Errors.error(Errors.IO_ERROR, null, "Could not find source file for " + cl.name());
        }
    }

    public static void makeClassListHDF(Data data, String base, ClassInfo[] classes) {
        for (int i = 0; i < classes.length; i++) {
            ClassInfo cl = classes[i];
            if (!cl.isHidden()) {
                cl.makeShortDescrHDF(data, base + "." + i);
            }
        }
    }

    public static String linkTarget(String source, String target) {
        String[] src = source.split("/");
        String[] tgt = target.split("/");
        int srclen = src.length;
        int tgtlen = tgt.length;
        int same = 0;
        while (same < (srclen - 1) && same < (tgtlen - 1) && (src[same].equals(tgt[same]))) {
            same++;
        }
        String s = "";
        int up = srclen - same - 1;
        for (int i = 0; i < up; i++) {
            s += "../";
        }
        int N = tgtlen - 1;
        for (int i = same; i < N; i++) {
            s += tgt[i] + '/';
        }
        s += tgt[tgtlen - 1];
        return s;
    }

    /**
   * Returns true if the given element has an @hide or @pending annotation.
   */
    private static boolean hasHideAnnotation(Doc doc) {
        String comment = doc.getRawCommentText();
        return comment.indexOf("@hide") != -1 || comment.indexOf("@pending") != -1;
    }

    /**
   * Returns true if the given element is hidden.
   */
    private static boolean isHidden(Doc doc) {
        if (doc instanceof MemberDoc) {
            return hasHideAnnotation(doc);
        }
        if (doc instanceof ClassDoc) {
            ClassDoc classDoc = (ClassDoc) doc;
            if (hasHideAnnotation(classDoc.containingPackage())) {
                return true;
            }
            ClassDoc current = classDoc;
            do {
                if (hasHideAnnotation(current)) {
                    return true;
                }
                current = current.containingClass();
            } while (current != null);
        }
        return false;
    }

    /**
   * Filters out hidden elements.
   */
    private static Object filterHidden(Object o, Class<?> expected) {
        if (o == null) {
            return null;
        }
        Class<?> type = o.getClass();
        if (type.getName().startsWith("com.sun.")) {
            return Proxy.newProxyInstance(type.getClassLoader(), type.getInterfaces(), new HideHandler(o));
        } else if (o instanceof Object[]) {
            Class<?> componentType = expected.getComponentType();
            Object[] array = (Object[]) o;
            List<Object> list = new ArrayList<Object>(array.length);
            for (Object entry : array) {
                if ((entry instanceof Doc) && isHidden((Doc) entry)) {
                    continue;
                }
                list.add(filterHidden(entry, componentType));
            }
            return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
        } else {
            return o;
        }
    }

    /**
   * Filters hidden elements out of method return values.
   */
    private static class HideHandler implements InvocationHandler {

        private final Object target;

        public HideHandler(Object target) {
            this.target = target;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (args != null) {
                if (methodName.equals("compareTo") || methodName.equals("equals") || methodName.equals("overrides") || methodName.equals("subclassOf")) {
                    args[0] = unwrap(args[0]);
                }
            }
            if (methodName.equals("getRawCommentText")) {
                return filterComment((String) method.invoke(target, args));
            }
            if (proxy instanceof Type && methodName.equals("toString")) {
                return ((String) method.invoke(target, args)).replace("&", "&amp;");
            }
            try {
                return filterHidden(method.invoke(target, args), method.getReturnType());
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private String filterComment(String s) {
            if (s == null) {
                return null;
            }
            s = s.trim();
            while (s.length() >= 5 && s.charAt(s.length() - 5) == '{') {
                s += "&nbsp;";
            }
            return s;
        }

        private static Object unwrap(Object proxy) {
            if (proxy instanceof Proxy) return ((HideHandler) Proxy.getInvocationHandler(proxy)).target;
            return proxy;
        }
    }

    /**
   * Collect the values used by the Dev tools and write them in files packaged with the SDK
   *
   * @param output the ouput directory for the files.
   */
    private static void writeSdkValues(String output) {
        ArrayList<String> activityActions = new ArrayList<String>();
        ArrayList<String> broadcastActions = new ArrayList<String>();
        ArrayList<String> serviceActions = new ArrayList<String>();
        ArrayList<String> categories = new ArrayList<String>();
        ArrayList<String> features = new ArrayList<String>();
        ArrayList<ClassInfo> layouts = new ArrayList<ClassInfo>();
        ArrayList<ClassInfo> widgets = new ArrayList<ClassInfo>();
        ArrayList<ClassInfo> layoutParams = new ArrayList<ClassInfo>();
        ClassInfo[] classes = Converter.allClasses();
        for (ClassInfo clazz : classes) {
            FieldInfo[] fields = clazz.allSelfFields();
            for (FieldInfo field : fields) {
                Object cValue = field.constantValue();
                if (cValue != null) {
                    AnnotationInstanceInfo[] annotations = field.annotations();
                    if (annotations.length > 0) {
                        for (AnnotationInstanceInfo annotation : annotations) {
                            if (SDK_CONSTANT_ANNOTATION.equals(annotation.type().qualifiedName())) {
                                AnnotationValueInfo[] values = annotation.elementValues();
                                if (values.length > 0) {
                                    String type = values[0].valueString();
                                    if (SDK_CONSTANT_TYPE_ACTIVITY_ACTION.equals(type)) {
                                        activityActions.add(cValue.toString());
                                    } else if (SDK_CONSTANT_TYPE_BROADCAST_ACTION.equals(type)) {
                                        broadcastActions.add(cValue.toString());
                                    } else if (SDK_CONSTANT_TYPE_SERVICE_ACTION.equals(type)) {
                                        serviceActions.add(cValue.toString());
                                    } else if (SDK_CONSTANT_TYPE_CATEGORY.equals(type)) {
                                        categories.add(cValue.toString());
                                    } else if (SDK_CONSTANT_TYPE_FEATURE.equals(type)) {
                                        features.add(cValue.toString());
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
            if (clazz.isHidden() == false && clazz.isPublic() && clazz.isAbstract() == false) {
                boolean annotated = false;
                AnnotationInstanceInfo[] annotations = clazz.annotations();
                if (annotations.length > 0) {
                    for (AnnotationInstanceInfo annotation : annotations) {
                        if (SDK_WIDGET_ANNOTATION.equals(annotation.type().qualifiedName())) {
                            widgets.add(clazz);
                            annotated = true;
                            break;
                        } else if (SDK_LAYOUT_ANNOTATION.equals(annotation.type().qualifiedName())) {
                            layouts.add(clazz);
                            annotated = true;
                            break;
                        }
                    }
                }
                if (annotated == false) {
                    PackageInfo pckg = clazz.containingPackage();
                    String packageName = pckg.name();
                    if ("android.widget".equals(packageName) || "android.view".equals(packageName)) {
                        int type = checkInheritance(clazz);
                        switch(type) {
                            case TYPE_WIDGET:
                                widgets.add(clazz);
                                break;
                            case TYPE_LAYOUT:
                                layouts.add(clazz);
                                break;
                            case TYPE_LAYOUT_PARAM:
                                layoutParams.add(clazz);
                                break;
                        }
                    }
                }
            }
        }
        Collections.sort(activityActions);
        writeValues(output + "/activity_actions.txt", activityActions);
        Collections.sort(broadcastActions);
        writeValues(output + "/broadcast_actions.txt", broadcastActions);
        Collections.sort(serviceActions);
        writeValues(output + "/service_actions.txt", serviceActions);
        Collections.sort(categories);
        writeValues(output + "/categories.txt", categories);
        Collections.sort(features);
        writeValues(output + "/features.txt", features);
        for (int i = 0; i < layoutParams.size(); ) {
            ClassInfo layoutParamClass = layoutParams.get(i);
            ClassInfo containingClass = layoutParamClass.containingClass();
            if (containingClass == null || layouts.indexOf(containingClass) == -1) {
                layoutParams.remove(i);
            } else {
                i++;
            }
        }
        writeClasses(output + "/widgets.txt", widgets, layouts, layoutParams);
    }

    /**
   * Writes a list of values into a text files.
   *
   * @param pathname the absolute os path of the output file.
   * @param values the list of values to write.
   */
    private static void writeValues(String pathname, ArrayList<String> values) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(pathname, false);
            bw = new BufferedWriter(fw);
            for (String value : values) {
                bw.append(value).append('\n');
            }
        } catch (IOException e) {
        } finally {
            try {
                if (bw != null) bw.close();
            } catch (IOException e) {
            }
            try {
                if (fw != null) fw.close();
            } catch (IOException e) {
            }
        }
    }

    /**
   * Writes the widget/layout/layout param classes into a text files.
   *
   * @param pathname the absolute os path of the output file.
   * @param widgets the list of widget classes to write.
   * @param layouts the list of layout classes to write.
   * @param layoutParams the list of layout param classes to write.
   */
    private static void writeClasses(String pathname, ArrayList<ClassInfo> widgets, ArrayList<ClassInfo> layouts, ArrayList<ClassInfo> layoutParams) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(pathname, false);
            bw = new BufferedWriter(fw);
            for (ClassInfo clazz : widgets) {
                writeClass(bw, clazz, 'W');
            }
            for (ClassInfo clazz : layoutParams) {
                writeClass(bw, clazz, 'P');
            }
            for (ClassInfo clazz : layouts) {
                writeClass(bw, clazz, 'L');
            }
        } catch (IOException e) {
        } finally {
            try {
                if (bw != null) bw.close();
            } catch (IOException e) {
            }
            try {
                if (fw != null) fw.close();
            } catch (IOException e) {
            }
        }
    }

    /**
   * Writes a class name and its super class names into a {@link BufferedWriter}.
   *
   * @param writer the BufferedWriter to write into
   * @param clazz the class to write
   * @param prefix the prefix to put at the beginning of the line.
   * @throws IOException
   */
    private static void writeClass(BufferedWriter writer, ClassInfo clazz, char prefix) throws IOException {
        writer.append(prefix).append(clazz.qualifiedName());
        ClassInfo superClass = clazz;
        while ((superClass = superClass.superclass()) != null) {
            writer.append(' ').append(superClass.qualifiedName());
        }
        writer.append('\n');
    }

    /**
   * Checks the inheritance of {@link ClassInfo} objects. This method return
   * <ul>
   * <li>{@link #TYPE_LAYOUT}: if the class extends <code>android.view.ViewGroup</code></li>
   * <li>{@link #TYPE_WIDGET}: if the class extends <code>android.view.View</code></li>
   * <li>{@link #TYPE_LAYOUT_PARAM}: if the class extends
   * <code>android.view.ViewGroup$LayoutParams</code></li>
   * <li>{@link #TYPE_NONE}: in all other cases</li>
   * </ul>
   *
   * @param clazz the {@link ClassInfo} to check.
   */
    private static int checkInheritance(ClassInfo clazz) {
        if ("android.view.ViewGroup".equals(clazz.qualifiedName())) {
            return TYPE_LAYOUT;
        } else if ("android.view.View".equals(clazz.qualifiedName())) {
            return TYPE_WIDGET;
        } else if ("android.view.ViewGroup.LayoutParams".equals(clazz.qualifiedName())) {
            return TYPE_LAYOUT_PARAM;
        }
        ClassInfo parent = clazz.superclass();
        if (parent != null) {
            return checkInheritance(parent);
        }
        return TYPE_NONE;
    }

    /**
   * Ensures a trailing '/' at the end of a string.
   */
    static String ensureSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
